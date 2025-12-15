package vn.project.magic_english.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GrammarErrorResponse {
    private Long id;
    private String errorType; // "spelling", "punctuation", "clarity", "grammar"
    private String beforeText;
    private String errorText;
    private String correctedText;
    private String afterText;
    private String explanation;
    private Integer startPosition;
    private Integer endPosition;
}
