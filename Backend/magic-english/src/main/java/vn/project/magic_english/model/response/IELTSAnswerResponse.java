package vn.project.magic_english.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IELTSAnswerResponse {
    private Long id;
    private String answerOption; // A, B, C, D
    private String answerText;
    private Boolean isCorrect; // Chỉ trả về khi submit test
    private String explanation; // Giải thích đáp án (chỉ trả về khi submit test)
}
