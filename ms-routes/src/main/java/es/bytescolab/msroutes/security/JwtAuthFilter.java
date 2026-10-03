package es.bytescolab.msroutes.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtUtil.parse(token);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                SecurityContextHolder.getContext()
                        .setAuthentication(buildAuthentication(claims));
            }

        } catch (ExpiredJwtException e) {
            // token caducado → 401 TOKEN_EXPIRED desde el entry point
            request.setAttribute(TokenAuthenticationEntryPoint.TOKEN_EXPIRED_ATTRIBUTE,
                    TokenAuthenticationEntryPoint.TOKEN_EXPIRED);
            SecurityContextHolder.clearContext();
        } catch (JwtException | IllegalArgumentException e) {
            // token inválido → 401 UNAUTHORIZED desde el entry point
            log.warn("Token inválido — URI: {}", request.getRequestURI());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken buildAuthentication(Claims claims) {
        String subject = claims.getSubject();
        String role = claims.get("role", String.class);
        List<GrantedAuthority> authorities = (role == null || role.isBlank())
                ? List.of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + role));
        log.debug("Request autenticada — subject='{}', role='{}'", subject, role);
        return new UsernamePasswordAuthenticationToken(subject, null, authorities);
    }
}
