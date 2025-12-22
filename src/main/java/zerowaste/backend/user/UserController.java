package zerowaste.backend.user;

import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import zerowaste.backend.security.AppUserDetails;
import zerowaste.backend.security.AppUserDetailsService;
import zerowaste.backend.user.dtos.UserDto;
import zerowaste.backend.user.properties.Allergy;
import zerowaste.backend.user.properties.AllergyRepository;
import zerowaste.backend.user.properties.Preference;
import zerowaste.backend.user.properties.PreferenceRepository;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepository;
    private final AllergyRepository allergyRepository;
    private final PreferenceRepository preferenceRepository;

    public UserController(UserRepository userRepository,  AllergyRepository allergyRepository, PreferenceRepository preferenceRepository) {
        this.userRepository = userRepository;
        this.allergyRepository = allergyRepository;
        this.preferenceRepository = preferenceRepository;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal AppUserDetails me) {
        return ResponseEntity.ok(new UserDto(
                userRepository
                        .findById(me.getDomainUser().getId())
                        .orElseThrow(() -> new AccessDeniedException("nu exista cont"))));
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


}
