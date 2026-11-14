-- Thêm điểm mục tiêu của người dùng (thang 0.0 - 9.0, giống current_score)
ALTER TABLE users ADD COLUMN target_score NUMERIC(4, 2);
