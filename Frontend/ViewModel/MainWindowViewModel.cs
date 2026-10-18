using System.Windows.Input;
using Frontend.ViewModel.LearnWithAI;   // chỉ cần nếu bạn chọn cách 2 ở câu trước

namespace Frontend.ViewModel
{
    class MainWindowViewModel : BaseViewModel
    {
        #region ViewModel của từng trang
        private readonly MainScreenViewModel _mainScreenVM = new();
        private readonly LearnWithAIViewModel _learnWithAIVM = new();
        private readonly StatisticsViewModel _statisticsVM = new();
        #endregion

        #region Commands
        public ICommand ShowMainScreenCommand { get; }
        public ICommand ShowLearnWithAICommand { get; }
        public ICommand ShowStatisticsCommand { get; }
        #endregion

        #region Trạng thái sáng của các tab
        public bool IsLearnWithAIActive => CurrentView == _learnWithAIVM;
        public bool IsStatisticsActive => CurrentView == _statisticsVM;
        #endregion

        private object? _currentView;
        public object? CurrentView
        {
            get => _currentView;
            set
            {
                if (SetProperty(ref _currentView, value))
                {
                    OnPropertyChanged(nameof(IsLearnWithAIActive));
                   OnPropertyChanged(nameof(IsStatisticsActive));
                }
            }
        }

        public MainWindowViewModel()
        {
            ShowMainScreenCommand = new RelayCommand(_ => CurrentView = _mainScreenVM);
            ShowLearnWithAICommand = new RelayCommand(_ => CurrentView = _learnWithAIVM);
            ShowStatisticsCommand = new RelayCommand(_ => CurrentView = _statisticsVM);

            CurrentView = _mainScreenVM;
        }
    }
}