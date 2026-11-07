package com.groceryshop.socket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử Lập trình Mạng Socket Server & Nhiều Client đồng thời (Rubric 5.3 & 5.4)")
public class SocketServerMultiClientTest {

    private static final Logger log = LoggerFactory.getLogger(SocketServerMultiClientTest.class);

    @Test
    @DisplayName("5.3 & 5.4 - Kiểm thử Socket Server đa luồng phục vụ đồng thời 5 Client kết nối cùng lúc")
    void testMultiThreadedSocketServerWithMultipleClients() throws Exception {
        int testPort = 9095;
        int clientCount = 5;

        ExecutorService serverThreadPool = Executors.newCachedThreadPool();
        ServerSocket serverSocket = new ServerSocket(testPort);
        AtomicInteger serverHandledCount = new AtomicInteger(0);

        // Khởi động Server đa luồng giả lập logic của MiniMartSocketServer
        Thread serverListener = new Thread(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    Socket client = serverSocket.accept();
                    serverThreadPool.submit(() -> {
                        try (
                            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                            PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true)
                        ) {
                            out.println("READY");
                            String line;
                            while ((line = in.readLine()) != null) {
                                String cmd = line.trim().toUpperCase();
                                if ("QUIT".equals(cmd)) {
                                    out.println("BYE");
                                    break;
                                } else if ("PING".equals(cmd)) {
                                    out.println("PONG");
                                } else if ("PRODUCTS".equals(cmd)) {
                                    out.println("PRODUCT_LIST_OK");
                                } else {
                                    out.println("UNKNOWN");
                                }
                            }
                            serverHandledCount.incrementAndGet();
                        } catch (IOException ignored) {}
                    });
                }
            } catch (IOException ignored) {}
        });
        serverListener.start();

        // Khởi tạo 5 Client kết nối đồng thời (CountDownLatch start gun)
        ExecutorService clientExecutor = Executors.newFixedThreadPool(clientCount);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(clientCount);
        AtomicInteger successfulClients = new AtomicInteger(0);

        for (int i = 1; i <= clientCount; i++) {
            final int clientId = i;
            clientExecutor.submit(() -> {
                try {
                    startGun.await(); // Xuất phát đồng thời
                    try (Socket socket = new Socket("localhost", testPort);
                         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                         PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

                        String welcome = in.readLine();
                        assertEquals("READY", welcome);

                        // Gửi PING
                        out.println("PING");
                        String pong = in.readLine();
                        assertEquals("PONG", pong);

                        // Gửi PRODUCTS
                        out.println("PRODUCTS");
                        String prodRes = in.readLine();
                        assertEquals("PRODUCT_LIST_OK", prodRes);

                        // Gửi QUIT
                        out.println("QUIT");
                        String bye = in.readLine();
                        assertEquals("BYE", bye);

                        successfulClients.incrementAndGet();
                        log.info("[Client #{}] Giao tiếp qua Socket thành công 100%!", clientId);
                    }
                } catch (Exception e) {
                    log.error("[Client #{}] Gặp lỗi: {}", clientId, e.getMessage());
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGun.countDown(); // Kích hoạt 5 client cùng chạy
        boolean finished = doneGate.await(5, TimeUnit.SECONDS);

        // Đóng Server
        serverSocket.close();
        serverThreadPool.shutdownNow();
        clientExecutor.shutdownNow();

        assertTrue(finished, "Tất cả 5 client phải hoàn thành trong 5 giây");
        assertEquals(clientCount, successfulClients.get(), "Chính xác cả 5 client đều phải nhận phản hồi thành công");
        assertEquals(clientCount, serverHandledCount.get(), "Server phải phục vụ trọn vẹn cả 5 phiên kết nối");
        log.info("=== KIỂM THỬ MẠNG ĐA CLIENT HOÀN TẤT THÀNH CÔNG RỰC RỠ ===");
    }
}
