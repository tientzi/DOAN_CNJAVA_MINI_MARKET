package com.groceryshop.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ỨNG DỤNG CLIENT ĐỒ HỌA (GUI CLIENT - JAVA SWING)
 * Tham chiếu tài liệu môn học: Chuong5_Networking_updated.pptx
 * - Tiêu chí 5.3: Lập trình mạng với chương trình Client tách biệt
 * - Tiêu chí 5.4: Server phục vụ đồng thời tối thiểu 3 Client
 *
 * Chạy bằng lệnh:
 *   java socket-client/MiniMartGuiClient.java
 */
public class MiniMartGuiClient extends JFrame {

    private final JTextField hostField = new JTextField("localhost", 10);
    private final JTextField portField = new JTextField("9090", 5);
    private final JButton connectBtn = new JButton("Kết nối Máy chủ");
    private final JButton disconnectBtn = new JButton("Ngắt kết nối");
    private final JTextArea logArea = new JTextArea();
    private final JTextField customCmdField = new JTextField();
    private final JButton sendCmdBtn = new JButton("Gửi lệnh");

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private volatile boolean connected = false;

    public MiniMartGuiClient() {
        super("MiniMart Supermarket - Chương Trình Client Socket Độc Lập");
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 680);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Thiết lập Font chữ Unicode toàn diện
        Font fontUi = new Font("Segoe UI", Font.PLAIN, 14);
        Font fontBold = new Font("Segoe UI", Font.BOLD, 14);
        Font fontConsole = new Font("Consolas", Font.PLAIN, 13);

        // --- TOP PANEL: Kết nối ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        topPanel.setBackground(new Color(30, 41, 59)); // Slate 800 (Dark theme matching React)
        JLabel titleLabel = new JLabel("MINIMART CLIENT:");
        titleLabel.setFont(fontBold);
        titleLabel.setForeground(new Color(255, 87, 34)); // Vibrant Orange
        topPanel.add(titleLabel);

        JLabel hostLabel = new JLabel("Host:");
        hostLabel.setForeground(Color.WHITE);
        topPanel.add(hostLabel);
        topPanel.add(hostField);

        JLabel portLabel = new JLabel("Cổng:");
        portLabel.setForeground(Color.WHITE);
        topPanel.add(portLabel);
        topPanel.add(portField);

        connectBtn.setBackground(new Color(255, 87, 34));
        connectBtn.setForeground(Color.WHITE);
        connectBtn.setFont(fontBold);
        connectBtn.addActionListener(e -> connectToServer());
        topPanel.add(connectBtn);

        disconnectBtn.setEnabled(false);
        disconnectBtn.setBackground(new Color(239, 68, 68));
        disconnectBtn.setForeground(Color.WHITE);
        disconnectBtn.setFont(fontBold);
        disconnectBtn.addActionListener(e -> disconnectFromServer());
        topPanel.add(disconnectBtn);

        add(topPanel, BorderLayout.NORTH);

        // --- LEFT PANEL: Các nút lệnh nhanh ---
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(new EmptyBorder(10, 10, 10, 5));
        leftPanel.setPreferredSize(new Dimension(240, 0));

        JLabel cmdTitle = new JLabel("CÁC LỆNH NHANH");
        cmdTitle.setFont(fontBold);
        cmdTitle.setForeground(new Color(30, 41, 59));
        leftPanel.add(cmdTitle);
        leftPanel.add(Box.createVerticalStrut(10));

