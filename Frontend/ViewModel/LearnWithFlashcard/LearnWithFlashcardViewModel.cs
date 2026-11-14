using Frontend.Model;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Input;

namespace Frontend.ViewModel.LearnWithFlashcard
{
    class LearnWithFlashcardViewModel : BaseViewModel
    {
        private int _nextDeckId = 1;
        private int _nextCardId = 1;

        #region Bộ thẻ
        public ObservableCollection<DeckViewModel> Decks { get; } = new();

        private DeckViewModel? _selectedDeck;
        public DeckViewModel? SelectedDeck
        {
            get => _selectedDeck;
            set
            {
                if (SetProperty(ref _selectedDeck, value))
                {
                    OnPropertyChanged(nameof(HasSelectedDeck));
                    IsAddingCard = false;
                }
            }
        }
        public bool HasSelectedDeck => SelectedDeck != null;
        #endregion

        #region Tạo bộ thẻ
        public IReadOnlyList<string> DeckIcons { get; } = new[] { "🗂", "🍽", "✈", "💼", "🏠", "🛍", "🎮", "📚" };

        private string _newDeckIcon = "🗂";
        public string NewDeckIcon { get => _newDeckIcon; set { if (value != null) SetProperty(ref _newDeckIcon, value); } }

        private string _newDeckName = "";
        public string NewDeckName { get => _newDeckName; set => SetProperty(ref _newDeckName, value); }

        public ICommand CreateDeckCommand { get; }
        #endregion

        #region Thêm thẻ
        private bool _isAddingCard;
        public bool IsAddingCard { get => _isAddingCard; set => SetProperty(ref _isAddingCard, value); }

        private string _newTerm = "";
        public string NewTerm { get => _newTerm; set => SetProperty(ref _newTerm, value); }

        private string _newPhonetic = "";
        public string NewPhonetic { get => _newPhonetic; set => SetProperty(ref _newPhonetic, value); }

        private string _newMeaning = "";
        public string NewMeaning { get => _newMeaning; set => SetProperty(ref _newMeaning, value); }

        private string _newExample = "";
        public string NewExample { get => _newExample; set => SetProperty(ref _newExample, value); }

        private string _newExampleMeaning = "";
        public string NewExampleMeaning { get => _newExampleMeaning; set => SetProperty(ref _newExampleMeaning, value); }

        public ICommand ToggleAddCardCommand { get; }
        public ICommand AddCardCommand { get; }
        #endregion

        #region Ôn tập
        private readonly Queue<FlashcardItemViewModel> _reviewQueue = new();

        private bool _isReviewing;
        public bool IsReviewing
        {
            get => _isReviewing;
            private set { if (SetProperty(ref _isReviewing, value)) OnPropertyChanged(nameof(IsManaging)); }
        }
        public bool IsManaging => !IsReviewing;

        private FlashcardItemViewModel? _currentCard;
        public FlashcardItemViewModel? CurrentCard { get => _currentCard; private set => SetProperty(ref _currentCard, value); }

        private bool _isFlipped;
        public bool IsFlipped
        {
            get => _isFlipped;
            private set { if (SetProperty(ref _isFlipped, value)) OnPropertyChanged(nameof(IsFront)); }
        }
        public bool IsFront => !IsFlipped;

        private bool _isReviewFinished;
        public bool IsReviewFinished
        {
            get => _isReviewFinished;
            private set { if (SetProperty(ref _isReviewFinished, value)) OnPropertyChanged(nameof(IsReviewActive)); }
        }
        public bool IsReviewActive => !IsReviewFinished;

        private int _reviewTotal;
        public int ReviewTotal { get => _reviewTotal; private set => SetProperty(ref _reviewTotal, value); }

        private int _completed;
        public int Completed { get => _completed; private set => SetProperty(ref _completed, value); }

        // Thời gian hiện lại ghi dưới 4 nút. Để trống cho tới khi Backend cung cấp.
        public string AgainText { get; private set; } = "";
        public string HardText { get; private set; } = "";
        public string GoodText { get; private set; } = "";
        public string EasyText { get; private set; } = "";

        public ICommand StartReviewCommand { get; }
        public ICommand FlipCommand { get; }
        public ICommand RateCommand { get; }
        public ICommand ExitReviewCommand { get; }
        #endregion

        public LearnWithFlashcardViewModel()
        {
            CreateDeckCommand = new RelayCommand(_ => CreateDeck(), _ => !string.IsNullOrWhiteSpace(NewDeckName));
            ToggleAddCardCommand = new RelayCommand(_ => IsAddingCard = !IsAddingCard, _ => HasSelectedDeck);
            AddCardCommand = new RelayCommand(_ => AddCard(), _ =>
                HasSelectedDeck && !string.IsNullOrWhiteSpace(NewTerm) && !string.IsNullOrWhiteSpace(NewMeaning));

            StartReviewCommand = new RelayCommand(_ => StartReview(), _ => SelectedDeck?.DueCount > 0);
            FlipCommand = new RelayCommand(_ => IsFlipped = !IsFlipped);
            RateCommand = new RelayCommand(Rate, _ => IsFlipped);
            ExitReviewCommand = new RelayCommand(_ => ExitReview());

            LoadSampleData();   // TODO: tải bộ thẻ và thẻ từ Backend
            SelectedDeck = Decks.FirstOrDefault();
        }

