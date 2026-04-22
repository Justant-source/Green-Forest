package com.vgc.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class NotifyTokenFilter extends OncePerRequestFilter {

    private final byte[] expectedTokenBytes;

    public NotifyTokenFilter(@Value("${app.notify.token:}") String token) {
        this.expectedTokenBytes = token == null ? new byte[0] : token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/notify/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        if (expectedTokenBytes.length == 0) {
            res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "notify token not configured");
            return;
        }

        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "missing bearer token");
            return;
        }
        byte[] presented = header.substring(7).getBytes(StandardCharsets.UTF_8);

        // timing-safe 비교 — equals() / == 로 바꾸지 말 것
        if (!MessageDigest.isEqual(presented, expectedTokenBytes)) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "invalid token");
            return;
        }

        // UsernamePasswordAuthenticationToken 의 3인자 생성자는 setAuthenticated(true) 를 수행 —
        // AnonymousAuthenticationToken 을 쓰면 .authenticated() 매처가 false 로 판정한다.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("notify-poller", "N/A",
                        AuthorityUtils.createAuthorityList("ROLE_NOTIFY_POLLER"))
        );
        chain.doFilter(req, res);
    }
}
