package com.lionlink.backend.domain.profile.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 관심 분야 선택: 1~3개, 중복 불가. 원소 null 검사는 쓰는 쪽에서 {@code List<@NotNull Interest>}로 한다. */
@Documented
@NotEmpty(message = "관심 분야를 선택해 주세요.")
@Size(max = 3, message = "관심 분야는 최대 3개까지 선택할 수 있어요.")
@DistinctItems(message = "같은 관심 분야를 두 번 선택할 수 없어요.")
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface InterestSelection {

	String message() default "관심 분야를 올바르게 선택해 주세요.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
