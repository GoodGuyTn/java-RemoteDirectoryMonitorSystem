package server;

import common.Protocol;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MonitorServer {

    // Danh sách quản lý các Client đang kết nối
    private static List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Server đang khởi động tại port " + Protocol.DEFAULT_PORT + "...");

        try (ServerSocket serverSocket = new ServerSocket(Protocol.DEFAULT_PORT)) {
            System.out.println("Đang chờ kết nối...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Có kết nối mới từ: " + socket.getInetAddress());

                // Tạo luồng xử lý riêng cho client này
                ClientHandler handler = new ClientHandler(socket);
                clients.add(handler); // Thêm vào danh sách quản lý

                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("Đã xóa một client khỏi danh sách. Tổng số: " + clients.size());
    }
}