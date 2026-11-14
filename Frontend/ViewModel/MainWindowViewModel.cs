using System.Windows.Input;
using Frontend.DTO;
using Frontend.ViewModel.Account;
using Frontend.ViewModel.LearnWithAI;
using Frontend.ViewModel.LearnWithFlashcard;   // chỉ cần nếu bạn chọn cách 2 ở câu trước

namespace Frontend.ViewModel
{
    class MainWindowViewModel : BaseViewModel
    {
        #region ViewModel của từng trang
        private readonly MainScreenViewModel _mainScreenVM = new();
        private readonly LearnWithAIViewModel _learnWithAIVM = new();
        private readonly StatisticsViewModel _statisticsVM = new();
        private readonly LearnWithFlashcardViewModel _learnWithFlashcardVM= new();
        private readonly AccountViewModel _accountVM = new();
        #endregion

        #region Commands
        public ICommand ShowMainScreenCommand { get; }
        public ICommand ShowLearnWithAICommand { get; }
        public ICommand ShowStatisticsCommand { get; }
        public ICommand ShowLearnWithFlashcardCommand { get; }
        public ICommand ShowAccountCommand { get; }
        #endregion

        #region Trạng thái sáng của các tab
        public bool IsLearnWithAIActive => CurrentView == _learnWithAIVM;
        public bool IsStatisticsActive => CurrentView == _statisticsVM;
        public bool IsLearnWithFlashcardActive => CurrentView == _learnWithFlashcardVM;
        public bool IsAccountActive => CurrentView == _accountVM;
        #endregion

        // Chữ cái đầu của username hiện trên nút tài khoản
        public string UserInitial => string.IsNullOrEmpty(Session.Username) ? "?" : Session.Username[..1].ToUpper();

        // Đăng xuất / hết phiên: MainWindow mở lại màn hình đăng nhập
        public event Action? LoggedOut;

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
                    OnPropertyChanged(nameof(IsLearnWithFlashcardActive));
                    OnPropertyChanged(nameof(IsAccountActive));
                }
            }
        }

        public MainWindowViewModel()
        {
            ShowMainScreenCommand = new RelayCommand(_ => CurrentView = _mainScreenVM);
            ShowLearnWithAICommand = new RelayCommand(_ => CurrentView = _learnWithAIVM);
            ShowStatisticsCommand = new RelayCommand(_ => CurrentView = _statisticsVM);
            ShowLearnWithFlashcardCommand = new RelayCommand(_ => CurrentView = _learnWithFlashcardVM);
            ShowAccountCommand = new RelayCommand(async _ =>
            {
                CurrentView = _accountVM;
                await _accountVM.LoadAsync();   // mỗi lần mở trang thì lấy dữ liệu mới nhất
            });

            _accountVM.LoggedOut += () => LoggedOut?.Invoke();

            CurrentView = _mainScreenVM;
        }
    }
}