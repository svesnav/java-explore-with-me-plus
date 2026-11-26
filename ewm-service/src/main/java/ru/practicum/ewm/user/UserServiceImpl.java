package ru.practicum.ewm.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.user.dto.NewUserRequest;
import ru.practicum.ewm.user.dto.UserDto;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto create(NewUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("User with email=" + request.getEmail() + " already exists");
        }
        User saved = userRepository.save(UserMapper.toUser(request));
        return UserMapper.toUserDto(saved);
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        // from это сколько пропустить
        Pageable page = PageRequest.of(from / size, size, Sort.by("id"));

        Page<User> users;
        if (ids == null || ids.isEmpty()) {
            users = userRepository.findAll(page);
        } else {
            users = userRepository.findAllByIdIn(ids, page);
        }

        return users.stream()
                .map(UserMapper::toUserDto)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
        userRepository.deleteById(userId);
    }
}
