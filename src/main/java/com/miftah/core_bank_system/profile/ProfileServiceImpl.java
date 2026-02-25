package com.miftah.core_bank_system.profile;

import com.miftah.core_bank_system.exception.DuplicateResourceException;
import com.miftah.core_bank_system.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;

    @Override
    @Transactional
    public ProfileResponse create(User user, ProfileRequest request) {
        log.info("Creating profile for user: {}", user.getUsername());

        if (profileRepository.findByUserId(user.getId()).isPresent()) {
            throw new DuplicateResourceException("user", "User already has a profile");
        }

        Map<String, String> errors = new HashMap<>();

        if (profileRepository.existsByIdentityNumber(request.getIdentityNumber())) {
            errors.put("identityNumber", "error.profile.identityNumber.duplicate");
        }

        if (profileRepository.existsByPhone(request.getPhone())) {
            errors.put("phone", "error.profile.phone.duplicate");
        }

        if (!errors.isEmpty()) {
            throw new DuplicateResourceException(errors);
        }

        Profile profile = Profile.builder()
                .user(user)
                .type(request.getType())
                .expiryDate(request.getExpiryDate())
                .identityNumber(request.getIdentityNumber())
                .name(request.getName())
                .country(request.getCountry())
                .placeOfBirth(request.getPlaceOfBirth())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .phone(request.getPhone())
                .nationality(request.getNationality())
                .maritalStatus(request.getMaritalStatus())
                .religion(request.getReligion())
                .address(request.getAddress())
                .building(request.getBuilding())
                .rt(request.getRt())
                .rw(request.getRw())
                .province(request.getProvince())
                .occupation(request.getOccupation())
                .officeName(request.getOfficeName())
                .officeAddress(request.getOfficeAddress())
                .officePhone(request.getOfficePhone())
                .build();

        profileRepository.save(profile);
        log.info("Profile created successfully for user: {}", user.getUsername());

        return toProfileResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse get(User user) {
        log.info("Fetching profile for user: {}", user.getUsername());
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        return toProfileResponse(profile);
    }

    @Override
    @Transactional
    public ProfileResponse update(User user, ProfileRequest request) {
        log.info("Updating profile for user: {}", user.getUsername());
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));

        Map<String, String> errors = new HashMap<>();

        if (profileRepository.existsByIdentityNumberAndIdNot(request.getIdentityNumber(), profile.getId())) {
            errors.put("identityNumber", "error.profile.identityNumber.duplicate");
        }

        if (profileRepository.existsByPhoneAndIdNot(request.getPhone(), profile.getId())) {
            errors.put("phone", "error.profile.phone.duplicate");
        }

        if (!errors.isEmpty()) {
            throw new DuplicateResourceException(errors);
        }

        profile.setType(request.getType());
        profile.setExpiryDate(request.getExpiryDate());
        profile.setIdentityNumber(request.getIdentityNumber());
        profile.setName(request.getName());
        profile.setCountry(request.getCountry());
        profile.setPlaceOfBirth(request.getPlaceOfBirth());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setGender(request.getGender());
        profile.setPhone(request.getPhone());
        profile.setNationality(request.getNationality());
        profile.setMaritalStatus(request.getMaritalStatus());
        profile.setReligion(request.getReligion());
        profile.setAddress(request.getAddress());
        profile.setBuilding(request.getBuilding());
        profile.setRt(request.getRt());
        profile.setRw(request.getRw());
        profile.setProvince(request.getProvince());
        profile.setOccupation(request.getOccupation());
        profile.setOfficeName(request.getOfficeName());
        profile.setOfficeAddress(request.getOfficeAddress());
        profile.setOfficePhone(request.getOfficePhone());

        profileRepository.save(profile);
        log.info("Profile updated successfully for user: {}", user.getUsername());

        return toProfileResponse(profile);
    }

    private ProfileResponse toProfileResponse(Profile profile) {
        return ProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .type(profile.getType())
                .expiryDate(profile.getExpiryDate())
                .identityNumber(profile.getIdentityNumber())
                .name(profile.getName())
                .country(profile.getCountry())
                .placeOfBirth(profile.getPlaceOfBirth())
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender())
                .phone(profile.getPhone())
                .nationality(profile.getNationality())
                .maritalStatus(profile.getMaritalStatus())
                .religion(profile.getReligion())
                .address(profile.getAddress())
                .building(profile.getBuilding())
                .rt(profile.getRt())
                .rw(profile.getRw())
                .province(profile.getProvince())
                .documentPhoto(profile.getDocumentPhoto())
                .occupation(profile.getOccupation())
                .officeName(profile.getOfficeName())
                .officeAddress(profile.getOfficeAddress())
                .officePhone(profile.getOfficePhone())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProfileResponse> getAll(Pageable pageable) {
        return profileRepository.findAll(pageable)
                .map(this::toProfileResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getById(UUID id) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        return toProfileResponse(profile);
    }
}
