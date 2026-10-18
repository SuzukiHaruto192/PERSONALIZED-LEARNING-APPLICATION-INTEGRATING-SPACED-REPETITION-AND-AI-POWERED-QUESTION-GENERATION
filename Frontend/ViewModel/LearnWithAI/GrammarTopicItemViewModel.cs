using System.Collections.ObjectModel;
using System.Windows.Input;
using Frontend.Model;

namespace Frontend.ViewModel
{
    public class GrammarTopicItemViewModel : BaseViewModel
    {
        private readonly GrammarTopic _model;

        public GrammarTopicItemViewModel(GrammarTopic model, Action<GrammarTopicItemViewModel> onOpen)
        {
            _model = model;
            OpenCommand = new RelayCommand(_ => onOpen(this));
        }

        public string Name => _model.Name;
        public string EnglishName => _model.EnglishName;
        public string Level => _model.Level;
        public int Mastery => _model.Mastery;
        public string MasteryText => Mastery == 0 ? "Chưa học" : $"{Mastery}%";
        public GrammarTopic Model => _model;

        private bool _isVisible = true;
        public bool IsVisible
        {
            get => _isVisible;
            set => SetProperty(ref _isVisible, value);
        }

        public ICommand OpenCommand { get; }
    }
}