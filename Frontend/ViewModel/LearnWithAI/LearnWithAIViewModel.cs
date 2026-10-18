using System.Windows.Input;
using Frontend.Model;

namespace Frontend.ViewModel.LearnWithAI
{
    public class LearnWithAIViewModel : BaseViewModel
    {
        public SkillPracticeViewModel Listening { get; }
        public SkillPracticeViewModel Reading { get; }
        public GrammarViewModel Grammar { get; } = new();

        private object? _currentSkill;
        public object? CurrentSkill
        {
            get => _currentSkill;
            set => SetProperty(ref _currentSkill, value);
        }

        //Dữ liệu mẫu cho AI suggestion
        public string AISuggestion { get; } =
            "Bạn hay nhầm thì Hiện tại hoàn thành. Thử ôn lại chủ đề này trong Grammar nhé!";

        public ICommand ShowListeningCommand { get; }
        public ICommand ShowReadingCommand { get; }
        public ICommand ShowGrammarCommand { get; }

        public LearnWithAIViewModel()
        {
            // Danh sách tình huống dùng chung cho Listening và Reading
            var situations = new List<SituationOption>
            {
                new() { Icon = "🎲", Name = "Ngẫu nhiên" },
                new() { Icon = "🍽", Name = "Nhà hàng" },
                new() { Icon = "🛍", Name = "Mua sắm" },
                new() { Icon = "🧭", Name = "Hỏi đường" },
                new() { Icon = "🏥", Name = "Khám bệnh" },
                new() { Icon = "✈", Name = "Sân bay" },
                new() { Icon = "🏨", Name = "Khách sạn" },
                new() { Icon = "💼", Name = "Phỏng vấn xin việc" },
                new() { Icon = "👥", Name = "Họp nhóm" },
                new() { Icon = "👋", Name = "Làm quen" },
                new() { Icon = "🎮", Name = "Sở thích" },
            };
            // end dữ liệu mẫu

            Listening = new SkillPracticeViewModel(SkillType.Listening, situations);
            Reading = new SkillPracticeViewModel(SkillType.Reading, situations);

            ShowListeningCommand = new RelayCommand(_ => CurrentSkill = Listening);
            ShowReadingCommand = new RelayCommand(_ => CurrentSkill = Reading);
            ShowGrammarCommand = new RelayCommand(_ => CurrentSkill = Grammar);

            CurrentSkill = Listening;
        }
    }
}