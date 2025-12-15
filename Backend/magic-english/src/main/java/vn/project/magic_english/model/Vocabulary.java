package vn.project.magic_english.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "vocabulary", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_word", columnList = "word"),
        @Index(name = "idx_word_type", columnList = "word_type"),
        @Index(name = "idx_cefr_level", columnList = "cefr_level")
})
@Getter
@Setter
public class Vocabulary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 255)
    private String word; // từ tiếng Anh

    @Column(length = 255)
    private String ipa; // phiên âm

    @Column(length = 500)
    private String audioUrl; // URL file âm thanh phát âm

    @Column(columnDefinition = "TEXT")
    private String meaning; // nghĩa Tiếng Việt

    @Column(name = "word_type", length = 50)
    private String wordType; // noun / verb / adj...

    @Column(columnDefinition = "TEXT")
    private String example; // câu ví dụ

    @Column(name = "cefr_level", length = 5)
    private String cefrLevel; // A1 - C2

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void handleBeforeCreate() {
        this.createdAt = Instant.now();
    }

    @PreUpdate
    public void handleBeforeUpdate() {
        this.updatedAt = Instant.now();
    }
}
