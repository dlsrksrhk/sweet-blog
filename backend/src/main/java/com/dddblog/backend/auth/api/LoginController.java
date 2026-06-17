package com.dddblog.backend.auth.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dddblog.backend.auth.application.LoginService;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

	private final LoginService loginService;

	public LoginController(LoginService loginService) {
		this.loginService = loginService;
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
		String accessToken = loginService.login(request.loginId(), request.password());
		return ResponseEntity.ok(new LoginResponse(accessToken));
	}
}
