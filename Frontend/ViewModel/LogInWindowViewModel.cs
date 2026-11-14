using Frontend.DTO;
using Frontend.DTO.Auth;
using Frontend.DTO.User;
using System.Text.RegularExpressions;
using System.Windows;
using System.Windows.Input;
using static SkiaSharp.HarfBuzz.SKShaper;

namespace Frontend.ViewModel
{
    class LogInWindowViewModel : BaseViewModel
    {
        private const string LoginUrl = "/api/v1/auth/login";
        private const string RegisterUrl = "/api/v1/users";

        // Quy tắc giống hệt SignupRequest phía Backend
        private static readonly Regex UsernamePattern = new(@"^[a-zA-Z0-9]+$");
        private static readonly Regex PasswordPattern = new(@"(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%/|&])^[a-zA-Z0-9!@#$%/|&]+$");
        private static readonly Regex EmailPattern = new(@"^[^@\s]+@[^@\s]+\.[^@\s]+$");
        private static readonly Regex PhonePattern = new(@"^[0-9]+$");

        private bool hasTriedSubmit = false;   // đã bấm nút chính ít nhất một lần chưa
        public event Action? ClearPassword;
        public event Action? LoginSucceeded;

        public ICommand SubmitCommand { get; }
        public ICommand ChangeUICommand { get; }

        #region Properties
        private bool _isRegisterMode;
        public bool isRegisterMode { get => _isRegisterMode; set => SetProperty(ref _isRegisterMode, value); }

        private string _username = "";
        public string username
        {
            get => _username;
            set { if (SetProperty(ref _username, value)) OnInputChanged(ValidateUsername); }
        }

        private string _password = "";
        public string password
        {
            get => _password;
            set
            {
                if (SetProperty(ref _password, value))
                {
                    OnInputChanged(ValidatePassword);
                    // Đổi mật khẩu thì kiểm tra lại ô xác nhận (nếu đã nhập)
                    if (hasTriedSubmit && isRegisterMode && passwordConfirm != "") ValidatePasswordConfirm();
                }
            }
        }

        private string _passwordConfirm = "";
        public string passwordConfirm
        {
            get => _passwordConfirm;
            set { if (SetProperty(ref _passwordConfirm, value)) OnInputChanged(ValidatePasswordConfirm); }
        }

        private string _email = "";
        public string email
        {
            get => _email;
            set { if (SetProperty(ref _email, value)) OnInputChanged(ValidateEmail); }
        }

        private string _phoneNumber = "";
        public string phoneNumber
        {
            get => _phoneNumber;
            set { if (SetProperty(ref _phoneNumber, value)) OnInputChanged(ValidatePhoneNumber); }
        }

        private bool _isBusy;
        public bool isBusy { get => _isBusy; set => SetProperty(ref _isBusy, value); }
        #endregion

        #region Errors
        private string _usernameError = "";
        public string usernameError { get => _usernameError; set => SetProperty(ref _usernameError, value); }

        private string _passwordError = "";
        public string passwordError { get => _passwordError; set => SetProperty(ref _passwordError, value); }

        private string _passwordConfirmError = "";
        public string passwordConfirmError { get => _passwordConfirmError; set => SetProperty(ref _passwordConfirmError, value); }

        private string _emailError = "";
        public string emailError { get => _emailError; set => SetProperty(ref _emailError, value); }

        private string _phoneNumberError = "";
        public string phoneNumberError { get => _phoneNumberError; set => SetProperty(ref _phoneNumberError, value); }
        #endregion

        public LogInWindowViewModel()
        {
            SubmitCommand = new RelayCommand(_ =>
            {
                if (isRegisterMode) ExecuteRegister();
                else ExecuteLogIn();
            }, _ => !isBusy);

            ChangeUICommand = new RelayCommand(ExecuteChangeUI, _ => !isBusy);
        }


        //Chỉ khi bấm nút submit ít nhất 1 lần mới báo lỗi
        private void OnInputChanged(Func<bool> validate)
        {
            if (hasTriedSubmit) validate();
        }

        #region Login
        private async void ExecuteLogIn()
        {
            hasTriedSubmit = true;

            bool canLogIn = ValidateUsername() & ValidatePassword();
            if (!canLogIn) return;

            isBusy = true;
            try
            {
                var result = await APIClient.PostAsync<string>(LoginUrl,
                    new LoginRequest { Username = username.Trim(), Password = password });

                if (result.IsSuccess && !string.IsNullOrEmpty(result.Data))
                {
                    Session.Start(result.Data);
                    password = "";
                    ClearPassword?.Invoke();
                    LoginSucceeded?.Invoke();
                    return;
                }

                HandleLoginError(result);
            }
            finally
            {
                isBusy = false;
                CommandManager.InvalidateRequerySuggested();
            }
        }

        private void HandleLoginError(APIResponse<string> result)
        {
            switch (result.Code)
            {
                case ErrorCodes.BadCredentials:
                case ErrorCodes.Uncategorized:   // Backend hiện trả 9999 khi tên không tồn tại
                    passwordError = "Sai tên đăng nhập hoặc mật khẩu!";
                    break;

                case ErrorCodes.InvalidInput:
                    ApplyFieldErrors(result);
                    break;

                case ErrorCodes.NetworkError:
                    MessageBox.Show(result.Message, "Không có kết nối",
                                    MessageBoxButton.OK, MessageBoxImage.Warning);
                    break;
                default:
                    MessageBox.Show("Vui lòng thử lại sau!", "Đăng nhập không thành công",
                                    MessageBoxButton.OK, MessageBoxImage.Error);
                    break;
            }
        }
        #endregion

