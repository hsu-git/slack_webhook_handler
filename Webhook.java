import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Webhook {
    public static void main(String[] args) {
        String prompt = System.getenv("LLM_PROMPT");
        String llmResult = useLLM(prompt);
        System.out.println("llmResult = " + llmResult);
        String template = System.getenv("LLM2_IMAGE_TEMPLATE");
        String imagePrompt = template.formatted(llmResult);
        System.out.println("imagePrompt = " + imagePrompt);
        String llmImageResult = useLLMForImage(imagePrompt);
        System.out.println("llmImageResult = " + llmImageResult);
        String title = System.getenv("SLACK_WEBHOOK_TITLE");
        sendSlackMessage(title, llmResult, llmImageResult);
    }

    public static String useLLMForImage(String prompt) {
        String apiUrl = System.getenv("LLM2_API_URL");
        String apiKey = System.getenv("LLM2_API_KEY");
        String model = System.getenv("LLM2_MODEL");
        String payload = """
                {
                  "prompt": "%s",
                  "model": "%s",
                  "width": 1440,
                  "height": 1440,
                  "steps": 4,
                  "n": 1
                }
                """.formatted(prompt, model);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        String result = null;
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("response.statusCode() = " + response.statusCode());
            System.out.println("response.body() = " + response.body());

            // JSON 응답이 예상된 형식인지 확인
            if (response.body().contains("url\": \"")) {
                result = response.body().split("url\": \"")[1].split("\",")[0];
            } else {
                System.err.println("Error: 'url' field not found in API response.");
                result = "No image URL returned.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch image from LLM API", e);
        }
        return result;
    }

    public static String useLLM(String prompt) {
        String apiUrl = System.getenv("LLM_API_URL");
        String apiKey = System.getenv("LLM_API_KEY");
        String model = System.getenv("LLM_MODEL");
        String payload = """
                {
                  "messages": [
                    {
                      "role": "user",
                      "content": "%s"
                    }
                  ],
                  "model": "%s"
                }
                """.formatted(prompt, model);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        String result = null;
        try {
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            System.out.println("response.statusCode() = " + response.statusCode());
            System.out.println("response.body() = " + response.body());
            result = response.body()
                    .split("\"content\":\"")[1]
                    .split("\"},\"logprobs\"")[0];
        } catch (Exception e) { 
            throw new RuntimeException(e);
        }
        return result;
    }

    public static void sendSlackMessage(String title, String text, String imageUrl) {
        String slackUrl1 = System.getenv("SLACK_WEBHOOK_URL1");
        String slackUrl2 = System.getenv("SLACK_WEBHOOK_URL2");
            String payload = """
                        {"attachments": [{
                            "title": "%s",
                            "text": "%s",
                            "image_url": "%s"
                        }]}
                    """.formatted(title, text, imageUrl);
            HttpClient client = HttpClient.newHttpClient();
            // HttpRequest request = HttpRequest.newBuilder()
            //         .uri(URI.create(slackUrl))
            //         .header("Content-Type", "application/json")
            //         .POST(HttpRequest.BodyPublishers.ofString(payload))
            //         .build();
            // Slack Webhook 1 전송
        if (slackUrl1 != null && !slackUrl1.isEmpty()) {
            try {
                HttpRequest request1 = HttpRequest.newBuilder()
                        .uri(URI.create(slackUrl1))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();
                HttpResponse<String> response1 = client.send(request1, HttpResponse.BodyHandlers.ofString());
                System.out.println("Sent to Slack 1: response.statusCode() = " + response1.statusCode());
                System.out.println("Response: " + response1.body());
            } catch (Exception e) {
                System.err.println("Failed to send message to Slack 1");
                e.printStackTrace();
            }
        }

        // Slack Webhook 2 전송
        if (slackUrl2 != null && !slackUrl2.isEmpty()) {
            try {
                HttpRequest request2 = HttpRequest.newBuilder()
                        .uri(URI.create(slackUrl2))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();
                HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());
                System.out.println("Sent to Slack 2: response.statusCode() = " + response2.statusCode());
                System.out.println("Response: " + response2.body());
            } catch (Exception e) {
                System.err.println("Failed to send message to Slack 2");
                e.printStackTrace();
            }
        }

        // try{    
        //     HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        //     System.out.println("response.statusCode() = " + response.statusCode());
        //     System.out.println("response.body() = " + response.body());
        // } catch (Exception e) {
        //     throw new RuntimeException(e);
        // }
    }
    // public static void sendSlackMessage(String title, String text, String imageUrl) {
    //     String slackUrl1 = System.getenv("SLACK_WEBHOOK_URL1");
    //     String slackUrl2 = System.getenv("SLACK_WEBHOOK_URL2");

    //     String payload = """
    //             {"attachments": [{
    //                 "title": "%s",
    //                 "text": "%s",
    //                 "image_url": "%s"
    //             }]}
    //             """.formatted(title, text, imageUrl);

    //     HttpClient client = HttpClient.newHttpClient();

    //     // 첫 번째 Slack Webhook으로 요청 보내기
    //     sendPostRequest(client, slackUrl1, payload);

    //     // 두 번째 Slack Webhook으로 요청 보내기
    //     sendPostRequest(client, slackUrl2, payload);
    // }

    // private static void sendPostRequest(HttpClient client, String url, String payload) {
    //     if (url == null || url.isEmpty()) {
    //         System.err.println("Slack Webhook URL is missing: " + url);
    //         return;
    //     }

    //     HttpRequest request = HttpRequest.newBuilder()
    //             .uri(URI.create(url))
    //             .header("Content-Type", "application/json")
    //             .POST(HttpRequest.BodyPublishers.ofString(payload))
    //             .build();

    //     try {
    //         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    //         System.out.println("Sent message to: " + url);
    //         System.out.println("response.statusCode() = " + response.statusCode());
    //         System.out.println("response.body() = " + response.body());
    //     } catch (Exception e) {
    //         System.err.println("Failed to send message to Slack: " + url);
    //         e.printStackTrace();
    //     }
    // }
}
