package com.minimarket.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ThreatDetectionFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(ThreatDetectionFilter.class);

    private static final List<Pattern> XSS_PATTERNS = List.of(
            Pattern.compile("<\\s*script", Pattern.CASE_INSENSITIVE),
            Pattern.compile("javascript\\s*:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<\\s*iframe", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<\\s*object", Pattern.CASE_INSENSITIVE)
    );

    private static final List<Pattern> SQL_INJECTION_PATTERNS = List.of(
            Pattern.compile("('\\s*or\\s*'?[\\w\\d]+\\s*'=\\s*'?[\\w\\d]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bunion\\s+select\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bdrop\\s+table\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\binformation_schema\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(--|/\\*|\\*/|;\\s*--)", Pattern.CASE_INSENSITIVE)
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        CachedBodyRequest wrappedRequest = new CachedBodyRequest(request);
        ThreatMatch match = inspect(wrappedRequest);

        if (match != null) {
            logger.warn(
                    "Actividad sospechosa bloqueada type={} location={} method={} path={} ip={} requestId={}",
                    match.type(),
                    match.location(),
                    request.getMethod(),
                    request.getRequestURI(),
                    clientIp(request),
                    MDC.get("requestId")
            );
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("""
                    {"status":400,"error":"Solicitud invalida","message":"La solicitud contiene patrones potencialmente maliciosos","path":"%s","requestId":"%s"}\
                    """.formatted(request.getRequestURI(), MDC.get("requestId") == null ? "" : MDC.get("requestId")));
            return;
        }

        filterChain.doFilter(wrappedRequest, response);
    }

    private ThreatMatch inspect(CachedBodyRequest request) {
        ThreatMatch queryMatch = scan("query", request.getQueryString());
        if (queryMatch != null) {
            return queryMatch;
        }

        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameter = parameterNames.nextElement();
            for (String value : request.getParameterValues(parameter)) {
                ThreatMatch parameterMatch = scan("parameter:" + parameter, value);
                if (parameterMatch != null) {
                    return parameterMatch;
                }
            }
        }

        return scan("body", request.getCachedBodyAsString());
    }

    private ThreatMatch scan(String location, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(value).find()) {
                return new ThreatMatch("XSS", location);
            }
        }

        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            if (pattern.matcher(value).find()) {
                return new ThreatMatch("SQL_INJECTION", location);
            }
        }

        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record ThreatMatch(String type, String location) {
    }

    private static class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        CachedBodyRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = request.getInputStream().readAllBytes();
        }

        String getCachedBodyAsString() {
            return new String(cachedBody, StandardCharsets.UTF_8);
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return inputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                }

                @Override
                public int read() {
                    return inputStream.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
