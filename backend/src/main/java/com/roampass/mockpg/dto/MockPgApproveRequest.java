package com.roampass.mockpg.dto;

public record MockPgApproveRequest(String merchantRequestId, String orderNo, int amount) {
}
