package vn.project.magic_english.model.response;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResRegister {
    private String name;

    private String email;


}
