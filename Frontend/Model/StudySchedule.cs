using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Frontend
{
    public class StudySchedule
    {
        public string Title { get; set; } = "";             // Ten nhiem vu
        public string Duration { get; set; } = "";          // Nhiem vu nay lam trong bao lau
        public string Frequency { get; set; }               // Tan suat (Thu 2, thu 3, thu 4, ..., hang ngay)
        public bool IsDone { get; set; }                    // Nhiem vu da xong chua
    }
}
