namespace Frontend.DTO.User
{
    public class UserResponse
    {
        public long Id { get; set; }
        public string Username { get; set; } = "";
        public string Email { get; set; } = "";
        public string PhoneNumber { get; set; } = "";
        // Điểm mục tiêu (0 - 9)
        [System.Text.Json.Serialization.JsonPropertyName("targetScore")]
        public decimal? TargetScore { get; set; }
        public string? AvatarUrl { get; set; }
    }
}
