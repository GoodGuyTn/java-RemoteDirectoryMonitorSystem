package server;

import common.Protocol;
import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private MonitorServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String clientName = "Unknown";
    private String currentPath = "";

    public ClientHandler(Socket socket, MonitorServer server) {
        this.socket = socket;
        this.server = server;
    }

    public String getClientInfo() {
        return "Client: " + clientName;
    }

    public String getMonitoredPath() {
        return (currentPath == null) ? "" : currentPath;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String message;
            while ((message = in.readLine()) != null) {
                // In ra Console để debug
                System.out.println("[Client " + clientName + "]: " + message);
                processMessage(message);
            }

        } catch (IOException e) {
            server.log(">>> Client ngắt kết nối: " + clientName);
        } finally {
            closeConnection();
        }
    }

    private void processMessage(String message) {
        String[] parts = message.split(Protocol.SEPARATOR);
        String command = parts[0];

        switch (command) {
            case Protocol.CMD_HELLO:
                if (parts.length > 1) {
                    this.clientName = parts[1];
                    server.updateClientList();
                    server.log(">>> Client mới kết nối: " + clientName);
                }
                break;

            case Protocol.CMD_NOTIFY:
                // Logic xử lý thông báo
                if (parts.length >= 3) {
                    String action = parts[1];
                    String path = parts[2];

                    server.log(">>> [" + clientName + "] " + action + " -> " + path);
                }
                else {
                    // Phòng trường hợp gửi thiếu thông tin
                    server.log(">>> Lỗi: Nhận thông báo không hợp lệ từ " + clientName);
                }
                break;

            case Protocol.CMD_MONITOR_RES:
                String result = parts[1];
                if ("OK".equals(result)) {
                    server.log(">>> [" + clientName + "] Đã kích hoạt giám sát THÀNH CÔNG!");
                } else {
                    String reason = (parts.length > 2) ? parts[2] : "Lỗi không xác định";
                    server.log(">>> [" + clientName + "] GIÁM SÁT THẤT BẠI: " + reason);
                }
                break;
        }
    }

    public void sendStopMonitor() {
        if (out != null) {
            out.println(Protocol.CMD_STOP_REQ);
            this.currentPath = "";
            server.log(">>> Đã gửi lệnh DỪNG giám sát tới " + clientName);
        }
    }

    public void closeConnection() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMonitorRequest(String path) {
        if (out != null) {
            // Protocol: MONITOR_REQ;Path
            String command = Protocol.CMD_MONITOR_REQ + Protocol.SEPARATOR + path;

            // Gửi cho client
            out.println(command);

            this.currentPath = path;
            server.log(">>> Đã gửi lệnh giám sát tới " + clientName);
        }
    }

    @Override
    public String toString() {
        return clientName + " (" + socket.getInetAddress().getHostAddress() + ")";
    }
}