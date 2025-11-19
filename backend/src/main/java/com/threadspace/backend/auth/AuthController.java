package com.threadspace.backend.auth;

import com.threadspace.backend.user.GoogleProfile;
import com.threadspace.backend.user.User;
import com.threadspace.backend.user.UsersService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsersService usersService;

    @Value("${app.internal-sync-token}")
    private String internalSyncToken;

    public AuthController(UsersService usersService) {
        this.usersService = usersService;
    }

    public record AuthResponse(Object id, String email) {
    }

    @PostMapping("/google/upsert")
    public ResponseEntity<AuthResponse> googleUpsert(
            @RequestBody GoogleProfile body,
            @RequestHeader(name = "x-internal-token", required = false) String token) {
        if (token == null || !token.equals(internalSyncToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = usersService.upsertGoogleUser(body);
        return ResponseEntity.ok(new AuthResponse(user.getId(), user.getEmail()));
    }
}
