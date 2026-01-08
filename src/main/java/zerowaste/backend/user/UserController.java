package zerowaste.backend.user;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import zerowaste.backend.security.AppUserDetails;
import zerowaste.backend.user.dtos.UserDto;
import zerowaste.backend.user.properties.Allergy;
import zerowaste.backend.user.properties.Preference;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {


    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal AppUserDetails me) {
        return ResponseEntity.ok(new UserDto(userService.getAuthUser(me)));
    }


    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me/update-preferences")
    public ResponseEntity<List<String>> updatePreferences(@AuthenticationPrincipal AppUserDetails me, @RequestBody UserService.PreferenceRequest request) {

        return ResponseEntity.ok(userService.updatePreferences(me,request)
                .stream()
                .map(Preference::getName)
                .toList());
    }



    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me/update-allergies")
    public ResponseEntity<List<String>> updateAllergies(@AuthenticationPrincipal AppUserDetails me, @RequestBody UserService.AllergyRequest request) {

        return ResponseEntity.ok(userService.updateAllergies(me,request)
                .stream()
                .map(Allergy::getName)
                .toList());
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me/update-dark-mode")
    public ResponseEntity<?> updateDarkMode(@AuthenticationPrincipal AppUserDetails me) {
        userService.updateDarkMode(me);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me/update-notification-day/{notificationDay}")
    public ResponseEntity<?> updateNotificationDay(@AuthenticationPrincipal AppUserDetails me, @PathVariable Integer notificationDay) {
        userService.updateNotificationDay(me, notificationDay);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me/update-preferred-notification-hour/{prefNotHour}")
    public ResponseEntity<?> updateNotificationHour(@AuthenticationPrincipal AppUserDetails me, @PathVariable String prefNotHour) {
        userService.updateNotificationHour(me, prefNotHour);
        return ResponseEntity.ok("Notification hour updated");
    }



    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me/change-list")
    public ResponseEntity<?> changeList(@AuthenticationPrincipal AppUserDetails me, @RequestBody UserService.changeListRequest request) {
        userService.changeList(me,request);
        return ResponseEntity.ok().build();
    }

}
