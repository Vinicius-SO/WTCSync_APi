package com.fiap.WtcSync.presentation.controllers;

import com.fiap.WtcSync.application.dtos.MessageDTO;
import com.fiap.WtcSync.application.services.*;
import com.fiap.WtcSync.domain.entities.*;
import com.fiap.WtcSync.domain.interfaces.*;
import com.fiap.WtcSync.infrastructure.repositories.ChatPushOutboxStore;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
    "spring.data.mongodb.uri=mongodb://localhost:27017/wtcsync_durable_push_test",
    "spring.mongodb.uri=mongodb://localhost:27017/wtcsync_durable_push_test",
    "spring.mongodb.database=wtcsync_durable_push_test", "chat.push.enabled=false"
})
@EnabledIfEnvironmentVariable(named = "RUN_MONGO_INTEGRATION", matches = "true")
class DurablePushIntegrationTest {
    @Autowired MessageService messages;
    @Autowired IMessageRepository repository;
    @Autowired IUserRepository users;
    @Autowired IClientRepository clients;
    @Autowired PushDevices devices;
    @Autowired SupportQueueService queue;
    @Autowired ChatPushOutboxStore store;
    @Autowired ChatPushOutbox outbox;
    @Autowired MongoTemplate mongo;
    @MockitoBean ChatPushGateway gateway;
    String customer, clientEmail, owner;
    @BeforeEach void fixture() {
        clientEmail=UUID.randomUUID()+"@example.invalid"; owner=UUID.randomUUID()+"@example.invalid";
        users.save(new User("Client", clientEmail, "unused", "CLIENT"));
        users.save(new User("Owner", owner, "unused", "OPERATOR"));
        var client = new Client(); client.setName("Client"); client.setEmail(clientEmail);
        customer=clients.save(client).getId(); queue.claim(customer, owner);
        devices.register(owner, "fake-"+UUID.randomUUID());
    }
    String send() { return messages.sendMessage(new MessageDTO(clientEmail, customer, "Private", null, UUID.randomUUID().toString())).id(); }
    Message persisted(String id) { return repository.findSendById(id).orElseThrow(); }
    void ready(String id) { mongo.updateFirst(Query.query(Criteria.where("_id").is(id)), new Update().set("push.dueAt", Instant.EPOCH), Message.class); }
    @Test void savedMessageAlreadyContainsThePendingJobAndAReplayDoesNotRequeueIt() throws Exception {
        var dto=new MessageDTO(clientEmail, customer, "Private", null, UUID.randomUUID().toString());
        var sent=messages.sendMessage(dto);
        assertEquals("PENDING", persisted(sent.id()).getPush().state);
        assertTrue(outbox.run(sent.id()));
        assertEquals("DONE", persisted(sent.id()).getPush().state);
        assertEquals(sent.id(), messages.sendMessage(dto).id());
        assertFalse(outbox.run(sent.id()));
        assertEquals("DONE", persisted(sent.id()).getPush().state);
    }
    @Test void providerFailureSurvivesAWorkerRestartAndRetriesOnlyTheFailedDevice() throws Exception {
        var registered=devices.forEmail(owner).getFirst();
        devices.register(owner, "fake-second-"+UUID.randomUUID());
        doThrow(new IllegalStateException("temporary outage")).when(gateway)
            .send(eq(registered.token), eq(owner), eq(customer), anyString());
        String id=send();
        assertTrue(outbox.run(id));
        var failed=persisted(id);
        assertEquals("PENDING", failed.getPush().state);
        assertEquals(1, failed.getPush().sentDevices.size());
        assertEquals(1, failed.getPush().attempts);
        assertFalse(outbox.run(id)); // respects persisted backoff
        doNothing().when(gateway).send(eq(registered.token), eq(owner), eq(customer), eq(id));
        ready(id);
        // A new worker/store instance has no memory of earlier attempts.
        var restarted=new ChatPushOutbox(new ChatPushOutboxStore(mongo),
            new ChatNotifications(queue, users, devices, gateway), Runnable::run, false);
        assertTrue(restarted.run(id));
        assertEquals("DONE", persisted(id).getPush().state);
        assertEquals(2, persisted(id).getPush().attempts);
        verify(gateway, times(3)).send(anyString(), eq(owner), eq(customer), eq(id));
    }
    @Test void onlyOneWorkerClaimsAJobAndAnExpiredLeaseCanBeRecovered() throws Exception {
        String id=send(); Instant now=Instant.now();
        try (var pool=Executors.newFixedThreadPool(2)) {
            var start=new CountDownLatch(1);
            var a=pool.submit(() -> { start.await(); return store.claim(id, now); });
            var b=pool.submit(() -> { start.await(); return store.claim(id, now); });
            start.countDown();
            var first=a.get(); var second=b.get();
            assertTrue((first == null) != (second == null));
            var original=first == null ? second : first;
            assertNull(store.claim(id, now.plusSeconds(119)));
            var recovered=store.claim(id, now.plusSeconds(121));
            assertNotNull(recovered);
            assertNotEquals(original.getPush().leaseId, recovered.getPush().leaseId);
            store.finish(original, false, now.plusSeconds(122));
            assertEquals("PROCESSING", persisted(id).getPush().state); // stale worker cannot finish the new lease
            store.finish(recovered, false, now.plusSeconds(122));
            assertEquals("DONE", persisted(id).getPush().state);
        }
    }
    @Test void readDeletedAndExpiredMessagesNeverProduceAStaleAlert() throws Exception {
        String read=send(), deleted=send(), expired=send();
        messages.updateStatus(read, MessageStatus.LIDO);
        messages.deleteMessage(deleted, clientEmail);
        mongo.updateFirst(Query.query(Criteria.where("_id").is(expired)),
            new Update().set("push.queuedAt", Instant.now().minusSeconds(86401)), Message.class);
        assertTrue(outbox.run(read)); assertFalse(outbox.run(deleted)); assertTrue(outbox.run(expired));
        verifyNoInteractions(gateway);
        assertNull(persisted(deleted).getPush());
        assertEquals("DONE", persisted(read).getPush().state);
        assertEquals("DONE", persisted(expired).getPush().state);
    }
    @Test void aReadDuringDeliveryStopsTheRemainingDevices() throws Exception {
        String id=send(); devices.register(owner, "fake-extra-"+UUID.randomUUID());
        doAnswer(invocation -> { messages.updateStatus(id, MessageStatus.LIDO); return null; }).when(gateway)
            .send(anyString(), eq(owner), eq(customer), eq(id));
        assertTrue(outbox.run(id));
        verify(gateway, times(1)).send(anyString(), eq(owner), eq(customer), eq(id));
        assertEquals("DONE", persisted(id).getPush().state);
    }
}
