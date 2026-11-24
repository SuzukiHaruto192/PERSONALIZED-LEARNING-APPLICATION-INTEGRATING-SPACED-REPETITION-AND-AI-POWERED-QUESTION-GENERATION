package backend.dev.PLA.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.dev.PLA.exceptions.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Phân quyền theo users.role (V6) qua đường JWT thật: đăng nhập lấy token rồi gọi API, không dùng
 * user() giả. Kiểm tra quyền lấy từ DB mỗi request, ADMIN kế thừa USER và user không tự nâng quyền.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RoleAuthorizationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    static final String PASSWORD = "Secret123@";
    static final String SKILL_BODY = "{\"name\":\"Từ vựng\",\"code\":\"VOCAB\",\"icon\":\"book\"}";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PasswordEncoder passwordEncoder;

    final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void resetData() {
        jdbc.execute("TRUNCATE users, token_expired RESTART IDENTITY CASCADE");
        jdbc.update("DELETE FROM skills WHERE code = 'VOCAB'");
    }

    // ===================== Migration V6 =====================

    @Nested
    class Migration {
        @Test
        void insertWithoutRole_defaultsToUser() {
            insertUser("plain");

            assertThat(roleOf("plain")).isEqualTo("USER");
        }

        @Test
        void invalidRole_violatesCheckConstraint() {
            assertThatThrownBy(() ->
                            jdbc.update("INSERT INTO users (username, password, role) VALUES ('x', 'p', 'SUPER')"))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    // ===================== Ghi danh mục =====================

    @Nested
    class CatalogWrite {
        @Test
        void normalUser_canRead_butCannotWrite() throws Exception {
            insertUser("normal");
            String token = login("normal");

            mockMvc.perform(get("/api/v1/skills").header("Authorization", bearer(token)))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/topics").header("Authorization", bearer(token)))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/topic-groups").header("Authorization", bearer(token)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/skills")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(SKILL_BODY))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.getCode()));
            assertThat(skillExists("VOCAB")).isFalse();
        }

        @Test
        void normalUser_invalidBody_gets403Not400() throws Exception {
            insertUser("normal");
            String token = login("normal");

            mockMvc.perform(post("/api/v1/skills")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.getCode()));
            mockMvc.perform(patch("/api/v1/topics/abc/active").header("Authorization", bearer(token)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete("/api/v1/topic-groups/1").header("Authorization", bearer(token)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void admin_canWrite_andTokenScopeIsAdmin() throws Exception {
            insertUser("boss");
            setRole("boss", "ADMIN");
            String token = login("boss");

            assertThat(SignedJWT.parse(token).getJWTClaimsSet().getStringClaim("scope"))
                    .isEqualTo("ADMIN");
            mockMvc.perform(post("/api/v1/skills")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(SKILL_BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.code").value("VOCAB"));
            assertThat(skillExists("VOCAB")).isTrue();
        }

        @Test
        void promoteInDb_oldTokenWithScopeUser_getsAdminImmediately() throws Exception {
            insertUser("rising");
            String token = login("rising");
            assertThat(SignedJWT.parse(token).getJWTClaimsSet().getStringClaim("scope"))
                    .isEqualTo("USER");

            setRole("rising", "ADMIN");

            mockMvc.perform(post("/api/v1/skills")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(SKILL_BODY))
                    .andExpect(status().isOk());
        }

        @Test
        void demoteInDb_oldAdminToken_losesAccessImmediately() throws Exception {
            insertUser("falling");
            setRole("falling", "ADMIN");
            String token = login("falling");

            setRole("falling", "USER");

            mockMvc.perform(post("/api/v1/skills")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(SKILL_BODY))
                    .andExpect(status().isForbidden());
            assertThat(skillExists("VOCAB")).isFalse();
        }
    }

    // ===================== ADMIN kế thừa USER =====================

    @Nested
    class Hierarchy {
        @Test
        void admin_passesHasRoleUser() throws Exception {
            insertUser("boss");
            setRole("boss", "ADMIN");

            mockMvc.perform(get("/api/v1/auth/hello").header("Authorization", bearer(login("boss"))))
                    .andExpect(status().isOk());
        }

        @Test
        void normalUser_passesHasRoleUser() throws Exception {
            insertUser("normal");

            mockMvc.perform(get("/api/v1/auth/hello").header("Authorization", bearer(login("normal"))))
                    .andExpect(status().isOk());
        }
    }

    // ===================== Không tự nâng quyền =====================

    @Nested
    class NoSelfPromotion {
        @Test
        void patchMe_withRoleAdmin_roleUnchanged() throws Exception {
            insertUser("sneaky");
            String token = login("sneaky");

            mockMvc.perform(patch("/api/v1/users/me")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"role\":\"ADMIN\",\"phoneNumber\":\"0706101617\"}"))
                    .andExpect(status().isOk());

            assertThat(roleOf("sneaky")).isEqualTo("USER");
            mockMvc.perform(get("/api/v1/users/me").header("Authorization", bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.role").value("USER"));
        }

        @Test
        void signup_withRoleAdmin_createsPlainUser() throws Exception {
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"newbie\",\"password\":\"" + PASSWORD
                                    + "\",\"email\":\"newbie@gmail.com\",\"phoneNumber\":\"0706101617\","
                                    + "\"role\":\"ADMIN\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.role").value("USER"));

            assertThat(roleOf("newbie")).isEqualTo("USER");
        }
    }

    // ===================== Helpers =====================

    private void insertUser(String username) {
        jdbc.update("INSERT INTO users (username, password) VALUES (?, ?)", username, passwordEncoder.encode(PASSWORD));
    }

    private void setRole(String username, String role) {
        jdbc.update("UPDATE users SET role = ? WHERE username = ?", role, username);
    }

    private String roleOf(String username) {
        return jdbc.queryForObject("SELECT role FROM users WHERE username = ?", String.class, username);
    }

    private boolean skillExists(String code) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM skills WHERE code = ?)", Boolean.class, code));
    }

    private String login(String username) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return node.get("data").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
