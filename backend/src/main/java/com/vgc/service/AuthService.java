package com.vgc.service;

import com.vgc.dto.AuthResponse;
import com.vgc.entity.User;
import com.vgc.repository.UserRepository;
import com.vgc.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(String email, String password, String nickname, String name) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("이미 사용 중인 이메일입니다.");
        }
        if (userRepository.existsByNickname(nickname)) {
            throw new RuntimeException("이미 사용 중인 닉네임입니다.");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setName(name);
        userRepository.save(user);

        String token = jwtUtil.generateToken(email);
        return new AuthResponse(token, nickname, user.getName(), user.getRole());
    }

    public AuthResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String token = jwtUtil.generateToken(email);
        return new AuthResponse(token, user.getNickname(), user.getName(), user.getRole());
    }
}
