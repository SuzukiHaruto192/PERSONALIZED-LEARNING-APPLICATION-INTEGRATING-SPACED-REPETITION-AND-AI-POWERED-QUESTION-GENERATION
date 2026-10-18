using Frontend.Model;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Frontend.ViewModel.LearnWithAI
{
    public class GrammarViewModel : BaseViewModel
    {
        public ObservableCollection<GrammarGroupViewModel> Groups { get; } = new();

        private string _searchText = "";
        public string SearchText
        {
            get => _searchText;
            set
            {
                if (SetProperty(ref _searchText, value))
                {
                    string keyword = value.Trim();
                    foreach (var g in Groups)
                        g.ApplyFilter(keyword);
                    HasResults = Groups.Any(g => g.HasVisibleTopics);
                }
            }
        }

        private bool _hasResults = true;
        public bool HasResults
        {
            get => _hasResults;
            private set => SetProperty(ref _hasResults, value);
        }

        public GrammarViewModel()
        {
            // Dữ liệu mẫu, sau này tải từ Backend
            GrammarTopicItemViewModel T(string name, string en, string level, int mastery) =>
                new(new GrammarTopic { Name = name, EnglishName = en, Level = level, Mastery = mastery }, OpenTopic);

            Groups.Add(new("⏱", "Các thì", new[]
            {
                T("Hiện tại đơn", "Present Simple", "A1", 90),
                T("Hiện tại tiếp diễn", "Present Continuous", "A1", 75),
                T("Hiện tại hoàn thành", "Present Perfect", "A2", 40),
                T("Quá khứ đơn", "Past Simple", "A1", 80),
                T("Quá khứ tiếp diễn", "Past Continuous", "A2", 0),
                T("Tương lai: will và be going to", "Future Forms", "A2", 20),
            }));

            Groups.Add(new("🧩", "Cấu trúc câu", new[]
            {
                T("Câu bị động", "Passive Voice", "B1", 30),
                T("Câu điều kiện", "Conditionals", "B1", 10),
                T("Câu tường thuật", "Reported Speech", "B1", 0),
                T("Câu hỏi đuôi", "Tag Questions", "A2", 0),
                T("Câu ước", "Wish Clauses", "B1", 0),
            }));

            Groups.Add(new("🔗", "Mệnh đề", new[]
            {
                T("Mệnh đề quan hệ", "Relative Clauses", "B1", 0),
                T("Mệnh đề chỉ thời gian", "Time Clauses", "A2", 0),
            }));

            Groups.Add(new("🛠", "Động từ khuyết thiếu", new[]
            {
                T("Can / Could", "Ability & Requests", "A1", 60),
                T("Must / Have to", "Obligation", "A2", 0),
                T("Should / Ought to", "Advice", "A2", 0),
            }));

            Groups.Add(new("📐", "Từ loại và so sánh", new[]
            {
                T("So sánh hơn và so sánh nhất", "Comparatives & Superlatives", "A2", 50),
                T("Tính từ và trạng từ", "Adjectives & Adverbs", "A2", 0),
                T("Mạo từ a / an / the", "Articles", "A1", 0),
                T("Danh động từ và động từ nguyên mẫu", "Gerunds & Infinitives", "B1", 0),
            }));
        }

        private void OpenTopic(GrammarTopicItemViewModel topic)
        {
            // TODO: gọi Backend để AI tạo phần giải thích và bài tập cho topic.Model,
            //       sau đó chuyển sang màn hình học chủ đề.
        }
    }
}
