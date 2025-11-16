package co.edu.uptc.broker.controller;

import co.edu.uptc.broker.dto.EventDTO;
import co.edu.uptc.broker.dto.SubscriptionDTO;
import co.edu.uptc.broker.service.EventBrokerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventBrokerController {

    private static final Logger log = LoggerFactory.getLogger(EventBrokerController.class);
    private final EventBrokerService brokerService;

    public EventBrokerController(EventBrokerService brokerService) {
        this.brokerService = brokerService;
    }

    @PostMapping("/publish")
    public ResponseEntity<?> publishEvent(@RequestBody EventDTO event) {
        try {
            log.info("Received event: type={}, id={}", event.getEventType(), event.getEventId());

            String eventId = brokerService.publishEvent(event);

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                    "message", "Event accepted for processing",
                    "eventId", eventId
            ));
        } catch (Exception e) {
            log.error("Error publishing event", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Failed to publish event: " + e.getMessage()));
        }
    }

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@RequestBody SubscriptionDTO subscription) {
        try {
            log.info("New subscription request: {} for event type: {}",
                    subscription.getCallbackUrl(), subscription.getEventType());

            String subscriptionId = brokerService.subscribe(subscription);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Subscription created successfully", "subscriptionId", subscriptionId
            ));
        } catch (Exception e) {
            log.error("Error creating subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Failed to create subscription: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/subscribe/{subscriptionId}")
    public ResponseEntity<?> unsubscribe(@PathVariable String subscriptionId) {
        try {
            boolean removed = brokerService.unsubscribe(subscriptionId);

            if (removed) {
                return ResponseEntity.ok().body(Map.of("message", "Subscription removed successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Subscription not found"));
            }
        } catch (Exception e) {
            log.error("Error removing subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Failed to remove subscription: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<?> listSubscriptions(@RequestParam(required = false) String eventType) {
        try {
            List<SubscriptionDTO> subscriptions = brokerService.listSubscriptions(eventType);
            return ResponseEntity.ok(subscriptions);
        } catch (Exception e) {
            log.error("Error listing subscriptions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Failed to list subscriptions: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "event-broker",
                "timestamp", System.currentTimeMillis()
        ));
    }

}
