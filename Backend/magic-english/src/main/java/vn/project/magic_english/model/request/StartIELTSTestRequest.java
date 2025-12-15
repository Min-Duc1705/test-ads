package vn.project.magic_english.model.request;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;

@Getter
@Setter
public class StartIELTSTestRequest {
    @NotNull(message = "Test ID is required")
    private Long testId;
}
