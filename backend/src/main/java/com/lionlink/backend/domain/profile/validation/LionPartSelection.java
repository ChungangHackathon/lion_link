package com.lionlink.backend.domain.profile.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 멋사 파트 선택: 정확히 1개. 원소 null 검사는 쓰는 쪽에서 {@code List<@NotNull LionPart>}로 한다. */
@Documented
@NotNull(message = "멋사 파트를 1개 골라 주세요.")
@Size(min = 1, max = 1, message = "멋사 파트를 1개 골라 주세요.")
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface LionPartSelection {

	String message() default "멋사 파트를 올바르게 선택해 주세요.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
