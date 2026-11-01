package backend.dev.PLA.security;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final CustomUserDetailService customUserDetailService;

    private final JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = createAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UserDetails userDetail = customUserDetailService.loadUserByUsername(jwt.getSubject());
        Collection<GrantedAuthority> authorities = grantedAuthoritiesConverter.convert(jwt);
        return new JwtAuthenticationToken(jwt, userDetail, authorities);
    }

    private static JwtGrantedAuthoritiesConverter createAuthoritiesConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthorityPrefix("ROLE_");
        converter.setAuthoritiesClaimName("scope");
        return converter;
    }
}
