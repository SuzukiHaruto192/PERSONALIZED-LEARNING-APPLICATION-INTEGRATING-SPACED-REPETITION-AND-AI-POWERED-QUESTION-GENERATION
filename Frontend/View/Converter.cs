using System.Globalization;
using System.Windows.Data;
using System.Windows.Media.Imaging;
using Frontend.Model;

namespace Frontend.View
{
    // 750 → "12 phút 30 giây"
    public class DurationToTextConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            if (value is not int seconds) return "";
            var t = TimeSpan.FromSeconds(seconds);
            if (t.TotalHours >= 1) return $"{(int)t.TotalHours} giờ {t.Minutes} phút";
            if (t.TotalMinutes >= 1) return $"{t.Minutes} phút {t.Seconds} giây";
            return $"{t.Seconds} giây";
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
            => throw new NotSupportedException();
    }

    // TestType.Listening → "🎧"
    public class TestTypeToIconConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
            => value switch
            {
                TestType.Listening => "🎧",
                TestType.Reading => "📖",
                TestType.Grammar => "✏",
                _ => ""
            };

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
            => throw new NotSupportedException();
    }

    // Ngày làm bài → "Tháng 10/2026" (dùng để nhóm Lịch sử)
    public class MonthHeaderConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
            => value is DateTimeOffset d ? $"Tháng {d:MM/yyyy}" : "";

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
            => throw new NotSupportedException();
    }

    // "https://.../avatar.png" → ảnh để hiển thị; đường dẫn sai thì trả null (giao diện hiện chữ cái đầu)
    public class UrlToImageConverter : IValueConverter
    {
        public object? Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            if (value is not string text
                || !Uri.TryCreate(text.Trim(), UriKind.Absolute, out Uri? uri)
                || (uri.Scheme != Uri.UriSchemeHttp && uri.Scheme != Uri.UriSchemeHttps))
                return null;

            try
            {
                var image = new BitmapImage();
                image.BeginInit();
                image.UriSource = uri;
                image.DecodePixelWidth = 240;
                image.EndInit();
                return image;
            }
            catch (Exception)
            {
                return null;
            }
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
            => throw new NotSupportedException();
    }
}