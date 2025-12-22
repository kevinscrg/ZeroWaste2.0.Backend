package zerowaste.backend.user.dtos;

import zerowaste.backend.user.User;
import zerowaste.backend.user.properties.Allergy;
import zerowaste.backend.user.properties.Preference;

import java.time.LocalTime;
import java.util.List;

public class UserDto {

    private String email;

    private LocalTime preferred_notification_hour;

    private List<String> preferences;

    private List<String> allergies;

    private int notification_day;

    private boolean dark_mode;


    public UserDto(User user){
        this.email = user.getEmail();
        this.preferred_notification_hour = user.getPreferred_notification_hour();
        this.notification_day = user.getNotification_day();
        this.dark_mode = user.isDark_mode();

        this.preferences = user.getPreferences().stream().map(Preference::getName).toList();
        this.allergies = user.getAllergies().stream().map(Allergy::getName).toList();

    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalTime getPreferred_notification_hour() {
        return preferred_notification_hour;
    }

    public void setPreferred_notification_hour(LocalTime preferred_notification_hour) {
        this.preferred_notification_hour = preferred_notification_hour;
    }

    public List<String> getPreferences() {
        return preferences;
    }

    public void setPreferences(List<String> preferences) {
        this.preferences = preferences;
    }

    public List<String> getAllergies() {
        return allergies;
    }

    public void setAllergies(List<String> allergies) {
        this.allergies = allergies;
    }

    public int getNotification_day() {
        return notification_day;
    }

    public void setNotification_day(int notification_day) {
        this.notification_day = notification_day;
    }

    public boolean isDark_mode() {
        return dark_mode;
    }

    public void setDark_mode(boolean dark_mode) {
        this.dark_mode = dark_mode;
    }
}
