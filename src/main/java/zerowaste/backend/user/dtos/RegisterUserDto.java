package zerowaste.backend.user.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterUserDto {
    @Email(message = "introduceți un mail valid")
    @NotNull
    @Pattern(regexp = "^[^\\s]+$", message = "Adresa de email nu trebuie să conțină spații!")
    private String email;

    @NotNull
    @Size(min = 6, message = "Parola trebuie să aibă cel puțin 6 caractere!")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "Parola trebuie sa contina cel putin o litera si cel putin o cifra"
    )
    @Pattern(regexp = "^[^\\s]+$", message = "Parola nu trebuie să conțină spații!")
    private String password;

    @NotNull
    private String confirmPassword;


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

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