        #region Register
        private async void ExecuteRegister()
        {
            hasTriedSubmit = true;

            bool canRegister = ValidateUsername() & ValidatePassword() & ValidatePasswordConfirm()
                             & ValidateEmail() & ValidatePhoneNumber();
            if (!canRegister) return;

            isBusy = true;
            try
            {
                var result = await APIClient.PostAsync<UserResponse>(RegisterUrl, new RegisterRequest
                {
                    Username = username.Trim(),
                    Password = password,
                    Email = email.Trim(),
                    PhoneNumber = phoneNumber.Trim()
                });

                if (result.IsSuccess && result.Data != null)
                {
                    MessageBox.Show(
                        $"Đăng ký thành công tài khoản \"{result.Data.Username}\".\nHãy đăng nhập để bắt đầu học.",
                        "Đăng ký thành công", MessageBoxButton.OK, MessageBoxImage.Information);

                    string registeredName = result.Data.Username;
                    ExecuteChangeUI(null);
                    username = registeredName;
                    return;
                }

                HandleRegisterError(result);
            }
            finally
            {
                isBusy = false;
                CommandManager.InvalidateRequerySuggested();
            }
        }

        private void HandleRegisterError(APIResponse<UserResponse> result)
        {
            switch (result.Code)
            {
                case ErrorCodes.UserExisted:
                    usernameError = result.Message;
                    break;

                case ErrorCodes.EmailExisted:
                    emailError = result.Message;
                    break;

                case ErrorCodes.PhoneExisted:
                    phoneNumberError = result.Message;
                    break;

                case ErrorCodes.InvalidInput:
                    ApplyFieldErrors(result);
                    break;

                case ErrorCodes.NetworkError:
                    MessageBox.Show(result.Message, "Không có kết nối",
                                    MessageBoxButton.OK, MessageBoxImage.Warning);
                    break;
                default:
                    MessageBox.Show(
                        string.IsNullOrWhiteSpace(result.Message) ? "Vui lòng thử lại sau!" : result.Message,
                        "Đăng ký không thành công", MessageBoxButton.OK, MessageBoxImage.Error);
                    break;
            }
        }
        #endregion


        private void ApplyFieldErrors<T>(APIResponse<T> result)
        {
            usernameError = result.GetFieldError("username");
            passwordError = result.GetFieldError("password");
            emailError = result.GetFieldError("email");
            phoneNumberError = result.GetFieldError("phoneNumber");
        }


        private void ExecuteChangeUI(object? parameter)
        {
            hasTriedSubmit = false;   // đặt trước để các setter bên dưới không kiểm tra lại
            isRegisterMode = !isRegisterMode;

            username = "";
            password = "";
            passwordConfirm = "";
            email = "";
            phoneNumber = "";
            ClearPassword?.Invoke();

            usernameError = "";
            passwordError = "";
            passwordConfirmError = "";
            emailError = "";
            phoneNumberError = "";
        }

        #region Validate input
        private bool ValidateUsername()
        {
            string value = username.Trim();

            if (value == "")
                usernameError = "Vui lòng nhập tên đăng nhập!";
            else if (isRegisterMode && (value.Length < 6 || value.Length > 50))
                usernameError = "Tên đăng nhập phải từ 6 đến 50 ký tự";
            else if (isRegisterMode && !UsernamePattern.IsMatch(value))
                usernameError = "Tên đăng nhập chỉ được chứa chữ cái và số";
            else
                usernameError = "";

            return usernameError == "";
        }

        private bool ValidatePassword()
        {
            if (password == "")
                passwordError = "Vui lòng nhập mật khẩu!";
            else if (isRegisterMode && (password.Length < 6 || password.Length > 50))
                passwordError = "Mật khẩu phải từ 6 đến 50 ký tự";
            else if (isRegisterMode && !PasswordPattern.IsMatch(password))
                passwordError = "Mật khẩu cần chữ hoa, chữ thường, số và 1 ký tự trong ! @ # $ % / | &";
            else
                passwordError = "";

            return passwordError == "";
        }

        private bool ValidatePasswordConfirm()
        {
            if (passwordConfirm == "")
                passwordConfirmError = "Vui lòng xác nhận mật khẩu!";
            else if (passwordConfirm != password)
                passwordConfirmError = "Mật khẩu không trùng khớp!";
            else
                passwordConfirmError = "";

            return passwordConfirmError == "";
        }

        private bool ValidateEmail()
        {
            string value = email.Trim();

            if (value == "")
                emailError = "Vui lòng nhập email!";
            else if (!EmailPattern.IsMatch(value))
                emailError = "Định dạng email không hợp lệ";
            else
                emailError = "";

            return emailError == "";
        }

        private bool ValidatePhoneNumber()
        {
            string value = phoneNumber.Trim();

            if (value == "")
                phoneNumberError = "Vui lòng nhập số điện thoại!";
            else if (!PhonePattern.IsMatch(value))
                phoneNumberError = "Số điện thoại chỉ được chứa chữ số";
            else if (value.Length < 10 || value.Length > 15)
                phoneNumberError = "Số điện thoại phải từ 10 đến 15 ký tự";
            else
                phoneNumberError = "";

            return phoneNumberError == "";
        }
        #endregion
    }
}
