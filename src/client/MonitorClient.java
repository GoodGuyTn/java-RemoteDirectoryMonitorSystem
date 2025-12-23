package client;

import common.Protocol;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.*;

public class MonitorClient extends JFrame {
    private JTextField txtServerIP;
    private JTextField txtPort;
    private JTextField txtClientName;
    private JButton btnConnect;
    private JTextArea txtLog;
    private JLabel lblStatus;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private FolderWatcher currentWatcher;
    private Thread watcherThread;

    private boolean isConnected = false;

    public MonitorClient() {
        setTitle("Client Giám Sát");
        setupGUI();
    }

    private void setupGUI() {
        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Tên máy:"));

        String defaultName = "Client_Unknown";
        txtClientName = new JTextField(defaultName, 15);
        topPanel.add(txtClientName);

        topPanel.add(new JLabel("Server IP:"));
        txtServerIP = new JTextField(Protocol.DEFAULT_SERVER_IP, 10);
        topPanel.add(txtServerIP);

        topPanel.add(new JLabel("Port:"));
        txtPort = new JTextField(String.valueOf(Protocol.DEFAULT_PORT), 5);
        topPanel.add(txtPort);

        btnConnect = new JButton("Kết nối");
        topPanel.add(btnConnect);

        add(topPanel, BorderLayout.NORTH);

        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(txtLog), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.setBorder(BorderFactory.createEtchedBorder());
        lblStatus = new JLabel("Trạng thái: Sẵn sàng");
        lblStatus.setForeground(Color.BLUE);
        bottomPanel.add(lblStatus);
        add(bottomPanel, BorderLayout.SOUTH);

        btnConnect.addActionListener(e -> handleConnectButton());

        setSize(800, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void handleConnectButton() {
        if (!isConnected) {
            connectToServer();
        } else {
            disconnect();
        }
    }

    private void connectToServer() {
        String ip = txtServerIP.getText().trim();
        int port;
        try {
            port = Integer.parseInt(txtPort.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Port phải là số!");
            return;
        }

        btnConnect.setEnabled(false);
        log("Đang kết nối tới " + ip + ":" + port + "...");

        // Chạy kết nối trên luồng riêng
        new Thread(() -> {
            try {
                socket = new Socket(ip, port);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Kết nối thành công
                updateConnectionState(true);

                // Gửi lời chào kèm tên máy
                String hostname = txtClientName.getText().trim();
                out.println(Protocol.CMD_HELLO + Protocol.SEPARATOR + hostname);

                listenServer();

            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    log("Lỗi kết nối: " + e.getMessage());
                    btnConnect.setEnabled(true);
                });
            }
        }).start();
    }

    private void listenServer() {
        try {
            String msg;
            while ((msg = in.readLine()) != null) {
                processCommand(msg);
            }
        } catch (IOException e) {
            log("Mất kết nối với Server.");
        } finally {
            disconnect();
        }
    }

    private void processCommand(String msg) {
        if (msg.startsWith(Protocol.CMD_MONITOR_REQ)) {
            String[] parts = msg.split(Protocol.SEPARATOR);
            if (parts.length > 1) {
                String path = parts[1];
                startWatcher(path);
            }
        }
        else if (msg.startsWith(Protocol.CMD_STOP_REQ)) {
            stopWatcher();
            setStatus("Đã dừng giám sát (Chờ lệnh mới...)");
            log("Server yêu cầu dừng giám sát.");
        }
    }

    private void startWatcher(String pathStr) {
        Path path = Paths.get(pathStr);

        // Kiểm tra tồn tại
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            log("Yêu cầu giám sát thất bại: " + pathStr + " (Không tồn tại)");
            out.println(Protocol.CMD_MONITOR_RES + Protocol.SEPARATOR + "FAIL" + Protocol.SEPARATOR + "Thu muc khong ton tai");
            return;
        }

        stopWatcher();

        // Chạy cái mới
        currentWatcher = new FolderWatcher(pathStr, this);
        watcherThread = new Thread(currentWatcher);
        watcherThread.start();

        out.println(Protocol.CMD_MONITOR_RES + Protocol.SEPARATOR + "OK");

        setStatus("Đang giám sát: " + pathStr);
        log("Bắt đầu giám sát: " + pathStr);
    }

    private void stopWatcher() {
        if (currentWatcher != null) {
            currentWatcher.stopWatching();
            currentWatcher = null;
        }
        if (watcherThread != null) {
            watcherThread.interrupt();
            watcherThread = null;
        }
    }

    public synchronized void sendNotify(String action, String filename) {
        if (out != null) {
            String msg = Protocol.CMD_NOTIFY + Protocol.SEPARATOR + action + Protocol.SEPARATOR + filename;
            out.println(msg);
            log("Phát hiện: " + action + " -> " + filename);
        }
    }

    private void disconnect() {
        stopWatcher();
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        isConnected = false;
        updateConnectionState(false);
    }

    // Cập nhật giao diện khi kết nối/ngắt kết nối
    private void updateConnectionState(boolean connected) {
        isConnected = connected;
        SwingUtilities.invokeLater(() -> {
            btnConnect.setEnabled(true);
            if (connected) {
                btnConnect.setText("Ngắt kết nối");
                txtServerIP.setEditable(false);
                txtPort.setEditable(false);
                txtClientName.setEditable(false);
                lblStatus.setText("Trạng thái: Đã kết nối");
                lblStatus.setForeground(Color.GREEN);
                log(">>> Đã kết nối tới Server.");
            } else {
                btnConnect.setText("Kết nối");
                txtServerIP.setEditable(true);
                txtPort.setEditable(true);
                txtClientName.setEditable(true);
                lblStatus.setText("Trạng thái: Chưa kết nối");
                lblStatus.setForeground(Color.RED);
                log(">>> Đã ngắt kết nối.");
            }
        });
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append(msg + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    private void setStatus(String status) {
        SwingUtilities.invokeLater(() -> lblStatus.setText("Trạng thái: " + status));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MonitorClient monitorClient = new MonitorClient();
        });
    }
}