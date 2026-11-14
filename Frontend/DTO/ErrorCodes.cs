namespace Frontend.DTO
{
    static class ErrorCodes
    {
        #region LoginError
        //Auth and User Business Error
        public const int Success = 1000;
        public const int Unauthenticated = 1001;    //Token sai hoặc hết hạn
        public const int AccessDenied = 1002;       //Từ chối thực hiện
        public const int UserNotFound = 1004;
        public const int BadCredentials = 1005;     // Sai tên đăng nhập hoặc mật khẩu
        public const int InvalidInput = 1006;       // Dữ liệu đầu vào không hợp lệ (kèm errors theo từng trường)
        
        // Duplicate Errors
        public const int EmailExisted = 1018;
        public const int PhoneExisted = 1019;
        public const int UserExisted = 1003;
        #endregion

        #region Đổi mật khẩu
        public const int OldPasswordRequired = 1100;
        public const int OldPasswordIncorrect = 1101;
        public const int NewPasswordSameAsOld = 1102;
        #endregion




        public const int Uncategorized = 9999;      // Lỗi không xác định
        public const int NetworkError = -1;   // do APIClient tự tạo khi lỗi mạng
    }
}
