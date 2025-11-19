package com.threadspace.backend.user;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class UsersService {

    private final UserRepository userRepository;

    public UsersService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User upsertGoogleUser(GoogleProfile profile) {

        return userRepository.findByProviderId(profile.providerId())
                .map(existing -> {
                    existing.setEmail(profile.email());
                    if (profile.name() != null) {
                        existing.setName(profile.name());
                    }
                    existing.setUpdatedAt(OffsetDateTime.now());
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    User user = new User();
                    user.setProvider("google");
                    user.setProviderId(profile.providerId());
                    user.setEmail(profile.email());
                    user.setName(profile.name());
                    user.setCreatedAt(OffsetDateTime.now());
                    user.setUpdatedAt(OffsetDateTime.now());
                    return userRepository.save(user);
                });
    }
}
