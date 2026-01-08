package zerowaste.backend.user;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import zerowaste.backend.notification.DailyPlanifierService;
import zerowaste.backend.product.models.UserProductList;
import zerowaste.backend.product.repos.UserProductListRepository;
import zerowaste.backend.security.AppUserDetails;
import zerowaste.backend.user.properties.Allergy;
import zerowaste.backend.user.properties.AllergyRepository;
import zerowaste.backend.user.properties.Preference;
import zerowaste.backend.user.properties.PreferenceRepository;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserProductListRepository userProductListRepository;
    private final AllergyRepository allergyRepository;
    private final PreferenceRepository preferenceRepository;
    private final DailyPlanifierService dailyPlanifierService;

    public record PreferenceRequest(List<String> preferences) {}
    public record AllergyRequest(List<String> allergies) {}
    public record changeListRequest(String shareCode){}

    public UserService(UserRepository userRepository,
                       UserProductListRepository userProductListRepository,
                       AllergyRepository allergyRepository,
                       PreferenceRepository preferenceRepository,
                       DailyPlanifierService dailyPlanifierService) {

        this.userRepository = userRepository;
        this.userProductListRepository = userProductListRepository;
        this.allergyRepository = allergyRepository;
        this.preferenceRepository = preferenceRepository;
        this.dailyPlanifierService = dailyPlanifierService;
    }


    public User getAuthUser(AppUserDetails me) {
        return userRepository.findById(me.getDomainUser().getId()).orElseThrow(() -> new AccessDeniedException("account does not exist"));
    }

    @Transactional
    public List<Preference> updatePreferences(AppUserDetails me, PreferenceRequest request) {

        List<String> prefs = request.preferences();

        User user = userRepository.findById(me.getDomainUser().getId()).orElseThrow();

        List<Preference> preferences = preferenceRepository.findByNameInIgnoreCase(prefs);

        user.setPreferences(preferences);
        userRepository.save(user);

        return preferences;
    }

    @Transactional
    public List<Allergy> updateAllergies(AppUserDetails me, AllergyRequest request) {
        List<String> allergiesNames = request.allergies();

        User user = userRepository.findById(me.getDomainUser().getId()).orElseThrow();

        List<Allergy> allergies = allergyRepository.findByNameInIgnoreCase(allergiesNames);

        user.setAllergies(allergies);
        userRepository.save(user);

        return allergies;
    }

    @Transactional
    public void updateDarkMode(AppUserDetails me) {
        User user = userRepository.findById(me.getDomainUser().getId()).orElseThrow();

        user.setDark_mode(!user.isDark_mode());

        userRepository.save(user);
    }

    @Transactional
    public void updateNotificationDay(AppUserDetails me, int notificationDay) {
        User user = userRepository.findById(me.getDomainUser().getId()).orElseThrow();

        user.setNotification_day(notificationDay);

        userRepository.save(user);

        dailyPlanifierService.updateUserNotification(user);
    }

    @Transactional
    public void updateNotificationHour(AppUserDetails me, String prefNotHour) {
        User user = userRepository.findById(me.getDomainUser().getId()).orElseThrow();

        try {
            LocalTime time = LocalTime.parse(prefNotHour);
            user.setPreferred_notification_hour(time);

            userRepository.save(user);

            dailyPlanifierService.updateUserNotification(user);

        }catch(DateTimeParseException e) {
            throw new DateTimeException("Invalid Hour Format, should be: HH:MM");
        }
    }

    @Transactional
    public void changeList( AppUserDetails me, changeListRequest request){
        User user = userRepository.findById(me.getDomainUser().getId()).orElseThrow();

        UserProductList oldList = user.getUserProductList();

        if (oldList != null && oldList.getShare_code().equals(request.shareCode())) {
            throw new IllegalArgumentException("You are already member of this list");
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
    }

}
