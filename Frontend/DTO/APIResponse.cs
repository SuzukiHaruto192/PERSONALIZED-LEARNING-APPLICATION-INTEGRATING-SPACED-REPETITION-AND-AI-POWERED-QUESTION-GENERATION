namespace Frontend.DTO
{
    public class APIResponse<T>
    {
        public int Code { get; set; }
        public string Message { get; set; } = "";
        public T? Data { get; set; }

        // Lỗi theo từng trường, ví dụ { "username": "Tên đăng nhập không tồn tại" }
        public Dictionary<string, string>? Errors { get; set; }

        public string Path { get; set; } = "";

        public bool IsSuccess => Code == ErrorCodes.Success;


        //Lấy mã lỗi trả về dịch sang câu Tiếng việt (không có thì trả về rỗng)
        public string GetFieldError(string field) => Errors != null && Errors.TryGetValue(field, out var value) ? ServerErrorMessages.Translate(value) : "";
    }
}
