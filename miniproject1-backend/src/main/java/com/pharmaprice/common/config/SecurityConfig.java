package com.pharmaprice.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * T-23에서 실제 인증(JWT)으로 교체될 임시 설정.
 * 그 전까지는 문서/헬스체크 경로만 열어둔다.
 */
@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(auth -> auth
			.requestMatchers(
				"/swagger-ui/**", "/swagger-ui.html",
				"/v3/api-docs/**", "/actuator/**"
			).permitAll()
			.anyRequest().authenticated()
		);
		return http.build();
	}
}
