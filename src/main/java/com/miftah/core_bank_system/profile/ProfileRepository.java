package com.miftah.core_bank_system.profile;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID> {
    Optional<Profile> findByUserId(UUID userId);

    boolean existsByIdentityNumber(String identityNumber);

    boolean existsByPhone(String phone);

    boolean existsByIdentityNumberAndIdNot(String identityNumber, UUID id);

    boolean existsByPhoneAndIdNot(String phone, UUID id);
}
