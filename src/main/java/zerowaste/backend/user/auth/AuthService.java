package zerowaste.backend.user.auth;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import zerowaste.backend.email.EmailTemplateService;
import zerowaste.backend.email.MailService;
import zerowaste.backend.exception.classes.ConstraintException;
import zerowaste.backend.exception.classes.ExpiredTokenException;
import zerowaste.backend.user.User;
import zerowaste.backend.user.UserRepository;
import zerowaste.backend.user.auth.tokens.EmailVerificationToken;
import zerowaste.backend.user.auth.tokens.EmailVerificationTokenRepository;
import zerowaste.backend.user.auth.tokens.PasswordResetToken;
import zerowaste.backend.user.auth.tokens.PasswordResetTokenRepository;
import zerowaste.backend.user.dtos.RegisterUserDto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {


    @Value("${frontend.url}")
    private String frontendUrl;

    private final UserRepository userRepository;
    private final PasswordEncoder encoder ;
    private final EmailVerificationTokenRepository tokenRepository;
    private final PasswordResetTokenRepository passwordTokenRepository;
    private final MailService mailService;
    private final EmailTemplateService emailTemplateService;

    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder encoder,
                       EmailVerificationTokenRepository tokenRepository, MailService mailService,
                       EmailTemplateService emailTemplateService, PasswordResetTokenRepository passwordTokenRepository) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.tokenRepository = tokenRepository;
        this.mailService = mailService;
        this.emailTemplateService = emailTemplateService;
        this.passwordTokenRepository = passwordTokenRepository;
    }

    @Transactional
    public void registerNewUser(RegisterUserDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email deja folosit");
        }

        if(dto.getEmail().contains(dto.getPassword())){
            throw new ConstraintException("Parola nu trebuie să semene cu adresa de email!");
        }


        User u = new User();
        u.setEmail(dto.getEmail());
        u.setPassword(encoder.encode(dto.getPassword()));
        u.setVerified(false);
        userRepository.save(u);

        createAndSendVerificationToken(u, null);
    }

    private void createAndSendVerificationToken(User user, EmailVerificationToken token) {
        String tokenValue = UUID.randomUUID().toString();

        if(token != null) {
            token.setToken(tokenValue);
            token.setExpiresAt(LocalDateTime.now().plusDays(1));
            tokenRepository.save(token);
        }else {
             token = new EmailVerificationToken(
                    tokenValue,
                    user,
                    LocalDateTime.now().plusDays(1)
            );

            tokenRepository.save(token);
        }

        String confirmationLink = frontendUrl + "/successfully-created-account?token=" + tokenValue;

        String html = emailTemplateService.render(
                "mail/confirm-email",
                Map.of("confirmationLink", confirmationLink)
        );

        mailService.sendHtmlEmail(
                user.getEmail(),
                "Confirmă contul tău",
                html
        );
    }

    public void createResetToken(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String tokenValue = UUID.randomUUID().toString();

            PasswordResetToken token = new PasswordResetToken();
            token.setToken(tokenValue);
            token.setUser(user);
            token.setExpiresAt(LocalDateTime.now().plusHours(1));
            passwordTokenRepository.save(token);

            String resetLink = frontendUrl + "/set-new-password?token=" + tokenValue;

            String html = emailTemplateService.render(
                    "mail/reset-password",
                    Map.of("resetLink", resetLink)
            );

            mailService.sendHtmlEmail(
                    user.getEmail(),
                    "Resetează parola contului tău",
                    html
            );
        });
    }

    @Transactional
    public void confirmEmail(String tokenValue){
        EmailVerificationToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Token invalid"));
        if(token.isUsed()){
            throw new IllegalArgumentException("Email deja confirmat");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ExpiredTokenException("Token expirat");
        }

        User user = token.getUser();
        user.setVerified(true);
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);
    }

    @Transactional
    public void resendConfirmationEmail(String tokenValue){
        EmailVerificationToken token  = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Token invalid"));
        if(token.isUsed()){
            throw new IllegalArgumentException("Email deja confirmat");
        }
        User user = token.getUser();

        createAndSendVerificationToken(user, token);
    }

    @Transactional
    public void changePassword(User user,String newPassword, String confirmNewPassword){
        if(!newPassword.equals(confirmNewPassword)){
            throw new  IllegalArgumentException("Parolele nu coincid");
        }

        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(String tokenValue, String newPassword, String confirmNewPassword){
        PasswordResetToken token = passwordTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Token invalid"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ExpiredTokenException("Token expirat");
        }

        if(token.isUsed()){
            throw new IllegalArgumentException("Email deja confirmat");
        }

        if(!newPassword.equals(confirmNewPassword)){
            throw new IllegalArgumentException("parolele nu coincid");
        }

        User user = token.getUser();
        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);

        token.setUsed(true);
        passwordTokenRepository.save(token);
    }


}
