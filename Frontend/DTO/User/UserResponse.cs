namespace Frontend.DTO.User
{
    public class UserResponse
    {
        public long Id { get; set; }
        public string Username { get; set; } = "";
        public string Email { get; set; } = "";
        public string PhoneNumber { get; set; } = "";
        // Backend đặt tên "currentScore" nhưng thực chất là điểm mục tiêu (0 - 10)
        [System.Text.Json.Serialization.JsonPropertyName("currentScore")]
        public decimal? TargetScore { get; set; }
        public string? AvatarUrl { get; set; }
    }
}
