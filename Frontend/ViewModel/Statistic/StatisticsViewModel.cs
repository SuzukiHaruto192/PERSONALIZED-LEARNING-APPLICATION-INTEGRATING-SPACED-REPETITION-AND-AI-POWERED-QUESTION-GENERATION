using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Runtime.CompilerServices;
using System.Windows.Data;
using System.Windows.Input;
using Frontend.Model;
using Frontend.View;
using LiveChartsCore;
using LiveChartsCore.Defaults;
using LiveChartsCore.Kernel;
using LiveChartsCore.SkiaSharpView;
using LiveChartsCore.SkiaSharpView.Painting;
using SkiaSharp;

namespace Frontend.ViewModel
{
    enum ChartMetric { Score, Count, Duration }

    record SkillOption(TestType Type, string Display);
    record RangeOption(int Days, string Display);
    record MetricOption(ChartMetric Metric, string Display);
    record HistoryFilterOption(TestType? Type, string Display);

    class StatisticsViewModel : BaseViewModel
    {
        // Màu biểu đồ: một chuỗi dữ liệu nên chỉ cần một màu thương hiệu
        private static readonly SKColor Brand = SKColor.Parse("#0B3A82");
        private static readonly SKColor Muted = SKColor.Parse("#6B7A99");
        private static readonly SKColor GridColor = SKColor.Parse("#E3EAF6");

        private readonly ObservableCollection<TestResult> _allResults = new();

        #region Tab
        private bool _isHistoryTab;
        public bool IsHistoryTab
        {
            get => _isHistoryTab;
            private set
            {
                if (SetProperty(ref _isHistoryTab, value))
                    OnPropertyChanged(nameof(IsStatsTab));
            }
        }
        public bool IsStatsTab => !IsHistoryTab;

        public ICommand ShowStatsTabCommand { get; }
        public ICommand ShowHistoryTabCommand { get; }
        #endregion

        #region Bộ lọc thống kê
        public IReadOnlyList<SkillOption> Skills { get; } = new[]
        {
            new SkillOption(TestType.Listening, "🎧  Listening"),
            new SkillOption(TestType.Reading,   "📖  Reading"),
            new SkillOption(TestType.Grammar,   "✏  Grammar"),
        };

        public IReadOnlyList<RangeOption> Ranges { get; } = new[]
        {
            new RangeOption(7, "7 ngày"),
            new RangeOption(30, "30 ngày"),
            new RangeOption(90, "90 ngày"),
        };

        public IReadOnlyList<MetricOption> Metrics { get; } = new[]
        {
            new MetricOption(ChartMetric.Score, "Điểm"),
            new MetricOption(ChartMetric.Count, "Số đề"),
            new MetricOption(ChartMetric.Duration, "Thời gian"),
        };

        private SkillOption _selectedSkill;
        public SkillOption SelectedSkill
        {
            get => _selectedSkill;
            set { if (SetFilter(ref _selectedSkill, value)) RefreshStatistics(); }
        }

        private RangeOption _selectedRange;
        public RangeOption SelectedRange
        {
            get => _selectedRange;
            set { if (SetFilter(ref _selectedRange, value)) RefreshStatistics(); }
        }

        private MetricOption _selectedMetric;
        public MetricOption SelectedMetric
        {
            get => _selectedMetric;
            set { if (SetFilter(ref _selectedMetric, value)) RefreshStatistics(); }
        }
        #endregion

        #region Số liệu tóm tắt
        private int _testCount;
        public int TestCount 
        { 
            get => _testCount; 
            private set => SetProperty(ref _testCount, value); 
        }

        private string _totalDurationText = "";
        public string TotalDurationText 
        { 
            get => _totalDurationText; 
            private set => SetProperty(ref _totalDurationText, value); 
        }

        private string _averageScoreText = "";
        public string AverageScoreText 
        { 
            get => _averageScoreText; 
            private set => SetProperty(ref _averageScoreText, value); 
        }
        #endregion

        #region Biểu đồ
        private ISeries[] _series = Array.Empty<ISeries>();
        public ISeries[] Series { get => _series; private set => SetProperty(ref _series, value); }

        private Axis[] _xAxes = { new Axis() };
        public Axis[] XAxes { get => _xAxes; private set => SetProperty(ref _xAxes, value); }

        private Axis[] _yAxes = { new Axis() };
        public Axis[] YAxes { get => _yAxes; private set => SetProperty(ref _yAxes, value); }

        private string _chartTitle = "";
        public string ChartTitle { get => _chartTitle; private set => SetProperty(ref _chartTitle, value); }

        private string _chartSubtitle = "";
        public string ChartSubtitle { get => _chartSubtitle; private set => SetProperty(ref _chartSubtitle, value); }

        private bool _hasChartData;
        public bool HasChartData { get => _hasChartData; private set => SetProperty(ref _hasChartData, value); }
        #endregion

