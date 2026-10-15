package backend.dev.PLA.mapper;

import backend.dev.PLA.dto.response.UserResponse;
import backend.dev.PLA.entities.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserResponse userToUserResponse(User user){
        return UserResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .currentScore(user.getCurrentScore())
                .targetScore(user.getTargetScore())
                .build();
    }
}
