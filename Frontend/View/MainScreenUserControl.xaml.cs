using Frontend.ViewModel;
using System.Windows.Controls;


namespace Frontend.View
{

    public partial class MainScreenUserControl : UserControl
    {
        private MainScreenViewModel viewModel = new MainScreenViewModel();

        public MainScreenUserControl()
        {
            InitializeComponent();
            DataContext = viewModel;
        }
    }
}
