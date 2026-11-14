using Frontend.ViewModel.Account;
using System.Windows;
using System.Windows.Controls;

namespace Frontend.View
{
    // DataContext (AccountViewModel) do DataTemplate trong MainWindow gán, không tự tạo ở đây
    public partial class AccountUserControl : UserControl
    {
        private AccountViewModel? viewModel;

        public AccountUserControl()
        {
            InitializeComponent();
            DataContextChanged += OnDataContextChanged;
            Unloaded += (_, _) => Detach();
        }

        private void OnDataContextChanged(object sender, DependencyPropertyChangedEventArgs e)
        {
            Detach();
            viewModel = e.NewValue as AccountViewModel;
            if (viewModel == null) return;

            viewModel.ClearPassword += ClearPasswordBoxes;

            // UserControl được tạo mới mỗi lần mở trang nên các ô mật khẩu đang trống, cho VM trống theo
            viewModel.CurrentPassword = "";
            viewModel.NewPassword = "";
            viewModel.ConfirmPassword = "";
        }

        private void Detach()
        {
            if (viewModel != null) viewModel.ClearPassword -= ClearPasswordBoxes;
        }

        private void ClearPasswordBoxes()
        {
            pwbCurrentPassword.Clear();
            pwbNewPassword.Clear();
            pwbConfirmPassword.Clear();
        }

        private void pwbCurrentPassword_PasswordChanged(object sender, RoutedEventArgs e)
        {
            if (viewModel != null) viewModel.CurrentPassword = ((PasswordBox)sender).Password;
        }

        private void pwbNewPassword_PasswordChanged(object sender, RoutedEventArgs e)
        {
            if (viewModel != null) viewModel.NewPassword = ((PasswordBox)sender).Password;
        }

        private void pwbConfirmPassword_PasswordChanged(object sender, RoutedEventArgs e)
        {
            if (viewModel != null) viewModel.ConfirmPassword = ((PasswordBox)sender).Password;
        }
    }
}
