namespace Frontend.DTO
{
    // Dịch mã lỗi validation của Backend sang câu tiếng Việt.
    static class ServerErrorMessages
    {
        private static readonly Dictionary<string, string> Messages = new()
        {
            ["USERNAME_BLANK"] = "Tên đăng nhập không được để trống",
            ["USERNAME_INVALID_SIZE"] = "Tên đăng nhập phải từ 6 đến 50 ký tự",
            ["USERNAME_INVALID_PATTERN"] = "Tên đăng nhập chỉ được chứa chữ cái và số",
            ["PASSWORD_BLANK"] = "Mật khẩu không được để trống",
            ["PASSWORD_INVALID_SIZE"] = "Mật khẩu phải từ 6 đến 50 ký tự",
            ["PASSWORD_INVALID_PATTERN"] = "Mật khẩu phải có ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt",
            ["EMAIL_BLANK"] = "Email không được để trống",
            ["EMAIL_INVALID_FORMAT"] = "Định dạng email không hợp lệ",
            ["PHONE_BLANK"] = "Số điện thoại không được để trống",
            ["PHONE_INVALID_SIZE"] = "Số điện thoại phải từ 10 đến 15 ký tự",
            ["PHONE_INVALID_PATTERN"] = "Số điện thoại chỉ được chứa chữ số",
            // Backend dùng trường currentScore để lưu điểm mục tiêu
            ["CURRENT_SCORE_MIN"] = "Điểm mục tiêu tối thiểu là 0.0",
            ["CURRENT_SCORE_MAX"] = "Điểm mục tiêu vượt quá mức tối đa máy chủ cho phép",
            ["TARGET_SCORE_MIN"] = "Điểm mục tiêu tối thiểu là 0.0",
            ["TARGET_SCORE_MAX"] = "Điểm mục tiêu tối đa là 10.0",
        };

        public static string Translate(string value)
            => Messages.TryGetValue(value, out var text) ? text : value;
    }
}
