package com.lionlink.backend.domain.profile.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 공개 범위에 CONNECTED(연결된 동문에게만)를 쓸 수 없는 항목(회사명)에 붙인다. null은 통과한다. */
@Documented
@Constraint(validatedBy = NoConnectedValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoConnected {

	String message() default "이 항목은 전체 공개 또는 비공개만 고를 수 있어요.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
