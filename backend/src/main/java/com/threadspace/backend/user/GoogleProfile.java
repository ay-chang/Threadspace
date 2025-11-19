package com.threadspace.backend.user;

public record GoogleProfile(
        String providerId,
        String email,
        String name) {
}
