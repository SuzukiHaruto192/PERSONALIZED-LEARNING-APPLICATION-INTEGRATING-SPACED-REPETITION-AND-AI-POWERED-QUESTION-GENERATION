using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Frontend.ViewModel.LearnWithAI
{
    public class GrammarGroupViewModel : BaseViewModel
    {
        public string Icon { get; }
        public string Name { get; }
        public ObservableCollection<GrammarTopicItemViewModel> Topics { get; }

        private bool _hasVisibleTopics = true;
        public bool HasVisibleTopics
        {
            get => _hasVisibleTopics;
            private set => SetProperty(ref _hasVisibleTopics, value);
        }

        public GrammarGroupViewModel(string icon, string name, IEnumerable<GrammarTopicItemViewModel> topics)
        {
            Icon = icon;
            Name = name;
            Topics = new ObservableCollection<GrammarTopicItemViewModel>(topics);
        }

        public void ApplyFilter(string keyword)
        {
            foreach (var t in Topics)
            {
                t.IsVisible = keyword.Length == 0
                    || t.Name.Contains(keyword, StringComparison.OrdinalIgnoreCase)
                    || t.EnglishName.Contains(keyword, StringComparison.OrdinalIgnoreCase);
            }
            HasVisibleTopics = Topics.Any(t => t.IsVisible);
        }
    }
}
