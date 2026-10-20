package com.fiap.WtcSync.presentation.controllers;

import com.fiap.WtcSync.application.services.*;
import com.fiap.WtcSync.application.services.SupportQueueService.TicketResponse;
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
    @GetMapping("/queue") public List<TicketResponse> list() { return queue.list().stream().map(TicketResponse::from).toList(); }
    @GetMapping("/{id}") public TicketResponse get(@PathVariable String id) {
        access.requireConversation(id); return TicketResponse.from(queue.get(id));
    }
    @GetMapping("/{id}/history") public List<SupportQueueService.HistoryEntry> history(@PathVariable String id) { return queue.history(id); }
    @PostMapping("/{id}/claim") public TicketResponse claim(@PathVariable String id) {
        var result = queue.claim(id, access.email()); events.publish(id); return TicketResponse.from(result);
    }
    @PostMapping("/{id}/release") public TicketResponse release(@PathVariable String id) {
        var result = queue.release(id, access.email()); events.publish(id); return TicketResponse.from(result);
    }
    @PostMapping("/{id}/close") public TicketResponse close(@PathVariable String id) {
        var result = queue.close(id, access.email()); events.publish(id); return TicketResponse.from(result);
    }
    @PostMapping("/{id}/reopen") public TicketResponse reopen(@PathVariable String id) {
        var result = queue.reopen(id, access.email()); events.publish(id); return TicketResponse.from(result);
    }
}
