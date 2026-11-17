package com.lionlink.backend.domain.profile.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link PeriodRequest}에 붙이는 기간 검사.
 * 시작은 이번 달까지, 종료는 시작보다 빠를 수 없음, 진행 중(isCurrent)이면 종료 금지, 종료만 있고 시작이 없으면 안 됨.
 * 위반은 startYm·endYm 필드 오류로 내려간다.
 */
@Documented
@Constraint(validatedBy = ValidPeriodValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPeriod {

	String message() default "기간을 올바르게 입력해 주세요.";

	/** true면 종료 년월이나 진행 중 표시 중 하나는 반드시 있어야 한다(경력). */
	boolean requireEndOrCurrent() default false;

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
