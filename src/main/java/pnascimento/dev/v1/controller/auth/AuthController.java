
package pnascimento.dev.v1.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pnascimento.dev.v1.dto.user.AuthResponseDto;
import pnascimento.dev.v1.dto.user.UserDto;
import pnascimento.dev.v1.service.auth.AuthService;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for login and registration")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new local user")
    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestParam String email,
                                            @RequestParam String password,
                                            @RequestParam String fullName) {
        return ResponseEntity.ok(authService.registerLocal(email, password, fullName));
    }

    @Operation(summary = "Local login with email and password")
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestParam String email,
                                                 @RequestParam String password) {
        return ResponseEntity.ok(authService.loginLocal(email, password));
    }

    @Operation(summary = "Logout and revoke token")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        // Remove "Bearer " prefix if present
        String token = authHeader.startsWith("Bearer ")
                ? authHeader.substring(7)
                : authHeader;
        authService.logoutLocal(token);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Logout from all sessions")
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@RequestParam Long userId) {
        authService.logoutAllSessions(userId);
        return ResponseEntity.noContent().build();
    }
}