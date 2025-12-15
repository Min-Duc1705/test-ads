package vn.project.magic_english.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "grammar_error")
@Getter
@Setter
public class GrammarError {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grammar_id", nullable = false)
    private Grammar grammar;

    @Column(length = 50, nullable = false)
    private String errorType; // "spelling", "punctuation", "clarity", "grammar"

    @Column(columnDefinition = "TEXT")
    private String beforeText; // Text trước lỗi

    @Column(columnDefinition = "TEXT", nullable = false)
    private String errorText; // Text bị lỗi (highlight đỏ)

    @Column(columnDefinition = "TEXT")
    private String correctedText; // Text đã sửa (highlight xanh) - có thể rỗng với clarity

    @Column(columnDefinition = "TEXT")
    private String afterText; // Text sau lỗi

    @Column(columnDefinition = "TEXT", nullable = false)
    private String explanation; // Giải thích lỗi

    private Integer startPosition; // Vị trí bắt đầu trong câu gốc
    private Integer endPosition; // Vị trí kết thúc trong câu gốc
}
