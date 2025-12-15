package vn.project.magic_english.model.response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.project.magic_english.model.Grammar;
import vn.project.magic_english.model.GrammarError;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GrammarCheckResponse {
    private Long id;
    private String inputText;
    private String correctedText;
    private int score;
    private List<GrammarErrorResponse> errors = new ArrayList<>();
    private Instant createdAt;

    public static GrammarCheckResponse fromEntity(Grammar grammar) {
        GrammarCheckResponse response = new GrammarCheckResponse();
        response.setId(grammar.getId());
        response.setInputText(grammar.getInputText());
        response.setCorrectedText(grammar.getCorrectedText());
        response.setScore(grammar.getScore());
        response.setCreatedAt(grammar.getCreatedAt());

        // Map errors
        if (grammar.getErrors() != null && !grammar.getErrors().isEmpty()) {
            response.setErrors(
                    grammar.getErrors().stream()
                            .map(GrammarCheckResponse::mapError)
                            .collect(Collectors.toList()));
        }

        return response;
    }

    private static GrammarErrorResponse mapError(GrammarError error) {
        return new GrammarErrorResponse(
                error.getId(),
                error.getErrorType(),
                error.getBeforeText(),
                error.getErrorText(),
                error.getCorrectedText(),
                error.getAfterText(),
                error.getExplanation(),
                error.getStartPosition(),
                error.getEndPosition());
    }
}
