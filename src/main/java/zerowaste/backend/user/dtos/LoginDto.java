package zerowaste.backend.user.dtos;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;

public class LoginDto {
    @Email
    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

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
}
