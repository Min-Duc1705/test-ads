-- IELTS Test System Database Schema
-- Add these tables to existing magic_english database

-- Table: ielts_tests (Bộ đề IELTS được tạo bởi AI)
CREATE TABLE ielts_tests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    skill VARCHAR(50) NOT NULL COMMENT 'Reading, Writing, Listening, Speaking',
    level VARCHAR(50) NOT NULL COMMENT 'General, Academic',
    difficulty VARCHAR(20) NOT NULL COMMENT 'Easy, Medium, Hard',
    title VARCHAR(255) NOT NULL,
    duration_minutes INT NOT NULL COMMENT 'Thời gian làm bài (phút)',
    total_questions INT NOT NULL COMMENT 'Tổng số câu hỏi',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_skill (skill),
    INDEX idx_level (level),
    INDEX idx_difficulty (difficulty)
);

-- Table: ielts_questions (Câu hỏi trong mỗi bộ đề)
CREATE TABLE ielts_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_id BIGINT NOT NULL,
    question_number INT NOT NULL,
    question_text TEXT NOT NULL,
    question_type VARCHAR(50) COMMENT 'multiple_choice, true_false, fill_blank, etc',
    passage TEXT COMMENT 'Đoạn văn cho Reading (nếu có)',
    audio_url VARCHAR(500) COMMENT 'Link audio cho Listening (nếu có)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (test_id) REFERENCES ielts_tests(id) ON DELETE CASCADE,
    INDEX idx_test_id (test_id)
);

-- Table: ielts_answers (Các đáp án cho mỗi câu hỏi)
CREATE TABLE ielts_answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    answer_option VARCHAR(10) NOT NULL COMMENT 'A, B, C, D hoặc TRUE, FALSE',
    answer_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    explanation TEXT COMMENT 'Giải thích tại sao đây là đáp án đúng/sai',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (question_id) REFERENCES ielts_questions(id) ON DELETE CASCADE,
    INDEX idx_question_id (question_id)
);

-- Table: ielts_test_history (Lịch sử làm bài của user)
CREATE TABLE ielts_test_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    test_id BIGINT NOT NULL,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    score DECIMAL(3, 1) COMMENT 'IELTS Band Score (4.0 - 9.0)',
    correct_answers INT DEFAULT 0,
    total_answers INT DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'in_progress' COMMENT 'in_progress, completed, abandoned',
    time_spent_seconds INT COMMENT 'Thời gian làm bài thực tế (giây)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (test_id) REFERENCES ielts_tests(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_test_id (test_id),
    INDEX idx_status (status)
);

-- Table: ielts_user_answers (Câu trả lời của user cho mỗi câu hỏi)
CREATE TABLE ielts_user_answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    history_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_answer_id BIGINT NULL COMMENT 'ID của answer được chọn',
    user_answer_text TEXT COMMENT 'Câu trả lời dạng text (cho fill_blank)',
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (history_id) REFERENCES ielts_test_history(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES ielts_questions(id) ON DELETE CASCADE,
    FOREIGN KEY (selected_answer_id) REFERENCES ielts_answers(id) ON DELETE SET NULL,
    INDEX idx_history_id (history_id),
    INDEX idx_question_id (question_id)
);
