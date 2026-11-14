namespace Frontend.Model
{
    class Deck
    {
        public int Id { get; set; }
        public string Name { get; set; } = "";
        public string Icon { get; set; } = "🗂";
        public DateTimeOffset CreatedAt { get; set; }
    }
}
