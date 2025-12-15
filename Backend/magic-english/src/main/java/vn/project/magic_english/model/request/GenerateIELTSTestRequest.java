package vn.project.magic_english.model.request;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class GenerateIELTSTestRequest {
    @NotBlank(message = "Skill is required")
    private String skill; // Reading, Writing, Listening, Speaking

    @NotBlank(message = "Level is required")
    private String level; // General, Academic

    @NotBlank(message = "Difficulty is required")
    private String difficulty; // Easy, Medium, Hard
}