        #region Lịch sử
        public IReadOnlyList<HistoryFilterOption> HistoryFilters { get; } = new[]
        {
            new HistoryFilterOption(null, "Tất cả"),
            new HistoryFilterOption(TestType.Listening, "🎧  Listening"),
            new HistoryFilterOption(TestType.Reading,   "📖  Reading"),
            new HistoryFilterOption(TestType.Grammar,   "✏  Grammar"),
        };

        private HistoryFilterOption _selectedHistoryFilter;
        public HistoryFilterOption SelectedHistoryFilter
        {
            get => _selectedHistoryFilter;
            set
            {
                if (SetFilter(ref _selectedHistoryFilter, value))
                {
                    HistoryView.Refresh();
                    UpdateHistoryCount();
                }
            }
        }

        public ICollectionView HistoryView { get; }

        private int _historyCount;
        public int HistoryCount 
        { 
            get => _historyCount; 
            private set => SetProperty(ref _historyCount, value); 
        }
        #endregion

        public StatisticsViewModel()
        {
            // Thay dữ liệu mẫu bằng dữ liệu tải từ Backend
            foreach (var r in CreateSampleData().OrderByDescending(r => r.CompletedAt))
            {
                r.CompletedAt = r.CompletedAt.ToLocalTime();
                _allResults.Add(r);
            }
            _selectedSkill = Skills[0];
            _selectedRange = Ranges[1];          // mặc định 30 ngày
            _selectedMetric = Metrics[0];
            _selectedHistoryFilter = HistoryFilters[0];

            // View riêng cho Lịch sử: sắp xếp, nhóm theo tháng, lọc theo kỹ năng
            HistoryView = new ListCollectionView(_allResults);
            HistoryView.SortDescriptions.Add(
                new SortDescription(nameof(TestResult.CompletedAt), ListSortDirection.Descending));
            HistoryView.GroupDescriptions.Add(
                new PropertyGroupDescription(nameof(TestResult.CompletedAt), new MonthHeaderConverter()));
            HistoryView.Filter = o => o is TestResult r
                && (SelectedHistoryFilter.Type == null || r.Type == SelectedHistoryFilter.Type);

            ShowStatsTabCommand = new RelayCommand(_ => IsHistoryTab = false);
            ShowHistoryTabCommand = new RelayCommand(_ => IsHistoryTab = true);

            RefreshStatistics();
            UpdateHistoryCount();
        }

        // Không cho bộ lọc về trạng thái "không chọn gì" (Ctrl + click trên ListBox)
        private bool SetFilter<T>(ref T field, T value, [CallerMemberName] string? name = null) where T : class
        {
            if (value is null)
            {
                OnPropertyChanged(name);   // trả lại lựa chọn cũ cho giao diện
                return false;
            }
            return SetProperty(ref field, value, name);
        }

        private void RefreshStatistics()
        {
            int days = SelectedRange.Days;
            DateTime today = DateTime.Today;
            DateTime start = today.AddDays(-(days - 1));
            DateTime endExclusive = today.AddDays(1);

            // CompletedAt trong item ViewModel đã đổi sang giờ máy
            var items = _allResults
                .Where(r => r.Type == SelectedSkill.Type
                         && r.CompletedAt.Date >= start
                         && r.CompletedAt.Date < endExclusive)
                .ToList();

            TestCount = items.Count;
            TotalDurationText = FormatDuration(items.Sum(r => r.DurationSeconds));
            AverageScoreText = items.Count == 0 ? "–" : items.Average(r => r.Score).ToString("0.0");
            HasChartData = items.Count > 0;

            // 7 và 30 ngày gom theo ngày, 90 ngày gom theo tuần
            bool weekly = days > 30;
            int stepDays = weekly ? 7 : 1;
            var buckets = new List<(DateTime From, DateTime To, string Label)>();
            for (var d = start; d < endExclusive; d = d.AddDays(stepDays))
            {
                var to = d.AddDays(stepDays);
                if (to > endExclusive) to = endExclusive;
                buckets.Add((d, to, d.ToString("dd/MM")));
            }

            var points = new List<ObservablePoint>();
            for (int i = 0; i < buckets.Count; i++)
            {
                var inBucket = items
                    .Where(r => r.CompletedAt.Date >= buckets[i].From && r.CompletedAt.Date < buckets[i].To)
                    .ToList();

                switch (SelectedMetric.Metric)
                {
                    case ChartMetric.Score:
                        // Không làm bài thì không có điểm (không phải điểm 0): bỏ qua, đường nối thẳng qua
                        if (inBucket.Count > 0)
                            points.Add(new ObservablePoint(i, Math.Round(inBucket.Average(r => r.Score), 1)));
                        break;
                    case ChartMetric.Count:
                        points.Add(new ObservablePoint(i, inBucket.Count));
                        break;
                    case ChartMetric.Duration:
                        points.Add(new ObservablePoint(i, Math.Round(inBucket.Sum(r => r.DurationSeconds) / 60.0)));
                        break;
                }
            }

            BuildChart(points, buckets.Select(b => b.Label).ToArray(), days, weekly);
        }

