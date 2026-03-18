package com.miftah.core_bank_system.user;

import com.miftah.core_bank_system.auth.RegisterRequest;
import com.miftah.core_bank_system.exception.DuplicateResourceException;
import com.miftah.core_bank_system.exception.ResourceNotFoundException;
import com.miftah.core_bank_system.profile.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final ProfileService profileService;

    @Override
    public Page<UserResponse> getAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toUserResponse);
    }

    @Override
    public UserResponse getById(UUID id) {
        return toUserResponse(findUserByIdOrThrow(id));
    }

    @Override
    @Transactional
    public UserResponse createUser(RegisterRequest request) {
        return createUserWithRole(request, Role.USER);
    }
    
    @Override
    @Transactional
    public UserResponse createAdmin(RegisterRequest request) {
        return createUserWithRole(request, Role.ADMIN);
    }

    @Override
    @Transactional
    public UserResponse createUserWithProfile(CreateUserWithProfileRequest request) {
        log.info("Creating user with profile: {}", request.getUser().getUsername());

        validateUniqueUsername(request.getUser().getUsername());

        User user = User.builder()
                .username(request.getUser().getUsername())
                .password(passwordEncoder.encode(request.getUser().getPassword()))
                .role(Role.USER)
                .build();

        userRepository.save(user);
        profileService.create(user, request.getProfile());

        log.info("User with profile created successfully: {}", user.getId());
        return toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        return updateUserById(id, request);
    }

    @Override
    @Transactional
    public UserResponse updateAdmin(UUID id, UpdateUserRequest request) {
        return updateUserById(id, request);
    }

    @Override
    @Transactional
    public void deleteAdmin(UUID id) {
        log.info("Deleting admin user: {}", id);

        User user = findUserByIdOrThrow(id);
        userRepository.delete(user);

        log.info("Admin user deleted successfully: {}", id);
    }

    // ========== Private Helpers ==========

    private UserResponse createUserWithRole(RegisterRequest request, Role role) {
        log.info("Creating {} user: {}", role, request.getUsername());

        validateUniqueUsername(request.getUsername());

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();
        userRepository.save(user);

        log.info("{} user created successfully: {}", role, user.getId());
        return toUserResponse(user);
    }

    private UserResponse updateUserById(UUID id, UpdateUserRequest request) {
        log.info("Updating user: {}", id);

        User user = findUserByIdOrThrow(id);
        validateUniqueUsername(request.getUsername(), id);

        user.setUsername(request.getUsername());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        userRepository.save(user);

        log.info("User updated successfully: {}", user.getId());
        return toUserResponse(user);
    }

    private User findUserByIdOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private void validateUniqueUsername(String username) {
        if (userRepository.existsByUsername(username)) {
            log.warn("Username already exists: {}", username);
            throw new DuplicateResourceException("username", "error.username.duplicate");
        }
    }

    private void validateUniqueUsername(String username, UUID excludeId) {
        if (userRepository.existsByUsernameAndIdNot(username, excludeId)) {
            log.warn("Username already exists: {}", username);
            throw new DuplicateResourceException("username", "error.username.duplicate");
        }
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
