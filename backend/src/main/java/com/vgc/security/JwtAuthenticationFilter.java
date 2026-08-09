package com.vgc.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.vgc.repository.UserRepository;
import com.vgc.util.AppTime;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.validateToken(token)) {
                String email = jwtUtil.extractEmail(token);
                userRepository.findByEmail(email).ifPresent(user -> {
                    Date issuedAt = jwtUtil.extractIssuedAt(token);
                    Instant tokenIssuedAt = issuedAt.toInstant();
                    LocalDateTime passwordChangedAt = user.getPasswordChangedAt();
                    // password_changed_at 은 JDBC serverTimezone=Asia/Seoul 경로로 KST 벽시계 DATETIME에 저장됨
                    Instant passwordChangedInstant = passwordChangedAt == null
                            ? null
                            : passwordChangedAt.atZone(AppTime.KST).toInstant();
                    if (passwordChangedInstant == null || tokenIssuedAt.isAfter(passwordChangedInstant)) {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                });
            }
        }

        filterChain.doFilter(request, response);
    }
}
