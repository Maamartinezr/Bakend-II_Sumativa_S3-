package com.minimarket.controller;

import com.minimarket.security.model.LoginRequest;
import com.minimarket.security.model.LoginResponse;
import com.minimarket.security.service.LoginAttemptService;
import com.minimarket.security.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.loginAttemptService = loginAttemptService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = clientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        if (loginAttemptService.isBlocked(request.getUsername(), ip)) {
            logger.warn("Login bloqueado temporalmente username={} ip={} userAgent={}", request.getUsername(), ip, userAgent);
            throw new BadCredentialsException("Cuenta bloqueada temporalmente por intentos fallidos");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            loginAttemptService.recordSuccess(request.getUsername(), ip);
            String token = jwtUtil.generateToken(authentication.getName(), authentication.getAuthorities());
            logger.info("Login exitoso username={} ip={} userAgent={}", request.getUsername(), ip, userAgent);
            return ResponseEntity.ok(new LoginResponse(token, jwtUtil.getExpirationMs()));
        } catch (BadCredentialsException ex) {
            loginAttemptService.recordFailure(request.getUsername(), ip);
            logger.warn("Intento de autenticacion fallido username={} ip={} userAgent={}", request.getUsername(), ip, userAgent);
            throw ex;
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
