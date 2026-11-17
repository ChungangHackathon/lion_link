package com.lionlink.backend.domain.coffeechat.entity;

import java.util.List;

/** 요일별 가능 시간대. coffee_settings.available_slots JSON 컬럼의 원소이고 API 응답 모양 그대로다. */
public record AvailableSlot(SlotDay day, List<SlotTime> times) {
}
