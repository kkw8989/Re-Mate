package com.example.backend.config;

import com.example.backend.global.common.ErrorResponse;
import com.example.backend.global.error.ErrorCode;
import com.example.backend.global.filter.TraceIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class DeviceAuthenticationFilter extends OncePerRequestFilter {

    private final DeviceKeyValidator deviceKeyValidator;

    public DeviceAuthenticationFilter(DeviceKeyValidator deviceKeyValidator) {
        this.deviceKeyValidator = deviceKeyValidator;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String deviceKey = request.getHeader("X-DEVICE-KEY");

        if (deviceKey != null && !deviceKey.isBlank()) {
            if (deviceKeyValidator.isValid(deviceKey)) {
                AbstractAuthenticationToken auth = new DeviceAuthenticationToken(deviceKey);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                writeUnauthorized(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        String traceId = MDC.get(TraceIdFilter.MDC_KEY);
        if (traceId == null || traceId.isBlank()) {
            traceId = "no-trace";
        }

        ErrorResponse body =
                ErrorResponse.of(
                        ErrorCode.DEVICE_UNAUTHORIZED, ErrorCode.DEVICE_UNAUTHORIZED.defaultMessage(), traceId);

        response.setStatus(ErrorCode.DEVICE_UNAUTHORIZED.status().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(body.toString());
    }

    static class DeviceAuthenticationToken extends AbstractAuthenticationToken {

        private final String deviceKey;

        DeviceAuthenticationToken(String deviceKey) {
            super(List.of(new SimpleGrantedAuthority("ROLE_DEVICE")));
            this.deviceKey = deviceKey;
            setAuthenticated(true);
        }

        @Override
        public Object getCredentials() {
            return deviceKey;
        }

        @Override
        public Object getPrincipal() {
            return "DEVICE";
        }
    }
}