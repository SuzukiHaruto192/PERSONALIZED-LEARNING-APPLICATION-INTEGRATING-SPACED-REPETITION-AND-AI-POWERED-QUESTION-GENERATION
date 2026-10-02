using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Frontend
{
    public class StudySchedule
    {
        public string Title { get; set; } = "";
        public string Duration { get; set; } = "";
        public string Frequency { get; set; }
        public bool IsDone { get; set; }
    }
}
