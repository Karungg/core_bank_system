package com.miftah.core_bank_system.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    Optional<User> findById(UUID id);

    boolean existsByUsernameAndIdNot(String username, UUID id);

    boolean existsByUsername(String username);
}
