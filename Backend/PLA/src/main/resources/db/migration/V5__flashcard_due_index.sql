-- V5: chuẩn hoá dữ liệu flashcard cho API ôn tập / sửa thẻ
-- 1. Thẻ chưa ôn lần nào trước đây lưu stability/difficulty = 0.0; đưa về NULL đúng nghĩa "chưa ôn"
--    (SpacedRepetitionService coi NULL là lần ôn đầu).
-- 2. Index tìm thẻ trùng nội dung theo người tạo (FlashCardRepository.findByOwnerAndContent),
--    vì thẻ không còn được dùng chung giữa các user.
-- Index cho thẻ đến hạn (deck_id, next_review) đã có từ V1: idx_fc_detail_due.

UPDATE flashcard_detail
SET stability  = NULL,
    difficulty = NULL
WHERE last_review IS NULL;

CREATE INDEX idx_flashcard_owner_content
    ON flashcard (created_by, LOWER(front_content), LOWER(back_content));
