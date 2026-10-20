package com.roampass.mockpg.dto;

// PUT /mock/pg/config 본문. null인 값은 바꾸지 않는다
public record MockPgSettingsPatch(Boolean failApprove, Long responseDelayMs, Integer webhookCount,
                                  Long webhookDelayMs, Integer webhookAmountDelta, Double cancelFailRate) {
}
