package com.fiap.WtcSync.presentation.controllers;

import com.fiap.WtcSync.application.services.*;
import com.fiap.WtcSync.domain.entities.User;
import com.fiap.WtcSync.domain.interfaces.IUserRepository;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.data.mongodb.uri=mongodb://localhost:27017/wtcsync_receipts_test",
    "spring.mongodb.uri=mongodb://localhost:27017/wtcsync_receipts_test",
    "spring.mongodb.database=wtcsync_receipts_test",
    "chat.push.enabled=false"
})
@EnabledIfEnvironmentVariable(named = "RUN_MONGO_INTEGRATION", matches = "true")
class ReadNotificationsIntegrationTest {
    @Value("${local.server.port}") int port;
    @Autowired IUserRepository users;
    @Autowired TokenService tokens;
    @Autowired PushDevices devices;
    @MockitoBean ChatPushGateway gateway;
    HttpClient http = HttpClient.newHttpClient();
    String account(String role) {
        String email = UUID.randomUUID()+"@example.invalid";
        users.save(new User("Receipt test", email, "unused", role));
        return tokens.generateToken(email);
    }
    String email(String token) { return tokens.getUsernameFromToken(token); }
    HttpResponse<String> call(String path, String token, String method, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer "+token);
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    JsonObject json(HttpResponse<String> response) { return JsonParser.parseString(response.body()).getAsJsonObject(); }
    String clientId(String token) throws Exception { return json(call("/api/clients/me", token, "GET", null)).get("id").getAsString(); }
    String send(String customer, String token) throws Exception {
        var response = call("/messages", token, "POST", "{\"senderId\":\"ignored\",\"customerId\":\""+customer+"\",\"text\":\"Confidencial\"}");
        assertEquals(201, response.statusCode(), response.body());
        assertFalse(json(response).has("push"), "Internal push queue must not appear in message responses");
        return "/messages/"+json(response).get("id").getAsString();
    }
    int status(String path, String token, String status) throws Exception {
        return call(path+"/status", token, "PATCH", "{\"status\":\""+status+"\"}").statusCode();
    }
    @Test void receiptsRequireTheRealRecipientAndNeverMoveBackwards() throws Exception {
        String client=account("CLIENT"), other=account("CLIENT"), owner=account("OPERATOR"), spectator=account("ATENDENTE");
        String customer=clientId(client), path=send(customer, client);
        assertEquals(403, status(path, client, "LIDO"));
        assertEquals(403, status(path, other, "LIDO"));
        assertEquals(403, status(path, owner, "ENTREGUE"));
        assertEquals(200, call("/api/support/"+customer+"/claim", owner, "POST", "{}").statusCode());
        assertEquals(403, status(path, spectator, "LIDO"));
        assertEquals(400, status(path, owner, "FALHA"));
        assertEquals(400, status(path, owner, "ENVIADO"));
        assertEquals(200, status(path, owner, "ENTREGUE"));
        assertEquals(200, status(path, owner, "LIDO"));
        assertEquals(200, status(path, owner, "ENTREGUE"));
        assertEquals("LIDO", json(call(path, client, "GET", null)).get("status").getAsString());
        String reply=send(customer, owner);
        assertEquals(403, status(reply, owner, "LIDO"));
        assertEquals(403, status(reply, spectator, "LIDO"));
        assertEquals(200, status(reply, client, "LIDO"));
    }
    @Test void concurrentDeliveryCannotOverwriteReadOrAnEdit() throws Exception {
        String client=account("CLIENT"), owner=account("OPERATOR"), customer=clientId(client);
        String path=send(customer, client);
        assertEquals(200, call("/api/support/"+customer+"/claim", owner, "POST", "{}").statusCode());
        try (var pool = Executors.newFixedThreadPool(3)) {
            var start=new CountDownLatch(1);
            var read=pool.submit(() -> {start.await(); return status(path, owner, "LIDO");});
            var delivered=pool.submit(() -> {start.await(); return status(path, owner, "ENTREGUE");});
            var edit=pool.submit(() -> {start.await(); return call(path, client, "PATCH", "{\"text\":\"Editada\"}").statusCode();});
            start.countDown();
            assertEquals(200, read.get()); assertEquals(200, delivered.get()); assertEquals(200, edit.get());
        }
        var result=json(call(path, client, "GET", null));
        assertEquals("LIDO", result.get("status").getAsString());
        assertEquals("Editada", result.get("text").getAsString());
    }
    @Test void tokenReassignmentAndLateLogoutCannotRemoveAnotherAccountsBinding() throws Exception {
        String first=account("CLIENT"), second=account("CLIENT"), token="fake-"+UUID.randomUUID();
        String body="{\"fcmToken\":\""+token+"\"}";
        assertEquals(403, call("/api/users/me/fcm-token", null, "PATCH", body).statusCode());
        assertEquals(400, call("/api/users/me/fcm-token", first, "PATCH", "{}").statusCode());
        assertEquals(200, call("/api/users/me/fcm-token", first, "PATCH", body).statusCode());
        assertEquals(200, call("/api/users/me/fcm-token", second, "PATCH", body).statusCode());
        assertTrue(devices.forEmail(email(first)).isEmpty());
        assertEquals(1, devices.forEmail(email(second)).size());
        assertEquals(204, call("/api/users/me/fcm-token", first, "DELETE", body).statusCode());
        assertEquals(1, devices.forEmail(email(second)).size());
        assertEquals(204, call("/api/users/me/fcm-token", second, "DELETE", body).statusCode());
        assertTrue(devices.forEmail(email(second)).isEmpty());
    }
}
