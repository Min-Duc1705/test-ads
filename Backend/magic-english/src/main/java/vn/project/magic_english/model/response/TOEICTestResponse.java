package vn.project.magic_english.model.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TOEICTestResponse {
    private Long id;
    private String section;
    private String part;
    private String difficulty;
    private String title;
    private Integer durationMinutes;
    private Integer totalQuestions;
    private List<TOEICQuestionResponse> questions;
}
