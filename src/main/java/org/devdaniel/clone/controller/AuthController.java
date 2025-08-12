package org.devdaniel.clone.controller;

import lombok.RequiredArgsConstructor;
import org.devdaniel.clone.entity.Profile;
import org.devdaniel.clone.repository.ProfileRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping
    public ResponseEntity register(@RequestBody Profile profile) {
        if(profileRepository.findByName(profile.getName()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        profile.setPassword(passwordEncoder.encode(profile.getPassword()));
        profileRepository.save(profile);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity delete(@RequestBody Profile profile) {
        if(profileRepository.findByName(profile.getName()).isPresent()) {
            profileRepository.delete(profile);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

}
