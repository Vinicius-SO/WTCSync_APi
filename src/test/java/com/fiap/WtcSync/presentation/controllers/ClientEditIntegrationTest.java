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
    "spring.data.mongodb.uri=mongodb://localhost:27017/wtcsync_client_edit_test",
    "spring.mongodb.uri=mongodb://localhost:27017/wtcsync_client_edit_test",
    "spring.mongodb.database=wtcsync_client_edit_test"
})
@EnabledIfEnvironmentVariable(named = "RUN_MONGO_INTEGRATION", matches = "true")
class ClientEditIntegrationTest {
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
    HttpResponse<String> edit(String id, String body, String token) throws Exception {
        var b = HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/clients/"+id))
            .header("Content-Type", "application/json").PUT(HttpRequest.BodyPublishers.ofString(body));
        if (token != null) b.header("Authorization", "Bearer "+token);
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test void staffCanEditCustomerWithoutLosingChatOwnershipOrAccountAccess() throws Exception {
        String client = account("CLIENT"), operator = account("OPERATOR"), attendant = account("ATENDENTE");
        String id = clientId(client);
        String email = tokens.getUsernameFromToken(client);
        String payload = "{\"name\":\"Updated name\",\"email\":\""+email+"\",\"phone\":\"11999999999\",\"status\":\"Active\",\"score\":82,\"tags\":[\"vip\"],\"notes\":\"Internal note\",\"segmentId\":\"original-segment\"}";
        var claim = request("/api/support/"+id+"/claim", "{}", operator);
        assertEquals(200, claim.statusCode());
        var result = edit(id, payload, operator);
        assertEquals(200, result.statusCode(), result.body());
        var updated = JsonParser.parseString(request("/api/clients/"+id, null, operator).body()).getAsJsonObject();
        assertEquals("Updated name", updated.get("name").getAsString());
        assertEquals("Internal note", updated.get("notes").getAsString());
        assertEquals("11999999999", updated.get("phone").getAsString());
        assertEquals("active", updated.get("status").getAsString());
        assertEquals(82, updated.get("score").getAsInt());
        assertEquals("vip", updated.getAsJsonArray("tags").get(0).getAsString());
        var ticket = JsonParser.parseString(request("/api/support/"+id, null, operator).body()).getAsJsonObject();
        assertEquals(tokens.getUsernameFromToken(operator), ticket.get("assignedTo").getAsString());
        assertEquals("Updated name", ticket.get("customerName").getAsString());
        assertEquals(id, clientId(client));
        var ownProfile = JsonParser.parseString(request("/api/clients/me", null, client).body()).getAsJsonObject();
        assertTrue(!ownProfile.has("notes") || ownProfile.get("notes").isJsonNull());
        assertEquals(200, request("/inbox/"+id, null, client).statusCode());
        // The Android form does not edit segment membership; omission must preserve it.
        String clearNotes = "{\"name\":\"New name\",\"email\":\""+email+"\",\"notes\":null}";
        assertEquals(200, edit(id, clearNotes, attendant).statusCode());
        var reloaded = JsonParser.parseString(request("/api/clients/"+id, null, operator).body()).getAsJsonObject();
        assertEquals("original-segment", reloaded.get("segmentId").getAsString());
        assertTrue(!reloaded.has("notes") || reloaded.get("notes").isJsonNull());
        assertEquals(403, edit(id, payload, client).statusCode());
        assertEquals(403, edit(id, payload, null).statusCode());
        assertEquals(404, edit("missing", payload, operator).statusCode());
        assertEquals(400, edit(id, "{\"name\":\"\",\"email\":\"invalid\"}", operator).statusCode());
        assertEquals(409, edit(id, payload.replace(email, "new-account@example.invalid"), operator).statusCode());
        assertEquals(id, clientId(client));
    }
}
