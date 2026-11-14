namespace Frontend.Model
{
    public enum CardState { New, Learning, Review, Relearning }

    public enum ReviewRating { Again = 1, Hard = 2, Good = 3, Easy = 4 }

    class Flashcard
    {
        public int Id { get; set; }
        public int DeckId { get; set; }
        public string Term { get; set; } = "";
        public string Phonetic { get; set; } = "";
        public string Meaning { get; set; } = "";
        public string Example { get; set; } = "";
        public string ExampleMeaning { get; set; } = "";
        public DateTimeOffset CreatedAt { get; set; }


        //Xử lý thuật toán FSRS
        // Do Backend tính, giao diện chỉ đọc để hiện trạng thái và đếm thẻ cần ôn
        public CardState State { get; set; } = CardState.New;
        public DateTimeOffset Due { get; set; }
    }
}
