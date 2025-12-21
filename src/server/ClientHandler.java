package server;

import common.Protocol;
import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String clientName = "Unknown";

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public String getClientInfo() {
        return "Client: " + clientName;
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
            System.out.println("Client ngắt kết nối: " + clientName);
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
                    System.out.println(">>> Client mới kết nối: " + clientName);
                }
                break;

            case Protocol.CMD_NOTIFY:
                // Logic xử lý thông báo
                System.out.println(">>> Nhận thông báo thay đổi từ " + clientName);
                break;
        }
    }

    private void closeConnection() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Gọi ngược về Server để xóa khỏi danh sách
        MonitorServer.removeClient(this);
    }

    public void sendMonitorRequest(String path) {
        if (out != null) {
            // Protocol: MONITOR_REQ;Path
            String command = Protocol.CMD_MONITOR_REQ + Protocol.SEPARATOR + path;

            // Gửi cho client
            out.println(command);

            System.out.println(">>> Đã gửi lệnh yêu cầu giám sát tới " + clientName);
        }
    }
}