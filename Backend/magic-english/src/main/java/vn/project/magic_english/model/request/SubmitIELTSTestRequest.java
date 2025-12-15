package vn.project.magic_english.model.request;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
public class SubmitIELTSTestRequest {
    @NotNull(message = "History ID is required")
    private Long historyId;

    @NotNull(message = "Answers are required")
    private List<UserAnswerRequest> answers;

    private Integer timeSpentSeconds;

    @Getter
    @Setter
    public static class UserAnswerRequest {
        @NotNull(message = "Question ID is required")
        private Long questionId;

        private Long selectedAnswerId; // Cho multiple choice (legacy single)
        private List<Long> selectedAnswerIds; // Cho multiple choice (multi-select)
        private String answerText; // Cho fill_blank
    }
}
