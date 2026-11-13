package com.lionlink.backend.global.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 승인 대기(PENDING) 회원도 호출할 수 있는 API에 붙인다.
 * 반려(REJECTED) 회원은 그대로 403 NOT_APPROVED로 막힌다. 어노테이션이 없으면 승인 회원만 통과한다.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowPendingMember {
}
