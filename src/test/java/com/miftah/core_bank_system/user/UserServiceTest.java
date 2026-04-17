package com.miftah.core_bank_system.user;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.miftah.core_bank_system.auth.RegisterRequest;
import com.miftah.core_bank_system.exception.DuplicateResourceException;
import com.miftah.core_bank_system.exception.ResourceNotFoundException;
import com.miftah.core_bank_system.profile.Gender;
import com.miftah.core_bank_system.profile.ProfileRequest;
import com.miftah.core_bank_system.profile.ProfileResponse;
import com.miftah.core_bank_system.profile.ProfileService;
import com.miftah.core_bank_system.profile.ProfileType;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private UserServiceImpl userService;

    private RegisterRequest registerRequest;
    private CreateUserWithProfileRequest createUserWithProfileRequest;
    private ProfileRequest profileRequest;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .username("adminuser")
                .password("password")
                .build();

        profileRequest = ProfileRequest.builder()
                .type(ProfileType.KTP)
                .identityNumber("1234567890123456")
                .name("John Doe")
                .country("Indonesia")
                .placeOfBirth("Jakarta")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .phone("08123456789")
                .nationality("Indonesia")
                .build();

        createUserWithProfileRequest = CreateUserWithProfileRequest.builder()
                .user(RegisterRequest.builder()
                        .username("newuser")
                        .password("password")
                        .build())
                .profile(profileRequest)
                .build();
    }

    @Test
    void createAdmin_Success() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        UserResponse response = userService.createAdmin(registerRequest);

        assertNotNull(response);
        assertEquals(Role.ADMIN, response.getRole());
        assertEquals(registerRequest.getUsername(), response.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createAdmin_DuplicateUsername_ThrowsException() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.createAdmin(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_Success() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        UserResponse response = userService.createUser(registerRequest);

        assertNotNull(response);
        assertEquals(Role.USER, response.getRole());
        assertEquals(registerRequest.getUsername(), response.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_DuplicateUsername_ThrowsException() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.createUser(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUserWithProfile_Success() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(profileService.create(any(User.class), any(ProfileRequest.class)))
                .thenReturn(ProfileResponse.builder().build());

        UserResponse response = userService.createUserWithProfile(createUserWithProfileRequest);

        assertNotNull(response);
        assertEquals(Role.USER, response.getRole());
        assertEquals(createUserWithProfileRequest.getUser().getUsername(), response.getUsername());
        verify(userRepository).save(any(User.class));
        verify(profileService).create(any(User.class), any(ProfileRequest.class));
    }

    @Test
    void createUserWithProfile_DuplicateUsername_ThrowsException() {
        when(userRepository.existsByUsername(createUserWithProfileRequest.getUser().getUsername())).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> userService.createUserWithProfile(createUserWithProfileRequest));
        verify(userRepository, never()).save(any(User.class));
        verify(profileService, never()).create(any(User.class), any(ProfileRequest.class));
    }

    @Test
    void updateUser_Success() {
        UUID userId = UUID.randomUUID();
        User existingUser = User.builder().id(userId).username("olduser").role(Role.USER).build();
        UpdateUserRequest updateRequest = UpdateUserRequest.builder().username("newusername").password("newpassword").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUsernameAndIdNot(updateRequest.getUsername(), userId)).thenReturn(false);
        when(passwordEncoder.encode("newpassword")).thenReturn("encodedPassword");

        UserResponse response = userService.updateUser(userId, updateRequest);

        assertNotNull(response);
        assertEquals(updateRequest.getUsername(), response.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_UserNotFound_ThrowsException() {
        UUID userId = UUID.randomUUID();
        UpdateUserRequest updateRequest = UpdateUserRequest.builder().username("newusername").build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.updateUser(userId, updateRequest));
    }

    @Test
    void updateUser_DuplicateUsername_ThrowsException() {
        UUID userId = UUID.randomUUID();
        User existingUser = User.builder().id(userId).username("olduser").role(Role.USER).build();
        UpdateUserRequest updateRequest = UpdateUserRequest.builder().username("newusername").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUsernameAndIdNot(updateRequest.getUsername(), userId)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.updateUser(userId, updateRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateAdmin_Success() {
        UUID userId = UUID.randomUUID();
        User existingUser = User.builder().id(userId).username("olduser").role(Role.ADMIN).build();
        UpdateUserRequest updateRequest = UpdateUserRequest.builder().username("newusername").password("newpassword").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUsernameAndIdNot(updateRequest.getUsername(), userId)).thenReturn(false);
        when(passwordEncoder.encode("newpassword")).thenReturn("encodedPassword");

        UserResponse response = userService.updateAdmin(userId, updateRequest);

        assertNotNull(response);
        assertEquals(updateRequest.getUsername(), response.getUsername());
        verify(userRepository).save(existingUser);
    }

    @Test
    void updateAdmin_DuplicateUsername_ThrowsException() {
        UUID userId = UUID.randomUUID();
        User existingUser = User.builder().id(userId).username("olduser").role(Role.ADMIN).build();
        UpdateUserRequest updateRequest = UpdateUserRequest.builder().username("newusername").password("newpassword").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUsernameAndIdNot(updateRequest.getUsername(), userId)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.updateAdmin(userId, updateRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateAdmin_UserNotFound_ThrowsException() {
        UUID userId = UUID.randomUUID();
        UpdateUserRequest updateRequest = UpdateUserRequest.builder().username("newusername").build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateAdmin(userId, updateRequest));
    }

    @Test
    void deleteAdmin_Success() {
        UUID userId = UUID.randomUUID();
        User existingUser = User.builder().id(userId).username("todelete").role(Role.ADMIN).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        userService.deleteAdmin(userId);

        verify(userRepository).delete(existingUser);
    }

    @Test
    void deleteAdmin_UserNotFound_ThrowsException() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.deleteAdmin(userId));
    }

    @Test
    void getAll_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        User user = User.builder().id(UUID.randomUUID()).username("user").role(Role.USER).build();
        Page<User> page = new PageImpl<>(List.of(user));

        when(userRepository.findAll(pageable)).thenReturn(page);

        Page<UserResponse> response = userService.getAll(pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals("user", response.getContent().get(0).getUsername());
    }

    @Test
    void getById_Success() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("user").role(Role.USER).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse response = userService.getById(userId);

        assertNotNull(response);
        assertEquals("user", response.getUsername());
    }

    @Test
    void getById_NotFound_ThrowsException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getById(userId));
    }
}
