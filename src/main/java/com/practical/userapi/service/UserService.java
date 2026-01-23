package com.practical.userapi.service;

import com.practical.userapi.dto.UserRequest;
import com.practical.userapi.dto.UserResponse;
import com.practical.userapi.model.User;
import com.practical.userapi.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createUser(UserRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        String encryptedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPhone(request.getPhone());
        user.setPassword(encryptedPassword);

        return userRepository.save(user);
    }

    public UserResponse getAllUsers() {
        List<UserResponse.UserInfo> userInfos = userRepository.findAll().stream()
                .map(user -> new UserResponse.UserInfo(
                        user.getId().toString(),
                        user.getPhone()
                ))
                .collect(Collectors.toList());

        return new UserResponse(userInfos);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}