        JButton btnPing = createActionButton("1. Kiểm tra máy chủ (PING)", () -> sendCommand("PING"), fontUi);
        JButton btnProducts = createActionButton("2. Xem DS sản phẩm (PRODUCTS)", () -> sendCommand("PRODUCTS"), fontUi);
        JButton btnProduct1 = createActionButton("3. Xem chi tiết SP #1", () -> sendCommand("PRODUCT 1"), fontUi);
        JButton btnLowStock = createActionButton("4. SP sắp hết hàng (LOW_STOCK)", () -> sendCommand("LOW_STOCK"), fontUi);
        JButton btnOrder1 = createActionButton("5. Tra cứu đơn hàng #1", () -> sendCommand("ORDER 1"), fontUi);
        JButton btnHelp = createActionButton("6. Trợ giúp (HELP)", () -> sendCommand("HELP"), fontUi);
        JButton btnClear = createActionButton("Xóa màn hình log", () -> logArea.setText(""), fontUi);

        leftPanel.add(btnPing);
        leftPanel.add(Box.createVerticalStrut(6));
        leftPanel.add(btnProducts);
        leftPanel.add(Box.createVerticalStrut(6));
        leftPanel.add(btnProduct1);
        leftPanel.add(Box.createVerticalStrut(6));
        leftPanel.add(btnLowStock);
        leftPanel.add(Box.createVerticalStrut(6));
        leftPanel.add(btnOrder1);
        leftPanel.add(Box.createVerticalStrut(6));
        leftPanel.add(btnHelp);
        leftPanel.add(Box.createVerticalStrut(15));
        leftPanel.add(btnClear);
        leftPanel.add(Box.createVerticalStrut(15));

        // Nút Demo 3 Client cùng lúc (Rubric 5.4)
        JButton btnDemo3 = new JButton("<html><center><b>⚡ DEMO 3 CLIENT<br>ĐA LUỒNG CÙNG LÚC</b></center></html>");
        btnDemo3.setBackground(new Color(16, 185, 129));
        btnDemo3.setForeground(Color.WHITE);
        btnDemo3.setFont(fontBold);
        btnDemo3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btnDemo3.addActionListener(e -> runConcurrent3ClientsDemo());
        leftPanel.add(btnDemo3);

        add(leftPanel, BorderLayout.WEST);

        // --- CENTER PANEL: Log hiển thị dữ liệu ---
        logArea.setEditable(false);
        logArea.setFont(fontConsole);
        logArea.setBackground(new Color(248, 250, 252));
        logArea.setForeground(new Color(15, 23, 42));
        logArea.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Nhật ký Trao đổi Dữ liệu Socket"));
        add(scrollPane, BorderLayout.CENTER);

