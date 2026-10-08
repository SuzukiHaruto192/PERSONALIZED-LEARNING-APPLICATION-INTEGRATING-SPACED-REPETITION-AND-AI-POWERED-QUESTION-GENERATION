using Frontend.Model;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Frontend.ViewModel
{
    class MainScreen_TestResultViewModel : BaseViewModel
    {
        private readonly TestResult _model;
        
        public MainScreen_TestResultViewModel(TestResult model)
        {
            _model = model;
        }


        #region Properties
        public TestType Type => _model.Type;
        public string Name => _model.Name;
        public string Date => _model.Date;
        public string Time => TimeSpan.FromSeconds(_model.Time).ToString(@"hh\:mm\:ss");
        public float Score => _model.Score;

        #endregion
    }
}
