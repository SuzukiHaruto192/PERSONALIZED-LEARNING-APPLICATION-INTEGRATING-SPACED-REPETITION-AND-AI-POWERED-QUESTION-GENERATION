using Frontend.DTO;
using Frontend.DTO.User;
using System.Globalization;
using System.Text.RegularExpressions;
using System.Windows;
using System.Windows.Input;

namespace Frontend.ViewModel.Account
{
    // Trang "Thông tin tài khoản": GET + PATCH /api/v1/users/me, POST /api/v1/auth/logout
    class AccountViewModel : BaseViewModel
    {
        // Backend tự lấy user từ token nên không cần gửi id
        private const string MeUrl = "/api/v1/users/me";
        private const string LogoutUrl = "/api/v1/auth/logout";

        // Quy tắc giống InformationUserRequest phía Backend
        private static readonly Regex PasswordPattern = new(@"(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%/|&])^[a-zA-Z0-9!@#$%/|&]+$");
        private static readonly Regex EmailPattern = new(@"^[^@\s]+@[^@\s]+\.[^@\s]+$");
        private static readonly Regex PhonePattern = new(@"^[0-9]+$");

        private UserResponse? _original;       // dữ liệu lần tải gần nhất từ server
        private bool _hasTriedSave;            // đã bấm Lưu ít nhất một lần chưa

        public event Action? ClearPassword;    // View xóa nội dung các ô PasswordBox
        public event Action? LoggedOut;        // MainWindow quay về màn hình đăng nhập

        public ICommand SaveCommand { get; }
        public ICommand ResetCommand { get; }
        public ICommand ReloadCommand { get; }
        public ICommand LogoutCommand { get; }

        #region Trạng thái
        private bool _isLoading;
        public bool IsLoading
        {
            get => _isLoading;
            set { if (SetProperty(ref _isLoading, value)) OnPropertyChanged(nameof(HasProfile)); }
        }

        private bool _isBusy;
        public bool IsBusy { get => _isBusy; set => SetProperty(ref _isBusy, value); }

        private string _loadError = "";
        public string LoadError { get => _loadError; set => SetProperty(ref _loadError, value); }

        public bool HasProfile => _original != null && !IsLoading;

        public bool HasChanges => _original != null && (
               !IsUnchanged(Email, _original.Email)
            || !IsUnchanged(PhoneNumber, _original.PhoneNumber)
            || !IsUnchanged(AvatarUrl, _original.AvatarUrl)
            || IsScoreChanged()
            || CurrentPassword != "" || NewPassword != "");
        #endregion

        #region Thông tin chỉ hiển thị
        public string Username => _original?.Username ?? Session.Username;

        public string UserInitial => string.IsNullOrEmpty(Username) ? "?" : Username[..1].ToUpper();

        public string TargetScoreDisplay => _original?.TargetScore is decimal s ? FormatScore(s) : "Chưa có";

        #region Tiến độ: điểm hiện tại (từ kết quả bài làm) so với mục tiêu
        public const double MaxScore = 9;

        // Điểm trung bình từ các bộ đề đã làm.
        // TODO: Backend chưa có API thống kê kết quả bài làm, khi có thì gán giá trị ở LoadAsync.
        private decimal? _achievedScore;
        public decimal? AchievedScore
        {
            get => _achievedScore;
            set
            {
                if (SetProperty(ref _achievedScore, value)) NotifyProgress();
            }
        }

        public bool HasAchievedScore => AchievedScore != null;
        public double AchievedValue => (double)(AchievedScore ?? 0);
        public string AchievedText => AchievedScore is decimal s ? FormatScore(s) : "—";

        // Mục tiêu đang nhập (nếu hợp lệ), không thì mục tiêu đã lưu
        public decimal? TargetPreview
        {
            get
            {
                if (TryParseScore(TargetScoreText, out decimal typed) && typed >= 0 && typed <= (decimal)MaxScore)
                    return typed;
                return _original?.TargetScore;
            }
        }

