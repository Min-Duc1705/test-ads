package vn.project.magic_english.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartTOEICTestRequest {
    @NotNull(message = "Test ID is required")
    private Long testId;
}
