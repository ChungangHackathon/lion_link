package com.lionlink.backend.global.error;

import com.lionlink.backend.domain.file.exception.FileErrorCode;
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
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
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

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ErrorResponse> handleInvalidParameter(HandlerMethodValidationException e) {
		List<FieldErrorResponse> fieldErrors = e.getParameterValidationResults().stream()
				.flatMap(result -> result.getResolvableErrors().stream()
						.map(error -> new FieldErrorResponse(
								result.getMethodParameter().getParameterName(),
								error.getDefaultMessage())))
				.toList();
		ErrorCode errorCode = CommonErrorCode.VALIDATION_ERROR;
		return toResponse(errorCode, ErrorResponse.of(errorCode, fieldErrors));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
		ErrorCode errorCode = CommonErrorCode.VALIDATION_ERROR;
		List<FieldErrorResponse> fieldErrors = List.of(new FieldErrorResponse(e.getName(), "지원하지 않는 값입니다."));
		return toResponse(errorCode, ErrorResponse.of(errorCode, fieldErrors));
	}

	@ExceptionHandler({
			MissingServletRequestParameterException.class,
			MissingServletRequestPartException.class,
			HttpMessageNotReadableException.class,
			MultipartException.class
	})
	public ResponseEntity<ErrorResponse> handleInvalidRequest(Exception e) {
		return toResponse(CommonErrorCode.VALIDATION_ERROR);
	}

	/** multipart 한도(application.yml)를 넘은 업로드. 컨트롤러에 닿기 전에 나는 예외라 여기서 FILE_TOO_LARGE로 바꾼다. */
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException e) {
		return toResponse(FileErrorCode.FILE_TOO_LARGE);
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
