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
    @Autowired com.fiap.WtcSync.application.services.SupportQueueService queueService;
    @Autowired org.springframework.data.mongodb.core.MongoTemplate mongo;

    @Test void concludedConversationsKeepMessagesAndHistoryAndOnlyNewClientMessagesReopenThem() throws Exception {
        String client=account("CLIENT"), other=account("CLIENT"), operator=account("OPERATOR"), attendant=account("ATENDENTE");
        String id=clientId(client); clientId(other);
        String sendId=UUID.randomUUID().toString();
        String body="{\"senderId\":\""+tokens.getUsernameFromToken(client)+"\",\"customerId\":\""+id+"\",\"text\":\"Primeiro pedido\",\"clientMessageId\":\""+sendId+"\"}";
        assertEquals(201, request("/messages", body, client).statusCode());
        assertEquals(200, request("/api/support/"+id+"/claim", "{}", operator).statusCode());
        assertEquals(200, request("/api/support/"+id+"/claim", "{}", operator).statusCode());
        assertEquals(403, request("/api/support/"+id+"/close", "{}", client).statusCode());
        assertEquals(409, request("/api/support/"+id+"/close", "{}", attendant).statusCode());
        var clientEvents=new LinkedBlockingQueue<String>(); var staffEvents=new LinkedBlockingQueue<String>(); var otherEvents=new LinkedBlockingQueue<String>();
        var clientSocket=connect(client, clientEvents); var staffSocket=connect(operator, staffEvents); var otherSocket=connect(other, otherEvents);
        try {
            assertNotNull(clientEvents.poll(5, TimeUnit.SECONDS)); assertNotNull(staffEvents.poll(5, TimeUnit.SECONDS)); assertNotNull(otherEvents.poll(5, TimeUnit.SECONDS));
            var result=request("/api/support/"+id+"/close", "{}", operator);
            assertEquals(200, result.statusCode(), result.body());
            var closed=JsonParser.parseString(result.body()).getAsJsonObject();
            assertEquals("CLOSED", closed.get("status").getAsString()); assertFalse(closed.get("closedAt").isJsonNull());
            assertTrue(!closed.has("assignedTo") || closed.get("assignedTo").isJsonNull()); assertFalse(closed.has("history"));
            assertTrue(clientEvents.poll(5, TimeUnit.SECONDS).contains("support_changed"));
            assertTrue(staffEvents.poll(5, TimeUnit.SECONDS).contains("support_changed"));
            assertNull(otherEvents.poll(300, TimeUnit.MILLISECONDS));
            assertEquals(200, request("/api/support/"+id+"/close", "{}", operator).statusCode());
            assertEquals(409, request("/api/support/"+id+"/claim", "{}", operator).statusCode());
            assertEquals(409, request("/messages", body.replace("Primeiro pedido", "Resposta de operador").replace(tokens.getUsernameFromToken(client), tokens.getUsernameFromToken(operator)).replace(sendId, UUID.randomUUID().toString()), operator).statusCode());
            assertEquals(403, request("/api/support/"+id+"/history", null, client).statusCode());
            assertEquals(403, request("/api/support/"+id+"/history", null, other).statusCode());
            assertEquals(403, request("/api/support/"+id+"/history", null, null).statusCode());
            var history=JsonParser.parseString(request("/api/support/"+id+"/history", null, attendant).body()).getAsJsonArray();
            assertEquals(3, history.size());
            assertEquals("CLAIMED", history.get(1).getAsJsonObject().get("action").getAsString());
            assertEquals(tokens.getUsernameFromToken(operator), history.get(2).getAsJsonObject().get("actor").getAsString());
            assertEquals("CLOSED", history.get(2).getAsJsonObject().get("action").getAsString());
            queueService.run(null); // Startup must not undo a conclusion.
            assertEquals("CLOSED", JsonParser.parseString(request("/api/support/"+id, null, client).body()).getAsJsonObject().get("status").getAsString());
            assertEquals(201, request("/messages", body, client).statusCode()); // Recover old acknowledgement only.
            assertEquals("CLOSED", JsonParser.parseString(request("/api/support/"+id, null, client).body()).getAsJsonObject().get("status").getAsString());
            assertTrue(request("/inbox/"+id, null, client).body().contains("Primeiro pedido"));
            assertEquals(201, request("/messages", body.replace(sendId, UUID.randomUUID().toString()).replace("Primeiro pedido", "Preciso de mais ajuda"), client).statusCode());
            var reopened=JsonParser.parseString(request("/api/support/"+id, null, client).body()).getAsJsonObject();
            assertEquals("WAITING", reopened.get("status").getAsString()); assertFalse(reopened.has("history"));
            assertEquals(200, request("/api/support/"+id+"/claim", "{}", attendant).statusCode());
            assertEquals(200, request("/api/support/"+id+"/close", "{}", attendant).statusCode());
            assertEquals(403, request("/api/support/"+id+"/reopen", "{}", client).statusCode());
            assertEquals(200, request("/api/support/"+id+"/reopen", "{}", operator).statusCode());
            assertEquals(409, request("/api/support/"+id+"/reopen", "{}", attendant).statusCode());
            assertEquals(2, JsonParser.parseString(request("/inbox/"+id, null, client).body()).getAsJsonArray().size());
        } finally { clientSocket.abort(); staffSocket.abort(); otherSocket.abort(); }
    }
    @Test void closeAndReleaseAreExclusiveAndMigrationPreservesLegacyOwnership() throws Exception {
        String client=account("CLIENT"), operator=account("OPERATOR"); String id=clientId(client);
        assertEquals(200, request("/api/support/"+id+"/claim", "{}", operator).statusCode());
        mongo.updateFirst(org.springframework.data.mongodb.core.query.Query.query(org.springframework.data.mongodb.core.query.Criteria.where("_id").is(id)),
            new org.springframework.data.mongodb.core.query.Update().unset("status").unset("history"),
            com.fiap.WtcSync.application.services.SupportQueueService.Ticket.class);
        queueService.run(null);
        var migrated=queueService.get(id);
        assertEquals("IN_PROGRESS", migrated.status); assertEquals(tokens.getUsernameFromToken(operator), migrated.assignedTo);
        try (var executor=Executors.newFixedThreadPool(2)) {
            var start=new CountDownLatch(1);
            var a=executor.submit(() -> { start.await(); return request("/api/support/"+id+"/close", "{}", operator).statusCode(); });
            var b=executor.submit(() -> { start.await(); return request("/api/support/"+id+"/release", "{}", operator).statusCode(); });
            start.countDown(); assertEquals(Set.of(200,409), Set.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS)));
        }
        assertEquals(1, queueService.history(id).size());
        var finalTicket=queueService.get(id);
        assertTrue(Set.of("CLOSED", "WAITING").contains(finalTicket.status));
        assertNull(finalTicket.assignedTo);
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
