package com.lionlink.backend.domain.profile.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 프로필 링크 주소: 500자 이하이고 http:// 또는 https://로 시작. null은 통과하니 필수면 @NotNull을 같이 쓴다. */
@Documented
@Size(max = 500, message = "링크는 500자 이하로 입력해 주세요.")
@Pattern(regexp = "^https?://\\S+$", message = "http:// 또는 https://로 시작하는 주소를 입력해 주세요.")
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpUrl {

	String message() default "올바른 링크를 입력해 주세요.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
