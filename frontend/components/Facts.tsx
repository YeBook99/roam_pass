// 이름–값 목록 한 묶음 (주문·결제·SIM 정보). 값이 null이면 회색 대시
// 화면 조각이라 page 파일이 아니라 여기 둔다 (app/에는 조립만, 프론트 설계 기준 §2)
export function Facts({ title, rows }: { title: string; rows: [string, React.ReactNode][] }) {
  return (
    <div className="kv-group">
      <h3>{title}</h3>
      <dl className="kv">
        {rows.map(([label, value]) => (
          <div key={label} className="kv-row">
            <dt>{label}</dt>
            <dd>{value ?? <span className="muted">–</span>}</dd>
          </div>
        ))}
      </dl>
    </div>
  );
}
