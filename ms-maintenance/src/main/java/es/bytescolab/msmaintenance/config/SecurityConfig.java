package es.bytescolab.msmaintenance.config;

import es.bytescolab.msmaintenance.security.InternalKeyFilter;
import es.bytescolab.msmaintenance.security.JwtAuthFilter;
import es.bytescolab.msmaintenance.security.TokenAccessDeniedHandler;
import es.bytescolab.msmaintenance.security.TokenAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final InternalKeyFilter internalKeyFilter;
    private final TokenAuthenticationEntryPoint tokenAuthenticationEntryPoint;
    private final TokenAccessDeniedHandler tokenAccessDeniedHandler;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          InternalKeyFilter internalKeyFilter,
                          TokenAuthenticationEntryPoint tokenAuthenticationEntryPoint,
                          TokenAccessDeniedHandler tokenAccessDeniedHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.internalKeyFilter = internalKeyFilter;
        this.tokenAuthenticationEntryPoint = tokenAuthenticationEntryPoint;
        this.tokenAccessDeniedHandler = tokenAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(internalKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(tokenAuthenticationEntryPoint)
                        .accessDeniedHandler(tokenAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
