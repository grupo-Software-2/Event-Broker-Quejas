package co.edu.uptc.broker.controller;

import co.edu.uptc.broker.monitor.AsyncTaskMonitor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
@CrossOrigin(origins = "*")
public class AsyncMetricsController {

    private final AsyncTaskMonitor monitor;

    public AsyncMetricsController(AsyncTaskMonitor monitor) {
        this.monitor = monitor;
    }

    @GetMapping("/async-task")
    public ResponseEntity<AsyncTaskMonitor.ExecutorMetrics> getAsyncTaskMetrics() {
        AsyncTaskMonitor.ExecutorMetrics metrics = monitor.getMetrics();
        return ResponseEntity.ok(metrics);
    }
}
