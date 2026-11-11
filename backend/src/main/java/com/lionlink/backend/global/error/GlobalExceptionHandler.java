package com.lionlink.backend.global.error;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
		return toResponse(e.getErrorCode(), ErrorResponse.of(e.getErrorCode(), e.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleInvalidBody(MethodArgumentNotValidException e) {
		List<FieldErrorResponse> fieldErrors = e.getBindingResult().getFieldErrors().stream()
				.map(FieldErrorResponse::from)
				.toList();
		ErrorCode errorCode = CommonErrorCode.VALIDATION_ERROR;
		return toResponse(errorCode, ErrorResponse.of(errorCode, fieldErrors));
	}

	@ExceptionHandler({
			HandlerMethodValidationException.class,
			MissingServletRequestParameterException.class,
			MethodArgumentTypeMismatchException.class,
			HttpMessageNotReadableException.class
	})
	public ResponseEntity<ErrorResponse> handleInvalidRequest(Exception e) {
		return toResponse(CommonErrorCode.VALIDATION_ERROR);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
		return toResponse(CommonErrorCode.METHOD_NOT_ALLOWED);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
		return toResponse(CommonErrorCode.NOT_FOUND);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e) {
		log.warn("Data integrity violation: {}", e.getMostSpecificCause().getMessage());
		return toResponse(CommonErrorCode.CONFLICT);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
		log.error("Unexpected error", e);
		return toResponse(CommonErrorCode.INTERNAL_SERVER_ERROR);
	}

	private ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode) {
		return toResponse(errorCode, ErrorResponse.of(errorCode));
	}

	private ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode, ErrorResponse body) {
		return ResponseEntity.status(errorCode.getStatus()).body(body);
	}
}
