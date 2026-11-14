using System.Text;
using System.Text.Json;

namespace Frontend.DTO
{
    // Lưu thông tin phiên đăng nhập hiện tại (token + username đọc từ JWT)
    static class Session
    {
        public static string? Token { get; private set; }
        public static string Username { get; private set; } = "";

        public static void Start(string token)
        {
            Token = token;
            APIClient.SetToken(token);

            Username = "";

            try
            {
                using JsonDocument payload = ReadPayload(token);
                JsonElement root = payload.RootElement;

                if (root.TryGetProperty("sub", out var sub))
                    Username = sub.GetString() ?? "";
            }
            catch (Exception ex) when (ex is FormatException || ex is JsonException || ex is IndexOutOfRangeException)
            {
                System.Diagnostics.Debug.WriteLine($"[Session] Không đọc được token: {ex.Message}");
            }
        }

        public static void End()
        {
            Token = null;
            Username = "";
            APIClient.SetToken(null);
        }

        // JWT = header.payload.signature, payload là JSON mã hóa Base64Url
        private static JsonDocument ReadPayload(string token)
        {
            string part = token.Split('.')[1].Replace('-', '+').Replace('_', '/');
            part = part.PadRight(part.Length + (4 - part.Length % 4) % 4, '=');
            return JsonDocument.Parse(Encoding.UTF8.GetString(Convert.FromBase64String(part)));
        }
    }
}
