package backend.dev.PLA.mapper;

import backend.dev.PLA.dto.request.InformationUserRequest;
import backend.dev.PLA.dto.response.UserResponse;
import backend.dev.PLA.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
public interface UserMapper {
    public UserResponse toResponse(User user);
    public void userUpdate(@MappingTarget User user, InformationUserRequest informationUserRequest);
}
