package org.devdaniel.clone.repository;

import org.devdaniel.clone.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {
    Optional<Profile> findByName(String name);
}