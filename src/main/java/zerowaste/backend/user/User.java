package zerowaste.backend.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import zerowaste.backend.product.models.UserProductList;
import zerowaste.backend.user.properties.Allergy;
import zerowaste.backend.user.properties.Preference;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Email
    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private LocalTime preferred_notification_hour;

    private int notification_day;

    private boolean dark_mode;

    private boolean verified;

    @ManyToMany
    private List<Preference> preferences =  new ArrayList<>();

    @ManyToMany
    private List<Allergy> allergies = new  ArrayList<>();

    @ManyToOne
    private UserProductList userProductList;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalTime getPreferred_notification_hour() {
        return preferred_notification_hour;
    }

    public void setPreferred_notification_hour(LocalTime preferred_notification_hour) {
        this.preferred_notification_hour = preferred_notification_hour;
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

    public List<Preference> getPreferences() {
        return preferences;
    }

    public void setPreferences(List<Preference> preferences) {
        this.preferences = preferences;
    }

    public List<Allergy> getAllergies() {
        return allergies;
    }

    public void setAllergies(List<Allergy> allergies) {
        this.allergies = allergies;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public UserProductList getUserProductList() {
        return userProductList;
    }

    public void setUserProductList(UserProductList userProductList) {
        this.userProductList = userProductList;
    }
}
