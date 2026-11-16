package com.groceryshop.controller;

import com.groceryshop.service.OrderQueueService;
import com.groceryshop.service.SystemActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/queue")
public class QueueController {

    @Autowired
    private OrderQueueService orderQueueService;

    @Autowired
    private SystemActivityService systemActivityService;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(orderQueueService.getSummary());
    }

    @GetMapping("/jobs")
    public ResponseEntity<Map<String, Object>> getJobs(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(orderQueueService.getJobsPage(status, keyword, page, size));
    }

    @GetMapping("/activity-logs")
    public ResponseEntity<List<SystemActivityService.ActivityLogEntry>> getActivityLogs(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(systemActivityService.getLogs(limit));
    }

    @PostMapping("/toggle-pause")
    public ResponseEntity<Map<String, Object>> togglePause() {
        return ResponseEntity.ok(orderQueueService.togglePause());
    }

    @PostMapping("/simulate-failure")
    public ResponseEntity<Map<String, Object>> simulateFailure() {
        return ResponseEntity.ok(orderQueueService.simulateFailureJob());
    }

    @PostMapping("/jobs/{jobId}/retry")
    public ResponseEntity<Map<String, Object>> retryJob(@PathVariable String jobId) {
        return ResponseEntity.ok(orderQueueService.retryJob(jobId));
    }

    @DeleteMapping("/jobs/{jobId}")
    public ResponseEntity<Map<String, Object>> deleteJob(@PathVariable String jobId) {
        orderQueueService.deleteJob(jobId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa Job " + jobId));
    }
}
