-- Đưa schema quiz về bản mới:
--   * quizzes: thêm description, bỏ content/audio_url (chuyển sang passages), difficulty 1-5
--   * passages: bảng mới, một quiz có nhiều passage
--   * questions.passage_id: câu hỏi gắn vào passage cùng quiz (FK kép), NULL = câu độc lập
--   * flashcard.source_passage_id thay cho source_quiz_id

-- ===================== QUIZZES =====================
ALTER TABLE quizzes ADD COLUMN description TEXT;

ALTER TABLE quizzes DROP CONSTRAINT IF EXISTS quizzes_difficulty_check;
ALTER TABLE quizzes ADD CONSTRAINT quizzes_difficulty_check CHECK (difficulty BETWEEN 1 AND 5);

-- ===================== PASSAGES =====================
CREATE TABLE passages (
    id        BIGSERIAL PRIMARY KEY,
    quiz_id   BIGINT   NOT NULL REFERENCES quizzes (id) ON DELETE CASCADE,
    position  SMALLINT NOT NULL,
    content   TEXT,
    audio_url VARCHAR(500),
    CONSTRAINT uq_passages_id_quiz UNIQUE (id, quiz_id),
    CONSTRAINT uq_passages_quiz_position UNIQUE (quiz_id, position),
    CONSTRAINT chk_passage_content CHECK (content IS NOT NULL OR audio_url IS NOT NULL)
);

-- Dữ liệu cũ: content/audio_url của quiz thành passage đầu tiên của quiz đó
INSERT INTO passages (quiz_id, position, content, audio_url)
SELECT id, 1, content, audio_url
FROM quizzes
WHERE content IS NOT NULL OR audio_url IS NOT NULL;

-- ===================== QUESTIONS =====================
ALTER TABLE questions ADD COLUMN passage_id BIGINT;
ALTER TABLE questions
    ADD CONSTRAINT questions_passage_id_quiz_id_fkey
        FOREIGN KEY (passage_id, quiz_id) REFERENCES passages (id, quiz_id);
CREATE INDEX idx_questions_passage ON questions (passage_id);

-- Câu hỏi cũ dùng chung đoạn văn của quiz -> gắn vào passage vừa tạo
UPDATE questions q
SET passage_id = p.id
FROM passages p
WHERE p.quiz_id = q.quiz_id;

-- ===================== FLASHCARD =====================
ALTER TABLE flashcard
    ADD COLUMN source_passage_id BIGINT REFERENCES passages (id) ON DELETE SET NULL;

UPDATE flashcard f
SET source_passage_id = p.id
FROM passages p
WHERE p.quiz_id = f.source_quiz_id;

ALTER TABLE flashcard DROP COLUMN source_quiz_id;

-- ===================== DỌN CỘT CŨ =====================
ALTER TABLE quizzes DROP COLUMN content;
ALTER TABLE quizzes DROP COLUMN audio_url;
