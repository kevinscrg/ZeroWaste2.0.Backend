package zerowaste.backend.user.auth;


import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import zerowaste.backend.security.AppUserDetails;
import zerowaste.backend.security.JwtService;
import zerowaste.backend.user.User;
import zerowaste.backend.user.UserRepository;
import zerowaste.backend.user.dtos.LoginDto;
import zerowaste.backend.user.dtos.RegisterUserDto;

import java.time.Duration;
import java.util.Map;

record AuthResponse(String access, String refresh) {}

@RestController
@RequestMapping("/auth")
public class AuthController {

    public record changePassword(String oldPassword, String newPassword, String confirmNewPassword) {}
    public record ForgotPasswordRequest(String email) {}
    public record ResetPasswordRequest(
            String token,
            String newPassword,
            String confirmNewPassword
    ) {}


    private final AuthService authService;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService,
                          AuthenticationManager authManager,
                          JwtService jwtService,
                          UserRepository userRepository) {
        this.authService = authService;
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterUserDto dto) {
        try {
            authService.registerNewUser(dto);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> confirmEmail(@RequestParam("token") String tokenValue) {
        authService.confirmEmail(tokenValue);
        return ResponseEntity.status(200).build();
    }

    @GetMapping("/resend-confirm-token")
    public ResponseEntity<?> resendConfirmToken(@RequestParam("token") String tokenValue) {
        authService.resendConfirmationEmail(tokenValue);
        return ResponseEntity.status(200).build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDto loginDto, HttpServletResponse response) {

        if(!userRepository.existsByEmail(loginDto.getEmail())) {
            throw new IllegalArgumentException("Nu există niciun cont creat cu această adresă de email.");
        }

        authManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginDto.getEmail(), loginDto.getPassword()
        ));

        User user = userRepository.findByEmail(loginDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Nu există niciun cont creat cu această adresă de email."));


        String access = jwtService.generateAccessToken(
                loginDto.getEmail(),
                Map.of("typ", "access")
        );

        String refresh = jwtService.generateRefreshToken(
                loginDto.getEmail(),
                Map.of()
        );

        ResponseCookie rc = ResponseCookie.from("refreshToken", refresh)
                .httpOnly(true)
                .secure(false)
                .path("/auth/refresh")
                .sameSite("Lax")
                .maxAge(Duration.ofDays(14))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, rc.toString())
                .body(new AuthResponse(access, refresh));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/auth/refresh")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshFromCookie(@CookieValue(value = "refreshToken", required = false) String refresh) {
        if (refresh == null || refresh.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Lipseste cookie-ul de refresh");
        }
        String subject = jwtService.extractSubject(refresh, true);


        User user = userRepository.findByEmail(subject)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User inexistent"));


        if (!jwtService.isTokenValid(refresh, subject, true)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalid");
        }
        String newAccess = jwtService.generateAccessToken(
                subject,
                Map.of("typ","access")
        );
        String newRefresh = jwtService.generateRefreshToken(
                subject,
                Map.of()
        );

        ResponseCookie rc = ResponseCookie.from("refreshToken", newRefresh)
                .httpOnly(true)
                .secure(false)
                .path("/auth/refresh")
                .maxAge(Duration.ofDays(14))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, rc.toString())
                .body(new AuthResponse(newAccess, rc.toString()));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal AppUserDetails me,
            @RequestBody changePassword request){
        authManager.authenticate(new UsernamePasswordAuthenticationToken(
                me.getDomainUser().getEmail(), request.oldPassword()));

        authService.changePassword(me.getDomainUser(), request.newPassword(), request.confirmNewPassword());
        return  ResponseEntity.ok().build();
    }


    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        authService.createResetToken(forgotPasswordRequest.email());

        return ResponseEntity.ok().body(Map.of(
                "message", "Dacă există un cont cu acest email, vei primi un link de resetare a parolei. Acesta este valabil timp de o oră."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest resetPasswordRequest) {
        authService.resetPassword(
                resetPasswordRequest.token(),
                resetPasswordRequest.newPassword(),
                resetPasswordRequest.confirmNewPassword()
        );

        return ResponseEntity.ok().body(Map.of(
                "message", "Parola a fost resetată cu succes."
        ));
    }


}
