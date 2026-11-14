using System.Net.Http;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json;

namespace Frontend.DTO
{
    static class APIClient
    {
        private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);

        private static readonly HttpClient Http = new()
        {
            BaseAddress = new Uri("http://localhost:8080"),
            Timeout = TimeSpan.FromSeconds(30)
        };

        public static void SetToken(string? token)
        {
            Http.DefaultRequestHeaders.Authorization =
                token == null ? null : new AuthenticationHeaderValue("Bearer", token);
        }

        public static Task<APIResponse<TResponse>> PostAsync<TResponse>(string url, object body)
            => SendAsync<TResponse>(() => Http.PostAsJsonAsync(url, body, JsonOptions));

        public static Task<APIResponse<TResponse>> PatchAsync<TResponse>(string url, object body)
            => SendAsync<TResponse>(() => Http.PatchAsJsonAsync(url, body, JsonOptions));

        public static Task<APIResponse<TResponse>> GetAsync<TResponse>(string url)
            => SendAsync<TResponse>(() => Http.GetAsync(url));

        private static async Task<APIResponse<TResponse>> SendAsync<TResponse>(Func<Task<HttpResponseMessage>> send)
        {
            try
            {
                var response = await send();

                APIResponse<TResponse>? result = null;
                try
                {
                    // Backend trả khung ApiResponse cả khi lỗi, nên luôn đọc body
                    result = await response.Content.ReadFromJsonAsync<APIResponse<TResponse>>(JsonOptions);
                }
                catch (JsonException ex)
                {
                    System.Diagnostics.Debug.WriteLine($"[API] Không đọc được JSON từ {response.RequestMessage?.RequestUri}: {ex.Message}");
                }

                result ??= new APIResponse<TResponse>();

                if (result.Code == 0 && string.IsNullOrWhiteSpace(result.Message))
                    result.Message = "Không đọc được phản hồi từ máy chủ.";

                if (!response.IsSuccessStatusCode && string.IsNullOrWhiteSpace(result.Message))
                    result.Message = $"Lỗi {(int)response.StatusCode} ({response.ReasonPhrase}) khi gọi {response.RequestMessage?.RequestUri}";

                return result;
            }
            catch (HttpRequestException)
            {
                return new APIResponse<TResponse> { Code = -1, Message = "Không kết nối được tới máy chủ. Backend đã chạy chưa?" };
            }
            catch (TaskCanceledException)
            {
                return new APIResponse<TResponse> { Code = -1, Message = "Máy chủ phản hồi quá lâu." };
            }

        }
    }
}