        public bool HasTargetPreview => TargetPreview != null;
        public double TargetValue => (double)(TargetPreview ?? 0);
        public string TargetText => TargetPreview is decimal t ? FormatScore(t) : "—";

        public string ProgressMessage
        {
            get
            {
                if (TargetPreview == null) return "Đặt điểm mục tiêu để theo dõi tiến độ.";
                if (AchievedScore == null) return "Chưa có kết quả bài làm. Hoàn thành một bộ đề để xem bạn còn cách mục tiêu bao xa.";
                decimal gap = TargetPreview.Value - AchievedScore.Value;
                return gap <= 0
                    ? "Bạn đã đạt mục tiêu! Hãy cân nhắc đặt mục tiêu cao hơn."
                    : $"Còn {FormatScore(gap)} điểm nữa để đạt mục tiêu.";
            }
        }

        public bool IsTargetReached => TargetPreview != null && AchievedScore != null && AchievedScore >= TargetPreview;

        private void NotifyProgress()
        {
            OnPropertyChanged(nameof(HasAchievedScore));
            OnPropertyChanged(nameof(AchievedValue));
            OnPropertyChanged(nameof(AchievedText));
            OnPropertyChanged(nameof(TargetPreview));
            OnPropertyChanged(nameof(HasTargetPreview));
            OnPropertyChanged(nameof(TargetValue));
            OnPropertyChanged(nameof(TargetText));
            OnPropertyChanged(nameof(ProgressMessage));
            OnPropertyChanged(nameof(IsTargetReached));
        }
        #endregion
        #endregion

        #region Ô nhập
        private string _email = "";
        public string Email
        {
            get => _email;
            set { if (SetProperty(ref _email, value)) OnInputChanged(ValidateEmail); }
        }

        private string _phoneNumber = "";
        public string PhoneNumber
        {
            get => _phoneNumber;
            set { if (SetProperty(ref _phoneNumber, value)) OnInputChanged(ValidatePhoneNumber); }
        }

        private string _targetScoreText = "";
        public string TargetScoreText
        {
            get => _targetScoreText;
            set
            {
                if (SetProperty(ref _targetScoreText, value))
                {
                    OnInputChanged(ValidateTargetScore);
                    NotifyProgress();
                }
            }
        }

        private string _avatarUrl = "";
        public string AvatarUrl
        {
            get => _avatarUrl;
            set { if (SetProperty(ref _avatarUrl, value)) OnInputChanged(ValidateAvatarUrl); }
        }

        // PasswordBox không binding được, code-behind gán vào 3 property mật khẩu
        private string _currentPassword = "";
        public string CurrentPassword
        {
            get => _currentPassword;
            set
            {
                if (SetProperty(ref _currentPassword, value))
                {
                    OnInputChanged(ValidateCurrentPassword);
                    if (_hasTriedSave) ValidateNewPassword();
                }
            }
        }

        private string _newPassword = "";
        public string NewPassword
        {
            get => _newPassword;
            set
            {
                if (SetProperty(ref _newPassword, value))
                {
                    OnInputChanged(ValidateNewPassword);
                    if (_hasTriedSave) ValidateCurrentPassword();
                    if (_hasTriedSave && ConfirmPassword != "") ValidateConfirmPassword();
                }
            }
        }

        private string _confirmPassword = "";
        public string ConfirmPassword
        {
            get => _confirmPassword;
            set { if (SetProperty(ref _confirmPassword, value)) OnInputChanged(ValidateConfirmPassword); }
        }
        #endregion

        #region Lỗi
        private string _emailError = "";
        public string EmailError { get => _emailError; set => SetProperty(ref _emailError, value); }

        private string _phoneNumberError = "";
        public string PhoneNumberError { get => _phoneNumberError; set => SetProperty(ref _phoneNumberError, value); }

