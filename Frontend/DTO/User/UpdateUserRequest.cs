using System.Text.Json.Serialization;

namespace Frontend.DTO.User
{
    // Khớp với InformationUserRequest (PATCH /api/v1/users/{id}).
    // Trường nào null thì không gửi, Backend sẽ giữ nguyên giá trị cũ.
    public class UpdateUserRequest
    {
        [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
        public string? ImageUrl { get; set; }

        // Điểm mục tiêu (0 - 10), Backend nhận qua trường "currentScore"
        [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
        [JsonPropertyName("currentScore")]
        public decimal? TargetScore { get; set; }

        [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
        public string? Password { get; set; }

        // Bắt buộc khi đổi mật khẩu
        [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
        public string? OldPassword { get; set; }

        [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
        public string? Email { get; set; }

        [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
        public string? PhoneNumber { get; set; }

        [JsonIgnore]
        public bool IsEmpty => ImageUrl == null && TargetScore == null && Password == null
                               && Email == null && PhoneNumber == null;
    }
}
