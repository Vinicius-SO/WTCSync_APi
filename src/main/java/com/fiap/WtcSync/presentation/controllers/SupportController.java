package com.fiap.WtcSync.presentation.controllers;

import com.fiap.WtcSync.application.services.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/support")
public class SupportController {
    private final SupportQueueService queue;
    private final AccountAccess access;
    private final SupportEvents events;
    public SupportController(SupportQueueService queue, AccountAccess access, SupportEvents events) {
        this.queue = queue; this.access = access; this.events = events;
    }
    @GetMapping("/queue") public List<SupportQueueService.Ticket> list() { return queue.list(); }
    @GetMapping("/{id}") public SupportQueueService.Ticket get(@PathVariable String id) {
        access.requireConversation(id); return queue.get(id);
    }
    @PostMapping("/{id}/claim") public SupportQueueService.Ticket claim(@PathVariable String id) {
        var result = queue.claim(id, access.email()); events.publish(id); return result;
    }
    @PostMapping("/{id}/release") public SupportQueueService.Ticket release(@PathVariable String id) {
        var result = queue.release(id, access.email()); events.publish(id); return result;
    }
}
