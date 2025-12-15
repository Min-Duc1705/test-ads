package vn.project.magic_english.model.request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqRegister {
    private String name;

    @NotBlank(message = "email không được để trống")
    @Column(unique = true)
    private String email;

    @NotBlank(message = "password không được để trống")
    private String password;
}
