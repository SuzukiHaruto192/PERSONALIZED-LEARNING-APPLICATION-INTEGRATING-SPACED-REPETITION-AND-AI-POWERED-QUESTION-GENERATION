-- Dữ liệu danh mục bắt buộc. Idempotent: an toàn khi chạy trên DB đã có sẵn dữ liệu.

INSERT INTO skills (code, name)
VALUES ('LISTENING', 'Listening'),
       ('READING', 'Reading'),
       ('GRAMMAR', 'Grammar')
ON CONFLICT (code) DO NOTHING;

INSERT INTO quiz_formats (skill_id, code, name, sort_order)
SELECT s.id, f.code, f.name, f.sort_order
FROM (VALUES ('LISTENING', 'CONVERSATION', 'Hội thoại', 1),
             ('LISTENING', 'PODCAST', 'Podcast ngắn', 2),
             ('LISTENING', 'ANNOUNCEMENT', 'Thông báo', 3),
             ('READING', 'MESSAGE', 'Tin nhắn', 1),
             ('READING', 'EMAIL', 'Email', 2),
             ('READING', 'SHORT_STORY', 'Truyện ngắn', 3),
             ('READING', 'ARTICLE', 'Bài báo', 4)) AS f(skill_code, code, name, sort_order)
JOIN skills s ON s.code = f.skill_code
ON CONFLICT (skill_id, code) DO NOTHING;
