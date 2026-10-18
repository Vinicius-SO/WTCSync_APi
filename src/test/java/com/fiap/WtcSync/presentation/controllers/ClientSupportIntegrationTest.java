package com.fiap.WtcSync.presentation.controllers;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import java.net.URI;
import java.net.http.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.data.mongodb.uri=mongodb://localhost:27017/wtcsync_support_test",
    "spring.mongodb.uri=mongodb://localhost:27017/wtcsync_support_test",
    "spring.mongodb.database=wtcsync_support_test"
})
@EnabledIfEnvironmentVariable(named = "RUN_MONGO_INTEGRATION", matches = "true")
class ClientSupportIntegrationTest {
    @Value("${local.server.port}") int port;
    HttpClient http = HttpClient.newHttpClient();
    HttpResponse<String> request(String path, String body, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Content-Type", "application/json");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (body != null) builder.POST(HttpRequest.BodyPublishers.ofString(body));
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test void registeredClientCanOpenAndReuseSupportConversation() throws Exception {
        String email = "support-test-" + UUID.randomUUID() + "@example.invalid";
        String credentials = "{\"email\":\"" + email + "\",\"password\":\"test-password-123\",\"name\":\"Support Test\"}";
        assertEquals(200, request("/api/auth/register", credentials, null).statusCode());
        var login = request("/api/auth/login", credentials, null);
        assertEquals(200, login.statusCode());
        String token = JsonParser.parseString(login.body()).getAsJsonObject().get("token").getAsString();
        var profile = request("/api/auth/me", null, token);
        assertEquals(200, profile.statusCode());
        var support = request("/api/clients/me", null, token);
        assertEquals(200, support.statusCode(), "Support endpoint: " + support.body());
        String id = JsonParser.parseString(support.body()).getAsJsonObject().get("id").getAsString();
        assertFalse(id.isBlank());
        var again = request("/api/clients/me", null, token);
        assertEquals(200, again.statusCode());
        assertEquals(id, JsonParser.parseString(again.body()).getAsJsonObject().get("id").getAsString());
        assertEquals(200, request("/inbox/" + id, null, token).statusCode());
        assertEquals(403, request("/api/clients", null, token).statusCode());
    }
}
