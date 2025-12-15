package vn.project.magic_english.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SubmitTOEICTestRequest {
    @NotNull(message = "History ID is required")
    private Long historyId;

    private Integer timeSpentSeconds;

    @NotEmpty(message = "Answers are required")
    private List<Map<String, Object>> answers; // [{questionId: 1, selectedAnswerId: 2}, ...]
}
