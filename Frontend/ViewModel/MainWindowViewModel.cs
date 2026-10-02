using Frontend.View;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Frontend.ViewModel
{
    class MainWindowViewModel : BaseViewModel
    {
        private MainScreenUserControl _mainScreenUC = new MainScreenUserControl();

        private object _currentView;
        public object CurrentView 
        {
            get => _currentView;
            set => SetProperty(ref _currentView, value);
        }

        public MainWindowViewModel() 
        {
            CurrentView = _mainScreenUC;
        }
    }
}
