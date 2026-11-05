package backend.dev.PLA.controller;

import backend.dev.PLA.dto.request.auth.InformationUserRequest;
import backend.dev.PLA.dto.request.flashcard.SignupRequest;
import backend.dev.PLA.dto.response.user.UserResponse;
import backend.dev.PLA.entities.user.User;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SignupRequest signupRequest;
    private UserResponse userResponse;
    private InformationUserRequest informationUserRequest;
    private CustomUserDetail customUserDetail;

    @BeforeEach
    public void initData() {
        signupRequest = SignupRequest.builder()
                .username("Quang123")
                .password("Quang2006@")
                .email("Quang123@gmail.com")
                .phoneNumber("0706101617")
                .build();

        userResponse = UserResponse.builder()
                .id(1L)
                .username("Quang123")
                .email("Quang123@gmail.com")
                .phoneNumber("0706101617")
                .build();

        informationUserRequest = InformationUserRequest.builder()
                .currentScore(new BigDecimal("3.0"))
                .imageUrl("https://123/234")
                .phoneNumber("0707101617")
                .email("Quang123@gmail.com")
                .build();
        customUserDetail = new CustomUserDetail(User.builder()
                .id(1L)
                .username("Quang123")
                .password("Quang2006@")
                .build());
    }

    @Test
    void createUserTest() throws Exception {
        log.info("createUser");
        // GIVEN
        String content = objectMapper.writeValueAsString(signupRequest);

        Mockito.when(userService.createNewUser(ArgumentMatchers.any(SignupRequest.class)))
                .thenReturn(userResponse);

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(1000))
                .andExpect(MockMvcResultMatchers.jsonPath("message").value("signup successfully!"))
                .andExpect(MockMvcResultMatchers.jsonPath("data.id").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("data.username").value("Quang123"))
                .andExpect(MockMvcResultMatchers.jsonPath("data.email").value("Quang123@gmail.com"))
                .andExpect(MockMvcResultMatchers.jsonPath("data.phoneNumber").value("0706101617"));
    }

    @Test
    void createUserTest_UserBeExist() throws Exception {
        log.info("createUser_UserBeExist");
        // GIVEN
        String content = objectMapper.writeValueAsString(signupRequest);

        Mockito.when(userService.createNewUser(ArgumentMatchers.any(SignupRequest.class)))
                .thenThrow(new AppException(ErrorCode.USER_EXISTED));

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(ErrorCode.USER_EXISTED.getCode()))
                .andExpect(MockMvcResultMatchers.jsonPath("message").value(ErrorCode.USER_EXISTED.getMessage()))
                .andExpect(MockMvcResultMatchers.jsonPath("path").value("/api/v1/users"))
                .andExpect(MockMvcResultMatchers.jsonPath("data").doesNotExist());
    }

    @Test
    void updateUserTest() throws Exception {
        log.info("updateUser");
        // GIVEN
        String content = objectMapper.writeValueAsString(informationUserRequest);

        Mockito.doNothing()
                .when(userService)
                .updateUser(
                        ArgumentMatchers.any(CustomUserDetail.class),
                        ArgumentMatchers.any(InformationUserRequest.class),
                        ArgumentMatchers.eq(1L));

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/users/1")
                        .with(SecurityMockMvcRequestPostProcessors.user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(1000))
                .andExpect(MockMvcResultMatchers.jsonPath("message").value("update user detail successfully!"));

        Mockito.verify(userService)
                .updateUser(
                        ArgumentMatchers.any(CustomUserDetail.class),
                        ArgumentMatchers.any(InformationUserRequest.class),
                        ArgumentMatchers.eq(1L));
    }

    @Test
    void updateUserTest_accessDeniedException() throws Exception {
        log.info("updateUser_accessDeniedException");
        // GIVEN: user id=1 cố sửa thông tin user id=2
        String content = objectMapper.writeValueAsString(informationUserRequest);

        Mockito.doThrow(new AppException(ErrorCode.ACCESS_DENIED))
                .when(userService)
                .updateUser(
                        ArgumentMatchers.any(CustomUserDetail.class),
                        ArgumentMatchers.any(InformationUserRequest.class),
                        ArgumentMatchers.eq(2L));

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/users/2")
                        .with(SecurityMockMvcRequestPostProcessors.user(customUserDetail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(ErrorCode.ACCESS_DENIED.getCode()))
                .andExpect(MockMvcResultMatchers.jsonPath("message").value(ErrorCode.ACCESS_DENIED.getMessage()));

        Mockito.verify(userService)
                .updateUser(
                        ArgumentMatchers.any(CustomUserDetail.class),
                        ArgumentMatchers.any(InformationUserRequest.class),
                        ArgumentMatchers.eq(2L));
    }

    @Test
    public void getUserTest() throws Exception {
        log.info("getUser");

        Mockito.when(userService.getUser(ArgumentMatchers.any(CustomUserDetail.class), ArgumentMatchers.eq(1L)))
                .thenReturn(userResponse);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/users/1")
                        .with(SecurityMockMvcRequestPostProcessors.user(customUserDetail)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("data.id").value(userResponse.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("data.username").value(userResponse.getUsername()))
                .andExpect(MockMvcResultMatchers.jsonPath("data.phoneNumber").value(userResponse.getPhoneNumber()))
                .andExpect(MockMvcResultMatchers.jsonPath("data.email").value(userResponse.getEmail()));
    }
}
