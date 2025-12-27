package vn.project.magic_english.model.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TOEICQuestionResponse {
    private Long id;
    private Integer questionNumber;
    private String questionText;
    private String questionType;
    private String passage;
    private String audioUrl;
    private String part; // Part 1, Part 2, Part 3, Part 4 for Listening
    private String imageUrl; // For Part 1 Photographs
    private List<TOEICAnswerResponse> answers;
}
