package pnascimento.dev.v1.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pnascimento.dev.v1.dto.user.UserDto;
import pnascimento.dev.v1.exception.AuthException;
import pnascimento.dev.v1.mapper.user.UserMapper;
import pnascimento.dev.v1.repository.user.UserRepository;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;


    @Transactional(readOnly = true)
    public UserDto findById(Long id) {
        return userRepository.findById(id)
                .map(UserMapper::toDto)
                .orElseThrow(() -> new RuntimeException("User not found / Usuário não encontrado"));
    }


    @Transactional
    public void deactivateUser(Long id) {
        userRepository.findById(id).ifPresent(user -> {
            user.setIsActive(false);
            userRepository.save(user);
        });
    }
}
