package vn.project.magic_english.model.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TOEICQuestionResultResponse {
    private Long questionId;
    private Integer questionNumber;
    private String questionText;
    private String passage;
    private String audioUrl;
    private Boolean isCorrect;
    private Long selectedAnswerId;
    private String selectedAnswerOption;
    private String selectedAnswerText;
    private Long correctAnswerId;
    private String correctAnswerOption;
    private String correctAnswerText;
    private String explanation;
}
