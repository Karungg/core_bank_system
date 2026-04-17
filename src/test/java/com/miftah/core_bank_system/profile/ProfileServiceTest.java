package com.miftah.core_bank_system.profile;

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

import com.miftah.core_bank_system.exception.DuplicateResourceException;
import com.miftah.core_bank_system.exception.ResourceNotFoundException;
import com.miftah.core_bank_system.user.User;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private User user;
    private ProfileRequest request;
    private Profile profile;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .build();

        request = ProfileRequest.builder()
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

        profile = Profile.builder()
                .id(UUID.randomUUID())
                .user(user)
                .identityNumber("1234567890123456")
                .phone("08123456789")
                .build();
    }



    @Test
    void create_Success() {
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(profileRepository.existsByIdentityNumber(anyString())).thenReturn(false);
        when(profileRepository.existsByPhone(anyString())).thenReturn(false);
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> {
            Profile savedProfile = invocation.getArgument(0);
            savedProfile.setId(UUID.randomUUID());
            return savedProfile;
        });

        ProfileResponse response = profileService.create(user, request);

        assertNotNull(response);
        assertEquals(request.getIdentityNumber(), response.getIdentityNumber());
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    void create_UserAlreadyHasProfile_ThrowsException() {
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () -> {
            profileService.create(user, request);
        });

        assertEquals("user", exception.getField());
        verify(profileRepository, never()).save(any());
    }

    @Test
    void create_DuplicateIdentityNumberAndPhone_ThrowsBatchException() {
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(profileRepository.existsByIdentityNumber(request.getIdentityNumber())).thenReturn(true);
        when(profileRepository.existsByPhone(request.getPhone())).thenReturn(true);

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () -> {
            profileService.create(user, request);
        });

        assertNotNull(exception.getErrors());
        assertTrue(exception.getErrors().containsKey("identityNumber"));
        assertTrue(exception.getErrors().containsKey("phone"));
        verify(profileRepository, never()).save(any());
    }

    @Test
    void create_DuplicateIdentityNumberOnly_ThrowsException() {
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(profileRepository.existsByIdentityNumber(request.getIdentityNumber())).thenReturn(true);
        when(profileRepository.existsByPhone(request.getPhone())).thenReturn(false);

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () -> {
            profileService.create(user, request);
        });

        assertNotNull(exception.getErrors());
        assertTrue(exception.getErrors().containsKey("identityNumber"));
        assertFalse(exception.getErrors().containsKey("phone"));
        verify(profileRepository, never()).save(any());
    }

    @Test
    void create_DuplicatePhoneOnly_ThrowsException() {
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(profileRepository.existsByIdentityNumber(request.getIdentityNumber())).thenReturn(false);
        when(profileRepository.existsByPhone(request.getPhone())).thenReturn(true);

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () -> {
            profileService.create(user, request);
        });

        assertNotNull(exception.getErrors());
        assertFalse(exception.getErrors().containsKey("identityNumber"));
        assertTrue(exception.getErrors().containsKey("phone"));
        verify(profileRepository, never()).save(any());
    }



    @Test
    void get_Success() {
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));

        ProfileResponse response = profileService.get(user);

        assertNotNull(response);
        assertEquals(profile.getIdentityNumber(), response.getIdentityNumber());
    }

    @Test
    void get_NotFound_ThrowsException() {
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            profileService.get(user);
        });

        assertEquals("userId", exception.getFieldName());
    }



    @Test
    void update_Success() {
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));
        when(profileRepository.existsByIdentityNumberAndIdNot(anyString(), any(UUID.class))).thenReturn(false);
        when(profileRepository.existsByPhoneAndIdNot(anyString(), any(UUID.class))).thenReturn(false);
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);

        ProfileRequest updateRequest = request;
        updateRequest.setName("Jane Doe");

        ProfileResponse response = profileService.update(user, updateRequest);

        assertNotNull(response);
        assertEquals("Jane Doe", response.getName());
        verify(profileRepository).save(profile);
    }

    @Test
    void update_ProfileNotFound_ThrowsException() {
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            profileService.update(user, request);
        });

        assertEquals("userId", exception.getFieldName());
        verify(profileRepository, never()).save(any());
    }

    @Test
    void update_DuplicateFields_ThrowsBatchException() {
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));

        // Simulate changing fields to conflicting values
        ProfileRequest updateRequest = request;
        updateRequest.setIdentityNumber("9999999999999999");
        updateRequest.setPhone("08999999999");

        when(profileRepository.existsByIdentityNumberAndIdNot(updateRequest.getIdentityNumber(), profile.getId()))
                .thenReturn(true);
        when(profileRepository.existsByPhoneAndIdNot(updateRequest.getPhone(), profile.getId())).thenReturn(true);

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () -> {
            profileService.update(user, updateRequest);
        });

        assertNotNull(exception.getErrors());
        assertTrue(exception.getErrors().containsKey("identityNumber"));
        assertTrue(exception.getErrors().containsKey("phone"));
        verify(profileRepository, never()).save(any());
    }



    @Test
    void getAll_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Profile> page = new PageImpl<>(
                List.of(profile));

        when(profileRepository.findAll(pageable)).thenReturn(page);

        Page<ProfileResponse> response = profileService.getAll(pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(profile.getIdentityNumber(), response.getContent().get(0).getIdentityNumber());
    }

    @Test
    void getAll_Empty_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Profile> page = new PageImpl<>(List.of());

        when(profileRepository.findAll(pageable)).thenReturn(page);

        Page<ProfileResponse> response = profileService.getAll(pageable);

        assertNotNull(response);
        assertEquals(0, response.getTotalElements());
        assertTrue(response.getContent().isEmpty());
    }



    @Test
    void getById_Success() {
        UUID profileId = UUID.randomUUID();
        profile.setId(profileId);

        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));

        ProfileResponse response = profileService.getById(profileId);

        assertNotNull(response);
        assertEquals(profile.getIdentityNumber(), response.getIdentityNumber());
    }

    @Test
    void getById_NotFound_ThrowsException() {
        UUID profileId = UUID.randomUUID();
        when(profileRepository.findById(profileId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> profileService.getById(profileId));
    }
}
