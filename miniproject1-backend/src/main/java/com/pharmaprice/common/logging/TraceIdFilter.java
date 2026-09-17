package com.pharmaprice.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// 요청마다 traceId를 MDC에 심어 로그와 에러 응답(ErrorResponse)이 같은 값을 참조하게 한다.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

	public static final String TRACE_ID_KEY = "traceId";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		MDC.put(TRACE_ID_KEY, UUID.randomUUID().toString().substring(0, 8));
		try {
			chain.doFilter(request, response);
		} finally {
			MDC.remove(TRACE_ID_KEY);
		}
	}
}
