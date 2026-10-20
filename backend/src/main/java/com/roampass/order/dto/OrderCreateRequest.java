package com.roampass.order.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

// travelStartDate는 KST 기준 날짜(YYYY-MM-DD)
public record OrderCreateRequest(@NotNull Long planId, @NotNull LocalDate travelStartDate) {
}
