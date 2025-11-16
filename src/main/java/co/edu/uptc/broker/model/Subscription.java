package co.edu.uptc.broker.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String subscriptionId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String callbackUrl;

    private String subscriberName;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastNotificationAt;

    @Column(nullable = false)
    private int successfulDeliveries;

    @Column(nullable = false)
    private int failedDeliveries;

    // Constructores
    public Subscription() {
        this.createdAt = LocalDateTime.now();
        this.active = true;
        this.successfulDeliveries = 0;
        this.failedDeliveries = 0;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getSubscriberName() {
        return subscriberName;
    }

    public void setSubscriberName(String subscriberName) {
        this.subscriberName = subscriberName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastNotificationAt() {
        return lastNotificationAt;
    }

    public void setLastNotificationAt(LocalDateTime lastNotificationAt) {
        this.lastNotificationAt = lastNotificationAt;
    }

    public int getSuccessfulDeliveries() {
        return successfulDeliveries;
    }

    public void setSuccessfulDeliveries(int successfulDeliveries) {
        this.successfulDeliveries = successfulDeliveries;
    }

    public int getFailedDeliveries() {
        return failedDeliveries;
    }

    public void setFailedDeliveries(int failedDeliveries) {
        this.failedDeliveries = failedDeliveries;
    }
}
