package com.fiap.WtcSync.presentation.controllers;

import com.fiap.WtcSync.application.services.TokenService;
import com.fiap.WtcSync.domain.entities.User;
import com.fiap.WtcSync.domain.interfaces.IUserRepository;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.data.mongodb.uri=mongodb://localhost:27017/wtcsync_queue_test",
    "spring.mongodb.uri=mongodb://localhost:27017/wtcsync_queue_test",
    "spring.mongodb.database=wtcsync_queue_test"
})
@EnabledIfEnvironmentVariable(named = "RUN_MONGO_INTEGRATION", matches = "true")
class SupportQueueIntegrationTest {
    @Value("${local.server.port}") int port;
    @Autowired IUserRepository users;
    @Autowired TokenService tokens;
    HttpClient http = HttpClient.newHttpClient();
    String account(String role) {
        String email = UUID.randomUUID() + "@example.invalid";
        users.save(new User("Queue test", email, "unused", role));
        return tokens.generateToken(email);
    }
    HttpResponse<String> request(String path, String body, String token) throws Exception {
        var b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).header("Content-Type", "application/json");
        if (token != null) b.header("Authorization", "Bearer " + token);
        if (body != null) b.POST(HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }
    String clientId(String token) throws Exception {
        var r = request("/api/clients/me", null, token);
        assertEquals(200, r.statusCode(), r.body());
        return JsonParser.parseString(r.body()).getAsJsonObject().get("id").getAsString();
    }
    @Test void queueHasExclusiveAtomicOwnerAndRealTimeNotifications() throws Exception {
        String client = account("CLIENT"), otherClient = account("CLIENT");
        String op1 = account("OPERATOR"), op2 = account("ATENDENTE");
        String id = clientId(client); clientId(otherClient);
        var operatorMessages = new LinkedBlockingQueue<String>();
        var clientMessages = new LinkedBlockingQueue<String>();
        var otherMessages = new LinkedBlockingQueue<String>();
        var operatorSocket = connect(op1, operatorMessages);
        var clientSocket = connect(client, clientMessages);
        var otherSocket = connect(otherClient, otherMessages);
        try {
            assertNotNull(operatorMessages.poll(5, TimeUnit.SECONDS));
            assertNotNull(clientMessages.poll(5, TimeUnit.SECONDS));
            assertNotNull(otherMessages.poll(5, TimeUnit.SECONDS));
            assertEquals(403, request("/api/support/queue", null, client).statusCode());
            assertEquals(403, request("/api/support/"+id+"/claim", "{}", client).statusCode());
            assertEquals(403, request("/api/support/"+id, null, otherClient).statusCode());
            String message = "{\"senderId\":\"spoof\",\"customerId\":\""+id+"\",\"text\":\"Hello\"}";
            assertEquals(409, request("/messages", message, op1).statusCode());
            assertEquals(201, request("/messages", message, client).statusCode());
            assertTrue(operatorMessages.poll(5, TimeUnit.SECONDS).contains("support_changed"));
            assertTrue(clientMessages.poll(5, TimeUnit.SECONDS).contains("support_changed"));
            assertNull(otherMessages.poll(300, TimeUnit.MILLISECONDS), "Unrelated client must not receive event");
            assertTrue(request("/api/support/queue", null, op1).body().contains(id));
            try (var executor = Executors.newFixedThreadPool(2)) {
                var start = new CountDownLatch(1);
                var a = executor.submit(() -> { start.await(); return request("/api/support/"+id+"/claim", "{}", op1).statusCode(); });
                var b = executor.submit(() -> { start.await(); return request("/api/support/"+id+"/claim", "{}", op2).statusCode(); });
                start.countDown();
                int first = a.get(10, TimeUnit.SECONDS), second = b.get(10, TimeUnit.SECONDS);
                assertEquals(Set.of(200,409), Set.of(first,second));
                String winner = first == 200 ? op1 : op2, loser = first == 200 ? op2 : op1;
                assertEquals(201, request("/messages", message, winner).statusCode());
                assertEquals(409, request("/messages", message, loser).statusCode());
                assertEquals(409, request("/api/support/"+id+"/release", "{}", loser).statusCode());
                assertEquals(200, request("/api/support/"+id+"/release", "{}", winner).statusCode());
                assertEquals(409, request("/messages", message, winner).statusCode());
                assertEquals(200, request("/api/support/"+id+"/claim", "{}", loser).statusCode());
                assertEquals(201, request("/messages", message, loser).statusCode());
            }
            assertEquals(200, request("/inbox/"+id, null, client).statusCode());
        } finally { operatorSocket.abort(); clientSocket.abort(); otherSocket.abort(); }
    }
    @Test void anonymousSocketIsRejected() {
        assertThrows(CompletionException.class, () -> http.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:"+port+"/ws/support"), new WebSocket.Listener() {}).join());
    }
    WebSocket connect(String token, BlockingQueue<String> messages) {
        return http.newWebSocketBuilder().header("Authorization", "Bearer " + token)
            .buildAsync(URI.create("ws://localhost:"+port+"/ws/support"), new WebSocket.Listener() {
                StringBuilder buffer = new StringBuilder();
                @Override public void onOpen(WebSocket ws) { ws.request(1); }
                @Override public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                    buffer.append(data);
                    if (last) { messages.add(buffer.toString()); buffer.setLength(0); }
                    ws.request(1); return CompletableFuture.completedFuture(null);
                }
            }).join();
    }
}