        private void BuildChart(List<ObservablePoint> points, string[] labels, int days, bool weekly)
        {
            var metric = SelectedMetric.Metric;
            string unit = metric switch
            {
                ChartMetric.Count => " đề",
                ChartMetric.Duration => " phút",
                _ => ""
            };

            string Tip(ChartPoint p)
            {
                int i = (int)p.Coordinate.SecondaryValue;
                string when = weekly ? $"Tuần từ {labels[i]}" : labels[i];
                return $"{when}: {p.Coordinate.PrimaryValue:0.#}{unit}";
            }

            ISeries series = metric == ChartMetric.Score
                ? new LineSeries<ObservablePoint>
                {
                    Values = points,
                    Name = "Điểm trung bình",
                    Stroke = new SolidColorPaint(Brand) { StrokeThickness = 2 },
                    Fill = null,
                    LineSmoothness = 0,
                    GeometrySize = 9,
                    GeometryStroke = new SolidColorPaint(Brand) { StrokeThickness = 2 },
                    GeometryFill = new SolidColorPaint(SKColors.White),
                    YToolTipLabelFormatter = p => Tip(p)
                }
                : new ColumnSeries<ObservablePoint>
                {
                    Values = points,
                    Name = metric == ChartMetric.Count ? "Số đề" : "Thời gian",
                    Fill = new SolidColorPaint(Brand),
                    Rx = 4,
                    Ry = 4,
                    MaxBarWidth = 26,
                    Padding = 2,
                    YToolTipLabelFormatter = p => Tip(p)
                };

            Series = new[] { series };

            // 30 ngày: chỉ ghi nhãn mỗi 5 ngày cho khỏi chồng chữ
            int labelStep = days == 30 ? 5 : 1;
            XAxes = new[]
            {
                new Axis
                {
                    MinLimit = -0.5,
                    MaxLimit = labels.Length - 0.5,
                    MinStep = 1,
                    ForceStepToMin = true,
                    Labeler = v =>
                    {
                        int i = (int)Math.Round(v);
                        return i >= 0 && i < labels.Length && i % labelStep == 0 ? labels[i] : "";
                    },
                    LabelsPaint = new SolidColorPaint(Muted),
                    TextSize = 12,
                    SeparatorsPaint = null
                }
            };

            YAxes = new[]
            {
                new Axis
                {
                    MinLimit = 0,
                    MaxLimit = metric == ChartMetric.Score ? 10 : null,
                    MinStep = metric == ChartMetric.Score ? 2 : 1,
                    Labeler = v => metric == ChartMetric.Duration ? $"{v:0}p" : $"{v:0.#}",
                    LabelsPaint = new SolidColorPaint(Muted),
                    TextSize = 12,
                    SeparatorsPaint = new SolidColorPaint(GridColor) { StrokeThickness = 1 }
                }
            };

            string per = weekly ? "theo tuần" : "theo ngày";
            (ChartTitle, ChartSubtitle) = metric switch
            {
                ChartMetric.Score => ($"Điểm trung bình {per}", "Thang điểm 10 · những ngày không làm bài được bỏ qua"),
                ChartMetric.Count => ($"Số đề đã làm {per}", ""),
                _ => ($"Thời gian học {per}", "Đơn vị: phút")
            };
        }

        private void UpdateHistoryCount()
        {
            HistoryCount = _allResults.Count(r =>
                SelectedHistoryFilter.Type == null || r.Type == SelectedHistoryFilter.Type);
        }

        private static string FormatDuration(int totalSeconds)
        {
            var t = TimeSpan.FromSeconds(totalSeconds);
            if (t.TotalHours >= 1) return $"{(int)t.TotalHours} giờ {t.Minutes} phút";
            return $"{t.Minutes} phút";
        }

        // Dữ liệu mẫu khoảng 200 ngày, có ngày nghỉ, để thử cả biểu đồ lẫn lịch sử
        private static IEnumerable<TestResult> CreateSampleData()
        {
            var rnd = new Random(7);
            string[] situations = { "Nhà hàng", "Sân bay", "Mua sắm", "Hỏi đường", "Khám bệnh", "Làm quen" };
            string[] grammar = { "Câu bị động", "Câu điều kiện", "Hiện tại hoàn thành", "Mệnh đề quan hệ" };
            int id = 1;

            for (int day = 0; day < 200; day++)
            {
                if (rnd.NextDouble() < 0.35) continue;      // ngày không học
                int count = rnd.Next(1, 3);
                for (int k = 0; k < count; k++)
                {
                    var type = (TestType)rnd.Next(0, 3);
                    yield return new TestResult
                    {
                        Id = id++,
                        Type = type,
                        Topic = type == TestType.Grammar
                            ? grammar[rnd.Next(grammar.Length)]
                            : situations[rnd.Next(situations.Length)],
                        CompletedAt = DateTimeOffset.Now.AddDays(-day).AddMinutes(-rnd.Next(0, 600)),
                        DurationSeconds = rnd.Next(300, 1800),
                        Score = Math.Round(5 + rnd.NextDouble() * 4.5 + (200 - day) / 400.0, 1),
                    };
                }
            }
        }
    }
}