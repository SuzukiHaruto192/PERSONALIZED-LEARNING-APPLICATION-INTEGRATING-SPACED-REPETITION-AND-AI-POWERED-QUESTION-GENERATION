package backend.dev.PLA.security;

import backend.dev.PLA.entities.User;
import backend.dev.PLA.exceptions.DataNotFoundException;
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
    public UserDetails loadUserByUsername(String username) throws DataNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new DataNotFoundException("user not found");
        }
        return new CustomUserDetail(user);
    }
}
