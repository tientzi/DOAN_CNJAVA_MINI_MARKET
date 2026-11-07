package com.groceryshop.client;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * ỨNG DỤNG CLIENT HTTP (Dùng HttpURLConnection)
 * Tham chiếu tài liệu môn học: Chuong5_Networking_updated.pptx
 * - Demo 3: Using URLConnection (Slide 18, 19, 20)
 *
 * Cách chạy:
 *   java socket-client/MiniMartHttpClient.java
 */
public class MiniMartHttpClient {

    private static final String DEFAULT_URL = "http://localhost:8080/api/public/products";

    public static void main(String[] args) {
        try {
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));
        } catch (Exception ignored) {}

        String targetUrl = args.length > 0 ? args[0] : DEFAULT_URL;

        System.out.println("==================================================");
        System.out.println("       MINIMART HTTP CLIENT (URLConnection)       ");
        System.out.println("   Tham chiếu: Slide 18-20 - Chuong5_Networking  ");
        System.out.println("==================================================");
        System.out.println("Đang gửi yêu cầu GET tới: " + targetUrl);

        try {
            // Slide 18-20: Tạo URL và mở kết nối HttpURLConnection
            URL url = URI.create(targetUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            System.out.println("Mã phản hồi HTTP: " + responseCode + " (" + conn.getResponseMessage() + ")");

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line).append("\n");
                    }

                    System.out.println("\n--- DỮ LIỆU NHẬN ĐƯỢC TỪ SERVER ---");
                    // In xem trước tối đa 500 ký tự JSON
                    String preview = response.toString();
                    if (preview.length() > 500) {
                        System.out.println(preview.substring(0, 500) + "\n... [Đã cắt bớt để dễ quan sát]");
                    } else {
                        System.out.println(preview);
                    }
                    System.out.println("\n[Thành công] Đã kết nối và nhận dữ liệu qua HttpURLConnection!");
                }
            } else {
                System.err.println("[Thất bại] Server trả về mã lỗi HTTP: " + responseCode);
            }

            conn.disconnect();
        } catch (java.net.ConnectException e) {
            System.err.println("\n[LỖI KẾT NỐI] Không thể kết nối tới HTTP Server tại: " + targetUrl);
            System.err.println("-> Vui lòng đảm bảo Spring Boot đang chạy trên cổng 8080.");
        } catch (Exception e) {
            System.err.println("\n[LỖI]: " + e.getMessage());
        }
    }
}
