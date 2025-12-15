-- Quick fix: Modify audio_url column to TEXT
USE magic_english;

ALTER TABLE toeic_questions MODIFY COLUMN audio_url TEXT COMMENT 'Link audio cho Listening (VoiceRSS URLs can be very long)';

-- Verify the change
DESCRIBE toeic_questions;
