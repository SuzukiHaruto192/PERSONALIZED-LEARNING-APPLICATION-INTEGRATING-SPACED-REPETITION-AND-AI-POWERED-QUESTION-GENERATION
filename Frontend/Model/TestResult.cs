
namespace Frontend.Model
{
    enum TestType
    {
        Listening,
        Reading,
        Grammar
    }

    class TestResult
    {
        public int Id { get; set; }
        public TestType Type { get; set; }
        public string Topic { get; set; }
        public DateTimeOffset CompletedAt { get; set; }                // Ngay lam bai
        public int DurationSeconds { get; set; }                   //Thoi gian lam bai trong bao lau (Tinh bang giay)
        public double Score { get; set; }                // Diem dat duoc

    }
}
