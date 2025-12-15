package vn.project.magic_english.model.response;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class TOEICTestResultResponse {
    private Long historyId;
    private Long testId;
    private String testTitle;
    private String section;
    private String difficulty;
    private Integer score; // 0-990
    private Integer correctAnswers;
    private Integer totalAnswers;
    private Double accuracyPercentage;
    private Integer timeSpentSeconds;
    private Instant completedAt;
    private List<TOEICQuestionResultResponse> questionResults;
}
