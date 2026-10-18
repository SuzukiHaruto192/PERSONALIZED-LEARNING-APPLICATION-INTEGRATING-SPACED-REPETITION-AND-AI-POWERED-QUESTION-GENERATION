namespace Frontend.Model
{
    public class GrammarTopic
    {
        public int Id { get; set; }
        public string Name { get; set; } = "";
        public string EnglishName { get; set; } = "";
        public string Level { get; set; } = "";
        public int Mastery { get; set; }          // 0 - 100
    }

    public class SituationOption
    {
        public string Icon { get; init; } = "";
        public string Name { get; init; } = "";
    }
}