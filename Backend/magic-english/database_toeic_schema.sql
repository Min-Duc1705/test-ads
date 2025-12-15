-- TOEIC Test System Database Schema
-- Add these tables to existing magic_english database

-- Drop existing tables if any (in correct order due to foreign keys)
DROP TABLE IF EXISTS toeic_user_answers;
DROP TABLE IF EXISTS toeic_test_history;
DROP TABLE IF EXISTS toeic_answers;
DROP TABLE IF EXISTS toeic_questions;
DROP TABLE IF EXISTS toeic_tests;

-- Table: toeic_tests (Bộ đề TOEIC được tạo bởi AI)
CREATE TABLE toeic_tests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    section VARCHAR(50) NOT NULL COMMENT 'Listening, Reading, Part 1-4, Part 5-7',
    part VARCHAR(20) COMMENT 'Part 1, Part 2, ..., Part 7 (nếu specific part)',
    difficulty VARCHAR(20) NOT NULL COMMENT 'Easy, Medium, Hard',
    title VARCHAR(255) NOT NULL,
    duration_minutes INT NOT NULL COMMENT 'Thời gian làm bài (phút)',
    total_questions INT NOT NULL COMMENT 'Tổng số câu hỏi',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_section (section),
    INDEX idx_part (part),
    INDEX idx_difficulty (difficulty)
);

-- Table: toeic_questions (Câu hỏi trong mỗi bộ đề)
CREATE TABLE toeic_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_id BIGINT NOT NULL,
    question_number INT NOT NULL,
    question_text TEXT NOT NULL,
    question_type VARCHAR(50) COMMENT 'multiple_choice, fill_blank, etc',
    passage TEXT COMMENT 'Đoạn văn cho Reading hoặc context cho Listening',
    audio_url TEXT COMMENT 'Link audio cho Listening (VoiceRSS URLs can be very long)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (test_id) REFERENCES toeic_tests(id) ON DELETE CASCADE,
    INDEX idx_test_id (test_id)
);

-- Table: toeic_answers (Các đáp án cho mỗi câu hỏi)
CREATE TABLE toeic_answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    answer_option VARCHAR(10) NOT NULL COMMENT 'A, B, C, D',
    answer_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    explanation TEXT COMMENT 'Giải thích tại sao đây là đáp án đúng/sai',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (question_id) REFERENCES toeic_questions(id) ON DELETE CASCADE,
    INDEX idx_question_id (question_id)
);

-- Table: toeic_test_history (Lịch sử làm bài của user)
CREATE TABLE toeic_test_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    test_id BIGINT NOT NULL,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    score INT COMMENT 'TOEIC Score (0-990)',
    correct_answers INT DEFAULT 0,
    total_answers INT DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'in_progress' COMMENT 'in_progress, completed, abandoned',
    time_spent_seconds INT COMMENT 'Thời gian làm bài thực tế (giây)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (test_id) REFERENCES toeic_tests(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_test_id (test_id),
    INDEX idx_status (status)
);

-- Table: toeic_user_answers (Câu trả lời của user cho mỗi câu hỏi)
CREATE TABLE toeic_user_answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    history_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_answer_id BIGINT NULL COMMENT 'ID của answer được chọn',
    user_answer_text TEXT COMMENT 'Câu trả lời dạng text (cho fill_blank nếu có)',
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (history_id) REFERENCES toeic_test_history(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES toeic_questions(id) ON DELETE CASCADE,
    FOREIGN KEY (selected_answer_id) REFERENCES toeic_answers(id) ON DELETE SET NULL,
    INDEX idx_history_id (history_id),
    INDEX idx_question_id (question_id)
);

-- TOEIC Scoring Reference
-- Part 1-4 (Listening): 100 questions, score 5-495
-- Part 5-7 (Reading): 100 questions, score 5-495
-- Total: 200 questions, score 10-990
