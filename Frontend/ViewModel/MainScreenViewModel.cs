using Frontend.Model;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Input;

namespace Frontend.ViewModel
{
    class MainScreenViewModel : BaseViewModel
    {
        public ICommand TaskCompleteCommand { get; }

        #region Properties
        public ObservableCollection<StudyScheduleViewModel> TodaySchedule {  get; } = new ObservableCollection<StudyScheduleViewModel>();
        public ObservableCollection<MainScreen_TestResultViewModel> NewestResults { get; } = new ObservableCollection<MainScreen_TestResultViewModel>();
        #endregion

        public MainScreenViewModel() 
        {
            TaskCompleteCommand = new RelayCommand(ExecuteTaskComplete);

            //Du lieu mau
            var studyScheduleData = new List<StudySchedule>()
            {
                new() { Title = "Từ vựng",   Duration = "20 phút", Frequency="Hằng ngày", IsDone = true },
                new() { Title = "Listening", Duration = "30 phút", Frequency ="Thứ 2, 3, 4" },
                new() { Title = "Writing",   Duration = "45 phút", Frequency = "Chủ nhật"},
                new() { Title = "Speaking với AI", Duration = "15 phút", Frequency="Thứ 6" }
            };

            var newestResultData = new List<TestResult>()
            {
                new() {Type = Model.TestType.Listening, Name = "Listening test 1", Date = "10/04/2026", Time = 15000, Score = 8},

            };
            //end du lieu mau

            foreach (var item in studyScheduleData)
            {
                TodaySchedule.Add(new StudyScheduleViewModel(item));
            }

            foreach (var item in newestResultData)
            {
                NewestResults.Add(new MainScreen_TestResultViewModel(item));
            }
        }

        private void ExecuteTaskComplete(object? parameter)
        {
            if (parameter is StudyScheduleViewModel item)
                item.IsDone = !item.IsDone;
        }
    }
}
