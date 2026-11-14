namespace Frontend.Model
{
    public class UserProfile
    {
        public long Id { get; set; }
        public string Username { get; set; } = "";
        public string Email { get; set; } = "";
        public string DisplayName { get; set; } = "";
        public string? AvatarUrl { get; set; }
        public DateTimeOffset CreatedAt { get; set; }

        // Cài đặt học tập, nếu lưu ở Backend
        public string TargetLevel { get; set; } = "";      // ví dụ "B1"
        public double DesiredRetention { get; set; } = 0.9; // cài đặt FSRS đã bàn
    }
}