        private string _targetScoreError = "";
        public string TargetScoreError { get => _targetScoreError; set => SetProperty(ref _targetScoreError, value); }

        private string _avatarUrlError = "";
        public string AvatarUrlError { get => _avatarUrlError; set => SetProperty(ref _avatarUrlError, value); }

        private string _currentPasswordError = "";
        public string CurrentPasswordError { get => _currentPasswordError; set => SetProperty(ref _currentPasswordError, value); }

        private string _newPasswordError = "";
        public string NewPasswordError { get => _newPasswordError; set => SetProperty(ref _newPasswordError, value); }

        private string _confirmPasswordError = "";
        public string ConfirmPasswordError { get => _confirmPasswordError; set => SetProperty(ref _confirmPasswordError, value); }
        #endregion

        public AccountViewModel()
        {
            SaveCommand = new RelayCommand(_ => ExecuteSave(), _ => !IsBusy && !IsLoading && HasChanges);
            ResetCommand = new RelayCommand(_ => ResetInputs(), _ => !IsBusy && HasChanges);
            ReloadCommand = new RelayCommand(async _ => await LoadAsync(force: true), _ => !IsBusy && !IsLoading);
            LogoutCommand = new RelayCommand(_ => ExecuteLogout(), _ => !IsBusy);
        }

        // Mỗi lần ô nhập đổi: báo lại HasChanges, và chỉ kiểm tra lỗi sau khi đã bấm Lưu
        private void OnInputChanged(Func<bool> validate)
        {
            OnPropertyChanged(nameof(HasChanges));
            if (_hasTriedSave) validate();
        }

        #region Tải thông tin
        // force = false: dùng khi mở trang, không ghi đè nếu người dùng đang sửa dở
        public async Task LoadAsync(bool force = false)
        {
            if (IsLoading || IsBusy) return;
            if (!force && HasChanges) return;

            if (force && HasChanges &&
                MessageBox.Show("Các thay đổi chưa lưu sẽ bị mất. Tiếp tục tải lại?", "Tải lại thông tin",
                                MessageBoxButton.YesNo, MessageBoxImage.Question) != MessageBoxResult.Yes)
                return;

            IsLoading = true;
            LoadError = "";
            try
            {
                var result = await APIClient.GetAsync<UserResponse>(MeUrl);

                if (result.IsSuccess && result.Data != null)
                {
                    _original = result.Data;
                    ResetInputs();
                    return;
                }

                if (result.Code == ErrorCodes.Unauthenticated)
                {
                    HandleSessionExpired();
                    return;
                }

                LoadError = string.IsNullOrWhiteSpace(result.Message)
                    ? "Không tải được thông tin tài khoản." : result.Message;
            }
            finally
            {
                IsLoading = false;
                CommandManager.InvalidateRequerySuggested();
            }
        }

        // Đưa các ô nhập về đúng dữ liệu đã lưu trên server
        private void ResetInputs()
        {
            _hasTriedSave = false;
            if (_original != null)
            {
                Email = _original.Email ?? "";
                PhoneNumber = _original.PhoneNumber ?? "";
                AvatarUrl = _original.AvatarUrl ?? "";
                TargetScoreText = FormatScore(_original.TargetScore);
            }

            CurrentPassword = "";
            NewPassword = "";
            ConfirmPassword = "";
            ClearPassword?.Invoke();

            EmailError = PhoneNumberError = TargetScoreError = "";
            AvatarUrlError = CurrentPasswordError = NewPasswordError = ConfirmPasswordError = "";

            OnPropertyChanged(nameof(Username));
            OnPropertyChanged(nameof(UserInitial));
            OnPropertyChanged(nameof(TargetScoreDisplay));
            NotifyProgress();
            OnPropertyChanged(nameof(HasProfile));
            OnPropertyChanged(nameof(HasChanges));
        }
        #endregion

