package ru.andrew.errorfreetext.speller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.andrew.errorfreetext.task.Language;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class SpellerClientTest {
    private HttpServer server;
    private SpellerClient client;
    private String response = "[[]]";
    private int responseStatus = 200;
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> method = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/check", exchange -> {
            requests.incrementAndGet();
            method.set(exchange.getRequestMethod());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseStatus, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        client = new SpellerClient(new ObjectMapper(),
                URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/check"), Duration.ofSeconds(2));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void sendsFormPostWithSelectedOptions() {
        assertThat(client.check("hello & world", Language.EN, 6)).isEmpty();
        assertThat(method.get()).isEqualTo("POST");
        assertThat(requestBody.get()).isEqualTo("text=hello+%26+world&lang=en&options=6&format=plain");
    }

    @Test
    void parsesCorrections() {
        response = "[[{\"pos\":0,\"len\":4,\"word\":\"Helo\",\"s\":[\"Hello\"]}]]";
        assertThat(client.check("Helo", Language.EN, 0)).singleElement()
                .satisfies(error -> assertThat(error.suggestions()).containsExactly("Hello"));
    }

    @Test
    void rejectsHttpErrorWithoutRetry() {
        responseStatus = 503;
        assertThatThrownBy(() -> client.check("Hello", Language.EN, 0))
                .isInstanceOfSatisfying(SpellerException.class, ex -> assertThat(ex.getCode()).isEqualTo(50201));
        assertThat(requests.get()).isEqualTo(1);
    }

    @Test
    void rejectsMalformedResponses() {
        for (String invalid : new String[]{"oops", "null", "{}", "[]", "[[],[]]",
                "[[{\"pos\":\"0\",\"len\":1,\"word\":\"a\",\"s\":[]}]]", "[[{\"pos\":0,\"len\":1,\"word\":\"a\",\"s\":[1]}]]"}) {
            response = invalid;
            assertThatThrownBy(() -> client.check("Hello", Language.EN, 0))
                    .isInstanceOfSatisfying(SpellerException.class, ex -> assertThat(ex.getCode()).isEqualTo(50202));
        }
    }
}
