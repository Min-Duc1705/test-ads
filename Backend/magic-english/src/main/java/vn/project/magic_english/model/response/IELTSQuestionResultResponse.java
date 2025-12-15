package vn.project.magic_english.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IELTSQuestionResultResponse {
    private Long questionId;
    private Integer questionNumber;
    private String questionText;
    private String userAnswer; // A, B, C, D
    private String correctAnswer;
    private Boolean isCorrect;
    private String explanation; // Giải thích đáp án đúng
}
