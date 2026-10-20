package com.fiap.WtcSync.presentation.controllers;

import com.fiap.WtcSync.application.services.TokenService;
import com.fiap.WtcSync.domain.entities.User;
import com.fiap.WtcSync.domain.interfaces.IUserRepository;
import com.google.gson.*;
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
    "spring.data.mongodb.uri=mongodb://localhost:27017/wtcsync_message_edit_test",
    "spring.mongodb.uri=mongodb://localhost:27017/wtcsync_message_edit_test",
    "spring.mongodb.database=wtcsync_message_edit_test"
})
@EnabledIfEnvironmentVariable(named = "RUN_MONGO_INTEGRATION", matches = "true")
class MessageEditIntegrationTest {
    @Value("${local.server.port}") int port;
    @Autowired IUserRepository users;
    @Autowired TokenService tokens;
    HttpClient http = HttpClient.newHttpClient();
    String account(String role) {
        String email = UUID.randomUUID()+"@example.invalid";
        users.save(new User("Edit test", email, "unused", role));
        return tokens.generateToken(email);
    }
    HttpResponse<String> call(String path, String token, String method, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer "+token);
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    JsonObject json(HttpResponse<String> response) { return JsonParser.parseString(response.body()).getAsJsonObject(); }
    String clientId(String token) throws Exception { return json(call("/api/clients/me", token, "GET", null)).get("id").getAsString(); }
    JsonObject send(String customer, String token) throws Exception {
        String body = "{\"senderId\":\"spoof\",\"customerId\":\""+customer+"\",\"text\":\"Texto original\",\"actionUrls\":{\"btn1\":\"https://wtc.com/event\"}}";
        var response = call("/messages", token, "POST", body);
        assertEquals(201, response.statusCode(), response.body());
        return json(response);
    }
    @Test void authorEditsPersistAndNotifyBothSidesWithoutLeakingToOtherClients() throws Exception {
        String client=account("CLIENT"), other=account("CLIENT"), operator=account("OPERATOR");
        String customer=clientId(client); clientId(other);
        JsonObject original=send(customer, client);
        String id=original.get("id").getAsString(), path="/messages/"+id;
        // Mongo stores timestamps with millisecond precision; compare persisted values.
        original=json(call(path, client, "GET", null));
        assertEquals(200, call(path, client, "PATCH", "{\"text\":\"Texto original\"}").statusCode());
        var unchanged=json(call(path, client, "GET", null));
        assertTrue(!unchanged.has("editedAt") || unchanged.get("editedAt").isJsonNull());
        for (String invalid : new String[]{"{}", "{\"text\":null}", "{\"text\":\"   \"}"})
            assertEquals(400, call(path, client, "PATCH", invalid).statusCode());
        assertEquals(403, call(path, null, "PATCH", "{\"text\":\"Wrong\"}").statusCode());
        assertEquals(403, call(path, other, "PATCH", "{\"text\":\"Wrong\"}").statusCode());
        assertEquals(403, call(path, operator, "PATCH", "{\"text\":\"Wrong\"}").statusCode());
        assertEquals(404, call("/messages/missing", client, "PATCH", "{\"text\":\"Wrong\"}").statusCode());
        var clientEvents=new LinkedBlockingQueue<String>();
        var operatorEvents=new LinkedBlockingQueue<String>();
        var otherEvents=new LinkedBlockingQueue<String>();
        var clientSocket=connect(client, clientEvents);
        var operatorSocket=connect(operator, operatorEvents);
        var otherSocket=connect(other, otherEvents);
        try {
            assertNotNull(clientEvents.poll(5, TimeUnit.SECONDS));
            assertNotNull(operatorEvents.poll(5, TimeUnit.SECONDS));
            assertNotNull(otherEvents.poll(5, TimeUnit.SECONDS));
            var edited=call(path, client, "PATCH", "{\"text\":\"Texto corrigido\",\"senderId\":\"spoof\",\"customerId\":\"other\"}");
            assertEquals(200, edited.statusCode(), edited.body());
            var data=json(edited);
            assertEquals("Texto corrigido", data.get("text").getAsString());
            assertFalse(data.get("editedAt").isJsonNull());
            for (String field : new String[]{"id", "senderId", "customerId", "status", "actionUrls", "createdAt"})
                assertEquals(original.get(field), data.get(field), field+" must remain unchanged");
            assertTrue(clientEvents.poll(5, TimeUnit.SECONDS).contains("support_changed"));
            assertTrue(operatorEvents.poll(5, TimeUnit.SECONDS).contains("support_changed"));
            assertNull(otherEvents.poll(300, TimeUnit.MILLISECONDS));
            var persisted=json(call(path, operator, "GET", null));
            assertEquals(data.get("text"), persisted.get("text"));
            assertEquals(data.get("editedAt"), persisted.get("editedAt"));
            assertTrue(call("/inbox/"+customer, client, "GET", null).body().contains("Texto corrigido"));
            // Delivery acknowledgements and edits must not overwrite each other.
            try (var executor=Executors.newFixedThreadPool(2)) {
                var start=new CountDownLatch(1);
                var a=executor.submit(() -> { start.await(); return call(path, client, "PATCH", "{\"text\":\"Correção final\"}").statusCode(); });
                var b=executor.submit(() -> { start.await(); return call(path+"/status", operator, "PATCH", "{\"status\":\"LIDO\"}").statusCode(); });
                start.countDown();
                assertEquals(200, a.get(10, TimeUnit.SECONDS)); assertEquals(200, b.get(10, TimeUnit.SECONDS));
            }
            persisted=json(call(path, client, "GET", null));
            assertEquals("Correção final", persisted.get("text").getAsString());
            assertEquals("LIDO", persisted.get("status").getAsString());
            assertEquals(original.get("createdAt"), persisted.get("createdAt"));
        } finally { clientSocket.abort(); operatorSocket.abort(); otherSocket.abort(); }
    }
    @Test void staffCanOnlyEditTheirOwnMessagesEvenAfterReturningTheConversationToQueue() throws Exception {
        String client=account("CLIENT"), operator=account("OPERATOR"), attendant=account("ATENDENTE");
        String customer=clientId(client);
        assertEquals(200, call("/api/support/"+customer+"/claim", operator, "POST", "{}").statusCode());
        String path="/messages/"+send(customer, operator).get("id").getAsString();
        assertEquals(200, call("/api/support/"+customer+"/release", operator, "POST", "{}").statusCode());
        assertEquals(200, call(path, operator, "PATCH", "{\"text\":\"Resposta corrigida\"}").statusCode());
        assertEquals(403, call(path, client, "PATCH", "{\"text\":\"Wrong\"}").statusCode());
        assertEquals(403, call(path, attendant, "PATCH", "{\"text\":\"Wrong\"}").statusCode());
    }
    WebSocket connect(String token, BlockingQueue<String> messages) {
        return http.newWebSocketBuilder().header("Authorization", "Bearer "+token)
            .buildAsync(URI.create("ws://localhost:"+port+"/ws/support"), new WebSocket.Listener() {
                StringBuilder buffer=new StringBuilder();
                @Override public void onOpen(WebSocket ws) { ws.request(1); }
                @Override public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                    buffer.append(data);
                    if (last) { messages.add(buffer.toString()); buffer.setLength(0); }
                    ws.request(1); return CompletableFuture.completedFuture(null);
                }
            }).join();
    }
}
