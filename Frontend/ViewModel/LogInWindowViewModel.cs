using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.CompilerServices;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Input;

namespace Frontend.ViewModel
{
    class LogInWindowViewModel : BaseViewModel
    {

        private bool hasTriedLogin = false;         //Kiem tra da tung nhan nut dang nhap / dang ky chua
        public event Action? ClearPassword;         //Xoa du lieu trong o nhap lieu password

        public ICommand SubmitCommand { get; }
        public ICommand ChangeUICommand { get; }

        #region Properties
        private bool _isRegisterMode = false;
        public bool isRegisterMode
        {
            get => _isRegisterMode;
            set => SetProperty(ref _isRegisterMode, value);
        }

        private string _username;
        public string username 
        {
            get => _username;
            set 
            {
                SetProperty(ref _username, value);
                if (hasTriedLogin)
                    ValidateUsername();
            }
        }

        private string _password;
        public string password
        {
            get => _password;
            set
            {
                SetProperty(ref _password, value);
                if (hasTriedLogin)
                    ValidatePassword();
            }
        }

        private string _passwordConfirm;
        public string passwordConfirm
        {
            get => _passwordConfirm;
            set
            {
                SetProperty(ref _passwordConfirm, value);
                if (hasTriedLogin)
                    ValidatePasswordConfirm();
            }
        }

        private string _email;
        public string email
        {
            get => _email;
            set
            {
                SetProperty(ref _email, value);
                if (hasTriedLogin)
                    ValidateEmail();
            }
        }

        private bool _isUsernameMissing = false;
        public bool isUsernameMissing
        {
            get => _isUsernameMissing;
            set => SetProperty(ref _isUsernameMissing, value);
        }

        private bool _isPasswordMissing = false;
        public bool isPasswordMissing 
        {
            get => _isPasswordMissing;
            set => SetProperty(ref _isPasswordMissing, value);
        }

        private bool _isPasswordConfirmMissing = false;
        public bool isPasswordConfirmMissing
        {
            get => _isPasswordConfirmMissing;
            set => SetProperty(ref _isPasswordConfirmMissing, value);

        }

        private bool _isEmailMissing = false;
        public bool isEmailMissing
        {
            get => _isEmailMissing;
            set => SetProperty(ref _isEmailMissing, value);
        }

        private bool _isExistUsername = true;
        public bool isExistUsername
        {
            get => _isExistUsername;
            set => SetProperty(ref _isExistUsername, value);
        }

        private bool _isPasswordWrong = false;
        public bool isPasswordWrong
        {
            get => _isPasswordWrong;
            set => SetProperty(ref _isPasswordWrong, value);
        }

        private bool _isPasswordConfirmSuccessful = true;
        public bool isPasswordConfirmSuccessful
        {
            get => _isPasswordConfirmSuccessful;
            set => SetProperty(ref _isPasswordConfirmSuccessful, value);
        }

        private bool _isExistEmail = true;
        public bool isExistEmail
        {
            get => _isExistEmail;
            set => SetProperty(ref _isExistEmail, value);
        }
        #endregion

        public LogInWindowViewModel()
        {
            SubmitCommand = new RelayCommand(p =>
            {
                if (isRegisterMode)
                    ExecuteRegister(p);
                else
                    ExecuteLogIn(p);
            });
            ChangeUICommand = new RelayCommand(ExecuteChangeUI);
        }

        private void ExecuteLogIn(object? parameter)
        {
            hasTriedLogin = true;

            bool CanLogIn = ValidateUsername() && ValidatePassword();
            if (!CanLogIn) return;

        }

        private void ExecuteRegister(object? parameter)
        {
            hasTriedLogin = true;

            bool CanRegister = ValidateUsername() && ValidatePassword() && ValidatePasswordConfirm() && ValidateEmail();
            if (!CanRegister) return;
        }

        private void ExecuteChangeUI(object? parameter)
        {
            hasTriedLogin = false;
            isRegisterMode = !isRegisterMode;

            username = "";
            password = "";
            passwordConfirm = "";
            email = "";
            ClearPassword?.Invoke();

            isUsernameMissing = false;
            isPasswordMissing = false;
            isPasswordConfirmMissing = false;
            isEmailMissing = false;
            isExistUsername = true;
            isPasswordWrong = false;
            isPasswordConfirmSuccessful = true;
            isExistEmail = true;
        }

        #region ValidateInput
        private bool ValidateUsername()
        {
            isUsernameMissing = string.IsNullOrEmpty(username);
            return !isUsernameMissing;
        }
        private bool ValidatePassword()
        { 
            isPasswordMissing = string.IsNullOrEmpty(password);
            return !isPasswordMissing;
        }
        private bool ValidatePasswordConfirm()
        {
            isPasswordConfirmMissing = string.IsNullOrEmpty(passwordConfirm);
            return !isPasswordConfirmMissing;
        }
        private bool ValidateEmail()
        {
            isEmailMissing = string.IsNullOrEmpty(email);
            return !isEmailMissing;
        }
        #endregion
    }
}
