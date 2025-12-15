package vn.project.magic_english.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.project.magic_english.model.Vocabulary;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyDetailResponse {
    private Long id;
    private String word; // Từ tiếng Anh
    private String ipa; // Phiên âm IPA
    private String audioUrl; // URL file âm thanh phát âm
    private String meaning; // Nghĩa tiếng Việt
    private String wordType; // Loại từ (noun, verb, adjective...)
    private List<String> examples; // Danh sách câu ví dụ
    private String cefrLevel; // Cấp độ CEFR (A1-C2)
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Factory method: Chuyển từ Entity sang Response DTO
     */
    public static VocabularyDetailResponse fromEntity(Vocabulary vocab) {
        VocabularyDetailResponse response = new VocabularyDetailResponse();
        response.setId(vocab.getId());
        response.setWord(vocab.getWord());
        response.setIpa(vocab.getIpa());
        response.setAudioUrl(vocab.getAudioUrl());
        response.setMeaning(vocab.getMeaning());
        response.setWordType(vocab.getWordType());
        response.setExamples(vocab.getExample() != null ? Arrays.asList(vocab.getExample().split("\n")) : List.of());
        response.setCefrLevel(vocab.getCefrLevel());
        response.setCreatedAt(vocab.getCreatedAt());
        response.setUpdatedAt(vocab.getUpdatedAt());
        return response;
    }
}
