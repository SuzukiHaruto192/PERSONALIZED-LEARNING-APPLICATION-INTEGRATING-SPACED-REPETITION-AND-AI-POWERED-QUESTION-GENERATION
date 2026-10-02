using Frontend.ViewModel;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.CompilerServices;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Shapes;

namespace Frontend.View
{
    public partial class LogInWindow : Window
    {
        private bool IsRegister = false;
        private LogInWindowViewModel viewModel;

        public LogInWindow()
        {
            InitializeComponent();

            viewModel = new LogInWindowViewModel();
            DataContext = viewModel;

            viewModel.ClearPassword += () =>
            {
                pwbPassword.Clear();
                pwbConfirmPassword.Clear();
            };
        }

        private void Button_Close(object sender, RoutedEventArgs e)
        {
            this.Close();
        }

        private void pwbPassword_PasswordChanged(object sender, RoutedEventArgs e)
        {
            viewModel.password = ((PasswordBox)sender).Password;   
        }

        private void pwbConfirmPassword_PasswordChanged(object sender, RoutedEventArgs e)
        {
            viewModel.passwordConfirm = ((PasswordBox)sender).Password;
        }
    }
}
