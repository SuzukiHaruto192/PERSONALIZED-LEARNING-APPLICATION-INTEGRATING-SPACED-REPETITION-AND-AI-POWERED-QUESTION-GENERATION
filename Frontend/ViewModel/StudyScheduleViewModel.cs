using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Frontend.ViewModel
{
    class StudyScheduleViewModel : BaseViewModel
    {
        private readonly StudySchedule _model;
        public StudyScheduleViewModel(StudySchedule model)
        {
            _model = model;
        }


        #region Properties
        public string Title => _model.Title;
        public string Duration => _model.Duration;
        public string Frequency => _model.Frequency;

        public bool IsDone
        {
            get => _model.IsDone;
            set
            {
                if (_model.IsDone == value) return;   
                _model.IsDone = value;                
                OnPropertyChanged();                  
            }
        }
        #endregion

    }
}
