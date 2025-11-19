package co.edu.uptc.broker.service;

import co.edu.uptc.broker.dto.EventDTO;
import co.edu.uptc.broker.model.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class AsyncRetryService {
    private static final Logger log = LoggerFactory.getLogger(AsyncRetryService.class);
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_DELAY_MS = 1000; 

    private final RestTemplate restTemplate;

    public AsyncRetryService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Async("subscriberNotificationExecutor")
    public CompletableFuture<Boolean> notifyWithRetry(Subscription subscription, EventDTO event) {
        return CompletableFuture.supplyAsync(() -> {
            int attempt = 0;
            long delay = INITIAL_DELAY_MS;

            while (attempt < MAX_RETRIES) {
                try {
                    if (attempt > 0) {
                        log.info("Retry attempt {} for subscriber {} after {} ms",
                                attempt, subscription.getCallbackUrl(), delay);
                        TimeUnit.MILLISECONDS.sleep(delay);
                    }

                    boolean success = sendNotification(subscription, event);

                    if (success) {
                        if (attempt > 0) {
                            log.info("Successfully notified subscriber {} after {} retries",
                                    subscription.getCallbackUrl(), attempt);
                        }
                        return true;
                    }

                    attempt++;
                    delay *= 2; 

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Notification thread interrupted for {}", subscription.getCallbackUrl());
                    return false;
                } catch (Exception e) {
                    log.error("Error on attempt {} for {}: {}",
                            attempt + 1, subscription.getCallbackUrl(), e.getMessage());
                    attempt++;
                    delay *= 2;
                }
            }

            log.error("Failed to notify subscriber {} after {} attempts",
                    subscription.getCallbackUrl(), MAX_RETRIES);
            return false;
        });
    }

    private boolean sendNotification(Subscription subscription, EventDTO event) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Event-Type", event.getEventType());
            headers.set("X-Event-Id", event.getEventId());

            HttpEntity<EventDTO> request = new HttpEntity<>(event, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    subscription.getCallbackUrl(),
                    request,
                    String.class
            );

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            log.debug("Failed to send notification to {}: {}",
                    subscription.getCallbackUrl(), e.getMessage());
            return false;
        }
    }

    @Async("subscriberNotificationExecutor")
    public CompletableFuture<Boolean> checkSubscriberHealth(String callbackUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                
                String healthUrl = callbackUrl.replace("/events/", "/");
                if (!healthUrl.endsWith("/health")) {
                    healthUrl = healthUrl.substring(0, healthUrl.lastIndexOf("/")) + "/health";
                }

                ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
                return response.getStatusCode().is2xxSuccessful();

            } catch (Exception e) {
                log.debug("Health check failed for {}: {}", callbackUrl, e.getMessage());
                return false;
            }
        });
    }
}
