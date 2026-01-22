package pnascimento.dev.v1.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    public ResponseEntity<UserDto> login(@RequestParam String email, 
                                       @RequestParam String password) {
        return ResponseEntity.ok(authService.loginLocal(email, password));
    }
}
