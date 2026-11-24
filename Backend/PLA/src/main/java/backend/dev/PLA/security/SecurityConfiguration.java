package backend.dev.PLA.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    @Value("${ldq.jwt.base64-secret}")
    private String jwtKey;

    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS512;

    /** Dữ liệu danh mục dùng chung: GET mở cho mọi user, ghi chỉ dành cho ADMIN. */
    private static final String[] CATALOG_PATHS = {
        "/api/v1/skills", "/api/v1/skills/**",
        "/api/v1/topics", "/api/v1/topics/**",
        "/api/v1/topic-groups", "/api/v1/topic-groups/**"
    };

    /**
     * ADMIN kế thừa mọi quyền của USER (làm quiz, flashcard, ...). Bean này được cả {@code
     * authorizeHttpRequests} lẫn {@code @PreAuthorize} dùng.
     */
    @Bean
    public static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN")
                .implies("USER")
                .build();
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(jwtKey).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, SecurityConfiguration.JWT_ALGORITHM.getName());
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(getSecretKey()));
    }

    //    @Bean
    //    public JwtDecoder jwtDecoder() {
    //        return NimbusJwtDecoder.withSecretKey(getSecretKey())
    //                .macAlgorithm(JWT_ALGORITHM)
    //                .build();
    //    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CustomAuthenticationEntryPoint authenticationEntryPoint,
            CustomAccessDeniedHandler accessDeniedHandler,
            CustomJwtDecoder jwtDecoder,
            CustomJwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/users",
                                "/error",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/v1/auth/checktoken")
                        .permitAll()
                        .requestMatchers("/api/v1/admin/**")
                        .hasRole("ADMIN")
                        // Chặn ghi danh mục ngay ở filter (trước khi đọc/validate body) để user thường
                        // luôn nhận 403, không phải 400. @PreAuthorize ở controller là lớp bảo vệ thứ hai.
                        .requestMatchers(HttpMethod.POST, CATALOG_PATHS)
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, CATALOG_PATHS)
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, CATALOG_PATHS)
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, CATALOG_PATHS)
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)
                                        .decoder(jwtDecoder))
                                .authenticationEntryPoint(authenticationEntryPoint))
                .exceptionHandling(exceptions -> exceptions.accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);
        return daoAuthenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
