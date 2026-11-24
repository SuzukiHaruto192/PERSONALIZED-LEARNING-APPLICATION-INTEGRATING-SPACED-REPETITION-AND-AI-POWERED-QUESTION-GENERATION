-- V6: thêm phân quyền cho người dùng.
-- Cột role (USER | ADMIN), mặc định USER cho mọi tài khoản cũ và mới.
-- Chỉ ADMIN được tạo/sửa/xóa skill, topic, topic group (dữ liệu danh mục dùng chung).
-- Cấp quyền admin thủ công: UPDATE users SET role = 'ADMIN' WHERE username = '<tên đăng nhập>';

ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

ALTER TABLE users
    ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));
