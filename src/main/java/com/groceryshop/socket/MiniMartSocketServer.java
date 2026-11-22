package com.groceryshop.socket;

import com.groceryshop.entity.*;
import com.groceryshop.repository.*;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.*;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Socket Server Đa luồng cho ứng dụng MiniMart
 * Tham chiếu tài liệu môn học: Chuong5_Networking_updated.pptx
 * - Quy trình 6 bước Server Socket: Slide 25
 * - Cơ chế Đa luồng xử lý đồng thời nhiều client: Slide 37, 39
 * Chạy trên cổng 9090 ngầm cùng Spring Boot mà không làm ảnh hưởng các luồng REST/MVC.
 */
@Component
public class MiniMartSocketServer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MiniMartSocketServer.class);
    private static final int PORT = 9090;
    private static final int MAX_CLIENTS_POOL = 50;
    private static final int CLIENT_IDLE_TIMEOUT_MS = 60000; // 60 giây timeout chống treo luồng

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private static final AtomicInteger workerSequence = new AtomicInteger(0);

    // Giới hạn ThreadPool bằng newFixedThreadPool(50) thay vì newCachedThreadPool (Tiêu chí 5.4)
    private final ExecutorService clientThreadPool = Executors.newFixedThreadPool(MAX_CLIENTS_POOL, r -> {
        Thread t = new Thread(r);
        t.setName("socket-worker-" + workerSequence.incrementAndGet());
        t.setDaemon(true);
        return t;
    });

    private static final AtomicInteger connectedClientsCount = new AtomicInteger(0);
    private static final AtomicInteger totalConnectionsServed = new AtomicInteger(0);

    @Override
    public void run(String... args) {
        Thread serverThread = new Thread(this::startServer, "socket-server-listener");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private void startServer() {
        try {
            // B1 (Slide 25): Khởi tạo ServerSocket để lắng nghe kết nối từ Client
            serverSocket = new ServerSocket(PORT);
            running = true;
            log.info("[Socket Server] Đã khởi động thành công trên cổng {} (FixedThreadPool={}, Timeout={}ms)",
                    PORT, MAX_CLIENTS_POOL, CLIENT_IDLE_TIMEOUT_MS);

            while (running && !serverSocket.isClosed()) {
                try {
                    // B2 (Slide 25): accept() chấp nhận kết nối từ client
                    Socket clientSocket = serverSocket.accept();
                    int currentConnected = connectedClientsCount.incrementAndGet();
                    totalConnectionsServed.incrementAndGet();
                    log.info("[Socket Server] Client mới kết nối từ: {} | connectedClientsCount = {}",
                            clientSocket.getRemoteSocketAddress(), currentConnected);

                    // Slide 37: Chuyển giao Socket cho một luồng riêng trong FixedThreadPool(50)
                    clientThreadPool.submit(new ClientHandler(clientSocket));
                } catch (IOException e) {
                    if (running) {
                        log.error("[Socket Server] Lỗi khi chấp nhận kết nối: {}", e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log.warn("[Socket Server] Cổng {} đã được sử dụng hoặc không thể mở: {}", PORT, e.getMessage());
        }
    }

    @PreDestroy
    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                log.info("[Socket Server] Đã đóng ServerSocket an toàn.");
            }
            clientThreadPool.shutdownNow();
        } catch (IOException e) {
            log.error("[Socket Server] Lỗi khi đóng server: {}", e.getMessage());
        }
    }

    /**
     * Lớp nội bộ xử lý giao tiếp với 1 client trong 1 luồng riêng biệt (Slide 37 - 39)
     */
    private class ClientHandler implements Runnable {
        private final Socket socket;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            String clientInfo = socket.getRemoteSocketAddress().toString();
            String threadName = Thread.currentThread().getName();

            // B3 (Slide 25): Lấy luồng vào-ra (InputStream, OutputStream)
            try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
            ) {
                // Kiểm tra xem client có gửi HTTP request từ trình duyệt hay không (500ms để kịp nhận request từ Firefox/Chrome)
                socket.setSoTimeout(500);
                String firstLine = null;
                try {
                    firstLine = reader.readLine();
                } catch (SocketTimeoutException ignored) {
                    // Timeout ngắn -> Đây là TCP Socket Client thông thường (đang chờ tín hiệu READY)
                }

                // Đặt timeout cho mỗi client (60 giây) để client treo không giữ luồng mãi (Tiêu chí 5.4)
                socket.setSoTimeout(CLIENT_IDLE_TIMEOUT_MS);

                // 1. Nếu là Trình duyệt Web (HTTP Request)
                if (firstLine != null && (firstLine.startsWith("GET ") || firstLine.startsWith("POST ") || firstLine.startsWith("HEAD ") || firstLine.startsWith("OPTIONS "))) {
                    // Đọc hết phần header HTTP trước khi phản hồi để trình duyệt không bị reset kết nối
                    try {
                        socket.setSoTimeout(300);
                        String hdr;
                        while ((hdr = reader.readLine()) != null && !hdr.isEmpty()) {
                            // consume HTTP headers
                        }
                    } catch (SocketTimeoutException ignored) {}
                    serveHttpDashboard(socket.getOutputStream(), threadName);
                    return;
                }

                // 2. Nếu là TCP Socket Client (MiniMartClient, MiniMartGuiClient)
                writer.println("=== CHÀO MỪNG ĐẾN VỚI HỆ THỐNG MINIMART SOCKET CONSOLE ===");
                writer.println("Đang phục vụ trên luồng: " + threadName + " | connectedClientsCount: " + connectedClientsCount.get());
                writer.println("Gõ 'HELP' để xem danh sách các lệnh (Đọc & Ghi CSDL SQL Server).");
                writer.println("READY");

                if (firstLine != null && !firstLine.trim().isEmpty()) {
                    handleCommand(firstLine.replace("\uFEFF", "").trim(), writer, threadName);
                    writer.println("END_OF_RESPONSE");
                }

                String line;
                // B4 (Slide 25): Trao đổi dữ liệu với client theo vòng lặp
                while ((line = reader.readLine()) != null) {
                    String command = line.replace("\uFEFF", "").trim();
                    if (command.isEmpty()) {
                        continue;
                    }
                    // Nếu trình duyệt mở kết nối chờ sẵn (speculative pre-connect) rồi mới gửi HTTP GET sau >500ms
                    if (command.matches("^(GET|POST|HEAD|OPTIONS)\\s+.*\\s+HTTP/1\\.[01]$")) {
                        break;
                    }
                    log.info("[{}] [Luồng: {}] Nhận lệnh: '{}'", clientInfo, threadName, command);

                    if (command.equalsIgnoreCase("QUIT") || command.equalsIgnoreCase("EXIT")) {
                        writer.println("Tạm biệt! Đóng kết nối phiên làm việc.");
                        break;
                    }

                    handleCommand(command, writer, threadName);
                    writer.println("END_OF_RESPONSE");
                }
            } catch (SocketTimeoutException e) {
                log.warn("[{}] [Luồng: {}] Client không phản hồi sau {}ms -> Tự động ngắt kết nối giải phóng luồng.",
                        clientInfo, threadName, CLIENT_IDLE_TIMEOUT_MS);
            } catch (IOException e) {
                log.warn("[{}] Mất kết nối hoặc client ngắt đột ngột: {}", clientInfo, e.getMessage());
            } finally {
                // B5 (Slide 25): Đóng Socket kết nối với client
                try {
                    if (!socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException ignored) {}
                int remaining = connectedClientsCount.decrementAndGet();
                log.info("[Socket Server] Đã đóng kết nối với {} | connectedClientsCount còn lại: {}", clientInfo, remaining);
            }
        }

        private void serveHttpDashboard(OutputStream out, String threadName) throws IOException {
            int activeCount = connectedClientsCount.get();
            int totalProducts = 0;
            try {
                totalProducts = (int) productRepository.count();
            } catch (Exception ignored) {}

            String html = "<!DOCTYPE html>\n"
                    + "<html lang=\"vi\">\n"
                    + "<head>\n"
                    + "    <meta charset=\"UTF-8\">\n"
                    + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                    + "    <title>MiniMart - Socket Server (Cổng 9090)</title>\n"
                    + "    <link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css\" rel=\"stylesheet\">\n"
                    + "    <link href=\"https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css\" rel=\"stylesheet\">\n"
                    + "</head>\n"
                    + "<body class=\"bg-light py-5\">\n"
                    + "<div class=\"container\" style=\"max-width: 820px;\">\n"
                    + "    <div class=\"card shadow-sm border-0 p-4 p-md-5\">\n"
                    + "        <h3 class=\"fw-bold mb-2 text-success\"><i class=\"bi bi-hdd-network-fill me-2\"></i>MiniMart Socket Server (Cổng 9090)</h3>\n"
                    + "        <p class=\"text-muted\">FixedThreadPool(50) | Timeout: 60s | Luồng hiện tại: <code>" + threadName + "</code></p>\n"
                    + "        <div class=\"row g-3 mb-4\">\n"
                    + "            <div class=\"col-sm-4\"><div class=\"p-3 bg-light rounded text-center border\"><small class=\"text-muted d-block\">Cổng TCP</small><strong class=\"fs-5\">9090</strong></div></div>\n"
                    + "            <div class=\"col-sm-4\"><div class=\"p-3 bg-light rounded text-center border\"><small class=\"text-muted d-block\">connectedClientsCount</small><strong class=\"fs-5 text-primary\">" + activeCount + " Client</strong></div></div>\n"
                    + "            <div class=\"col-sm-4\"><div class=\"p-3 bg-light rounded text-center border\"><small class=\"text-muted d-block\">Sản phẩm trong DB</small><strong class=\"fs-5 text-success\">" + totalProducts + " SP</strong></div></div>\n"
                    + "        </div>\n"
                    + "        <div class=\"d-flex gap-2\">\n"
                    + "            <a href=\"http://localhost:8080/admin\" class=\"btn btn-primary\">Quản trị (React)</a>\n"
                    + "            <a href=\"http://localhost:8080/admin/queue\" class=\"btn btn-success\">Hàng đợi Đơn hàng</a>\n"
                    + "            <a href=\"http://localhost:8080/\" class=\"btn btn-outline-secondary\">Cửa hàng</a>\n"
                    + "        </div>\n"
                    + "    </div>\n"
                    + "</div>\n"
                    + "</body>\n"
                    + "</html>\n";

            byte[] bodyBytes = html.getBytes(StandardCharsets.UTF_8);
            String headers = "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: text/html; charset=UTF-8\r\n"
                    + "Content-Length: " + bodyBytes.length + "\r\n"
                    + "Cache-Control: no-store\r\n"
                    + "Connection: close\r\n"
                    + "\r\n";

            out.write(headers.getBytes(StandardCharsets.US_ASCII));
            out.write(bodyBytes);
            out.flush();
        }

        private void handleCommand(String command, PrintWriter writer, String threadName) {
            String[] parts = command.split("\\s+", 2);
            String action = parts[0].toUpperCase();
            String param = parts.length > 1 ? parts[1].trim() : "";

            switch (action) {
                case "PING" -> writer.println("PONG | Server MiniMart port 9090 ALIVE | Luồng: " + threadName + " | connectedClientsCount: " + connectedClientsCount.get());

                case "STATS" -> {
                    writer.println("--- THỐNG KÊ TRẠNG THÁI SOCKET SERVER (CỔNG 9090) ---");
                    writer.println("  connectedClientsCount : " + connectedClientsCount.get() + " client đang kết nối đồng thời");
                    writer.println("  totalConnectionsServed: " + totalConnectionsServed.get() + " lượt kết nối từ khi khởi động");
                    writer.println("  workerThreadPool      : FixedThreadPool(" + MAX_CLIENTS_POOL + ")");
                    writer.println("  socketSoTimeout       : " + (CLIENT_IDLE_TIMEOUT_MS / 1000) + " giây");
                    writer.println("  currentWorkerThread   : " + threadName);
                }

                case "HELP", "?" -> {
                    writer.println("Danh sách lệnh hỗ trợ (Đọc & Ghi trực tiếp CSDL SQL Server):");
                    writer.println("  PING                        : Kiểm tra kết nối máy chủ");
                    writer.println("  STATS                       : Xem số lượng Client đang kết nối (connectedClientsCount)");
                    writer.println("  PRODUCTS                    : [ĐỌC DB] Xem danh sách sản phẩm & tồn kho");
                    writer.println("  PRODUCT <id>                : [ĐỌC DB] Tra cứu chi tiết sản phẩm theo ID");
                    writer.println("  LOW_STOCK                   : [ĐỌC DB] Cảnh báo sản phẩm sắp hết hàng (tồn < 10)");
                    writer.println("  ORDER <id>                  : [ĐỌC DB] Tra cứu đơn hàng theo ID (VD: ORDER 1)");
                    writer.println("  ORDER <productId> <qty>     : [GHI DB] Tạo đơn hàng thật & trừ kho trong CSDL (VD: ORDER 1 2)");
                    writer.println("  RESTOCK <productId> <qty>   : [GHI DB] Cộng thêm tồn kho sản phẩm trong CSDL (VD: RESTOCK 1 5)");
                    writer.println("  QUIT / EXIT                 : Thoát phiên làm việc");
                }

                case "ECHO" -> writer.println("ECHO: " + param);

                case "PRODUCTS" -> {
                    try {
                        transactionTemplate.executeWithoutResult(status -> {
                            List<Product> products = productRepository.findAll();
                            writer.println("--- DANH SÁCH SẢN PHẨM (Tổng: " + products.size() + ") ---");
                            int limit = Math.min(products.size(), 15);
                            for (int i = 0; i < limit; i++) {
                                Product p = products.get(i);
                                int stock = p.getInventory() != null ? p.getInventory().getCurrentStock() : 0;
                                writer.println(String.format(" #%-3d | SKU: %-12s | %-25s | Giá: %-9s đ | Tồn: %d",
                                        p.getId(), p.getSku(), truncate(p.getName(), 25), p.getPrice(), stock));
                            }
                            if (products.size() > limit) {
                                writer.println("... và còn " + (products.size() - limit) + " sản phẩm khác.");
                            }
                        });
                    } catch (Exception e) {
                        writer.println("Lỗi khi đọc danh sách sản phẩm: " + e.getMessage());
                    }
                }

                case "PRODUCT" -> {
                    if (param.isEmpty()) {
                        writer.println("LỖI: Vui lòng cung cấp Product ID (ví dụ: PRODUCT 1)");
                        return;
                    }
                    try {
                        Long id = Long.valueOf(param);
                        transactionTemplate.executeWithoutResult(status -> {
                            productRepository.findById(id).ifPresentOrElse(p -> {
                                int stock = p.getInventory() != null ? p.getInventory().getCurrentStock() : 0;
                                writer.println("--- CHI TIẾT SẢN PHẨM #" + p.getId() + " ---");
                                writer.println("  Tên SP   : " + p.getName());
                                writer.println("  Mã SKU   : " + p.getSku());
                                writer.println("  Danh mục : " + (p.getCategory() != null ? p.getCategory().getName() : "Không"));
                                writer.println("  Giá bán  : " + p.getPrice() + " đ");
                                writer.println("  Tồn kho  : " + stock + " " + (p.getUnit() != null ? p.getUnit() : ""));
                                writer.println("  Trạng thái: " + (Boolean.TRUE.equals(p.getIsActive()) ? "Đang bán" : "Ngừng bán"));
                            }, () -> writer.println("Không tìm thấy sản phẩm có ID = " + id));
                        });
                    } catch (NumberFormatException e) {
                        writer.println("LỖI: ID sản phẩm phải là số nguyên!");
                    } catch (Exception e) {
                        writer.println("Lỗi tra cứu: " + e.getMessage());
                    }
                }

                case "LOW_STOCK" -> {
                    try {
                        transactionTemplate.executeWithoutResult(status -> {
                            List<Product> products = productRepository.findAll().stream()
                                    .filter(p -> p.getInventory() != null && p.getInventory().getCurrentStock() < 10)
                                    .toList();
                            writer.println("--- CẢNH BÁO TỒN KHO THẤP (< 10 ĐƠN VỊ) ---");
                            if (products.isEmpty()) {
                                writer.println("Tất cả sản phẩm đều có tồn kho an toàn.");
                            } else {
                                for (Product p : products) {
                                    int stock = p.getInventory().getCurrentStock();
                                    writer.println(String.format(" #%-3d | %-25s | Tồn: %d", p.getId(), truncate(p.getName(), 25), stock));
                                }
                            }
                        });
                    } catch (Exception e) {
                        writer.println("Lỗi khi quét tồn kho: " + e.getMessage());
                    }
                }

                case "ORDER" -> {
                    if (param.isEmpty()) {
                        writer.println("LỖI: Cú pháp: 'ORDER <orderId>' để tra cứu, hoặc 'ORDER <productId> <soLuong>' để tạo đơn hàng thật vào CSDL.");
                        return;
                    }
                    String[] orderArgs = param.split("\\s+");
                    // Nếu chỉ có 1 tham số số nguyên -> Tra cứu đơn hàng (ĐỌC CSDL)
                    if (orderArgs.length == 1) {
                        try {
                            Long id = Long.valueOf(orderArgs[0]);
                            transactionTemplate.executeWithoutResult(status -> {
                                orderRepository.findById(id).ifPresentOrElse(o -> {
                                    writer.println("--- THÔNG TIN ĐƠN HÀNG #" + o.getId() + " ---");
                                    writer.println("  Khách nhận: " + o.getShippingName() + " (" + o.getShippingPhone() + ")");
                                    writer.println("  Địa chỉ   : " + o.getShippingAddress());
                                    writer.println("  Trạng thái: " + o.getStatus());
                                    writer.println("  Thanh toán: " + o.getPaymentMethod() + " (" + (o.getPayment() != null ? o.getPayment().getPaymentStatus() : "Chưa có") + ")");
                                    writer.println("  Tổng tiền : " + o.getFinalAmount() + " đ");
                                    writer.println("  Ngày tạo  : " + o.getCreatedAt());
                                }, () -> writer.println("Không tìm thấy đơn hàng #" + id));
                            });
                        } catch (NumberFormatException e) {
                            writer.println("LỖI: ID đơn hàng phải là số nguyên!");
                        } catch (Exception e) {
                            writer.println("Lỗi tra cứu: " + e.getMessage());
                        }
                    } else {
                        // Nếu có >= 2 tham số (ORDER <productId> <qty> hoặc ORDER CREATE <productId> <qty>) -> GHI DỮ LIỆU THẬT XUỐNG CSDL!
                        int idx = orderArgs[0].equalsIgnoreCase("CREATE") ? 1 : 0;
                        if (orderArgs.length < idx + 2) {
                            writer.println("LỖI: Cú pháp tạo đơn: ORDER <productId> <soLuong> (ví dụ: ORDER 1 2)");
                            return;
                        }
                        try {
                            Long productId = Long.valueOf(orderArgs[idx]);
                            int qty = Integer.parseInt(orderArgs[idx + 1]);
                            if (qty <= 0) {
                                writer.println("LỖI: Số lượng đặt mua phải > 0");
                                return;
                            }
                            createSocketOrderInDatabase(productId, qty, writer, threadName);
                        } catch (NumberFormatException e) {
                            writer.println("LỖI: productId và số lượng phải là số nguyên (ví dụ: ORDER 1 2)!");
                        } catch (Exception e) {
                            writer.println("LỖI KHI TẠO ĐƠN HÀNG: " + e.getMessage());
                        }
                    }
                }

                case "RESTOCK" -> {
                    String[] args = param.split("\\s+");
                    if (args.length < 2) {
                        writer.println("LỖI: Cú pháp: RESTOCK <productId> <soLuongThem> (ví dụ: RESTOCK 1 5)");
                        return;
                    }
                    try {
                        Long productId = Long.valueOf(args[0]);
                        int addQty = Integer.parseInt(args[1]);
                        if (addQty <= 0) {
                            writer.println("LỖI: Số lượng nhập thêm phải > 0");
                            return;
                        }
                        transactionTemplate.executeWithoutResult(status -> {
                            Inventory inv = inventoryRepository.findByProductIdWithLock(productId)
                                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tồn kho cho SP #" + productId));
                            int before = inv.getCurrentStock() != null ? inv.getCurrentStock() : 0;
                            inv.setCurrentStock(before + addQty);
                            inventoryRepository.save(inv);
                            writer.println("--- [GHI CSDL THÀNH CÔNG] CẬP NHẬT TỒN KHO ---");
                            writer.println("  Sản phẩm      : #" + productId + " - " + inv.getProduct().getName());
                            writer.println("  Tồn kho trước : " + before);
                            writer.println("  Số lượng cộng : +" + addQty);
                            writer.println("  Tồn kho mới   : " + inv.getCurrentStock() + " (Đã lưu vào bảng inventory trong SQL Server)");
                        });
                    } catch (Exception e) {
                        writer.println("LỖI KHI CẬP NHẬT KHO: " + e.getMessage());
                    }
                }

                default -> writer.println("Lệnh không hợp lệ: '" + command + "'. Gõ 'HELP' để xem các lệnh khả dụng.");
            }
        }

        private void createSocketOrderInDatabase(Long productId, int qty, PrintWriter writer, String threadName) {
            transactionTemplate.executeWithoutResult(status -> {
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm #" + productId));
                Inventory inv = inventoryRepository.findByProductIdWithLock(productId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kho của sản phẩm #" + productId));

                int stockBefore = inv.getCurrentStock() != null ? inv.getCurrentStock() : 0;
                if (stockBefore < qty) {
                    throw new IllegalStateException("Tồn kho không đủ (Hiện còn: " + stockBefore + ", Yêu cầu: " + qty + ")");
                }

                // Trừ tồn kho thật trong SQL Server dưới khóa PESSIMISTIC_WRITE
                inv.setCurrentStock(stockBefore - qty);
                inventoryRepository.save(inv);

                User buyer = userRepository.findAll().stream().findFirst().orElse(null);
                BigDecimal unitPrice = product.getSalePrice() != null ? product.getSalePrice() : product.getPrice();
                BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(qty));

                Order order = Order.builder()
                        .user(buyer)
                        .totalAmount(total)
                        .discountAmount(BigDecimal.ZERO)
                        .finalAmount(total)
                        .status("CHO_XAC_NHAN")
                        .shippingName("Khách đặt qua Socket Client (" + threadName + ")")
                        .shippingPhone("0909009090")
                        .shippingAddress("Quầy thu ngân POS / Socket Terminal")
                        .paymentMethod("COD")
                        .note("Đơn hàng tạo trực tiếp từ chương trình Socket Client")
                        .build();
                Order savedOrder = orderRepository.save(order);

                OrderItem item = OrderItem.builder()
                        .order(savedOrder)
                        .product(product)
                        .productName(product.getName())
                        .productImage(product.getMainImage())
                        .quantity(qty)
                        .price(unitPrice)
                        .build();
                orderItemRepository.save(item);

                Payment payment = Payment.builder()
                        .order(savedOrder)
                        .paymentMethod("COD")
                        .paymentStatus("PENDING")
                        .amount(total)
                        .build();
                paymentRepository.save(payment);

                writer.println("--- [GHI CSDL THÀNH CÔNG] ĐÃ TẠO ĐƠN HÀNG MỚI TRONG SQL SERVER ---");
                writer.println("  Mã đơn vừa tạo : #" + savedOrder.getId() + " (Gõ 'ORDER " + savedOrder.getId() + "' để kiểm tra lại)");
                writer.println("  Sản phẩm mua   : #" + product.getId() + " - " + product.getName());
                writer.println("  Số lượng mua   : " + qty + " x " + unitPrice + " đ = " + total + " đ");
                writer.println("  Tồn kho SQL    : " + stockBefore + " -> " + inv.getCurrentStock() + " (Đã trừ kho an toàn)");
                writer.println("  Luồng xử lý    : " + threadName);
            });
        }

        private String truncate(String text, int max) {
            if (text == null) return "";
            return text.length() <= max ? text : text.substring(0, max - 3) + "...";
        }
    }
}
