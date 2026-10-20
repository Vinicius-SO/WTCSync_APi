package com.fiap.WtcSync.application.services;

import org.springframework.stereotype.Service;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import com.fiap.WtcSync.domain.entities.User;
import java.util.List;

/** One atomic account binding per installation token; multiple devices per account are supported. */
@Service
public class PushDevices {
    @Document("push_devices")
    public static class Device {
        @Id public String token;
        public String email;
    }
    private final MongoTemplate mongo;
    public PushDevices(MongoTemplate mongo) { this.mongo = mongo; }
    public void register(String email, String token) {
        validate(token);
        mongo.upsert(Query.query(Criteria.where("_id").is(token)), new Update().set("email", email), Device.class);
        // Maintain compatibility with existing campaign dispatches without storing a stale User snapshot.
        mongo.updateMulti(Query.query(Criteria.where("fcmToken").is(token).and("email").ne(email)), new Update().unset("fcmToken"), User.class);
        mongo.updateFirst(Query.query(Criteria.where("email").is(email)), new Update().set("fcmToken", token), User.class);
    }
    public void revoke(String email, String token) {
        validate(token);
        mongo.remove(Query.query(Criteria.where("_id").is(token).and("email").is(email)), Device.class);
        mongo.updateFirst(Query.query(Criteria.where("email").is(email).and("fcmToken").is(token)), new Update().unset("fcmToken"), User.class);
    }
    public List<Device> forEmail(String email) {
        return mongo.find(Query.query(Criteria.where("email").is(email)), Device.class);
    }
    private void validate(String token) {
        if (token == null || token.isBlank() || token.length() > 4096)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid FCM token");
    }
}
