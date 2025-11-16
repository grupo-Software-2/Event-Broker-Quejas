package co.edu.uptc.broker.service;

import co.edu.uptc.broker.dto.EventDTO;
import co.edu.uptc.broker.dto.SubscriptionDTO;
import co.edu.uptc.broker.model.Event;
import co.edu.uptc.broker.model.Subscription;
import co.edu.uptc.broker.repository.EventRepository;
import co.edu.uptc.broker.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class EventBrokerService {

    private static final Logger log = LoggerFactory.getLogger(EventBrokerService.class);

    private final EventRepository eventRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final RestTemplate restTemplate;

    public EventBrokerService(EventRepository eventRepository, SubscriptionRepository subscriptionRepository,
                              RestTemplate restTemplate) {
        this.eventRepository = eventRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.restTemplate = restTemplate;
    }

    public String publishEvent(EventDTO eventDTO) {
        Event event = new Event();
        event.setEventId(eventDTO.getEventId() != null ? eventDTO.getEventId() : UUID.randomUUID().toString());
        event.setEventType(eventDTO.getEventType());
        event.setPayload(eventDTO.toString()); // Serializar el DTO completo
        event.setSource(eventDTO.getSource());
        event.setTimestamp(eventDTO.getTimestamp() != null ? eventDTO.getTimestamp() : LocalDateTime.now());
        event.setProcessed(false);

        Event savedEvent = eventRepository.save(event);
        log.info("Event saved with id: {}", savedEvent.getEventId());

        distributeEvent(eventDTO);

        return savedEvent.getEventId();
    }

    @Async("eventDistributionExecutor")
    public CompletableFuture<Void> distributeEvent(EventDTO event) {
        return CompletableFuture.runAsync(() -> {
            try {
                List<Subscription> subscribers = subscriptionRepository
                        .findByEventTypeAndActiveTrue(event.getEventType());

                if (subscribers.isEmpty()) {
                    log.warn("No active subscribers found for event type: {}", event.getEventType());
                    return;
                }

                log.info("Distributing event {} to {} subscribers", event.getEventId(), subscribers.size());

                List<CompletableFuture<Void>> notifications = subscribers.stream()
                        .map(subscription -> notifySubscriberAsync(subscription, event))
                        .collect(Collectors.toList());

                CompletableFuture.allOf(notifications.toArray(new CompletableFuture[0])).join();

                markEventAsProcessed(event.getEventId());

                log.info("Event {} distributed successfully to all subscribers", event.getEventId());

            } catch (Exception e) {
                log.error("Error distributing event {}: {}", event.getEventId(), e.getMessage());
            }
        });
    }

    @Async("subscriberNotificationExecutor")
    public CompletableFuture<Void> notifySubscriberAsync(Subscription subscription, EventDTO event) {
        return CompletableFuture.runAsync(() -> {
            try {
                notifySubscriber(subscription, event);
            } catch (Exception e) {
                log.error("Failed to notify subscriber {}: {}", subscription.getSubscriptionId(), e.getMessage());
            }
        });
    }

    private void notifySubscriber(Subscription subscription, EventDTO event) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Event-Type", event.getEventType());
            headers.set("X-Event-Id", event.getEventId());

            HttpEntity<EventDTO> request = new HttpEntity<>(event, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(subscription.getCallbackUrl(),
                    request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully notified subscriber: {}", subscription.getCallbackUrl());
                updateSubscriptionStats(subscription, true);
            } else {
                log.warn("Subscriber returned non-2xx status: {} - {}", response.getStatusCode(),
                        subscription.getCallbackUrl());
                updateSubscriptionStats(subscription, false);
            }
        } catch (Exception e) {
            log.error("Error notifying subscriber {}: {}", subscription.getCallbackUrl(), e.getMessage());
            updateSubscriptionStats(subscription, false);
        }
    }

    @Async("eventProcessingExecutor")
    public CompletableFuture<Void> updateSubscriptionStats(Subscription subscription, boolean success) {
        return CompletableFuture.runAsync(() -> {
            try {
                subscription.setLastNotificationAt(LocalDateTime.now());
                if (success) {
                    subscription.setSuccessfulDeliveries(subscription.getSuccessfulDeliveries() + 1);
                } else {
                    subscription.setFailedDeliveries(subscription.getFailedDeliveries() + 1);
                }
                subscriptionRepository.save(subscription);
            } catch (Exception e) {
                log.error("Error updating subscription stats: {}", e.getMessage());
            }
        });
    }

    private void markEventAsProcessed(String eventId) {
        eventRepository.findByEventId(eventId).ifPresent(e -> {
            e.setProcessed(true);
            e.setProcessedAt(LocalDateTime.now());
            eventRepository.save(e);
        });
    }

    public String subscribe(SubscriptionDTO subscriptionDTO) {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(UUID.randomUUID().toString());
        subscription.setEventType(subscriptionDTO.getEventType());
        subscription.setCallbackUrl(subscriptionDTO.getCallbackUrl());
        subscription.setSubscriberName(subscriptionDTO.getSubscriberName());
        subscription.setActive(true);
        subscription.setCreatedAt(LocalDateTime.now());
        subscription.setSuccessfulDeliveries(0);
        subscription.setFailedDeliveries(0);

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("New subscription created: {} for event type: {}", saved.getSubscriptionId(), saved.getEventType());

        return saved.getSubscriptionId();
    }

    public boolean unsubscribe(String subscriptionId) {
        return subscriptionRepository.findBySubscriptionId(subscriptionId).map(subscription -> {
            subscriptionRepository.delete(subscription);
            log.info("Subscription removed: {}", subscriptionId);
            return true;
        }).orElse(false);
    }

    public List<SubscriptionDTO> listSubscriptions(String eventType) {
        List<Subscription> subscriptions;

        if (eventType != null && !eventType.isEmpty()) {
            subscriptions = subscriptionRepository.findByEventTypeAndActiveTrue(eventType);
        } else {
            subscriptions = subscriptionRepository.findByActiveTrue();
        }
        return subscriptions.stream().map(this::toDTO).collect(Collectors.toList());
    }

    private SubscriptionDTO toDTO(Subscription subscription) {
        SubscriptionDTO dto = new SubscriptionDTO();
        dto.setSubscriptionId(subscription.getSubscriptionId());
        dto.setEventType(subscription.getEventType());
        dto.setCallbackUrl(subscription.getCallbackUrl());
        dto.setSubscriberName(subscription.getSubscriberName());
        dto.setActive(subscription.isActive());
        dto.setCreatedAt(subscription.getCreatedAt());
        return dto;
    }
}
