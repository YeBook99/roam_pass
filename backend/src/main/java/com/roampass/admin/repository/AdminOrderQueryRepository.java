package com.roampass.admin.repository;

import com.roampass.admin.dto.AdminOrderDetailResponse;
import com.roampass.admin.dto.AdminOrderFilter;
import com.roampass.admin.dto.AdminOrderSummaryResponse;
import com.roampass.admin.dto.TimelineItem;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

// 어드민 화면용 조회 전용 SQL. 주문·결제·프로필·이력 테이블을 묶어 읽기만 한다 (상태는 바꾸지 않는다)
// 여러 테이블을 JOIN하는 읽기라 JPA 엔티티 대신 SQL을 그대로 쓴다 (ADR-16)
@Repository
@RequiredArgsConstructor
public class AdminOrderQueryRepository {

    // 주문마다 결제 중 가장 최근 1건만 붙인다 (실패 후 다시 결제하면 결제가 여러 건일 수 있다)
    private static final String FROM = """
            FROM sim_order o
            LEFT JOIN LATERAL (
                SELECT status, pg_tid, approved_at FROM payment WHERE order_id = o.id ORDER BY id DESC LIMIT 1
            ) p ON true
            LEFT JOIN sim_profile sp ON sp.order_id = o.id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    // 조건은 값이 있을 때만 붙인다. 값은 항상 :파라미터로 넘겨서 SQL 문자열에 직접 넣지 않는다
    public List<AdminOrderSummaryResponse> search(AdminOrderFilter filter, int page, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = where(filter, params);
        params.addValue("limit", size);
        params.addValue("offset", (long) page * size);
        String sql = """
                SELECT o.order_no, o.user_id, o.sim_type, o.amount, o.status AS order_status,
                       p.status AS pay_status, sp.install_status, o.created_at, o.paid_at
                """ + FROM + where + " ORDER BY o.created_at DESC, o.id DESC LIMIT :limit OFFSET :offset";
        return jdbc.query(sql, params, (rs, rowNum) -> new AdminOrderSummaryResponse(
                rs.getString("order_no"), rs.getString("user_id"), rs.getString("sim_type"), rs.getInt("amount"),
                rs.getString("order_status"), rs.getString("pay_status"), rs.getString("install_status"),
                instant(rs, "created_at"), instant(rs, "paid_at")));
    }

    public long count(AdminOrderFilter filter) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = "SELECT COUNT(*) " + FROM + where(filter, params);
        Long count = jdbc.queryForObject(sql, params, Long.class);
        return count == null ? 0 : count;
    }

    public Optional<AdminOrderDetailResponse.Order> findOrder(String orderNo) {
        String sql = """
                SELECT o.order_no, o.user_id, o.sim_type, o.amount, o.travel_start_date, o.status AS order_status,
                       o.created_at, o.paid_at, o.supplier_order_no,
                       p.status AS pay_status, p.pg_tid, p.approved_at,
                       sp.iccid, sp.install_status, sp.qr_viewed_at, sp.installed_at, sp.activated_at, sp.expires_at
                """ + FROM + " WHERE o.order_no = :orderNo";
        List<AdminOrderDetailResponse.Order> rows = jdbc.query(sql, new MapSqlParameterSource("orderNo", orderNo),
                (rs, rowNum) -> new AdminOrderDetailResponse.Order(
                        rs.getString("order_no"), rs.getString("user_id"), rs.getString("sim_type"),
                        rs.getInt("amount"), rs.getDate("travel_start_date").toLocalDate(),
                        rs.getString("order_status"), instant(rs, "created_at"), instant(rs, "paid_at"),
                        rs.getString("supplier_order_no"),
                        rs.getString("pay_status"), rs.getString("pg_tid"), instant(rs, "approved_at"),
                        rs.getString("iccid"), rs.getString("install_status"), instant(rs, "qr_viewed_at"),
                        instant(rs, "installed_at"), instant(rs, "activated_at"), instant(rs, "expires_at")));
        return rows.stream().findFirst();
    }

    // 이 주문의 이력 + 이 주문에 딸린 결제들의 이력 + 프로필 이력을 기록한 순서대로 한 줄로 모은다
    public List<TimelineItem> findTimeline(String orderNo) {
        String sql = """
                SELECT h.target_type, h.from_status, h.to_status, h.actor, h.signal, h.reason,
                       h.occurred_at, h.created_at
                FROM status_history h
                JOIN sim_order o ON o.order_no = :orderNo
                WHERE (h.target_type = 'ORDER' AND h.target_id = o.id)
                   OR (h.target_type = 'PAYMENT' AND h.target_id IN (SELECT id FROM payment WHERE order_id = o.id))
                   OR (h.target_type = 'PROFILE' AND h.target_id IN (SELECT id FROM sim_profile WHERE order_id = o.id))
                ORDER BY h.created_at, h.id
                """;
        return jdbc.query(sql, new MapSqlParameterSource("orderNo", orderNo), (rs, rowNum) -> new TimelineItem(
                rs.getString("target_type"), rs.getString("from_status"), rs.getString("to_status"),
                rs.getString("actor"), rs.getString("signal"), rs.getString("reason"),
                instant(rs, "occurred_at"), instant(rs, "created_at")));
    }

    private String where(AdminOrderFilter filter, MapSqlParameterSource params) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (filter.orderStatus() != null) {
            where.append(" AND o.status = :orderStatus");
            params.addValue("orderStatus", filter.orderStatus());
        }
        if (filter.payStatus() != null) {
            where.append(" AND p.status = :payStatus");
            params.addValue("payStatus", filter.payStatus());
        }
        if (filter.installStatus() != null) {
            where.append(" AND sp.install_status = :installStatus");
            params.addValue("installStatus", filter.installStatus());
        }
        if (filter.simType() != null) {
            where.append(" AND o.sim_type = :simType");
            params.addValue("simType", filter.simType());
        }
        if (filter.createdFrom() != null) {
            where.append(" AND o.created_at >= :createdFrom");
            params.addValue("createdFrom", Timestamp.from(filter.createdFrom()));
        }
        if (filter.createdBefore() != null) {
            where.append(" AND o.created_at < :createdBefore");
            params.addValue("createdBefore", Timestamp.from(filter.createdBefore()));
        }
        return where.toString();
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
