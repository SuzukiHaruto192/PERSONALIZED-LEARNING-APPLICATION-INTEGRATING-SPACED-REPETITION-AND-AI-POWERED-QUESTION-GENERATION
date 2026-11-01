package backend.dev.PLA.service;

import backend.dev.PLA.dto.request.auth.InformationUserRequest;
import backend.dev.PLA.dto.request.flashcard.SignupRequest;
import backend.dev.PLA.dto.response.user.UserResponse;
import backend.dev.PLA.entities.User;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.mapper.UserMapper;
import backend.dev.PLA.repositories.UserRepository;
import backend.dev.PLA.security.CustomUserDetail;
import backend.dev.PLA.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)   // không cần @SpringBootTest
class UserServiceTest {

    @Mock
    private UserRepository userRepository;          // MOCK: chạm DB

    @Spy
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();      // thật
    @Spy
    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);        // thật

    @InjectMocks
    private UserService userService;

    private SignupRequest signupRequest;
    private User user;
    private InformationUserRequest informationUserRequest;
    private CustomUserDetail customUserDetail;

    @BeforeEach
    void initData() {
//        userService = new UserService(userRepository, passwordEncoder, userMapper);

        signupRequest = SignupRequest.builder()
                .password("Quang2006@")
                .email("Quang123@gmail.com")
                .phoneNumber("0706101617")
                .username("Quang123")
                .build();

        user = User.builder()
                .id(1L)
                .username("Quang123")
                .password("encoded")
                .email("Quang123@gmail.com")
                .phoneNumber("0706101617")
                .build();

        informationUserRequest = InformationUserRequest.builder()
                .imageUrl("1234.com")
                .targetScore(new BigDecimal(9.0))
                .currentScore(new BigDecimal(3.0))
                .password("Quang123@")
                .phoneNumber("0706101617")
                .build();
        customUserDetail = new CustomUserDetail(user);
    }

    @Test
    void createNewUser_success() {
        // GIVEN
        when(userRepository.findByUsername("Quang123")).thenReturn(null);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // WHEN
        UserResponse response = userService.createNewUser(signupRequest);

        // THEN
        assertEquals(1L, response.getId());
        assertEquals("Quang123", response.getUsername());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertNotEquals("Quang2006@", saved.getPassword());
        assertTrue(passwordEncoder.matches("Quang2006@", saved.getPassword()));
    }

    @Test
    void createNewUser_usernameExisted_throws() {
        // GIVEN: username đã tồn tại
        when(userRepository.findByUsername("Quang123")).thenReturn(user);

        // WHEN + THEN
        AppException ex = assertThrows(AppException.class,
                () -> userService.createNewUser(signupRequest));
        assertEquals(ErrorCode.USER_EXISTED, ex.getErrorCode());

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_success() {
        // GIVEN
        when(userRepository.findById(user.getId())).thenReturn(Optional.ofNullable(user));

        // WHEN
        userService.updateUser(customUserDetail, informationUserRequest, 1L);

        // THEN
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertTrue(passwordEncoder.matches("Quang123@", saved.getPassword()));
        assertEquals("0706101617", saved.getPhoneNumber());
        assertEquals("1234.com", saved.getAvatarUrl());
        assertEquals(0, new BigDecimal("9.0").compareTo(saved.getTargetScore()));
        assertEquals(0, new BigDecimal("3.0").compareTo(saved.getCurrentScore()));
        assertEquals("Quang123@gmail.com", saved.getEmail());   // request không có email → giữ nguyên
    }
    @Test
    void updateUser_throwsUserNotFound() {
        // GIVEN
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // WHEN + THEN
        AppException ex = assertThrows(AppException.class,
                () -> userService.updateUser(customUserDetail, informationUserRequest, 1L));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());

        verify(userRepository, never()).save(any());
    }
    @Test
    void updateUser_throwsAccessDenied() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // WHEN + THEN
        AppException ex = assertThrows(AppException.class,
                () -> userService.updateUser(customUserDetail, informationUserRequest, 2L));
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());

        verify(userRepository, never()).save(any());
        
    }

    @Test
    void getUser_success() {
        Long userId = user.getId();
        when(customUserDetail.getId()).thenReturn(userId); // nếu customUserDetail là mock
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUser(customUserDetail, userId);

        assertAll(
                () -> assertEquals(user.getId(), response.getId()),
                () -> assertEquals(user.getUsername(), response.getUsername()),
                () -> assertEquals(user.getEmail(), response.getEmail()),
                () -> assertEquals(user.getPhoneNumber(), response.getPhoneNumber()),
                () -> assertEquals(user.getAvatarUrl(), response.getAvatarUrl()),
                () -> assertEquals(user.getTargetScore(), response.getTargetScore()),
                () -> assertEquals(user.getCurrentScore(), response.getCurrentScore())
        );
        verify(userRepository).findById(userId);
    }

    @Test
    void getUser_notFound_throwsException() {
        Long userId = 99L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> userService.getUser(customUserDetail, userId));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(userRepository).findById(userId);
    }

    @Test
    void getUser_otherUser_forbidden() {
        Long otherId = 2L;
        // customUserDetail.getId() = 1L, role USER
        AppException ex = assertThrows(AppException.class,
                () -> userService.getUser(customUserDetail , 1L));
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        verify(userRepository, never()).findById(any());
    }
}