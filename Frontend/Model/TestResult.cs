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
        public string Name { get; set; }
        public string Date { get; set; }

        //Thoi gian lam bai (Tinh bang giay)
        public int Time { get; set; }
        public float Score { get; set; }

    }
}