        #region Lưu thay đổi
        private async void ExecuteSave()
        {
            if (_original == null) return;
            _hasTriedSave = true;

            bool isValid = ValidateEmail() & ValidatePhoneNumber() & ValidateTargetScore()
                         & ValidateAvatarUrl() & ValidateCurrentPassword() & ValidateNewPassword() & ValidateConfirmPassword();
            if (!isValid) return;

            UpdateUserRequest request = BuildRequest();
            if (request.IsEmpty)
            {
                MessageBox.Show("Bạn chưa thay đổi thông tin nào.", "Thông tin tài khoản",
                                MessageBoxButton.OK, MessageBoxImage.Information);
                return;
            }

            bool success = false;
            IsBusy = true;
            try
            {
                // Backend trả ApiResponse<Void> nên Data luôn rỗng
                var result = await APIClient.PatchAsync<object>(MeUrl, request);

                if (result.IsSuccess)
                {
                    success = true;
                    MessageBox.Show(request.Password != null
                                        ? "Đã cập nhật thông tin và đổi mật khẩu thành công."
                                        : "Đã cập nhật thông tin thành công.",
                                    "Cập nhật thành công", MessageBoxButton.OK, MessageBoxImage.Information);
                }
                else
                {
                    HandleSaveError(result, request);
                }
            }
            finally
            {
                IsBusy = false;
                CommandManager.InvalidateRequerySuggested();
            }

            if (success)
            {
                ApplyToOriginal(request);   // cập nhật ngay trên giao diện
                ResetInputs();
                await LoadAsync();          // rồi lấy lại dữ liệu chuẩn từ server
            }
        }

        // Chỉ gửi những trường đã thay đổi
        private UpdateUserRequest BuildRequest()
        {
            var request = new UpdateUserRequest();
            if (_original == null) return request;

            if (!IsUnchanged(Email, _original.Email)) request.Email = Email.Trim();
            if (!IsUnchanged(PhoneNumber, _original.PhoneNumber)) request.PhoneNumber = PhoneNumber.Trim();
            if (!IsUnchanged(AvatarUrl, _original.AvatarUrl)) request.ImageUrl = AvatarUrl.Trim();
            if (IsScoreChanged() && TryParseScore(TargetScoreText, out decimal score)) request.TargetScore = score;
            if (NewPassword != "")
            {
                request.Password = NewPassword;
                request.OldPassword = CurrentPassword;
            }

            return request;
        }

        private void ApplyToOriginal(UpdateUserRequest request)
        {
            if (_original == null) return;
            if (request.Email != null) _original.Email = request.Email;
            if (request.PhoneNumber != null) _original.PhoneNumber = request.PhoneNumber;
            if (request.ImageUrl != null) _original.AvatarUrl = request.ImageUrl;
            if (request.TargetScore != null) _original.TargetScore = request.TargetScore;
        }

        private void HandleSaveError(APIResponse<object> result, UpdateUserRequest request)
        {
            switch (result.Code)
            {
                case ErrorCodes.InvalidInput:
                    ApplyFieldErrors(result);
                    break;

                case ErrorCodes.EmailExisted:
                    EmailError = result.Message;
                    break;

                case ErrorCodes.OldPasswordRequired:
                case ErrorCodes.OldPasswordIncorrect:
                    CurrentPasswordError = result.Message;
                    break;

                case ErrorCodes.NewPasswordSameAsOld:
                    NewPasswordError = result.Message;
                    break;

                case ErrorCodes.PhoneExisted:
                    PhoneNumberError = result.Message;
                    break;

                case ErrorCodes.Unauthenticated:
                    HandleSessionExpired();
                    break;

                case ErrorCodes.NetworkError:
                    MessageBox.Show(result.Message, "Không có kết nối",
                                    MessageBoxButton.OK, MessageBoxImage.Warning);
                    break;

                // Backend chưa kiểm tra trùng email: email trùng sẽ vi phạm unique trong DB và trả 9999
                case ErrorCodes.Uncategorized when request.Email != null:
                    EmailError = "Email này có thể đã được tài khoản khác sử dụng";
                    break;

                default:
                    MessageBox.Show(string.IsNullOrWhiteSpace(result.Message) ? "Vui lòng thử lại sau!" : result.Message,
                                    "Cập nhật không thành công", MessageBoxButton.OK, MessageBoxImage.Error);
                    break;
            }
        }

