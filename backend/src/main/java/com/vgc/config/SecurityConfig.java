package com.vgc.config;

import com.vgc.security.JwtAuthenticationFilter;
import com.vgc.security.BotTokenFilter;
import com.vgc.security.NotifyTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final BotTokenFilter botTokenFilter;
    private final NotifyTokenFilter notifyTokenFilter;

    @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          BotTokenFilter botTokenFilter,
                          NotifyTokenFilter notifyTokenFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.botTokenFilter = botTokenFilter;
        this.notifyTokenFilter = notifyTokenFilter;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/uploads/**", "/api/media/**");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 0. Bot API - 토큰 검증 (BotTokenFilter에서 처리)
                        .requestMatchers("/api/bot/**").permitAll()

                        // 0-1. Notify polling - NotifyTokenFilter 에서 Bearer 토큰 검증
                        .requestMatchers("/api/notify/**").authenticated()

                        // 0-2. Swagger UI / OpenAPI 스펙 - 공개 (prod 는 nginx 에서 차단)
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // 1. 인증 불필요 - 공개 API
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/posts").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/posts/*/comments").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/announcements/active").permitAll()

                        // 2. 인증 필요 - 포스트 CUD
                        .requestMatchers(HttpMethod.POST, "/api/posts").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/posts/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/posts/*").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/posts/*/status").authenticated()

                        // 3. 인증 필요 - 좋아요, 북마크, 댓글 작성
                        .requestMatchers(HttpMethod.GET, "/api/posts/*/like").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/posts/*/like").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/posts/*/bookmark").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/posts/*/bookmark").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/posts/*/comments").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/posts/*/comments/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/posts/*/comments/*").authenticated()

                        // 4. 개별 포스트 조회 - 공개
                        .requestMatchers(HttpMethod.GET, "/api/posts/*").permitAll()

                        // 5. 대화(쪽지) - 인증 필요
                        .requestMatchers("/api/conversations/**").authenticated()

                        // 6. WebSocket - STOMP 레벨에서 JWT 인증
                        .requestMatchers("/ws/**").permitAll()

                        // 7. 리더보드 - 공개
                        .requestMatchers(HttpMethod.GET, "/api/leaderboard").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/leaderboard/party/*").permitAll()

                        // 8-1. 출석 - 공개/인증
                        .requestMatchers(HttpMethod.GET, "/api/attendance/today").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/attendance/phrases/random").permitAll()
                        .requestMatchers("/api/attendance/**").authenticated()

                        // 8-2. 뽑기 - 공개/인증
                        .requestMatchers(HttpMethod.GET, "/api/gacha/prizes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/gacha/recent-wins").permitAll()
                        .requestMatchers("/api/gacha/**").authenticated()

                        // 8-4. 광장 위너 - 공개
                        .requestMatchers(HttpMethod.GET, "/api/plaza/winners").permitAll()

                        // 8-3. 식물 성장 - 인증 필요
                        .requestMatchers("/api/plant/**").authenticated()

                        // 8. 퀘스트, 알림 - 인증 필요
                        .requestMatchers("/api/quests/**").authenticated()
                        .requestMatchers("/api/notifications/**").authenticated()

                        // 8-5. 이벤트(사진 빙고 등 타임어택 활동) - mode/list/detail 공개, 참여는 인증 필요
                        .requestMatchers(HttpMethod.GET, "/api/events/mode").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/*").permitAll()
                        .requestMatchers("/api/events/**").authenticated()

                        // 8-6. 설문 - 조회 공개, 생성/투표/옵션추가 인증 필요
                        .requestMatchers(HttpMethod.GET, "/api/surveys/notices").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/surveys/by-post/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/surveys/*/votes").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/surveys").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/surveys/*/vote").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/surveys/*/options").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/surveys/*/close").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/surveys/close-by-post/*").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/surveys/*/meta").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/surveys/*/options/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/surveys/*/options/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/surveys/*/admin-options").authenticated()

                        // 9. 유저 프로필 업데이트 - 인증 필요
                        .requestMatchers("/api/users/**").authenticated()

                        // 10. 카테고리 요청, 프로필, 관리자
                        .requestMatchers(HttpMethod.POST, "/api/categories/request").authenticated()
                        .requestMatchers("/api/profile/**").authenticated()
                        .requestMatchers("/api/admin/**").authenticated()

                        // 11. 그 외 모든 요청 - 인증 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(botTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(notifyTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<BotTokenFilter> botFilterRegistration(BotTokenFilter filter) {
        FilterRegistrationBean<BotTokenFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<NotifyTokenFilter> notifyFilterRegistration(NotifyTokenFilter filter) {
        FilterRegistrationBean<NotifyTokenFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
