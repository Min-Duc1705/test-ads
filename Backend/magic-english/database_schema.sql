-- Database Schema for Magic English Application
-- Generated from JPA Entity Models

-- Table: users
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    avatar_url VARCHAR(500),
    refresh_token MEDIUMTEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Table: achievements
CREATE TABLE achievements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    description TEXT,
    icon_url VARCHAR(500),
    required_value INT,
    metric_type VARCHAR(50) COMMENT 'vocab_added, grammar_check, learning_streak',
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Table: vocabulary
CREATE TABLE vocabulary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    word VARCHAR(255),
    ipa VARCHAR(255),
    audio_url VARCHAR(500),
    meaning TEXT,
    word_type VARCHAR(50),
    example TEXT,
    cefr_level VARCHAR(5) COMMENT 'A1 - C2',
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_word (word),
    INDEX idx_word_type (word_type),
    INDEX idx_cefr_level (cefr_level)
);

-- Table: grammar
CREATE TABLE grammar (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    input_text TEXT NOT NULL COMMENT 'câu người dùng nhập',
    score INT COMMENT 'điểm số đánh giá (0-100)',
    corrected_text TEXT COMMENT 'câu đã được sửa hết lỗi',
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table: grammar_error
CREATE TABLE grammar_error (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    grammar_id BIGINT NOT NULL,
    error_type VARCHAR(50) NOT NULL COMMENT 'spelling, punctuation, clarity, grammar',
    before_text TEXT,
    error_text TEXT NOT NULL COMMENT 'Text bị lỗi (highlight đỏ)',
    corrected_text TEXT COMMENT 'Text đã sửa (highlight xanh)',
    after_text TEXT,
    explanation TEXT NOT NULL COMMENT 'Giải thích lỗi',
    start_position INT,
    end_position INT,
    FOREIGN KEY (grammar_id) REFERENCES grammar(id) ON DELETE CASCADE
);

-- Table: user_achievement
CREATE TABLE user_achievement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    achievement_id BIGINT NOT NULL,
    achieved_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON DELETE CASCADE
);

-- Indexes for better query performance
CREATE INDEX idx_grammar_user_id ON grammar(user_id);
CREATE INDEX idx_grammar_error_grammar_id ON grammar_error(grammar_id);
CREATE INDEX idx_user_achievement_user_id ON user_achievement(user_id);
CREATE INDEX idx_user_achievement_achievement_id ON user_achievement(achievement_id);
