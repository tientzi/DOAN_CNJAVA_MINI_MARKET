package com.groceryshop.client;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * ỨNG DỤNG CLIENT SOCKET ĐỘC LẬP (Console Client)
 * Tham chiếu tài liệu môn học: Chuong5_Networking_updated.pptx
 * - Quy trình 5 bước xây dựng Client Socket: Slide 28
 *
 * Cách chạy:
 *   1. Mở cửa sổ Terminal / PowerShell riêng biệt
 *   2. Chạy lệnh: java socket-client/MiniMartClient.java
 *      (Hoặc nhấp đúp file run-client.bat)
 */
public class MiniMartClient {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 9090;

    public static void main(String[] args) {
        // Đảm bảo Terminal trên Windows in đúng phông chữ tiếng Việt UTF-8
        try {
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));
        } catch (Exception ignored) {}

        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;

        System.out.println("==================================================");
        System.out.println("       MINIMART SOCKET CONSOLE CLIENT (JAVA)     ");
        System.out.println("   Tham chiếu: Slide 28 - Chuong5_Networking     ");
        System.out.println("==================================================");
        System.out.printf("Đang kết nối tới máy chủ %s:%d...\n", host, port);

        // B1 (Slide 28): Khởi tạo đối tượng Socket để kết nối đến Server
        try (Socket socket = new Socket(host, port)) {
            System.out.println("[Thành công] Đã thiết lập kết nối Socket tới Server!");

            // B2 (Slide 28): Sử dụng getInputStream() và getOutputStream() lấy luồng vào-ra
            BufferedReader serverReader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter serverWriter = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            Scanner consoleScanner = new Scanner(System.in, StandardCharsets.UTF_8);

            // Đọc thông điệp chào mừng từ Server cho tới dấu hiệu READY
            String serverMsg;
            while ((serverMsg = serverReader.readLine()) != null) {
                if ("READY".equals(serverMsg)) {
                    break;
                }
                System.out.println(serverMsg);
            }

            System.out.println("--------------------------------------------------");
            System.out.println("Nhập lệnh (PING, PRODUCTS, PRODUCT <id>, LOW_STOCK, ORDER <id>, QUIT):");

            // B4 (Slide 28): Trao đổi dữ liệu với Server theo vòng lặp
            while (true) {
                System.out.print("\nMiniMart-Client> ");
                if (!consoleScanner.hasNextLine()) {
                    break;
                }
                String userInput = consoleScanner.nextLine().trim();
                if (userInput.isEmpty()) {
                    continue;
                }

                // Gửi lệnh lên Server qua Socket
                serverWriter.println(userInput);

                if (userInput.equalsIgnoreCase("QUIT") || userInput.equalsIgnoreCase("EXIT")) {
                    String farewell = serverReader.readLine();
                    if (farewell != null) {
                        System.out.println("[Server]: " + farewell);
                    }
                    break;
                }

                // Nhận phản hồi từ Server (kết thúc bởi END_OF_RESPONSE)
                while ((serverMsg = serverReader.readLine()) != null) {
                    if ("END_OF_RESPONSE".equals(serverMsg)) {
                        break;
                    }
                    System.out.println(serverMsg);
                }
            }

            System.out.println("\nĐã đóng phiên làm việc Client an toàn.");
            // B5 (Slide 28): Đóng Socket tự động bởi try-with-resources
        } catch (java.net.ConnectException e) {
            System.err.println("\n[LỖI KẾT NỐI] Không thể kết nối tới Server tại " + host + ":" + port);
            System.err.println("-> Vui lòng đảm bảo ứng dụng Spring Boot / Socket Server đã được khởi động trước.");
        } catch (Exception e) {
            System.err.println("\n[LỖI]: " + e.getMessage());
        }
    }
}