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

/** 멋사 파트 선택: 1~2개, 중복 불가. 원소 null 검사는 쓰는 쪽에서 {@code List<@NotNull LionPart>}로 한다. */
@Documented
@NotEmpty(message = "멋사 파트를 선택해 주세요.")
@Size(max = 2, message = "멋사 파트는 최대 2개까지 선택할 수 있어요.")
@DistinctItems(message = "같은 멋사 파트를 두 번 선택할 수 없어요.")
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface LionPartSelection {

	String message() default "멋사 파트를 올바르게 선택해 주세요.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
