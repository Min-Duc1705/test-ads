package vn.project.magic_english.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckGrammarRequest {

    @NotBlank(message = "Input text cannot be blank")
    @Size(max = 5000, message = "Text must not exceed 5000 characters")
    private String text;
}
