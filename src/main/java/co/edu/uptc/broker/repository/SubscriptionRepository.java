package co.edu.uptc.broker.repository;

import co.edu.uptc.broker.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findBySubscriptionId(String subscriptionId);

    List<Subscription> findByEventTypeAndActiveTrue(String eventType);

    List<Subscription> findByActiveTrue();
}
