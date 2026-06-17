package com.dddblog.backend.member.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dddblog.backend.auth.application.AuthenticationFailedException;
import com.dddblog.backend.auth.security.AuthenticatedMember;

@RestController
@RequestMapping("/api/members")
public class MeController {

	private final MeService meService;

	public MeController(MeService meService) {
		this.meService = meService;
	}

	@GetMapping("/me")
	public ResponseEntity<MeResponse> me(@AuthenticationPrincipal AuthenticatedMember authenticatedMember) {
		if (authenticatedMember == null) {
			throw new AuthenticationFailedException();
		}
		return ResponseEntity.ok(meService.getMe(authenticatedMember.memberId()));
	}
}
