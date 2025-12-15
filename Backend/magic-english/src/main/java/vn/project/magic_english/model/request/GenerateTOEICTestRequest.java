package vn.project.magic_english.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerateTOEICTestRequest {
    @NotBlank(message = "Section is required")
    private String section; // Listening, Reading, Part 1-4, Part 5-7

    @NotBlank(message = "Difficulty is required")
    private String difficulty; // Easy, Medium, Hard
}
