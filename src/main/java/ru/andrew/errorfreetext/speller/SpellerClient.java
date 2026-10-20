package ru.andrew.errorfreetext.speller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.andrew.errorfreetext.task.Language;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class SpellerClient {
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final URI endpoint;
    private final Duration timeout;

    public SpellerClient(ObjectMapper mapper, @Value("${app.speller-url}") URI endpoint,
                         @Value("${app.speller-timeout}") Duration timeout) {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        this.mapper = mapper;
        this.endpoint = endpoint;
        this.timeout = timeout;
    }

    public List<SpellingError> check(String text, Language language, int options) {
        String body = "text=" + URLEncoder.encode(text, StandardCharsets.UTF_8)
                + "&lang=" + language.name().toLowerCase(Locale.ROOT)
                + "&options=" + options + "&format=plain";
        HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(timeout)
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        try {
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new SpellerException(50201, "Сервис проверки недоступен: HTTP " + response.statusCode());
            }
            return parse(response.body());
        } catch (HttpTimeoutException ex) {
            throw new SpellerException(50401, "Сервис проверки не ответил вовремя");
        } catch (IOException ex) {
            throw new SpellerException(50201, "Не удалось связаться с сервисом проверки");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SpellerException(50002, "Обработка прервана");
        }
    }

    private List<SpellingError> parse(String body) {
        try {
            JsonNode root = mapper.readTree(body);
            if (root == null || !root.isArray() || root.size() != 1 || !root.get(0).isArray()) {
                throw invalidResponse();
            }
            List<SpellingError> errors = new ArrayList<>();
            for (JsonNode node : root.get(0)) {
                if (!node.path("pos").isIntegralNumber() || !node.path("pos").canConvertToInt()
                        || !node.path("len").isIntegralNumber() || !node.path("len").canConvertToInt()
                        || !node.path("word").isTextual() || !node.path("s").isArray()) {
                    throw invalidResponse();
                }
                List<String> suggestions = new ArrayList<>();
                for (JsonNode suggestion : node.path("s")) {
                    if (!suggestion.isTextual()) {
                        throw invalidResponse();
                    }
                    suggestions.add(suggestion.textValue());
                }
                errors.add(new SpellingError(node.path("pos").intValue(), node.path("len").intValue(),
                        node.path("word").textValue(), List.copyOf(suggestions)));
            }
            return List.copyOf(errors);
        } catch (IOException ex) {
            throw invalidResponse();
        }
    }

    private SpellerException invalidResponse() {
        return new SpellerException(50202, "Сервис проверки вернул некорректный ответ");
    }
}
