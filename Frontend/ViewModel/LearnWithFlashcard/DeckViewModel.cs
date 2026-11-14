using Frontend.Model;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Frontend.ViewModel.LearnWithFlashcard
{
    class DeckViewModel : BaseViewModel
    {
        public Deck Model { get; }
        public ObservableCollection<FlashcardItemViewModel> Cards { get; } = new();

        public DeckViewModel(Deck model)
        {
            Model = model;
            Cards.CollectionChanged += (_, _) => RefreshCounts();
        }

        public string Name => Model.Name;
        public string Icon => Model.Icon;
        public int TotalCount => Cards.Count;
        public int DueCount => Cards.Count(c => c.IsDue);

        public void RefreshCounts()
        {
            OnPropertyChanged(nameof(TotalCount));
            OnPropertyChanged(nameof(DueCount));
        }
    }
}
