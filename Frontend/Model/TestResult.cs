using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Frontend.Model
{
    enum TestType
    {
        Listening,
        Reading,
        Vocabulary
    }

    class TestResult
    {
        public TestType Type { get; set; }
        public string Name { get; set; }      //Ví dụ Listening test 1
        public string Date { get; set; }                // Ngay lam bai
        public int Time { get; set; }                   //Thoi gian lam bai trong bao lau (Tinh bang giay)
        public float Score { get; set; }                // Diem dat duoc

    }
}
