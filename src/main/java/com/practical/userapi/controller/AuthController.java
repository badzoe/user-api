package com.practical.userapi.controller;

import com.practical.userapi.config.JwtTokenUtil;
import com.practical.userapi.dto.LoginRequest;
import com.practical.userapi.dto.LoginResponse;
import com.practical.userapi.dto.LogoutRequest;
import com.practical.userapi.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;

    public AuthController(AuthService authService,
                          UserDetailsService userDetailsService,
                          JwtTokenUtil jwtTokenUtil) {
        this.authService = authService;
        this.userDetailsService = userDetailsService;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.info("Received login request for user: {}", request.getUsername());
            LoginResponse response = authService.login(request);
            log.info("Login successful for user: {}. Returning token.", request.getUsername());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Login failed for user: {}. Error: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout/{id}")
    public ResponseEntity<Void> logout(
            @PathVariable String id,
            @Valid @RequestBody LogoutRequest request) {
        try {
            log.info("Received logout request for user ID: {}", id);

            try {
                Long.parseLong(id);
            } catch (NumberFormatException e) {
                log.warn("Invalid user ID format: {}", id);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            authService.logout(request.getToken(), id);

            log.info("Logout successful for user ID: {}", id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("Invalid logout request for user ID: {}. Error: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (RuntimeException e) {
            log.error("Logout failed for user ID: {}. Error: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/test-token")
    public ResponseEntity<String> testToken(@RequestParam String username) {
        try {
            log.info("Testing token generation for user: {}", username);

            var userDetails = userDetailsService.loadUserByUsername(username);
            log.info("UserDetails loaded successfully for: {}", username);

            var token = jwtTokenUtil.generateToken(userDetails);
            log.info("Token generated successfully for: {}", username);

            var isValid = jwtTokenUtil.validateToken(token, userDetails);
            log.info("Token validation result for {}: {}", username, isValid);

            var expiration = jwtTokenUtil.getExpirationDateFromToken(token);

            return ResponseEntity.ok(String.format("""
                User: %s
                Token: %s
                Token valid: %s
                Token expires: %s
                Current time: %s
                """,
                    username,
                    token,
                    isValid,
                    expiration,
                    new java.util.Date()
            ));
        } catch (Exception e) {
            log.error("Error testing token for {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: " + e.getMessage() + "\n" + e.getClass().getName());
        }
    }
}