package com.miftah.core_bank_system.user;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.miftah.core_bank_system.auth.RegisterRequest;

public interface UserService {

    Page<UserResponse> getAll(Pageable pageable);

    UserResponse getById(UUID id);

    UserResponse createUser(RegisterRequest request);

    UserResponse createAdmin(RegisterRequest request);

    UserResponse createUserWithProfile(CreateUserWithProfileRequest request);

    UserResponse updateUser(UUID id, UpdateUserRequest request);

    UserResponse updateAdmin(UUID id, UpdateUserRequest request);

    void deleteAdmin(UUID id);
}
