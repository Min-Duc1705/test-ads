package vn.project.magic_english.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IELTSTestResultResponse {
    private Long historyId;
    private Double score;
    private Integer correctAnswers;
    private Integer totalQuestions;
    private Integer timeSpentSeconds;
    private List<IELTSQuestionResultResponse> questionResults;
}
