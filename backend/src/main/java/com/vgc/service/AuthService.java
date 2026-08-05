package com.vgc.service;

import com.vgc.dto.AuthResponse;
import com.vgc.entity.User;
import com.vgc.repository.UserRepository;
import com.vgc.security.JwtUtil;
import com.vgc.util.BirthMonthDay;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SystemSettingService systemSettingService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       SystemSettingService systemSettingService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.systemSettingService = systemSettingService;
    }

    public AuthResponse register(String email, String password, String nickname, String name,
                                 Integer birthMonth, Integer birthDay) {
        if (!systemSettingService.isRegistrationOpen()) {
            throw new RuntimeException("현재 신규 가입을 받지 않습니다. 관리자에게 문의해 주세요.");
        }
        String loginId = requireValidLoginId(email);
        if (userRepository.existsByEmail(loginId)) {
            throw new RuntimeException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByNickname(nickname)) {
            throw new RuntimeException("이미 사용 중인 닉네임입니다.");
        }
        BirthMonthDay.requireValid(birthMonth, birthDay);

        User user = new User();
        user.setEmail(loginId);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setName(name);
        user.setBirthMonth(birthMonth);
        user.setBirthDay(birthDay);
        userRepository.save(user);

        String token = jwtUtil.generateToken(loginId);
        return new AuthResponse(token, nickname, user.getName(), user.getRole());
    }

    public AuthResponse login(String email, String password) {
        String loginId = email == null ? "" : email.trim();
        User user = userRepository.findByEmail(loginId)
                .orElseThrow(() -> new RuntimeException("아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        String token = jwtUtil.generateToken(loginId);
        return new AuthResponse(token, user.getNickname(), user.getName(), user.getRole());
    }

    private String requireValidLoginId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new RuntimeException("아이디를 입력해주세요.");
        }
        String loginId = raw.trim();
        if (loginId.length() > 20) {
            throw new RuntimeException("아이디는 20자 이하여야 합니다.");
        }
        return loginId;
    }
}
