package com.groceryshop.controller;

import com.groceryshop.service.BackupService;
import com.groceryshop.service.SystemActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/backups")
public class BackupController {

    @Autowired
    private BackupService backupService;

    @Autowired
    private SystemActivityService systemActivityService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(backupService.getStatus());
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listBackups() {
        return ResponseEntity.ok(backupService.listBackups());
    }

    @GetMapping("/progress")
    public ResponseEntity<BackupService.ProgressState> getProgress() {
        return ResponseEntity.ok(backupService.getProgress());
    }

    @GetMapping("/activity-logs")
    public ResponseEntity<List<SystemActivityService.ActivityLogEntry>> getActivityLogs(
            @RequestParam(defaultValue = "30") int limit) {
        return ResponseEntity.ok(systemActivityService.getLogs(limit));
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createBackup() {
        return ResponseEntity.ok(backupService.createBackup("backup"));
    }

    @PostMapping("/restore/{filename}")
    public ResponseEntity<Map<String, Object>> restoreBackup(@PathVariable String filename) {
        return ResponseEntity.ok(backupService.restoreFromSavedFile(filename));
    }

    @PostMapping(value = "/upload-restore", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadAndRestore(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(backupService.restoreFromUploadedFile(file));
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadBackup(@PathVariable String filename) throws Exception {
        Path path = backupService.resolveBackupFileForDownload(filename);
        Resource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName().toString() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/{filename}")
    public ResponseEntity<Map<String, Object>> deleteBackup(@PathVariable String filename) {
        backupService.deleteBackupFile(filename);
        return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa tệp sao lưu " + filename));
    }
}
