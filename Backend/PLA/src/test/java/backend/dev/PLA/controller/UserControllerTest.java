package backend.dev.PLA.controller;

import backend.dev.PLA.dto.request.flashcard.SignupRequest;
import backend.dev.PLA.dto.response.user.UserResponse;
import backend.dev.PLA.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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

    }

    @Test
    void createUserTest() throws Exception {
        log.info("createUser");
        // GIVEN
        String content = objectMapper.writeValueAsString(signupRequest);

        Mockito.when(userService.createNewUser(ArgumentMatchers.any(SignupRequest.class)))
                .thenReturn(userResponse);

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/v1/users")
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
}
