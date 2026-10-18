using LiveChartsCore;
using LiveChartsCore.SkiaSharpView;
using SkiaSharp;
using System.Windows;

namespace Frontend
{
    public partial class App : Application
    {
        protected override void OnStartup(StartupEventArgs e)
        {
            base.OnStartup(e);
            // Chọn font có đủ dấu tiếng Việt cho biểu đồ
            LiveCharts.Configure(config =>
                config.HasGlobalSKTypeface(SKFontManager.Default.MatchCharacter('ệ')));
        }
    }

}
