-- Baseline schema (khớp với DB dev hiện tại tại thời điểm tích hợp Flyway).
-- DB đã tồn tại sẽ được baseline ở version 1 nên file này chỉ chạy trên DB trống.

-- ===================== USERS / AUTH =====================
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    email         VARCHAR(255) UNIQUE,
    phone_number  VARCHAR(15),
    current_score NUMERIC(4, 2),
    avatar_url    VARCHAR(255)
);

CREATE TABLE token_expired (
    id         VARCHAR(255) PRIMARY KEY,
    expired_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_token_expired_at ON token_expired (expired_at);

-- ===================== SKILLS / TOPICS =====================
CREATE TABLE skills (
    id   SMALLSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    icon VARCHAR(50)
);

CREATE TABLE quiz_formats (
    id         SMALLSERIAL PRIMARY KEY,
    skill_id   SMALLINT    NOT NULL REFERENCES skills (id),
    code       VARCHAR(30) NOT NULL,
    name       VARCHAR(50) NOT NULL,
    sort_order INTEGER     NOT NULL DEFAULT 0,
    UNIQUE (skill_id, code),
    UNIQUE (id, skill_id)
);

CREATE TABLE topic_groups (
    id         BIGSERIAL PRIMARY KEY,
    type       VARCHAR(20)  NOT NULL CHECK (type IN ('SITUATION', 'GRAMMAR')),
    name       VARCHAR(100) NOT NULL,
    icon       VARCHAR(50),
    sort_order INTEGER      NOT NULL DEFAULT 0
);

CREATE TABLE topics (
    id          BIGSERIAL PRIMARY KEY,
    group_id    BIGINT       NOT NULL REFERENCES topic_groups (id),
    slug        VARCHAR(100) NOT NULL UNIQUE,
    name_vi     VARCHAR(100) NOT NULL,
    name_en     VARCHAR(100),
    keywords    VARCHAR(255),
    icon        VARCHAR(50),
    cefr_level  VARCHAR(2) CHECK (cefr_level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')),
    description TEXT,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_topics_group ON topics (group_id, sort_order);

CREATE TABLE skill_topics (
    skill_id SMALLINT NOT NULL REFERENCES skills (id) ON DELETE CASCADE,
    topic_id BIGINT   NOT NULL REFERENCES topics (id) ON DELETE CASCADE,
    PRIMARY KEY (skill_id, topic_id)
);

-- ===================== QUIZ =====================
CREATE TABLE quizzes (
    id           BIGSERIAL PRIMARY KEY,
    created_by   BIGINT REFERENCES users (id) ON DELETE SET NULL,
    skill_id     SMALLINT     NOT NULL REFERENCES skills (id),
    topic_id     BIGINT REFERENCES topics (id),
    custom_topic VARCHAR(200),
    format_id    SMALLINT,
    difficulty   SMALLINT     NOT NULL DEFAULT 2 CHECK (difficulty BETWEEN 1 AND 3),
    title        VARCHAR(200) NOT NULL,
    content      TEXT,
    audio_url    VARCHAR(500),
    status       VARCHAR(20)  NOT NULL DEFAULT 'READY' CHECK (status IN ('GENERATING', 'READY', 'FAILED')),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_quiz_topic CHECK (topic_id IS NOT NULL OR custom_topic IS NOT NULL),
    CONSTRAINT quizzes_format_id_skill_id_fkey
        FOREIGN KEY (format_id, skill_id) REFERENCES quiz_formats (id, skill_id)
);
CREATE INDEX idx_quizzes_creator ON quizzes (created_by, created_at DESC);
CREATE INDEX idx_quizzes_topic ON quizzes (topic_id);

CREATE TABLE questions (
    id               BIGSERIAL PRIMARY KEY,
    quiz_id          BIGINT      NOT NULL REFERENCES quizzes (id) ON DELETE CASCADE,
    position         SMALLINT    NOT NULL,
    type             VARCHAR(20) NOT NULL DEFAULT 'SINGLE_CHOICE' CHECK (type IN ('SINGLE_CHOICE', 'FILL_BLANK')),
    content          TEXT        NOT NULL,
    correct_text     VARCHAR(255),
    explanation      TEXT,
    grammar_topic_id BIGINT REFERENCES topics (id),
    UNIQUE (quiz_id, position)
);
CREATE INDEX idx_questions_grammar ON questions (grammar_topic_id);

CREATE TABLE options (
    id          BIGSERIAL PRIMARY KEY,
    question_id BIGINT   NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    position    SMALLINT NOT NULL,
    content     TEXT     NOT NULL,
    is_correct  BOOLEAN  NOT NULL DEFAULT FALSE,
    UNIQUE (question_id, position)
);
-- Mỗi câu hỏi chỉ có tối đa một đáp án đúng
CREATE UNIQUE INDEX uq_options_one_correct ON options (question_id) WHERE is_correct;

CREATE TABLE quiz_attempts (
    id              BIGSERIAL PRIMARY KEY,
    quiz_id         BIGINT      NOT NULL REFERENCES quizzes (id) ON DELETE CASCADE,
    user_id         BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    started_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    submitted_at    TIMESTAMPTZ,
    correct_count   INTEGER,
    total_questions INTEGER
);
CREATE INDEX idx_attempts_user ON quiz_attempts (user_id, submitted_at DESC);

CREATE TABLE attempt_answers (
    attempt_id         BIGINT  NOT NULL REFERENCES quiz_attempts (id) ON DELETE CASCADE,
    question_id        BIGINT  NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    selected_option_id BIGINT REFERENCES options (id),
    answer_text        VARCHAR(255),
    is_correct         BOOLEAN NOT NULL,
    PRIMARY KEY (attempt_id, question_id)
);

CREATE TABLE user_topic_progress (
    user_id            BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    skill_id           SMALLINT      NOT NULL,
    topic_id           BIGINT        NOT NULL,
    attempts_count     INTEGER       NOT NULL DEFAULT 0,
    questions_answered INTEGER       NOT NULL DEFAULT 0,
    questions_correct  INTEGER       NOT NULL DEFAULT 0,
    mastery            NUMERIC(5, 2) NOT NULL DEFAULT 0 CHECK (mastery BETWEEN 0 AND 100),
    last_practiced_at  TIMESTAMPTZ,
    difficulty         DOUBLE PRECISION,
    stability          DOUBLE PRECISION,
    lapses             INTEGER       NOT NULL DEFAULT 0,
    fsrs_state         VARCHAR(20)   NOT NULL DEFAULT 'NEW' CHECK (fsrs_state IN ('NEW', 'LEARNING', 'REVIEW', 'RELEARNING')),
    last_review        DATE,
    next_review        DATE,
    PRIMARY KEY (user_id, skill_id, topic_id),
    CONSTRAINT user_topic_progress_skill_id_topic_id_fkey
        FOREIGN KEY (skill_id, topic_id) REFERENCES skill_topics (skill_id, topic_id) ON DELETE CASCADE
);
CREATE INDEX idx_progress_due ON user_topic_progress (user_id, next_review);
CREATE INDEX idx_progress_weak ON user_topic_progress (user_id, skill_id, mastery);

-- ===================== FLASHCARD =====================
CREATE TABLE flashcard (
    id             BIGSERIAL PRIMARY KEY,
    created_by     BIGINT REFERENCES users (id) ON DELETE SET NULL,
    front_content  VARCHAR(255) NOT NULL,
    back_content   VARCHAR(255) NOT NULL,
    example        TEXT,
    source_quiz_id BIGINT REFERENCES quizzes (id) ON DELETE SET NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE flashcard_decks (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (user_id, name)
);

CREATE TABLE flashcard_detail (
    deck_id      BIGINT      NOT NULL REFERENCES flashcard_decks (id) ON DELETE CASCADE,
    flashcard_id BIGINT      NOT NULL REFERENCES flashcard (id) ON DELETE CASCADE,
    difficulty   DOUBLE PRECISION,
    stability    DOUBLE PRECISION,
    lapses       INTEGER     NOT NULL DEFAULT 0,
    fsrs_state   VARCHAR(20) NOT NULL DEFAULT 'NEW' CHECK (fsrs_state IN ('NEW', 'LEARNING', 'REVIEW', 'RELEARNING')),
    last_review  DATE,
    next_review  DATE,
    added_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (deck_id, flashcard_id)
);
CREATE INDEX idx_fc_detail_due ON flashcard_detail (deck_id, next_review);
