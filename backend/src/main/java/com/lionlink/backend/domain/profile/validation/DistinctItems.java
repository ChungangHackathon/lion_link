package com.lionlink.backend.domain.profile.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 목록 안에 같은 값이 두 번 들어 있으면 안 된다. null 목록은 통과한다. */
@Documented
@Constraint(validatedBy = DistinctItemsValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DistinctItems {

	String message() default "중복된 항목이 있어요.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
