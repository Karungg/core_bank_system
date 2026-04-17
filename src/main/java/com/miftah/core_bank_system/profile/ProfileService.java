package com.miftah.core_bank_system.profile;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.miftah.core_bank_system.user.User;

public interface ProfileService {
    ProfileResponse create(User user, ProfileRequest request);

    ProfileResponse get(User user);

    ProfileResponse update(User user, ProfileRequest request);

    Page<ProfileResponse> getAll(Pageable pageable);

    ProfileResponse getById(UUID id);
}
