package backend.dev.PLA.services;

import backend.dev.PLA.dto.request.InformationUserRequest;
import backend.dev.PLA.dto.request.SignupRequest;
import backend.dev.PLA.entities.User;
import backend.dev.PLA.exceptions.DataAlreadyExistException;
import backend.dev.PLA.exceptions.DataNotFoundException;
import backend.dev.PLA.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Transactional
    public String  createNewUser(SignupRequest signupRequest) {
        if (userRepository.findByUsername(signupRequest.getUsername()) != null) {
            throw new DataAlreadyExistException("Username is already taken!");
        }

        User user = User.builder()
                .username(signupRequest.getUsername())
                .password(passwordEncoder.encode(signupRequest.getPassword()))
                .email(signupRequest.getEmail())
                .phoneNumber(signupRequest.getPhoneNumber())
                .build();
        userRepository.save(user);
        return signupRequest.getUsername();


    }
    @Transactional
    public void updateUser(InformationUserRequest informationUserRequest) {
          Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
          User currentUser = userRepository.findByUsername(authentication.getName());

          if(currentUser==null){
              throw new DataNotFoundException("User not found!");
          }

         if(!informationUserRequest.getPassword().isEmpty())
             currentUser.setPassword(passwordEncoder.encode(informationUserRequest.getPassword()));
         if(!informationUserRequest.getEmail().isEmpty())
             currentUser.setEmail(informationUserRequest.getEmail());
         if(!informationUserRequest.getPhoneNumber().isEmpty())
             currentUser.setPhoneNumber(informationUserRequest.getPhoneNumber());
         if(!informationUserRequest.getImageUrl().isEmpty())
             currentUser.setAvatarUrl(informationUserRequest.getImageUrl());
         if(informationUserRequest.getCurrentScore()!=null)
             currentUser.setCurrentScore(informationUserRequest.getCurrentScore());
         if(informationUserRequest.getTargetScore()!=null)
             currentUser.setTargetScore(informationUserRequest.getTargetScore());

         userRepository.save(currentUser);

    }
}
