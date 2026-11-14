using Frontend.Model;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Input;

namespace Frontend.ViewModel.LearnWithFlashcard
{
    class FlashcardItemViewModel : BaseViewModel
    {
        private readonly Flashcard _model;

        public FlashcardItemViewModel(Flashcard model, Action<FlashcardItemViewModel> onDelete)
        {
            _model = model;
            DeleteCommand = new RelayCommand(_ => onDelete(this));
        }

        public Flashcard Model => _model;

        public string Term => _model.Term;
        public string Phonetic => _model.Phonetic;
        public string Meaning => _model.Meaning;
        public string Example => _model.Example;
        public string ExampleMeaning => _model.ExampleMeaning;
        public bool HasPhonetic => !string.IsNullOrWhiteSpace(Phonetic);
        public bool HasExample => !string.IsNullOrWhiteSpace(Example);

        public bool IsNew => _model.State == CardState.New;
        public bool IsLearning => _model.State is CardState.Learning or CardState.Relearning;
        public bool IsDue => _model.Due.LocalDateTime.Date <= DateTime.Today;

        public string StatusText
        {
            get
            {
                if (IsNew) return "Mới";
                if (IsLearning) return "Đang học";
                if (IsDue) return "Đến hạn";
                int days = (_model.Due.LocalDateTime.Date - DateTime.Today).Days;
                return $"Ôn sau {days} ngày";
            }
        }

        public ICommand DeleteCommand { get; }

        // Gọi khi Backend trả về trạng thái mới của thẻ
        public void Refresh()
        {
            OnPropertyChanged(nameof(IsNew));
            OnPropertyChanged(nameof(IsLearning));
            OnPropertyChanged(nameof(IsDue));
            OnPropertyChanged(nameof(StatusText));
        }
    }
}
