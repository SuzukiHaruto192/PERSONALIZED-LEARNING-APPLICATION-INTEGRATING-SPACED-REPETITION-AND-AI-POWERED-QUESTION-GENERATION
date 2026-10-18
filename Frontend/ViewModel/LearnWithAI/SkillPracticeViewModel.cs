using System.Windows.Input;
using Frontend.Model;

namespace Frontend.ViewModel.LearnWithAI
{
    public enum SkillType { Listening, Reading }

    public class SkillPracticeViewModel : BaseViewModel
    {
        public SkillType Skill { get; }
        public string Icon { get; }
        public string Title { get; }
        public string Subtitle { get; }
        public string Hint { get; }

        public IReadOnlyList<SituationOption> Situations { get; }
        public IReadOnlyList<string> Formats { get; }
        public IReadOnlyList<string> Difficulties { get; } = new[]{ "Dễ", "Vừa", "Khó" };

        private SituationOption? _selectedSituation;
        public SituationOption? SelectedSituation
        {
            get => _selectedSituation;
            set
            {
                // Chọn tình huống có sẵn thì xóa chủ đề tự nhập
                if (SetProperty(ref _selectedSituation, value) && value != null)
                    CustomTopic = "";
            }
        }

        private string _customTopic = "";
        public string CustomTopic
        {
            get => _customTopic;
            set
            {
                // Đang tự nhập thì bỏ chọn tình huống
                if (SetProperty(ref _customTopic, value) && !string.IsNullOrWhiteSpace(value))
                    SelectedSituation = null;
            }
        }

        private string? _selectedFormat;
        public string? SelectedFormat
        {
            get => _selectedFormat;
            set => SetProperty(ref _selectedFormat, value);
        }

        private string? _selectedDifficulty;
        public string? SelectedDifficulty
        {
            get => _selectedDifficulty;
            set => SetProperty(ref _selectedDifficulty, value);
        }

        public ICommand StartCommand { get; }

        public SkillPracticeViewModel(SkillType skill, IReadOnlyList<SituationOption> situations)
        {
            Skill = skill;
            Situations = situations;

            if (skill == SkillType.Listening)
            {
                Icon = "🎧";
                Title = "Listening";
                Subtitle = "Nghe hội thoại và bài nói theo tình huống đời sống";
                Hint = "🔊 AI sẽ tạo bài nghe kèm giọng đọc, có thể mất vài giây.";
                Formats = new[] { "Hội thoại", "Podcast ngắn", "Thông báo" };
            }
            else
            {
                Icon = "📖";
                Title = "Reading";
                Subtitle = "Đọc tin nhắn, email, truyện ngắn theo tình huống";
                Hint = "📝 Sau bài đọc, bạn có thể lưu từ mới vào Flashcard.";
                Formats = new[] { "Tin nhắn", "Email", "Truyện ngắn", "Bài báo" };
            }

            SelectedSituation = Situations[0];      // "Ngẫu nhiên"
            SelectedFormat = Formats[0];
            SelectedDifficulty = "Vừa";

            StartCommand = new RelayCommand(ExecuteStart, _ => CanStart);
        }

        private bool CanStart =>
            SelectedSituation != null || !string.IsNullOrWhiteSpace(CustomTopic);

        private void ExecuteStart(object? parameter)
        {
            string topic = !string.IsNullOrWhiteSpace(CustomTopic)
                ? CustomTopic.Trim()
                : SelectedSituation!.Name;

            // gửi { Skill, topic, SelectedFormat, SelectedDifficulty } tới Backend,
            // sau đó chuyển sang màn hình làm bài.
        }
    }
}