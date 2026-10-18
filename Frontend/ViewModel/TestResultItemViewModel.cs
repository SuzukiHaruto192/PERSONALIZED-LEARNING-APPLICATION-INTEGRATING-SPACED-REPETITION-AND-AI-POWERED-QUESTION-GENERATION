using Frontend.Model;


namespace Frontend.ViewModel
{
    class TestResultItemViewModel : BaseViewModel
    {
        private readonly TestResult _model;
        
        public TestResultItemViewModel(TestResult model)
        {
            _model = model;
        }

        #region Properties
        public TestType Type => _model.Type;
        public string Topic => _model.Topic;
        public string CompletedAt => _model.CompletedAt.ToLocalTime().ToString("dd/MM/yyyy");
        public string DurationSeconds => TimeSpan.FromSeconds(_model.DurationSeconds).ToString(@"hh\:mm\:ss");
        public double Score => _model.Score;

        #endregion
    }
}
