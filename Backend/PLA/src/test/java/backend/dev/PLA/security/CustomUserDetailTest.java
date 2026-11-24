package backend.dev.PLA.security;

import static org.assertj.core.api.Assertions.assertThat;

import backend.dev.PLA.entities.user.User;
import backend.dev.PLA.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class CustomUserDetailTest {

    @Test
    void defaultRole_isUser() {
        CustomUserDetail detail = new CustomUserDetail(
                User.builder().id(1L).username("u").password("p").build());

        assertThat(detail.getRole()).isEqualTo(Role.USER);
        assertThat(detail.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    void adminRole_hasRoleAdminAuthority() {
        CustomUserDetail detail = new CustomUserDetail(User.builder()
                .id(1L)
                .username("a")
                .password("p")
                .role(Role.ADMIN)
                .build());

        assertThat(detail.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void nullRole_fallsBackToUser() {
        User user = User.builder().id(1L).username("u").password("p").build();
        user.setRole(null);
        CustomUserDetail detail = new CustomUserDetail(user);

        assertThat(detail.getRole()).isEqualTo(Role.USER);
        assertThat(detail.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }
}
