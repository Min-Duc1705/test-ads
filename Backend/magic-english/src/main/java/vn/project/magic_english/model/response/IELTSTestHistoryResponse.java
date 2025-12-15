package vn.project.magic_english.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IELTSTestHistoryResponse {
    private Long id;
    private Long testId;
    private String testTitle;
    private String skill;
    private String level;
    private String difficulty;
    private Instant startedAt;
    private Instant completedAt;
    private Double score;
    private Integer correctAnswers;
    private Integer totalAnswers;
    private String status;
    private Integer timeSpentSeconds;
}
