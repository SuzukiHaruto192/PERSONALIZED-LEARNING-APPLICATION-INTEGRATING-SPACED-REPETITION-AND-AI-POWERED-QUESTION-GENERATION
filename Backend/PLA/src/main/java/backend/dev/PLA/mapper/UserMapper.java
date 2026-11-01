package backend.dev.PLA.mapper;

import backend.dev.PLA.dto.request.auth.InformationUserRequest;
import backend.dev.PLA.dto.response.user.UserResponse;
import backend.dev.PLA.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    public UserResponse toResponse(User user);
    public void userUpdate(@MappingTarget User user, InformationUserRequest informationUserRequest);
}
