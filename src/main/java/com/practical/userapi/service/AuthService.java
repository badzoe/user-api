package com.practical.userapi.service;

import com.practical.userapi.config.JwtTokenUtil;
import com.practical.userapi.dto.LoginRequest;
import com.practical.userapi.dto.LoginResponse;
import com.practical.userapi.model.User;
import com.practical.userapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       UserDetailsService userDetailsService,
                       JwtTokenUtil jwtTokenUtil,
                       UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userRepository = userRepository;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        try {
            log.info("Attempting login for user: {}", request.getUsername());

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            log.info("Authentication successful for user: {}", request.getUsername());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            log.info("UserDetails loaded for user: {}", request.getUsername());

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("Generating JWT token for user: {}", request.getUsername());
            String token = jwtTokenUtil.generateToken(userDetails);
            log.info("JWT token generated successfully for user: {}", request.getUsername());

            LoginResponse response = new LoginResponse(user.getId().toString(), token);
            log.info("LoginResponse created for user: {}", request.getUsername());

            return response;

        } catch (Exception e) {
            log.error("Login failed for user: {}. Error: {}", request.getUsername(), e.getMessage(), e);
            throw new RuntimeException("Invalid username or password: " + e.getMessage());
        }
    }

    @Transactional
    public void logout(String token, String userId) {
        log.info("Attempting logout for user ID: {} with token", userId);

        Long userIdLong;
        try {
            userIdLong = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid user ID format. Must be a number.");
        }

        User user = userRepository.findById(userIdLong)
                .orElseThrow(() -> new RuntimeException("User with ID " + userId + " does not exist"));

        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
            log.info("Removed 'Bearer ' prefix from token");
        }

        if (!isValidTokenFormat(token)) {
            throw new IllegalArgumentException("Invalid token format. JWT must have 3 parts separated by dots.");
        }

        if (!jwtTokenUtil.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid token. Token may be expired or malformed.");
        }

        try {
            String usernameFromToken = jwtTokenUtil.getUsernameFromToken(token);
            log.info("Token belongs to user: {}", usernameFromToken);

            if (!user.getUsername().equals(usernameFromToken)) {
                throw new RuntimeException("Token does not belong to user ID " + userId +
                        ". Token user: " + usernameFromToken +
                        ", Requested user: " + user.getUsername());
            }

        } catch (Exception e) {
            throw new RuntimeException("Could not validate token ownership: " + e.getMessage());
        }

        if (jwtTokenUtil.isTokenBlacklisted(token)) {
            throw new RuntimeException("Token is already invalidated (logged out)");
        }

        jwtTokenUtil.blacklistToken(token);
        log.info("Token blacklisted successfully for user ID: {}", userId);

        SecurityContextHolder.clearContext();
        log.info("Security context cleared");
    }

    @Transactional
    public void logout(String token) {
        logout(token, "unknown");
    }

    private boolean isValidTokenFormat(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        String[] parts = token.split("\\.");
        return parts.length == 3;
    }
}