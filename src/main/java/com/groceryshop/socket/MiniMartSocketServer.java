package com.groceryshop.socket;

import com.groceryshop.entity.Order;
import com.groceryshop.entity.Product;
import com.groceryshop.repository.InventoryRepository;
import com.groceryshop.repository.OrderRepository;
import com.groceryshop.repository.ProductRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.*;
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

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private final ExecutorService clientThreadPool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setName("socket-worker-" + t.getId());
        t.setDaemon(true);
        return t;
    });

    private static final AtomicInteger connectedClientsCount = new AtomicInteger(0);

    @Override
    public void run(String... args) {
        // Chạy Socket Server trong luồng ngầm (daemon thread) để không block Spring Boot startup
        Thread serverThread = new Thread(this::startServer, "socket-server-listener");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private void startServer() {
        try {
            // B1 (Slide 25): Khởi tạo ServerSocket để lắng nghe kết nối từ Client
            serverSocket = new ServerSocket(PORT);
            running = true;
            log.info("[Socket Server] Đã khởi động thành công trên cổng {} (Đa luồng, non-blocking)", PORT);

            // Vòng lặp nhận kết nối từ các client
            while (running && !serverSocket.isClosed()) {
                try {
                    // B2 (Slide 25): accept() chấp nhận kết nối từ client
                    Socket clientSocket = serverSocket.accept();
                    int currentConnected = connectedClientsCount.incrementAndGet();
                    log.info("[Socket Server] Client mới kết nối từ: {} | Tổng client đang kết nối: {}",
                            clientSocket.getRemoteSocketAddress(), currentConnected);

                    // Slide 37: Chuyển giao Socket cho một luồng riêng để xử lý đồng thời
                    clientThreadPool.submit(new ClientHandler(clientSocket));
                } catch (IOException e) {
                    if (running) {
                        log.error("[Socket Server] Lỗi khi chấp nhận kết nối: {}", e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log.error("[Socket Server] Không thể mở cổng {}: {}", PORT, e.getMessage());
        }
    }

    @PreDestroy
    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                // B6 (Slide 25): Đóng ServerSocket khi ứng dụng dừng
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
                // Kiểm tra xem client có gửi request ngay (như Web Browser truy cập http://localhost:9090) hay không
                socket.setSoTimeout(150);
                String firstLine = null;
                try {
                    firstLine = reader.readLine();
                } catch (SocketTimeoutException ignored) {
                    // Timeout -> Đây là Socket Client thông thường đang chờ server chào mừng
                }
                socket.setSoTimeout(0); // Bỏ timeout cho phiên làm việc bình thường

                // 1. Nếu là Trình duyệt Web (HTTP Request)
                if (firstLine != null && (firstLine.startsWith("GET ") || firstLine.startsWith("POST ") || firstLine.startsWith("HEAD "))) {
                    serveHttpDashboard(writer, threadName);
                    return;
                }

                // 2. Nếu là TCP Socket Client (như MiniMartClient, MiniMartGuiClient)
                writer.println("=== CHÀO MỪNG ĐẾN VỚI HỆ THỐNG MINIMART SOCKET CONSOLE ===");
                writer.println("Đang phục vụ trên luồng: " + threadName);
                writer.println("Gõ 'HELP' để xem danh sách các lệnh được hỗ trợ.");
                writer.println("READY");

                // Nếu client đã gửi lệnh đầu tiên trước khi chào mừng
                if (firstLine != null && !firstLine.trim().isEmpty()) {
                    handleCommand(firstLine.trim(), writer);
                    writer.println("END_OF_RESPONSE");
                }

                String line;
                // B4 (Slide 25): Trao đổi dữ liệu với client theo vòng lặp
                while ((line = reader.readLine()) != null) {
                    String command = line.trim();
                    log.info("[{}] [Luồng: {}] Nhận lệnh: '{}'", clientInfo, threadName, command);

                    if (command.equalsIgnoreCase("QUIT") || command.equalsIgnoreCase("EXIT")) {
                        writer.println("Tạm biệt! Đóng kết nối phiên làm việc.");
                        break;
                    }

                    handleCommand(command, writer);
                    writer.println("END_OF_RESPONSE");
                }
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
                log.info("[Socket Server] Đã đóng kết nối với {} | Còn lại: {} client", clientInfo, remaining);
            }
        }

        private void serveHttpDashboard(PrintWriter writer, String threadName) {
            int activeCount = connectedClientsCount.get();
            int totalProducts = 0;
            try {
                totalProducts = (int) productRepository.count();
            } catch (Exception ignored) {}

            writer.print("HTTP/1.1 200 OK\r\n");
            writer.print("Content-Type: text/html; charset=UTF-8\r\n");
            writer.print("Connection: close\r\n");
            writer.print("\r\n");

            writer.println("<!DOCTYPE html>");
            writer.println("<html lang=\"vi\">");
            writer.println("<head>");
            writer.println("    <meta charset=\"UTF-8\">");
            writer.println("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
            writer.println("    <title>MiniMart - Socket Server (Cổng 9090)</title>");
            writer.println("    <link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css\" rel=\"stylesheet\">");
            writer.println("    <link href=\"https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css\" rel=\"stylesheet\">");
            writer.println("    <style>");
            writer.println("        body { background: #f8fafc; font-family: 'Segoe UI', system-ui, sans-serif; }");
            writer.println("        .hero-card { border-radius: 16px; border: 1px solid #e2e8f0; box-shadow: 0 10px 25px rgba(0,0,0,0.05); }");
            writer.println("        .badge-orange { background: #ff5722; color: #fff; }");
            writer.println("        .btn-orange { background: #ff5722; color: #fff; border: none; font-weight: 600; }");
            writer.println("        .btn-orange:hover { background: #e64a19; color: #fff; }");
            writer.println("        .code-box { background: #1e293b; color: #f8fafc; border-radius: 10px; padding: 15px; font-family: monospace; }");
            writer.println("    </style>");
            writer.println("</head>");
            writer.println("<body class=\"py-5\">");
            writer.println("<div class=\"container\" style=\"max-width: 800px;\">");
            writer.println("    <div class=\"card hero-card bg-white p-4 p-md-5 mb-4\">");
            writer.println("        <div class=\"d-flex align-items-center gap-3 mb-4\">");
            writer.println("            <div class=\"rounded-circle d-flex align-items-center justify-content-center text-white\" style=\"width: 56px; height: 56px; background: linear-gradient(135deg, #ff5722, #f97316);\">");
            writer.println("                <i class=\"bi bi-hdd-network-fill fs-3\"></i>");
            writer.println("            </div>");
            writer.println("            <div>");
            writer.println("                <h3 class=\"fw-bold mb-1 text-dark\">MiniMart Socket Server (Đa luồng)</h3>");
            writer.println("                <span class=\"badge bg-success-subtle text-success border border-success-subtle\"><i class=\"bi bi-check-circle-fill me-1\"></i> Cổng 9090 Đang Hoạt Động (Non-blocking)</span>");
            writer.println("                <span class=\"badge bg-primary-subtle text-primary border border-primary-subtle ms-1\">Luồng: " + threadName + "</span>");
            writer.println("            </div>");
            writer.println("        </div>");
            writer.println("        <p class=\"text-secondary\">Bạn đang kết nối trực tiếp vào <strong>Cổng TCP Socket 9090</strong> của ứng dụng MiniMart (Được xây dựng theo chuẩn <strong>Chương 5 - Networking</strong>, Slide 25 & 37).</p>");
            writer.println("        <div class=\"row g-3 mb-4\">");
            writer.println("            <div class=\"col-sm-4\"><div class=\"p-3 bg-light rounded text-center\"><small class=\"text-muted d-block\">Cổng Socket</small><strong class=\"fs-5 text-dark\">9090</strong></div></div>");
            writer.println("            <div class=\"col-sm-4\"><div class=\"p-3 bg-light rounded text-center\"><small class=\"text-muted d-block\">Client đang nối</small><strong class=\"fs-5 text-primary\">" + activeCount + " Client</strong></div></div>");
            writer.println("            <div class=\"col-sm-4\"><div class=\"p-3 bg-light rounded text-center\"><small class=\"text-muted d-block\">Tổng SP trong CSDL</small><strong class=\"fs-5 text-success\">" + totalProducts + " Mặt hàng</strong></div></div>");
            writer.println("        </div>");
            writer.println("        <h6 class=\"fw-bold text-dark mb-2\"><i class=\"bi bi-terminal me-2 text-warning\"></i>Cách kết nối với Client chuyên dụng:</h6>");
            writer.println("        <div class=\"code-box mb-4\">");
            writer.println("            <div class=\"text-muted mb-1\"># Cách 1: Chạy Console Client (Terminal UTF-8)</div>");
            writer.println("            <div class=\"text-warning mb-2\">java socket-client/MiniMartClient.java</div>");
            writer.println("            <div class=\"text-muted mb-1\"># Cách 2: Chạy Client Đồ Họa Swing (Giao diện cửa sổ)</div>");
            writer.println("            <div class=\"text-warning\">java socket-client/MiniMartGuiClient.java</div>");
            writer.println("        </div>");
            writer.println("        <h6 class=\"fw-bold text-dark mb-2\"><i class=\"bi bi-chat-left-dots me-2 text-info\"></i>Các lệnh văn bản hỗ trợ qua Socket:</h6>");
            writer.println("        <ul class=\"list-group list-group-flush small mb-4\">");
            writer.println("            <li class=\"list-group-item px-0\"><code>PING</code>: Kiểm tra trạng thái máy chủ (Trả về <code>PONG</code>)</li>");
            writer.println("            <li class=\"list-group-item px-0\"><code>PRODUCTS</code>: Liệt kê danh sách sản phẩm lấy trực tiếp từ CSDL SQL Server</li>");
            writer.println("            <li class=\"list-group-item px-0\"><code>PRODUCT &lt;id&gt;</code>: Xem chi tiết sản phẩm theo ID (ví dụ: <code>PRODUCT 1</code>)</li>");
            writer.println("            <li class=\"list-group-item px-0\"><code>LOW_STOCK</code>: Cảnh báo danh sách các mặt hàng tồn kho dưới 10 đơn vị</li>");
            writer.println("            <li class=\"list-group-item px-0\"><code>ORDER &lt;id&gt;</code>: Tra cứu trạng thái đơn hàng (ví dụ: <code>ORDER 1</code>)</li>");
            writer.println("            <li class=\"list-group-item px-0\"><code>QUIT</code>: Đóng kết nối an toàn với máy chủ</li>");
            writer.println("        </ul>");
            writer.println("        <div class=\"d-flex gap-2\">");
            writer.println("            <a href=\"http://localhost:8080/mvc\" class=\"btn btn-orange\"><i class=\"bi bi-speedometer2 me-1\"></i> Mở Quản trị Thymeleaf MVC</a>");
            writer.println("            <a href=\"http://localhost:8080/\" class=\"btn btn-outline-secondary\"><i class=\"bi bi-shop me-1\"></i> Mở Cửa hàng React SPA</a>");
            writer.println("        </div>");
            writer.println("    </div>");
            writer.println("</div>");
            writer.println("</body>");
            writer.println("</html>");
            writer.flush();
        }

        private void handleCommand(String command, PrintWriter writer) {
            String[] parts = command.split("\\s+", 2);
            String action = parts[0].toUpperCase();
            String param = parts.length > 1 ? parts[1].trim() : "";

            switch (action) {
                case "PING" -> writer.println("PONG (Server MiniMart port 9090 is ALIVE)");

                case "HELP", "?" -> {
                    writer.println("Danh sách lệnh hỗ trợ:");
                    writer.println("  PING                 : Kiểm tra trạng thái máy chủ");
                    writer.println("  PRODUCTS             : Xem danh sách tóm tắt các sản phẩm");
                    writer.println("  PRODUCT <id>         : Tra cứu chi tiết sản phẩm theo mã ID");
                    writer.println("  LOW_STOCK            : Xem danh sách sản phẩm sắp hết hàng (tồn < 10)");
                    writer.println("  ORDER <id>           : Tra cứu trạng thái đơn hàng theo ID");
                    writer.println("  ECHO <text>          : Lặp lại nội dung đã gửi (Test echo)");
                    writer.println("  QUIT / EXIT          : Thoát phiên làm việc");
                }

                case "ECHO" -> writer.println("ECHO: " + param);

                case "PRODUCTS" -> {
                    try {
                        List<Product> products = productRepository.findAll();
                        writer.println("--- DANH SÁCH SẢN PHẨM (Tổng: " + products.size() + ") ---");
                        int limit = Math.min(products.size(), 15);
                        for (int i = 0; i < limit; i++) {
                            Product p = products.get(i);
                            int stock = p.getInventory() != null ? p.getInventory().getCurrentStock() : 0;
                            writer.println(String.format(" #%-3d | SKU: %-10s | %-25s | Giá: %-8s đ | Tồn: %d",
                                    p.getId(), p.getSku(), truncate(p.getName(), 25), p.getPrice(), stock));
                        }
                        if (products.size() > limit) {
                            writer.println("... và còn " + (products.size() - limit) + " sản phẩm khác.");
                        }
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
                        productRepository.findById(id).ifPresentOrElse(p -> {
                            int stock = p.getInventory() != null ? p.getInventory().getCurrentStock() : 0;
                            writer.println("--- CHI TIẾT SẢN PHẨM #" + p.getId() + " ---");
                            writer.println("  Tên SP   : " + p.getName());
                            writer.println("  Mã SKU   : " + p.getSku());
                            writer.println("  Danh mục : " + (p.getCategory() != null ? p.getCategory().getName() : "Không"));
                            writer.println("  Giá niêm yết: " + p.getPrice() + " đ");
                            writer.println("  Giá khuyến mãi: " + (p.getSalePrice() != null ? p.getSalePrice() + " đ" : "Không có"));
                            writer.println("  Tồn kho  : " + stock + " " + (p.getUnit() != null ? p.getUnit() : ""));
                            writer.println("  Trạng thái: " + (Boolean.TRUE.equals(p.getIsActive()) ? "Đang bán" : "Ngừng bán"));
                        }, () -> writer.println("Không tìm thấy sản phẩm có ID = " + id));
                    } catch (NumberFormatException e) {
                        writer.println("LỖI: ID sản phẩm phải là số nguyên!");
                    } catch (Exception e) {
                        writer.println("Lỗi tra cứu: " + e.getMessage());
                    }
                }

                case "LOW_STOCK" -> {
                    try {
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
                    } catch (Exception e) {
                        writer.println("Lỗi khi quét tồn kho: " + e.getMessage());
                    }
                }

                case "ORDER" -> {
                    if (param.isEmpty()) {
                        writer.println("LỖI: Vui lòng cung cấp Order ID (ví dụ: ORDER 1)");
                        return;
                    }
                    try {
                        Long id = Long.valueOf(param);
                        orderRepository.findById(id).ifPresentOrElse(o -> {
                            writer.println("--- THÔNG TIN ĐƠN HÀNG #" + o.getId() + " ---");
                            writer.println("  Khách nhận: " + o.getShippingName() + " (" + o.getShippingPhone() + ")");
                            writer.println("  Địa chỉ   : " + o.getShippingAddress());
                            writer.println("  Trạng thái: " + o.getStatus());
                            writer.println("  Thanh toán: " + o.getPaymentMethod() + " (" + (o.getPayment() != null ? o.getPayment().getPaymentStatus() : "Chưa có") + ")");
                            writer.println("  Tổng tiền : " + o.getFinalAmount() + " đ");
                            writer.println("  Ngày tạo  : " + o.getCreatedAt());
                        }, () -> writer.println("Không tìm thấy đơn hàng #" + id));
                    } catch (NumberFormatException e) {
                        writer.println("LỖI: ID đơn hàng phải là số nguyên!");
                    } catch (Exception e) {
                        writer.println("Lỗi tra cứu: " + e.getMessage());
                    }
                }

                default -> writer.println("Lệnh không hợp lệ: '" + command + "'. Gõ 'HELP' để xem các lệnh khả dụng.");
            }
        }

        private String truncate(String text, int max) {
            if (text == null) return "";
            return text.length() <= max ? text : text.substring(0, max - 3) + "...";
        }
    }
}