        private void ApplyFieldErrors(APIResponse<object> result)
        {
            EmailError = result.GetFieldError("email");
            PhoneNumberError = result.GetFieldError("phoneNumber");
            TargetScoreError = result.GetFieldError("targetScore");
            AvatarUrlError = result.GetFieldError("imageUrl");
            NewPasswordError = result.GetFieldError("password");
            CurrentPasswordError = result.GetFieldError("oldPassword");

            bool hasFieldError = EmailError != "" || PhoneNumberError != "" || TargetScoreError != ""
                              || AvatarUrlError != "" || NewPasswordError != "" || CurrentPasswordError != "";
            if (!hasFieldError)
                MessageBox.Show(result.Message, "Dữ liệu không hợp lệ", MessageBoxButton.OK, MessageBoxImage.Warning);
        }
        #endregion

        #region Đăng xuất
        private async void ExecuteLogout()
        {
            string question = HasChanges
                ? "Bạn có thay đổi chưa lưu. Vẫn đăng xuất?"
                : "Bạn có chắc muốn đăng xuất?";
            if (MessageBox.Show(question, "Đăng xuất", MessageBoxButton.YesNo, MessageBoxImage.Question)
                != MessageBoxResult.Yes)
                return;

            IsBusy = true;
            try
            {
                // Đánh dấu token hết hạn ở server; lỗi (mất mạng, token đã hết hạn) cũng vẫn đăng xuất
                await APIClient.PostAsync<object>(LogoutUrl, new { });
            }
            finally
            {
                IsBusy = false;
            }

            EndSession();
        }

        private void HandleSessionExpired()
        {
            MessageBox.Show("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.", "Hết phiên đăng nhập",
                            MessageBoxButton.OK, MessageBoxImage.Warning);
            EndSession();
        }

        private void EndSession()
        {
            Session.End();
            _original = null;
            LoggedOut?.Invoke();
        }
        #endregion

        #region Kiểm tra dữ liệu
        // Ô không đổi so với dữ liệu đã lưu thì luôn hợp lệ
        private static bool IsUnchanged(string value, string? original) => value.Trim() == (original ?? "");

        private bool ValidateEmail()
        {
            string value = Email.Trim();
            if (_original == null || IsUnchanged(value, _original.Email)) EmailError = "";
            else if (value == "") EmailError = "Email không được để trống";
            else if (!EmailPattern.IsMatch(value)) EmailError = ServerErrorMessages.Translate("EMAIL_INVALID_FORMAT");
            else EmailError = "";
            return EmailError == "";
        }

        private bool ValidatePhoneNumber()
        {
            string value = PhoneNumber.Trim();
            if (_original == null || IsUnchanged(value, _original.PhoneNumber)) PhoneNumberError = "";
            else if (value == "") PhoneNumberError = "Số điện thoại không được để trống";
            else if (value.Length < 10 || value.Length > 15) PhoneNumberError = ServerErrorMessages.Translate("PHONE_INVALID_SIZE");
            else if (!PhonePattern.IsMatch(value)) PhoneNumberError = ServerErrorMessages.Translate("PHONE_INVALID_PATTERN");
            else PhoneNumberError = "";
            return PhoneNumberError == "";
        }

