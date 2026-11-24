package backend.dev.PLA.security;

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final CustomUserDetailService customUserDetailService;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UserDetails userDetail = customUserDetailService.loadUserByUsername(jwt.getSubject());
        // Lấy quyền từ role trong DB thay vì claim "scope" của token: hạ quyền admin có hiệu lực ngay
        // và token cũ (scope cứng "USER") vẫn dùng được.
        Collection<? extends GrantedAuthority> authorities = userDetail.getAuthorities();
        return new JwtAuthenticationToken(jwt, userDetail, authorities);
    }
}