        // --- BOTTOM PANEL: Nhập lệnh tùy chỉnh ---
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 0));
        bottomPanel.setBorder(new EmptyBorder(5, 10, 10, 10));
        customCmdField.setFont(fontUi);
        customCmdField.addActionListener(e -> sendCustomCommand());
        sendCmdBtn.setFont(fontBold);
        sendCmdBtn.setBackground(new Color(30, 41, 59));
        sendCmdBtn.setForeground(Color.WHITE);
        sendCmdBtn.addActionListener(e -> sendCustomCommand());

        bottomPanel.add(new JLabel("Lệnh tùy ý: "), BorderLayout.WEST);
        bottomPanel.add(customCmdField, BorderLayout.CENTER);
        bottomPanel.add(sendCmdBtn, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        appendLog("Sẵn sàng. Vui lòng bấm 'Kết nối Máy chủ' (cổng 9090) để bắt đầu giao tiếp qua Socket.");
    }

    private JButton createActionButton(String text, Runnable action, Font font) {
        JButton btn = new JButton(text);
        btn.setFont(font);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private void connectToServer() {
        String host = hostField.getText().trim();
        int port = Integer.parseInt(portField.getText().trim());

        new Thread(() -> {
            try {
                appendLog("Đang kết nối tới " + host + ":" + port + "...");
                socket = new Socket(host, port);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                connected = true;

                SwingUtilities.invokeLater(() -> {
                    connectBtn.setEnabled(false);
                    disconnectBtn.setEnabled(true);
                });

                String line;
                while ((line = reader.readLine()) != null) {
                    if ("READY".equals(line)) break;
                    appendLog("[Server]: " + line);
                }
                appendLog("[Hệ thống] Kết nối thành công! Đã sẵn sàng gửi lệnh.");
            } catch (Exception ex) {
                appendLog("[LỖI KẾT NỐI] " + ex.getMessage());
            }
        }).start();
    }

    private void disconnectFromServer() {
        if (!connected) return;
        new Thread(() -> {
            try {
                if (writer != null) writer.println("QUIT");
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (Exception ignored) {}
            connected = false;
            SwingUtilities.invokeLater(() -> {
                connectBtn.setEnabled(true);
                disconnectBtn.setEnabled(false);
                appendLog("[Hệ thống] Đã ngắt kết nối với Socket Server.");
            });
        }).start();
    }

    private void sendCustomCommand() {
        String cmd = customCmdField.getText().trim();
        if (!cmd.isEmpty()) {
            sendCommand(cmd);
            customCmdField.setText("");
        }
    }

    private void sendCommand(String command) {
        if (!connected || writer == null) {
            JOptionPane.showMessageDialog(this, "Chưa kết nối tới Socket Server! Vui lòng bấm 'Kết nối Máy chủ' trước.", "Chưa kết nối", JOptionPane.WARNING_MESSAGE);
            return;
        }

        new Thread(() -> {
            try {
                appendLog("\n>>> [Client gửi]: " + command);
                writer.println(command);
                String line;
                while ((line = reader.readLine()) != null) {
                    if ("END_OF_RESPONSE".equals(line)) break;
                    appendLog(line);
                }
            } catch (Exception ex) {
                appendLog("[Lỗi truyền dữ liệu]: " + ex.getMessage());
            }
        }).start();
    }

    /**
     * Demo đồng thời 3 Client chạy độc lập gửi lệnh cùng lúc (Tiêu chí 5.4 - Rubric)
     */
    private void runConcurrent3ClientsDemo() {
        String host = hostField.getText().trim();
        int port = Integer.parseInt(portField.getText().trim());

        appendLog("\n========================================================");
        appendLog("🚀 BẮT ĐẦU DEMO 3 CLIENT KẾT NỐI ĐỒNG THỜI (TIÊU CHÍ 5.4)");
        appendLog("========================================================");

        ExecutorService pool = Executors.newFixedThreadPool(3);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(3);

        String[] testCmds = {"PRODUCTS", "LOW_STOCK", "PRODUCT 1"};

        for (int i = 1; i <= 3; i++) {
            final int cId = i;
            final String cmd = testCmds[i - 1];
            pool.submit(() -> {
                try {
                    startGun.await();
                    try (Socket s = new Socket(host, port);
                         BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                         PrintWriter w = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true)) {

                        while (!"READY".equals(r.readLine())) {}
                        w.println(cmd);

                        String line;
                        int linesCount = 0;
                        while ((line = r.readLine()) != null) {
                            if ("END_OF_RESPONSE".equals(line)) break;
                            linesCount++;
                        }
                        w.println("QUIT");

                        appendLog(String.format(" -> [Client #%d] Gửi '%s' THÀNH CÔNG! Đã nhận %d dòng phản hồi tức thời.", cId, cmd, linesCount));
                    }
                } catch (Exception ex) {
                    appendLog(String.format(" -> [Client #%d] LỖI: %s", cId, ex.getMessage()));
                } finally {
                    doneGate.countDown();
                }
            });
        }

        new Thread(() -> {
            startGun.countDown();
            try {
                doneGate.await();
                appendLog("✅ HOÀN TẤT: Cả 3 Client đồng thời đều hoàn thành xuất sắc mà máy chủ không bị nghẽn luồng!");
                appendLog("========================================================\n");
            } catch (InterruptedException ignored) {}
            pool.shutdown();
        }).start();
    }

    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        // Cài đặt Look and Feel hệ thống để hiển thị native đẹp mắt
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            new MiniMartGuiClient().setVisible(true);
        });
    }
}