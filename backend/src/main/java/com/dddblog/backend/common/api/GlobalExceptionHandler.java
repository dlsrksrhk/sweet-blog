package com.dddblog.backend.common.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dddblog.backend.auth.application.AuthenticationFailedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(AuthenticationFailedException.class)
	ResponseEntity<ErrorResponse> handleAuthenticationFailedException(AuthenticationFailedException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.body(new ErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleIllegalArgumentException(IllegalArgumentException exception) {
		return new ErrorResponse(exception.getMessage());
	}
}
