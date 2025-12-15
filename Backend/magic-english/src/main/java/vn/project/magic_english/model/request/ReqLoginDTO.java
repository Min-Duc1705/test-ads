package vn.project.magic_english.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqLoginDTO {
    @NotBlank(message = "Email không được để trống")
    private String Email;

    @NotBlank(message = "password không được để trống")
    private String password;


}
