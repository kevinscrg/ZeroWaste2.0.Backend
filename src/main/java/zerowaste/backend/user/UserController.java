package zerowaste.backend.user;

import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import zerowaste.backend.product.models.UserProductList;
import zerowaste.backend.product.repos.UserProductListRepository;
import zerowaste.backend.security.AppUserDetails;
import zerowaste.backend.user.dtos.UserDto;
import zerowaste.backend.user.properties.Allergy;
import zerowaste.backend.user.properties.AllergyRepository;
import zerowaste.backend.user.properties.Preference;
import zerowaste.backend.user.properties.PreferenceRepository;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepository;
    private final UserProductListRepository  userProductListRepository;
    private final AllergyRepository allergyRepository;
    private final PreferenceRepository preferenceRepository;

    public UserController(UserRepository userRepository,  AllergyRepository allergyRepository, PreferenceRepository preferenceRepository,
                          UserProductListRepository  userProductListRepository) {
        this.userRepository = userRepository;
        this.allergyRepository = allergyRepository;
        this.preferenceRepository = preferenceRepository;
        this.userProductListRepository = userProductListRepository;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal AppUserDetails me) {
        return ResponseEntity.ok(new UserDto(
                userRepository
                        .findById(me.getDomainUser().getId())
                        .orElseThrow(() -> new AccessDeniedException("account does not exist"))));
    }

    public record PreferenceRequest(List<String> preferences) {}

    @Transactional
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me/update-preferences")
    public ResponseEntity<List<String>> updatePreferences(@AuthenticationPrincipal AppUserDetails me, @RequestBody PreferenceRequest request) {
        List<String> prefs = request.preferences();

        User user = userRepository.findById(me.getDomainUser().getId()).orElseThrow();

        List<Preference> preferences = preferenceRepository.findByNameInIgnoreCase(prefs);

        user.setPreferences(preferences);
        userRepository.save(user);

        return ResponseEntity.ok(preferences.stream().map(Preference::getName).toList());
    }


    public record AllergyRequest(List<String> allergies) {}

    @Transactional
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me/update-allergies")
    public ResponseEntity<List<String>> updateAllergies(@AuthenticationPrincipal AppUserDetails me, @RequestBody AllergyRequest request) {
        List<String> allergs = request.allergies();

        User user = userRepository.findById(me.getDomainUser().getId()).orElseThrow();

        List<Allergy> allergies = allergyRepository.findByNameInIgnoreCase(allergs);

        user.setAllergies(allergies);
        userRepository.save(user);

        return ResponseEntity.ok(allergies.stream().map(Allergy::getName).toList());
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me/update-dark-mode")
    public ResponseEntity<?> updateDarkMode(@AuthenticationPrincipal AppUserDetails me) {
        User user = userRepository.findById(me.getDomainUser().getId()).orElseThrow();

        user.setDark_mode(!user.isDark_mode());

        userRepository.save(user);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me/update-notification-day/{notificationDay}")
    public ResponseEntity<?> updateNotificationDay(@AuthenticationPrincipal AppUserDetails me, @PathVariable Integer notificationDay) {
        User user = userRepository.findById(me.getDomainUser().getId()).orElseThrow();

        user.setNotification_day(notificationDay);

        userRepository.save(user);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me/update-preferred-notification-hour/{prefNotHour}")
    public ResponseEntity<?> updateNotificationHour(@AuthenticationPrincipal AppUserDetails me, @PathVariable String prefNotHour) {
        User user = userRepository.findById(me.getDomainUser().getId()).orElseThrow();

        try {
            LocalTime time = LocalTime.parse(prefNotHour);
            user.setPreferred_notification_hour(time);

            userRepository.save(user);

            return ResponseEntity.ok("Notification hour updated");

        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body("Invalid Hour Format, should be: HH:MM");
        }
    }

    public record changeListRequest(String shareCode){}

    @Transactional
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me/change-list")
    public ResponseEntity<?> changeList(@AuthenticationPrincipal AppUserDetails me, @RequestBody changeListRequest request) {
        User user = userRepository.findById(me.getDomainUser().getId()).orElseThrow();

        UserProductList oldList = user.getUserProductList();

        if (oldList != null && oldList.getShare_code().equals(request.shareCode())) {
            return ResponseEntity.badRequest().body("You are already member of this list");
        }

        UserProductList newList = userProductListRepository.findByShareCode(request.shareCode())
                .orElseThrow(() -> new IllegalArgumentException("Product list with provided share code does not exist."));


        if (oldList != null) {
            oldList.getCollaborators().remove(user);

            if (oldList.getCollaborators().isEmpty()) {
                userProductListRepository.delete(oldList);
            }
        }

        user.setUserProductList(newList);
        newList.getCollaborators().add(user);

        userRepository.save(user);

        return ResponseEntity.ok().build();
    }

}
