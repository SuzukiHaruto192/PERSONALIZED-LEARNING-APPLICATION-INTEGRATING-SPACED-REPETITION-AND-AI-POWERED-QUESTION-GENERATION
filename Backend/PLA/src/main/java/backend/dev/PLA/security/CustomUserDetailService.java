package backend.dev.PLA.security;

import backend.dev.PLA.entities.User;
import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username)  {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        return new CustomUserDetail(user);
    }
}
