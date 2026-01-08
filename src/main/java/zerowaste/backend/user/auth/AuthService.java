package zerowaste.backend.user.auth;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import zerowaste.backend.email.EmailTemplateService;
import zerowaste.backend.email.MailService;
import zerowaste.backend.exception.classes.ConstraintException;
import zerowaste.backend.exception.classes.ExpiredTokenException;
import zerowaste.backend.product.models.UserProductList;
import zerowaste.backend.product.repos.UserProductListRepository;
import zerowaste.backend.security.AppUserDetails;
import zerowaste.backend.user.User;
import zerowaste.backend.user.UserRepository;
import zerowaste.backend.user.auth.tokens.EmailVerificationToken;
import zerowaste.backend.user.auth.tokens.EmailVerificationTokenRepository;
import zerowaste.backend.user.auth.tokens.PasswordResetToken;
import zerowaste.backend.user.auth.tokens.PasswordResetTokenRepository;
import zerowaste.backend.user.dtos.RegisterUserDto;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {


    @Value("${frontend.url}")
    private String frontendUrl;

    private static final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final SecureRandom random = new SecureRandom();

    private final UserRepository userRepository;
    private final UserProductListRepository userProductListRepository;
    private final PasswordEncoder encoder ;
    private final EmailVerificationTokenRepository tokenRepository;
    private final PasswordResetTokenRepository passwordTokenRepository;
    private final MailService mailService;
    private final EmailTemplateService emailTemplateService;

    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder encoder,
                       EmailVerificationTokenRepository tokenRepository, MailService mailService,
                       EmailTemplateService emailTemplateService, PasswordResetTokenRepository passwordTokenRepository,
                       UserProductListRepository  userProductListRepository) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.tokenRepository = tokenRepository;
        this.mailService = mailService;
        this.emailTemplateService = emailTemplateService;
        this.passwordTokenRepository = passwordTokenRepository;
        this.userProductListRepository = userProductListRepository;
    }

    @Transactional
    public void registerNewUser(RegisterUserDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("The email already exists!");
        }

        if(dto.getEmail().contains(dto.getPassword())){
            throw new ConstraintException("Password should not be included in your email!");
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

        String confirmationLink = frontendUrl + "successfully-created-account?token=" + tokenValue;

        String html = emailTemplateService.render(
                "mail/confirm-email",
                Map.of("confirmationLink", confirmationLink)
        );

        mailService.sendHtmlEmail(
                user.getEmail(),
                "Verify account",
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

            String resetLink = frontendUrl + "set-new-password?token=" + tokenValue;

            String html = emailTemplateService.render(
                    "mail/reset-password",
                    Map.of("resetLink", resetLink)
            );

            mailService.sendHtmlEmail(
                    user.getEmail(),
                    "Reset your password",
                    html
            );
        });
    }

    public String generateUniqueShareCode() {
        StringBuilder sb = new StringBuilder(6);

        while (true) {
            sb.setLength(0);
            for (int i = 0; i < 6; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }

            String shareCode = sb.toString();
            if (!userProductListRepository.existsByShareCode(shareCode)) {
                return shareCode;
            }
        }
    }

    @Transactional
    public void confirmEmail(String tokenValue){
        EmailVerificationToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Token invalid"));
        if(token.isUsed()){
            throw new IllegalArgumentException("Account already verified!");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ExpiredTokenException("Token expired");
        }

        User user = token.getUser();
        user.setVerified(true);

        String shCode = generateUniqueShareCode();
        UserProductList userProductList = new UserProductList();
        userProductList.setShare_code(shCode);
        userProductList.getCollaborators().add(user);

        userProductListRepository.save(userProductList);

        user.setUserProductList(userProductList);


        userRepository.save(user);

        tokenRepository.delete(token);
    }

    @Transactional
    public void resendConfirmationEmail(String tokenValue){
        EmailVerificationToken token  = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token!"));

        User user = token.getUser();

        if(token.isUsed() || user.isVerified()){
            throw new IllegalArgumentException("Account already verified!");
        }

        createAndSendVerificationToken(user, token);
    }

    @Transactional
    public void changePassword(User user,String newPassword, String confirmNewPassword){
        if(!newPassword.equals(confirmNewPassword)){
            throw new  IllegalArgumentException("Passwords do not match!");
        }

        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(String tokenValue, String newPassword, String confirmNewPassword){
        PasswordResetToken token = passwordTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Token invalid"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ExpiredTokenException("Token expired");
        }

        if(token.isUsed()){
            throw new IllegalArgumentException("You already changed your password using this link!");
        }

        if(!newPassword.equals(confirmNewPassword)){
            throw new IllegalArgumentException("Passwords do not match!");
        }

        User user = token.getUser();
        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);

        passwordTokenRepository.delete(token);
    }

    @Transactional
    public void deleteAccount(AppUserDetails me){

        User user = userRepository.findById(me.getDomainUser().getId()).orElseThrow();

        UserProductList list = user.getUserProductList();
        if (list != null) {
            list.getCollaborators().remove(user);

            if (list.getCollaborators().isEmpty()) {
                userProductListRepository.delete(list);
            }
        }
        userRepository.delete(user);
    }

}
