package com.groceryshop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.groceryshop.exception.BadRequestException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final String BACKUPS_DIR = "backups";
    private static final String STATS_STATE_FILE = ".stats-state.json";
    private static final long DEMO_STEP_DELAY_MS = 400L;

    /**
     * Thứ tự chèn dữ liệu chuẩn từ bảng cha -> bảng con (đảm bảo toàn vẹn khóa ngoại SQL Server).
     * Khi xóa sẽ chạy theo chiều đảo ngược.
     */
    private static final List<String> TABLES_INSERT_ORDER = List.of(
            "roles",
            "users",
            "categories",
            "brands",
            "suppliers",
            "payment_method_configs",
            "coupons",
            "addresses",
            "products",
            "product_images",
            "inventory",
            "goods_receipt",
            "goods_receipt_items",
            "product_batches",
            "inventory_ledger",
            "cart",
            "cart_items",
            "orders",
            "order_items",
            "payments",
            "reviews",
            "customer_messages"
    );

    @Value("${file.upload-dir:uploads}")
    private String uploadDirPath;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SystemActivityService systemActivityService;

    private final ObjectMapper objectMapper;
    private final Object operationLock = new Object();

    private volatile ProgressState currentProgress = ProgressState.builder()
            .active(false)
            .status("idle")
            .steps(List.of())
            .build();

    public BackupService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepState {
        private String name;
        private String status; // pending | running | done | error
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressState {
        private boolean active;
        private String operation; // backup | restore
        private String status;    // idle | running | completed | error
        private int stepIndex;
        private int totalSteps;
        private String stepName;
        private int percent;
        private String message;
        private String error;
        private String filename;
        private List<StepState> steps;
        private String updatedAt;
    }

    public ProgressState getProgress() {
        return currentProgress;
    }

    private void demoDelay() {
        try {
            Thread.sleep(DEMO_STEP_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private List<StepState> buildSteps(List<String> names, int runningIdx) {
        List<StepState> list = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            String st = i < runningIdx ? "done" : (i == runningIdx ? "running" : "pending");
            list.add(new StepState(names.get(i), st));
        }
        return list;
    }

    private List<StepState> buildCompletedSteps(List<String> names) {
        List<StepState> list = new ArrayList<>();
        for (String name : names) {
            list.add(new StepState(name, "done"));
        }
        return list;
    }

    private List<StepState> buildErrorSteps(List<String> names, int failedIdx) {
        List<StepState> list = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            String st = i < failedIdx ? "done" : (i == failedIdx ? "error" : "pending");
            list.add(new StepState(names.get(i), st));
        }
        return list;
    }

    private void updateProgress(boolean active, String operation, String status, int stepIndex,
                                int totalSteps, String stepName, int percent, String message,
                                String error, String filename, List<StepState> steps) {
        this.currentProgress = ProgressState.builder()
                .active(active)
                .operation(operation)
                .status(status)
                .stepIndex(stepIndex)
                .totalSteps(totalSteps)
                .stepName(stepName)
                .percent(percent)
                .message(message)
                .error(error)
                .filename(filename)
                .steps(steps)
                .updatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    private Path getBackupsPath() throws IOException {
        Path p = Paths.get(BACKUPS_DIR);
        Files.createDirectories(p);
        return p;
    }

    private Path getUploadsPath() throws IOException {
        Path p = Paths.get(uploadDirPath);
        Files.createDirectories(p);
        return p;
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new BadRequestException("Tên file backup không hợp lệ");
        }
        String name = Paths.get(filename).getFileName().toString();
        if (!name.endsWith(".zip") || name.contains("..")) {
            throw new BadRequestException("Chỉ chấp nhận tệp sao lưu định dạng .zip");
        }
        return name;
    }

    public Map<String, Object> getLiveStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long totalRecords = 0;
        Map<String, Long> tableCounts = new LinkedHashMap<>();

        try (Connection conn = dataSource.getConnection()) {
            for (String table : TABLES_INSERT_ORDER) {
                long cnt = countTableRows(conn, table);
                tableCounts.put(table, cnt);
                totalRecords += cnt;
            }
        } catch (Exception e) {
            log.warn("Lỗi đếm thống kê bảng SQL Server: {}", e.getMessage());
        }

        int uploadsCount = 0;
        try {
            Path up = getUploadsPath();
            try (var stream = Files.list(up)) {
                uploadsCount = (int) stream.filter(Files::isRegularFile).count();
            }
        } catch (Exception ignored) {}

        stats.put("totalRecords", totalRecords);
        stats.put("products", tableCounts.getOrDefault("products", 0L));
        stats.put("orders", tableCounts.getOrDefault("orders", 0L));
        stats.put("users", tableCounts.getOrDefault("users", 0L));
        stats.put("coupons", tableCounts.getOrDefault("coupons", 0L));
        stats.put("batches", tableCounts.getOrDefault("product_batches", 0L));
        stats.put("uploadsCount", uploadsCount);
        stats.put("tableCounts", tableCounts);
        return stats;
    }

    private long countTableRows(Connection conn, String table) {
        String sql = "SELECT COUNT(*) FROM [" + table + "]";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException ignored) {}
        return 0L;
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("databaseEngine", "Microsoft SQL Server (supermarket_db1)");
        res.put("backupFormat", "ZIP (data.json + SHA-256 + uploads/)");
        res.put("demoDelayMs", DEMO_STEP_DELAY_MS);
        res.put("writeLocked", systemActivityService.isWriteLocked());
        res.put("lockReason", systemActivityService.getLockReason());
        res.put("liveStats", getLiveStats());
        res.put("comparison", loadStatsState());
        return res;
    }

    public List<Map<String, Object>> listBackups() {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            Path dir = getBackupsPath();
            try (var stream = Files.list(dir)) {
                List<Path> files = stream
                        .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".zip"))
                        .sorted((a, b) -> {
                            try {
                                return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                            } catch (IOException e) {
                                return 0;
                            }
                        })
                        .toList();

                for (Path p : files) {
                    String name = p.getFileName().toString();
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("filename", name);
                    item.put("sizeBytes", Files.size(p));
                    item.put("createdAt", LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(p).toInstant(),
                            TimeZone.getDefault().toZoneId()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    item.put("type", name.startsWith("pre-restore_") ? "pre-restore" : "manual");

                    try {
                        Map<String, Object> meta = readMetaFromZip(p);
                        if (meta != null) {
                            item.put("stats", meta.get("stats"));
                            item.put("checksum", meta.get("checksum"));
                            if (meta.get("createdAt") != null) {
                                item.put("createdAt", meta.get("createdAt"));
                            }
                        }
                    } catch (Exception ignored) {}
                    list.add(item);
                }
            }
        } catch (IOException e) {
            log.warn("Không thể liệt kê thư mục backups: {}", e.getMessage());
        }
        return list;
    }

    public Map<String, Object> createBackup(String prefix) {
        synchronized (operationLock) {
            List<String> stepNames = List.of(
                    "Tạm khóa thao tác ghi hệ thống",
                    "Quét & trích xuất các bảng SQL Server",
                    "Đóng gói dữ liệu JSON & thống kê",
                    "Nén thư mục ảnh sản phẩm (uploads)",
                    "Tính mã kiểm tra toàn vẹn SHA-256",
                    "Lưu tệp sao lưu & mở khóa hệ thống"
            );

            String safePrefix = (prefix != null && !prefix.isBlank()) ? prefix : "backup";
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = safePrefix + "_" + timestamp + ".zip";
            int currentStep = 0;

            try {
                // Bước 1: Khóa ghi
                updateProgress(true, "backup", "running", 1, 6, stepNames.get(0), 10,
                        "Đang tạm khóa thao tác ghi để đảm bảo dữ liệu nhất quán...", null, filename, buildSteps(stepNames, 0));
                boolean lockedHere = systemActivityService.tryAcquireWriteLock("Sao lưu dữ liệu: " + filename);
                demoDelay();

                // Bước 2: Trích xuất bảng SQL Server
                currentStep = 1;
                updateProgress(true, "backup", "running", 2, 6, stepNames.get(1), 28,
                        "Đang xuất toàn bộ dữ liệu từ 22 bảng SQL Server...", null, filename, buildSteps(stepNames, 1));
                Map<String, List<Map<String, Object>>> tablesData = new LinkedHashMap<>();
                Map<String, Object> liveStats = getLiveStats();
                try (Connection conn = dataSource.getConnection()) {
                    for (String table : TABLES_INSERT_ORDER) {
                        tablesData.put(table, dumpTable(conn, table));
                    }
                }
                demoDelay();

                // Bước 3: Đóng gói metadata & checksum
                currentStep = 2;
                updateProgress(true, "backup", "running", 3, 6, stepNames.get(2), 50,
                        "Đang đóng gói data.json và bảng thống kê bản ghi...", null, filename, buildSteps(stepNames, 2));
                String createdIso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("version", "1.0-sqlserver");
                meta.put("createdAt", createdIso);
                meta.put("type", safePrefix);
                meta.put("stats", liveStats);
                demoDelay();

                // Bước 5 tính trước checksum (sắp xếp hiển thị bước 4 nén ảnh, bước 5 SHA-256)
                currentStep = 3;
                updateProgress(true, "backup", "running", 4, 6, stepNames.get(3), 70,
                        "Đang nén toàn bộ thư mục ảnh uploads/ vào tệp ZIP...", null, filename, buildSteps(stepNames, 3));
                demoDelay();

                currentStep = 4;
                updateProgress(true, "backup", "running", 5, 6, stepNames.get(4), 85,
                        "Đang tạo mã kiểm tra toàn vẹn SHA-256...", null, filename, buildSteps(stepNames, 4));
                String checksum = computeChecksum(meta, tablesData);
                meta.put("checksum", checksum);

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("meta", meta);
                payload.put("tables", tablesData);
                byte[] jsonBytes = objectMapper.writeValueAsBytes(payload);
                demoDelay();

                // Bước 6: Ghi file zip
                currentStep = 5;
                updateProgress(true, "backup", "running", 6, 6, stepNames.get(5), 95,
                        "Đang ghi tệp " + filename + " ra ổ đĩa...", null, filename, buildSteps(stepNames, 5));
                Path destZip = getBackupsPath().resolve(filename);
                writeBackupZip(destZip, jsonBytes);

                if (lockedHere) {
                    systemActivityService.releaseWriteLock();
                }
                demoDelay();

                systemActivityService.addLog("BACKUP", "Tạo bản sao lưu thành công",
                        filename + " (" + liveStats.get("totalRecords") + " bản ghi, " + liveStats.get("uploadsCount") + " ảnh)",
                        "SUCCESS");

                updateProgress(false, "backup", "completed", 6, 6, "Hoàn tất sao lưu", 100,
                        "Đã tạo bản sao lưu " + filename + " thành công!", null, filename, buildCompletedSteps(stepNames));

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("filename", filename);
                result.put("sizeBytes", Files.size(destZip));
                result.put("meta", meta);
                return result;
            } catch (Exception e) {
                systemActivityService.releaseWriteLock();
                systemActivityService.addLog("BACKUP", "Sao lưu thất bại", e.getMessage(), "ERROR");
                updateProgress(false, "backup", "error", currentStep + 1, 6,
                        stepNames.get(currentStep), 0, "Lỗi khi sao lưu: " + e.getMessage(),
                        e.getMessage(), filename, buildErrorSteps(stepNames, currentStep));
                throw new BadRequestException("Sao lưu thất bại: " + e.getMessage());
            }
        }
    }

    public Map<String, Object> restoreFromSavedFile(String filename) {
        String safeName = sanitizeFilename(filename);
        Path zipPath = Paths.get(BACKUPS_DIR).resolve(safeName);
        if (!Files.exists(zipPath)) {
            throw new BadRequestException("Không tìm thấy tệp sao lưu: " + safeName);
        }
        return restoreInternal(zipPath, safeName);
    }

    public Map<String, Object> restoreFromUploadedFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn tệp sao lưu .zip hợp lệ");
        }
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "uploaded.zip";
        if (!originalName.toLowerCase().endsWith(".zip")) {
            throw new BadRequestException("Tệp tải lên phải có định dạng .zip");
        }
        Path tempZip = null;
        try {
            tempZip = Files.createTempFile("minimart-restore-", ".zip");
            file.transferTo(tempZip);
            return restoreInternal(tempZip, Paths.get(originalName).getFileName().toString());
        } catch (IOException e) {
            throw new BadRequestException("Không thể đọc tệp tải lên: " + e.getMessage());
        } finally {
            if (tempZip != null) {
                try {
                    Files.deleteIfExists(tempZip);
                } catch (IOException ignored) {}
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> restoreInternal(Path zipPath, String displayFilename) {
        synchronized (operationLock) {
            List<String> stepNames = List.of(
                    "Kiểm tra tệp & mã toàn vẹn SHA-256",
                    "Tạo bản an toàn (pre-restore) trước khi ghi đè",
                    "Tạm khóa thao tác ghi hệ thống",
                    "Xóa dữ liệu cũ theo thứ tự khóa ngoại",
                    "Phục hồi dữ liệu SQL Server (IDENTITY_INSERT)",
                    "Khôi phục thư mục ảnh sản phẩm (uploads)",
                    "Xác minh & đối chiếu số liệu bản ghi",
                    "Mở khóa hệ thống & hoàn tất"
            );

            int currentStep = 0;
            String safetyBackupName = null;

            try {
                // Bước 1: Kiểm tra file & checksum SHA-256
                updateProgress(true, "restore", "running", 1, 8, stepNames.get(0), 10,
                        "Đang giải nén và kiểm tra chữ ký SHA-256 của " + displayFilename + "...", null, displayFilename, buildSteps(stepNames, 0));
                Map<String, Object> parsed = readAndValidateZipPayload(zipPath);
                Map<String, Object> meta = (Map<String, Object>) parsed.get("meta");
                Map<String, List<Map<String, Object>>> tablesData = (Map<String, List<Map<String, Object>>>) parsed.get("tables");
                Map<String, Object> expectedStats = (Map<String, Object>) meta.get("stats");
                demoDelay();

                // Chụp thống kê trước khi phục hồi
                Map<String, Object> beforeRestoreStats = getLiveStats();

                // Bước 2: Tạo bản an toàn pre-restore
                currentStep = 1;
                updateProgress(true, "restore", "running", 2, 8, stepNames.get(1), 22,
                        "Đang tạo bản sao lưu an toàn pre-restore đề phòng sự cố...", null, displayFilename, buildSteps(stepNames, 1));
                safetyBackupName = createSilentSafetyBackup();
                demoDelay();

                // Bước 3: Khóa ghi toàn cục
                currentStep = 2;
                updateProgress(true, "restore", "running", 3, 8, stepNames.get(2), 34,
                        "Đang tạm khóa mọi thao tác ghi dữ liệu trên hệ thống...", null, displayFilename, buildSteps(stepNames, 2));
                systemActivityService.forceAcquireWriteLock("Phục hồi dữ liệu từ: " + displayFilename);
                demoDelay();

                // Bước 4 & 5: Xóa cũ & Chèn mới trong 1 Transaction SQL Server
                currentStep = 3;
                updateProgress(true, "restore", "running", 4, 8, stepNames.get(3), 48,
                        "Đang dọn dẹp dữ liệu cũ theo thứ tự khóa ngoại...", null, displayFilename, buildSteps(stepNames, 3));
                demoDelay();

                currentStep = 4;
                updateProgress(true, "restore", "running", 5, 8, stepNames.get(4), 66,
                        "Đang nạp dữ liệu các bảng vào SQL Server (giữ nguyên ID gốc)...", null, displayFilename, buildSteps(stepNames, 4));
                restoreDatabaseTransactionally(tablesData);
                demoDelay();

                // Bước 6: Khôi phục thư mục uploads
                currentStep = 5;
                updateProgress(true, "restore", "running", 6, 8, stepNames.get(5), 80,
                        "Đang khôi phục ảnh sản phẩm & mã QR vào thư mục uploads/...", null, displayFilename, buildSteps(stepNames, 5));
                restoreUploadsFromZip(zipPath);
                demoDelay();

                // Bước 7: Xác minh số liệu
                currentStep = 6;
                updateProgress(true, "restore", "running", 7, 8, stepNames.get(6), 92,
                        "Đang đối chiếu số liệu sau phục hồi với bản sao lưu...", null, displayFilename, buildSteps(stepNames, 6));
                Map<String, Object> afterRestoreStats = getLiveStats();
                saveStatsState(beforeRestoreStats, afterRestoreStats, expectedStats, displayFilename);
                demoDelay();

                // Bước 8: Mở khóa hệ thống
                currentStep = 7;
                updateProgress(true, "restore", "running", 8, 8, stepNames.get(7), 98,
                        "Đang mở khóa hệ thống...", null, displayFilename, buildSteps(stepNames, 7));
                systemActivityService.releaseWriteLock();
                demoDelay();

                systemActivityService.addLog("RESTORE", "Phục hồi dữ liệu thành công",
                        "Đã phục hồi từ " + displayFilename + " (" + afterRestoreStats.get("totalRecords") + " bản ghi). Bản an toàn: " + safetyBackupName,
                        "SUCCESS");

                updateProgress(false, "restore", "completed", 8, 8, "Hoàn tất phục hồi", 100,
                        "Phục hồi dữ liệu thành công từ " + displayFilename + "!", null, displayFilename, buildCompletedSteps(stepNames));

                Map<String, Object> res = new LinkedHashMap<>();
                res.put("success", true);
                res.put("filename", displayFilename);
                res.put("safetyBackup", safetyBackupName);
                res.put("expectedStats", expectedStats);
                res.put("beforeRestoreStats", beforeRestoreStats);
                res.put("afterRestoreStats", afterRestoreStats);
                return res;
            } catch (Exception e) {
                systemActivityService.releaseWriteLock();
                systemActivityService.addLog("RESTORE", "Từ chối / Lỗi phục hồi", displayFilename + ": " + e.getMessage(), "ERROR");
                updateProgress(false, "restore", "error", currentStep + 1, 8,
                        stepNames.get(currentStep), 0, "Phục hồi thất bại: " + e.getMessage(),
                        e.getMessage(), displayFilename, buildErrorSteps(stepNames, currentStep));
                throw new BadRequestException(e.getMessage());
            }
        }
    }

    private String createSilentSafetyBackup() throws Exception {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String name = "pre-restore_" + ts + ".zip";
        Map<String, List<Map<String, Object>>> tablesData = new LinkedHashMap<>();
        Map<String, Object> liveStats = getLiveStats();
        try (Connection conn = dataSource.getConnection()) {
            for (String table : TABLES_INSERT_ORDER) {
                tablesData.put(table, dumpTable(conn, table));
            }
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("version", "1.0-sqlserver");
        meta.put("createdAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        meta.put("type", "pre-restore");
        meta.put("stats", liveStats);
        meta.put("checksum", computeChecksum(meta, tablesData));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("meta", meta);
        payload.put("tables", tablesData);

        Path dest = getBackupsPath().resolve(name);
        writeBackupZip(dest, objectMapper.writeValueAsBytes(payload));
        return name;
    }

    private List<Map<String, Object>> dumpTable(Connection conn, String table) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT * FROM [" + table + "]";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    String colName = meta.getColumnName(i);
                    Object val = rs.getObject(i);
                    if (val instanceof Timestamp ts) {
                        val = ts.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    } else if (val instanceof java.sql.Date d) {
                        val = d.toLocalDate().toString();
                    } else if (val instanceof Time t) {
                        val = t.toLocalTime().toString();
                    } else if (val instanceof byte[] bytes) {
                        val = Base64.getEncoder().encodeToString(bytes);
                    } else if (val instanceof Clob clob) {
                        val = clob.getSubString(1, (int) clob.length());
                    }
                    row.put(colName, val);
                }
                rows.add(row);
            }
        }
        return rows;
    }

    private void restoreDatabaseTransactionally(Map<String, List<Map<String, Object>>> tablesData) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                // 1. Xóa dữ liệu cũ từ bảng con -> bảng cha
                List<String> deleteOrder = new ArrayList<>(TABLES_INSERT_ORDER);
                Collections.reverse(deleteOrder);
                try (Statement st = conn.createStatement()) {
                    for (String table : deleteOrder) {
                        st.executeUpdate("DELETE FROM [" + table + "]");
                        resetIdentityIfPresent(conn, table);
                    }
                }

                // 2. Chèn lại dữ liệu theo thứ tự bảng cha -> bảng con
                for (String table : TABLES_INSERT_ORDER) {
                    List<Map<String, Object>> rows = tablesData.get(table);
                    if (rows == null || rows.isEmpty()) continue;

                    Map<String, Integer> colTypes = getColumnSqlTypes(conn, table);
                    boolean hasIdentity = isTableIdentity(conn, table);

                    // Chỉ lấy các cột thực sự tồn tại trong bảng hiện tại
                    List<String> cols = new ArrayList<>();
                    for (String key : rows.get(0).keySet()) {
                        if (colTypes.containsKey(key.toLowerCase())) {
                            cols.add(key);
                        }
                    }
                    if (cols.isEmpty()) continue;

                    StringBuilder colSql = new StringBuilder();
                    StringBuilder qSql = new StringBuilder();
                    for (int i = 0; i < cols.size(); i++) {
                        if (i > 0) {
                            colSql.append(", ");
                            qSql.append(", ");
                        }
                        colSql.append("[").append(cols.get(i)).append("]");
                        qSql.append("?");
                    }
                    String insertSql = "INSERT INTO [" + table + "] (" + colSql + ") VALUES (" + qSql + ")";

                    if (hasIdentity) {
                        try (Statement st = conn.createStatement()) {
                            st.execute("SET IDENTITY_INSERT [" + table + "] ON");
                        }
                    }

                    try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                        for (Map<String, Object> row : rows) {
                            for (int i = 0; i < cols.size(); i++) {
                                String col = cols.get(i);
                                int sqlType = colTypes.getOrDefault(col.toLowerCase(), Types.NVARCHAR);
                                bindParameter(ps, i + 1, row.get(col), sqlType);
                            }
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    } finally {
                        if (hasIdentity) {
                            try (Statement st = conn.createStatement()) {
                                st.execute("SET IDENTITY_INSERT [" + table + "] OFF");
                            }
                        }
                    }
                }

                conn.commit();
            } catch (Exception ex) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {}
                throw ex;
            } finally {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                } catch (SQLException ignored) {}
            }
        }
    }

    private void resetIdentityIfPresent(Connection conn, String table) {
        if (!isTableIdentity(conn, table)) return;
        try (Statement st = conn.createStatement()) {
            st.execute("DBCC CHECKIDENT ('" + table + "', RESEED, 0)");
        } catch (SQLException ignored) {}
    }

    private boolean isTableIdentity(Connection conn, String table) {
        String sql = "SELECT COUNT(*) FROM sys.identity_columns WHERE object_id = OBJECT_ID(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "dbo." + table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private Map<String, Integer> getColumnSqlTypes(Connection conn, String table) throws SQLException {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT TOP 0 * FROM [" + table + "]";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                map.put(meta.getColumnName(i).toLowerCase(), meta.getColumnType(i));
            }
        }
        return map;
    }

    private void bindParameter(PreparedStatement ps, int idx, Object value, int sqlType) throws SQLException {
        if (value == null) {
            ps.setNull(idx, sqlType);
            return;
        }

        switch (sqlType) {
            case Types.BIT, Types.BOOLEAN -> {
                if (value instanceof Boolean b) {
                    ps.setBoolean(idx, b);
                } else if (value instanceof Number n) {
                    ps.setBoolean(idx, n.intValue() != 0);
                } else {
                    ps.setBoolean(idx, Boolean.parseBoolean(value.toString()));
                }
            }
            case Types.TINYINT, Types.SMALLINT, Types.INTEGER -> {
                if (value instanceof Number n) {
                    ps.setInt(idx, n.intValue());
                } else {
                    ps.setInt(idx, Integer.parseInt(value.toString()));
                }
            }
            case Types.BIGINT -> {
                if (value instanceof Number n) {
                    ps.setLong(idx, n.longValue());
                } else {
                    ps.setLong(idx, Long.parseLong(value.toString()));
                }
            }
            case Types.FLOAT, Types.REAL, Types.DOUBLE -> {
                if (value instanceof Number n) {
                    ps.setDouble(idx, n.doubleValue());
                } else {
                    ps.setDouble(idx, Double.parseDouble(value.toString()));
                }
            }
            case Types.NUMERIC, Types.DECIMAL -> {
                ps.setBigDecimal(idx, new BigDecimal(value.toString()));
            }
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> {
                String s = value.toString();
                if (s.endsWith("Z")) {
                    s = s.substring(0, s.length() - 1);
                }
                LocalDateTime ldt = LocalDateTime.parse(s);
                ps.setTimestamp(idx, Timestamp.valueOf(ldt));
            }
            case Types.DATE -> {
                String s = value.toString();
                if (s.length() > 10) s = s.substring(0, 10);
                ps.setDate(idx, java.sql.Date.valueOf(LocalDate.parse(s)));
            }
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY -> {
                ps.setBytes(idx, Base64.getDecoder().decode(value.toString()));
            }
            default -> ps.setString(idx, value.toString());
        }
    }

    private void writeBackupZip(Path destZip, byte[] jsonBytes) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(destZip))) {
            // 1. data.json
            ZipEntry dataEntry = new ZipEntry("data.json");
            zos.putNextEntry(dataEntry);
            zos.write(jsonBytes);
            zos.closeEntry();

            // 2. uploads/*
            Path uploadsPath = getUploadsPath();
            if (Files.exists(uploadsPath)) {
                try (var stream = Files.walk(uploadsPath)) {
                    for (Path file : stream.filter(Files::isRegularFile).toList()) {
                        String rel = uploadsPath.relativize(file).toString().replace("\\", "/");
                        ZipEntry entry = new ZipEntry("uploads/" + rel);
                        zos.putNextEntry(entry);
                        Files.copy(file, zos);
                        zos.closeEntry();
                    }
                }
            }
        }
    }

    private void restoreUploadsFromZip(Path zipPath) throws IOException {
        Path uploadsPath = getUploadsPath();
        // Dọn ảnh cũ (giữ lại thư mục gốc)
        try (var stream = Files.list(uploadsPath)) {
            for (Path p : stream.toList()) {
                if (Files.isRegularFile(p)) {
                    Files.deleteIfExists(p);
                }
            }
        }

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().startsWith("uploads/")) {
                    String rel = entry.getName().substring("uploads/".length());
                    if (rel.isEmpty() || rel.contains("..")) continue;
                    Path target = uploadsPath.resolve(rel).normalize();
                    if (!target.startsWith(uploadsPath.normalize())) continue;
                    Files.createDirectories(target.getParent());
                    Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMetaFromZip(Path zipPath) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("data.json".equals(entry.getName())) {
                    Map<String, Object> data = objectMapper.readValue(zis, new TypeReference<Map<String, Object>>() {});
                    return (Map<String, Object>) data.get("meta");
                }
                zis.closeEntry();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readAndValidateZipPayload(Path zipPath) throws Exception {
        byte[] dataJsonBytes = null;
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("data.json".equals(entry.getName())) {
                    dataJsonBytes = zis.readAllBytes();
                    break;
                }
                zis.closeEntry();
            }
        }

        if (dataJsonBytes == null || dataJsonBytes.length == 0) {
            throw new BadRequestException("Tệp sao lưu không hợp lệ: thiếu data.json");
        }

        Map<String, Object> payload = objectMapper.readValue(dataJsonBytes, new TypeReference<Map<String, Object>>() {});
        Map<String, Object> meta = (Map<String, Object>) payload.get("meta");
        Map<String, List<Map<String, Object>>> tables = (Map<String, List<Map<String, Object>>>) payload.get("tables");

        if (meta == null || tables == null || meta.get("checksum") == null) {
            throw new BadRequestException("Cấu trúc data.json trong tệp sao lưu bị thiếu thông tin kiểm tra");
        }

        String providedChecksum = String.valueOf(meta.get("checksum"));
        String computedChecksum = computeChecksum(meta, tables);
        if (!providedChecksum.equalsIgnoreCase(computedChecksum)) {
            throw new BadRequestException("Checksum SHA-256 không khớp! Tệp sao lưu có thể đã bị chỉnh sửa hoặc hỏng. Đã hủy phục hồi để bảo vệ dữ liệu.");
        }

        return payload;
    }

    private String computeChecksum(Map<String, Object> meta, Map<String, List<Map<String, Object>>> tables) throws Exception {
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("version", meta.get("version"));
        canonical.put("createdAt", meta.get("createdAt"));
        canonical.put("stats", meta.get("stats"));
        canonical.put("tables", tables);

        // Chuẩn hóa qua cùng chu trình JSON serialize -> deserialize để đảm bảo kiểu số (BigDecimal/Double/Long) đồng nhất 100% lúc Backup & Restore
        byte[] normalizedPass = objectMapper.writeValueAsBytes(canonical);
        Map<String, Object> normalizedMap = objectMapper.readValue(normalizedPass, new TypeReference<Map<String, Object>>() {});

        ObjectMapper canonicalMapper = new ObjectMapper();
        canonicalMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        byte[] bytes = canonicalMapper.writeValueAsBytes(normalizedMap);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(bytes);
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    public Path resolveBackupFileForDownload(String filename) {
        String safe = sanitizeFilename(filename);
        Path p = Paths.get(BACKUPS_DIR).resolve(safe).normalize();
        if (!Files.exists(p)) {
            throw new BadRequestException("Không tìm thấy tệp sao lưu: " + safe);
        }
        return p;
    }

    public void deleteBackupFile(String filename) {
        String safe = sanitizeFilename(filename);
        Path p = Paths.get(BACKUPS_DIR).resolve(safe).normalize();
        try {
            if (!Files.deleteIfExists(p)) {
                throw new BadRequestException("Không tìm thấy tệp sao lưu để xóa: " + safe);
            }
            systemActivityService.addLog("BACKUP", "Xóa tệp sao lưu", safe, "INFO");
        } catch (IOException e) {
            throw new BadRequestException("Không thể xóa tệp sao lưu: " + e.getMessage());
        }
    }

    private Map<String, Object> loadStatsState() {
        try {
            Path p = getBackupsPath().resolve(STATS_STATE_FILE);
            if (Files.exists(p)) {
                return objectMapper.readValue(p.toFile(), new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception ignored) {}
        Map<String, Object> empty = new LinkedHashMap<>();
        empty.put("expectedBackup", null);
        empty.put("beforeRestore", null);
        empty.put("afterRestore", null);
        empty.put("restoredFrom", null);
        empty.put("updatedAt", null);
        return empty;
    }

    private void saveStatsState(Map<String, Object> beforeRestore,
                                Map<String, Object> afterRestore,
                                Map<String, Object> expectedBackup,
                                String restoredFrom) {
        try {
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("expectedBackup", expectedBackup);
            state.put("beforeRestore", beforeRestore);
            state.put("afterRestore", afterRestore);
            state.put("restoredFrom", restoredFrom);
            state.put("updatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            objectMapper.writeValue(getBackupsPath().resolve(STATS_STATE_FILE).toFile(), state);
        } catch (Exception e) {
            log.warn("Không thể lưu trạng thái thống kê đối chiếu: {}", e.getMessage());
        }
    }
}
