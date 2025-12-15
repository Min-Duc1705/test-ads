package vn.project.magic_english.model.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TOEICAnswerResponse {
    private Long id;
    private String answerOption; // A, B, C, D
    private String answerText;
    private Boolean isCorrect; // null if not submitting, true/false if submitting
    private String explanation; // null if not submitting
}
