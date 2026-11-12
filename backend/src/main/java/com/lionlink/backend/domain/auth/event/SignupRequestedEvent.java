package com.lionlink.backend.domain.auth.event;

/** 가입 신청(AUTH-02) 또는 재신청(AUTH-07)이 들어왔을 때 발행한다. */
public record SignupRequestedEvent(Long userId, String name, Integer generation) {
}
