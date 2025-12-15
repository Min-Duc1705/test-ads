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
public class IELTSTestResponse {
    private Long id;
    private String skill;
    private String level;
    private String difficulty;
    private String title;
    private Integer durationMinutes;
    private Integer totalQuestions;
    private List<IELTSQuestionResponse> questions;
}
