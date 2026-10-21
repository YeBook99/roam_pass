package com.roampass.global.web;

import java.time.Instant;
import java.util.List;

// 목록 응답 형식. Spring의 Page를 그대로 내보내지 않는다 (JSON 모양이 버전마다 바뀔 수 있다, ARCHITECTURE.md §8-7)
// fetchedAt = 이 목록을 읽은 시각. 화면이 "N초 전 기준"을 보여 줄 때 쓴다
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, Instant fetchedAt) {
}
