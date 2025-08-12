package org.devdaniel.clone.security;

import lombok.RequiredArgsConstructor;
import org.devdaniel.clone.entity.Profile;
import org.devdaniel.clone.repository.ProfileRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final ProfileRepository profileRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Profile user = profileRepository.findByName(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));

        return new CustomUserDetails(user);
    }
}