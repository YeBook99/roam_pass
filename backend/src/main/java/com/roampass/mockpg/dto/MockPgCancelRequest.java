package com.roampass.mockpg.dto;

public record MockPgCancelRequest(String pgTid, String requestKey, int amount) {
}
