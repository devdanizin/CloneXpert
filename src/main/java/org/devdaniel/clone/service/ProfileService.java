package org.devdaniel.clone.service;

import org.devdaniel.clone.entity.Profile;
import org.devdaniel.clone.repository.ProfileRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProfileService {
    // Injeção de dependência do repositório
    private final ProfileRepository repository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(ProfileRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Profile> findAll() {
        return repository.findAll();
    }

    public Optional<Profile> findById(UUID id) {
        return repository.findById(id);
    }

    public Profile create(Profile profile) {
        if (profile.getName() == null || profile.getName().isBlank()) {
            throw new IllegalArgumentException("Nome de usuário obrigatório");
        }
        if (profile.getPassword() == null || profile.getPassword().isBlank()) {
            throw new IllegalArgumentException("Senha obrigatória");
        }
        if (profile.getCredit() == null) {
            throw new IllegalArgumentException("Crédito obrigatório");
        }
        if (repository.findByName(profile.getName()).isPresent()) {
            throw new IllegalStateException("Nome de usuário já existe");
        }
        profile.setPassword(passwordEncoder.encode(profile.getPassword()));
        return repository.save(profile);
    }

    public Optional<Profile> update(UUID id, Profile newProfile) {
        return repository.findById(id).map(profile -> {
            profile.setName(newProfile.getName());
            profile.setPassword(newProfile.getPassword());
            profile.setRole(newProfile.getRole());
            profile.setCredit(newProfile.getCredit());
            return repository.save(profile);
        });
    }

    public boolean delete(UUID id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<Profile> addCredits(UUID id, int creditsToAdd) {
        return repository.findById(id).map(profile -> {
            profile.setCredit(profile.getCredit() + creditsToAdd);
            return repository.save(profile);
        });
    }

    public boolean removeCredits(UUID id, int quantity) {
        if(quantity < 0) return false;
        return repository.findById(id).map(profile -> {
            profile.removeCredits(quantity);
            repository.save(profile);
            return true;
        }).orElse(false);
    }
}
