package com.lionlink.backend.domain.admin.event;

/** 가입 승인(ADMIN-03·05)이 반영됐을 때 발행한다. */
public record SignupApprovedEvent(Long userId, String name) {
}
