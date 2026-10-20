package com.fiap.WtcSync.presentation.controllers;

import com.fiap.WtcSync.application.services.TokenService;
import com.fiap.WtcSync.domain.entities.*;
import com.fiap.WtcSync.domain.enums.CampaignStatus;
import com.fiap.WtcSync.domain.interfaces.*;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.data.mongodb.uri=mongodb://localhost:27017/wtcsync_campaign_test",
    "spring.mongodb.uri=mongodb://localhost:27017/wtcsync_campaign_test",
    "spring.mongodb.database=wtcsync_campaign_test"
})
@EnabledIfEnvironmentVariable(named = "RUN_MONGO_INTEGRATION", matches = "true")
class ClientCampaignIntegrationTest {
    @Value("${local.server.port}") int port;
    @Autowired IUserRepository users;
    @Autowired IClientRepository clients;
    @Autowired ICampaignRepository campaigns;
    @Autowired ISegmentRepository segments;
    @Autowired TokenService tokens;
    HttpClient http = HttpClient.newHttpClient();

    String account(String role) {
        String email = UUID.randomUUID() + "@example.invalid";
        users.save(new User("Cliente", email, "unused", role));
        return tokens.generateToken(email);
    }
    HttpResponse<String> call(String path, String token, String method, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    String segment() {
        Segment segment = new Segment();
        segment.setName("Audience " + UUID.randomUUID());
        return segments.save(segment).getId();
    }
    Client customer(String token, String segment) {
        Client client = clients.findOrCreateForAccount(tokens.getUsernameFromToken(token), "Cliente");
        client.setSegmentId(segment);
        return clients.save(client);
    }
    Campaign campaign(String segment, String title, LocalDateTime created) {
        Campaign c = new Campaign();
        c.setTitle(title); c.setBody("Conteúdo exclusivo"); c.setSegmentId(segment);
        c.setStatus(CampaignStatus.DRAFT); c.setCreatedAt(created); c.setCreatedBy("private-operator@example.invalid");
        return campaigns.save(c);
    }
    @Test void dispatchRecordsOwnAudienceAndLatestSendWithoutExposingOperatorData() throws Exception {
        String client = account("CLIENT"), other = account("CLIENT"), operator = account("OPERATOR");
        String segment = segment(), otherSegment = segment();
        Client own = customer(client, segment);
        customer(other, otherSegment);
        String path = "/api/campaigns/me/latest";
        assertEquals(204, call(path, client, "GET", null).statusCode());
        assertEquals(403, call(path, null, "GET", null).statusCode());
        assertEquals(403, call(path, operator, "GET", null).statusCode());
        Campaign first = campaign(segment, "First", LocalDateTime.now());
        Campaign last = campaign(segment, "Last sent, older draft", LocalDateTime.now().minusDays(3));
        // No FCM token: content still belongs in the client's in-app inbox.
        assertEquals(200, call("/api/campaigns/"+first.getId()+"/send", operator, "POST", "{}").statusCode());
        assertEquals(200, call("/api/campaigns/"+last.getId()+"/send", operator, "POST", "{}").statusCode());
        var response = call(path, client, "GET", null);
        assertEquals(200, response.statusCode(), response.body());
        var data = JsonParser.parseString(response.body()).getAsJsonObject();
        assertEquals(last.getId(), data.get("id").getAsString());
        assertTrue(data.has("receivedAt"));
        for (String privateField : new String[]{"stats", "createdBy", "segmentId", "recipientEmails"})
            assertFalse(data.has(privateField));
        assertEquals(204, call(path, other, "GET", null).statusCode());
        assertEquals(403, call("/api/campaigns", client, "GET", null).statusCode());
        assertEquals(403, call("/api/campaigns/"+last.getId(), client, "GET", null).statusCode());
        assertEquals(409, call("/api/campaigns/"+last.getId(), operator, "PATCH", "{\"body\":\"Changed\"}").statusCode());
        // Membership changes do not grant access to campaigns sent before joining.
        own.setSegmentId(otherSegment); clients.save(own);
        assertEquals(last.getId(), JsonParser.parseString(call(path, client, "GET", null).body())
            .getAsJsonObject().get("id").getAsString());
        Campaign onlyOther = campaign(otherSegment, "Other audience", LocalDateTime.now());
        assertEquals(200, call("/api/campaigns/"+onlyOther.getId()+"/send", operator, "POST", "{}").statusCode());
        assertEquals(onlyOther.getId(), JsonParser.parseString(call(path, other, "GET", null).body())
            .getAsJsonObject().get("id").getAsString());
    }
}