        private bool ValidateTargetScore()
        {
            string value = TargetScoreText.Trim();
            if (!IsScoreChanged()) TargetScoreError = "";
            else if (value == "") TargetScoreError = "Không thể xóa điểm mục tiêu, hãy nhập một giá trị từ 0.0 đến 9.0";
            else if (!TryParseScore(value, out decimal score)) TargetScoreError = "Điểm phải là một số, ví dụ 8.5";
            else if (score < 0) TargetScoreError = ServerErrorMessages.Translate("TARGET_SCORE_MIN");
            else if (score > (decimal)MaxScore) TargetScoreError = ServerErrorMessages.Translate("TARGET_SCORE_MAX");
            else if (decimal.Round(score, 2) != score) TargetScoreError = "Điểm chỉ được có tối đa 2 chữ số thập phân";
            else TargetScoreError = "";
            return TargetScoreError == "";
        }

        private bool ValidateAvatarUrl()
        {
            string value = AvatarUrl.Trim();
            if (_original == null || IsUnchanged(value, _original.AvatarUrl)) AvatarUrlError = "";
            else if (value == "") AvatarUrlError = "Không thể xóa ảnh đại diện, hãy nhập một đường dẫn khác";
            else if (value.Length > 255) AvatarUrlError = "Đường dẫn ảnh tối đa 255 ký tự";
            else if (!Uri.TryCreate(value, UriKind.Absolute, out Uri? uri)
                     || (uri.Scheme != Uri.UriSchemeHttp && uri.Scheme != Uri.UriSchemeHttps))
                AvatarUrlError = "Đường dẫn ảnh phải bắt đầu bằng http:// hoặc https://";
            else AvatarUrlError = "";
            return AvatarUrlError == "";
        }

        // Chỉ bắt buộc khi có nhập mật khẩu mới
        private bool ValidateCurrentPassword()
        {
            if (NewPassword == "") CurrentPasswordError = "";
            else if (CurrentPassword == "") CurrentPasswordError = "Vui lòng nhập mật khẩu hiện tại để đổi mật khẩu";
            else CurrentPasswordError = "";
            return CurrentPasswordError == "";
        }

        // Để trống = không đổi mật khẩu
        private bool ValidateNewPassword()
        {
            if (NewPassword == "")
                NewPasswordError = CurrentPassword != "" ? "Vui lòng nhập mật khẩu mới" : "";
            else if (NewPassword.Length < 6 || NewPassword.Length > 50) NewPasswordError = ServerErrorMessages.Translate("PASSWORD_INVALID_SIZE");
            else if (!PasswordPattern.IsMatch(NewPassword)) NewPasswordError = ServerErrorMessages.Translate("PASSWORD_INVALID_PATTERN");
            else if (NewPassword == CurrentPassword) NewPasswordError = "Mật khẩu mới phải khác mật khẩu hiện tại";
            else NewPasswordError = "";
            return NewPasswordError == "";
        }

        private bool ValidateConfirmPassword()
        {
            if (NewPassword == "" && ConfirmPassword == "") ConfirmPasswordError = "";
            else if (ConfirmPassword == "") ConfirmPasswordError = "Vui lòng nhập lại mật khẩu mới";
            else if (ConfirmPassword != NewPassword) ConfirmPasswordError = "Mật khẩu xác nhận không khớp";
            else ConfirmPasswordError = "";
            return ConfirmPasswordError == "";
        }

        private bool IsScoreChanged()
        {
            string value = TargetScoreText.Trim();
            decimal? original = _original?.TargetScore;
            if (value == FormatScore(original)) return false;
            // "6.50" và "6.5" coi như không đổi
            return !(TryParseScore(value, out decimal score) && score == original);
        }

        // Chấp nhận cả "6.5" và "6,5"
        private static bool TryParseScore(string text, out decimal score)
            => decimal.TryParse(text.Trim().Replace(',', '.'), NumberStyles.AllowDecimalPoint | NumberStyles.AllowLeadingSign,
                                CultureInfo.InvariantCulture, out score);

        private static string FormatScore(decimal? score)
            => score?.ToString("0.0#", CultureInfo.InvariantCulture) ?? "";
        #endregion
    }
}