        #region Xử lý bộ thẻ và thẻ
        private void CreateDeck()
        {
            var deck = new DeckViewModel(new Deck
            {
                Id = _nextDeckId++,
                Name = NewDeckName.Trim(),
                Icon = NewDeckIcon,
                CreatedAt = DateTimeOffset.Now
            });
            Decks.Add(deck);

            SelectedDeck = deck;
            NewDeckName = "";
            IsAddingCard = true;
            // TODO: gửi bộ thẻ mới lên Backend
        }

        private void AddCard()
        {
            if (SelectedDeck == null) return;
            var now = DateTimeOffset.Now;

            var card = new Flashcard
            {
                Id = _nextCardId++,
                DeckId = SelectedDeck.Model.Id,
                Term = NewTerm.Trim(),
                Phonetic = NewPhonetic.Trim(),
                Meaning = NewMeaning.Trim(),
                Example = NewExample.Trim(),
                ExampleMeaning = NewExampleMeaning.Trim(),
                CreatedAt = now,
                State = CardState.New,
                Due = now
            };
            SelectedDeck.Cards.Insert(0, CreateItem(card, SelectedDeck));

            NewTerm = NewPhonetic = NewMeaning = NewExample = NewExampleMeaning = "";
            // TODO: gửi thẻ mới lên Backend
        }

        private FlashcardItemViewModel CreateItem(Flashcard card, DeckViewModel deck)
            => new(card, item => deck.Cards.Remove(item));   // TODO: xóa trên Backend
        #endregion

        #region Xử lý ôn tập
        private void StartReview()
        {
            if (SelectedDeck == null) return;

            _reviewQueue.Clear();
            foreach (var c in SelectedDeck.Cards.Where(c => c.IsDue).OrderBy(c => c.Model.Due))
                _reviewQueue.Enqueue(c);

            ReviewTotal = _reviewQueue.Count;
            Completed = 0;
            IsReviewFinished = false;
            IsReviewing = true;
            ShowNextCard();
        }

        private void ShowNextCard()
        {
            if (_reviewQueue.Count == 0)
            {
                CurrentCard = null;
                IsReviewFinished = true;
                return;
            }

            CurrentCard = _reviewQueue.Dequeue();
            IsFlipped = false;
        }

        private void Rate(object? parameter)
        {
            if (parameter is not ReviewRating rating || CurrentCard == null) return;

            // TODO: gửi (CurrentCard.Model.Id, rating) lên Backend,
            //       nhận State và Due mới, gán vào CurrentCard.Model rồi gọi CurrentCard.Refresh()

            Completed++;
            ShowNextCard();
        }

        private void ExitReview()
        {
            IsReviewing = false;
            CurrentCard = null;
            SelectedDeck?.RefreshCounts();
        }
        #endregion

        private void LoadSampleData()
        {
            var now = DateTimeOffset.Now;

            void AddDeck(string icon, string name,
                params (string Term, string Phon, string Mean, string Ex, string ExVi, CardState State, int DueInDays)[] cards)
            {
                var deck = new DeckViewModel(new Deck { Id = _nextDeckId++, Name = name, Icon = icon, CreatedAt = now });
                foreach (var c in cards)
                {
                    var card = new Flashcard
                    {
                        Id = _nextCardId++,
                        DeckId = deck.Model.Id,
                        Term = c.Term,
                        Phonetic = c.Phon,
                        Meaning = c.Mean,
                        Example = c.Ex,
                        ExampleMeaning = c.ExVi,
                        CreatedAt = now.AddDays(-10),
                        State = c.State,
                        Due = now.AddDays(c.DueInDays)
                    };
                    deck.Cards.Add(CreateItem(card, deck));
                }
                Decks.Add(deck);
            }

            AddDeck("🍽", "Nhà hàng",
                ("make a reservation", "/meɪk ə ˌrez.əˈveɪ.ʃən/", "đặt chỗ trước",
                 "I'd like to make a reservation for two at 7 p.m.", "Tôi muốn đặt bàn cho hai người lúc 7 giờ tối.",
                 CardState.New, 0),
                ("Could I get the bill?", "/kʊd aɪ ɡet ðə bɪl/", "Cho tôi xin hóa đơn được không?",
                 "Excuse me, could I get the bill, please?", "Xin lỗi, cho tôi xin hóa đơn nhé.",
                 CardState.Review, 0),
                ("side dish", "/ˈsaɪd ˌdɪʃ/", "món ăn kèm",
                 "Would you like a side dish with your steak?", "Bạn có muốn gọi món ăn kèm với bít tết không?",
                 CardState.Learning, 0),
                ("I'm allergic to...", "/aɪm əˈlɜː.dʒɪk tuː/", "Tôi bị dị ứng với...",
                 "I'm allergic to peanuts.", "Tôi bị dị ứng đậu phộng.",
                 CardState.Review, 5));

            AddDeck("✈", "Sân bay",
                ("boarding pass", "/ˈbɔː.dɪŋ ˌpɑːs/", "thẻ lên máy bay",
                 "Please show your boarding pass at the gate.", "Vui lòng xuất trình thẻ lên máy bay tại cửa.",
                 CardState.New, 0),
                ("check in", "/tʃek ɪn/", "làm thủ tục",
                 "We need to check in two hours before the flight.", "Chúng ta cần làm thủ tục trước chuyến bay hai tiếng.",
                 CardState.Review, -1),
                ("aisle seat", "/aɪl siːt/", "ghế cạnh lối đi",
                 "Can I have an aisle seat, please?", "Cho tôi ghế cạnh lối đi được không?",
                 CardState.Review, 3));
        }
    }
}
