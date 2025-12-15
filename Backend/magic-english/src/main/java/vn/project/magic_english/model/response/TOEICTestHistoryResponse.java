package vn.project.magic_english.model.response;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class TOEICTestHistoryResponse {
    private Long id;
    private Long testId;
    private String testTitle;
    private String section;
    private String difficulty;
    private Instant startedAt;
    private Instant completedAt;
    private Integer score;
    private Integer correctAnswers;
    private Integer totalAnswers;
    private String status;
    private Integer timeSpentSeconds;
}
