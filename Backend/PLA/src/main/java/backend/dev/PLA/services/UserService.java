package backend.dev.PLA.services;

import backend.dev.PLA.dto.request.auth.InformationUserRequest;
import backend.dev.PLA.dto.request.flashcard.SignupRequest;
import backend.dev.PLA.dto.response.user.UserResponse;
import backend.dev.PLA.entities.User;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.mapper.UserMapper;
import backend.dev.PLA.repositories.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE ,  makeFinal = true)
public class UserService {
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    UserMapper userMapper;
    @Transactional
    public UserResponse  createNewUser(SignupRequest signupRequest) {
        if (userRepository.findByUsername(signupRequest.getUsername()) != null) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        User user = User.builder()
                .username(signupRequest.getUsername())
                .password(passwordEncoder.encode(signupRequest.getPassword()))
                .email(signupRequest.getEmail())
                .phoneNumber(signupRequest.getPhoneNumber())
                .build();
        User saveUser =  userRepository.save(user);

        return userMapper.toResponse(saveUser);
    }
    @Transactional
    public void updateUser(InformationUserRequest informationUserRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(authentication.getName());

        if (currentUser == null) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        if (informationUserRequest.getPassword() != null && !informationUserRequest.getPassword().isBlank()) {
            currentUser.setPassword(passwordEncoder.encode(informationUserRequest.getPassword()));
        }
        if (informationUserRequest.getEmail() != null && !informationUserRequest.getEmail().isBlank()) {
            currentUser.setEmail(informationUserRequest.getEmail());
        }
        if (informationUserRequest.getPhoneNumber() != null && !informationUserRequest.getPhoneNumber().isBlank()) {
            currentUser.setPhoneNumber(informationUserRequest.getPhoneNumber());
        }
        if (informationUserRequest.getImageUrl() != null && !informationUserRequest.getImageUrl().isBlank()) {
            currentUser.setAvatarUrl(informationUserRequest.getImageUrl());
        }
        if (informationUserRequest.getCurrentScore() != null) {
            currentUser.setCurrentScore(informationUserRequest.getCurrentScore());
        }
        if (informationUserRequest.getTargetScore() != null) {
            currentUser.setTargetScore(informationUserRequest.getTargetScore());
        }

        userRepository.save(currentUser);
    }
    public UserResponse getUser (){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(authentication.getName());
        return userMapper.toResponse(currentUser);
    }
}
