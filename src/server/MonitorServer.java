package server;

import common.Protocol;
import java.io.*;
import java.util.*;
import java.net.ServerSocket;
import java.net.Socket;

public class MonitorServer {

    // Danh sách quản lý các Client đang kết nối
    private static List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Server đang khởi động tại port " + Protocol.DEFAULT_PORT + "...");

        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Gõ 'help' để xem hướng dẫn.");

            while (true) {
                System.out.print("Server-Admin> ");
                String input = scanner.nextLine().trim();

                if (input.isEmpty()) continue;

                processAdminCommand(input);
            }
        }).start();

        try (ServerSocket serverSocket = new ServerSocket(Protocol.DEFAULT_PORT)) {
            System.out.println("Đang chờ kết nối...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Có kết nối mới từ: " + socket.getInetAddress());
                System.out.print("Server-Admin> ");

                // Tạo luồng xử lý riêng cho client này
                ClientHandler handler = new ClientHandler(socket);
                clients.add(handler);

                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processAdminCommand(String input) {
        String[] parts = input.split(" ", 3);
        String command = parts[0].toLowerCase();

        switch (command) {
            case "list":
                showClientList();
                break;

            case "monitor":
                // Cú pháp: monitor <ID> <Đường dẫn>
                if (parts.length < 3) {
                    System.out.println("Sai cú pháp! Dùng: monitor <ID> <Đường dẫn>");
                } else {
                    try {
                        int id = Integer.parseInt(parts[1]);
                        String path = parts[2].replace("\"", "").trim();

                        sendMonitorCommand(id, path);
                    } catch (NumberFormatException e) {
                        System.out.println("ID phải là số nguyên.");
                    }
                }
                break;

            case "help":
                System.out.println("--- CÁC LỆNH ---");
                System.out.println("1. list                : Xem danh sách client");
                System.out.println("2. monitor <ID> <Path> : Giám sát thư mục");
                System.out.println("   VD: monitor 0 D:/Data");
                break;

            default:
                System.out.println("Lệnh không tồn tại. Gõ 'help' để xem.");
        }
    }

    private static void showClientList() {
        System.out.println("--- DANH SÁCH CLIENT ---");
        synchronized (clients) {
            if (clients.isEmpty()) {
                System.out.println("(Trống)");
            } else {
                for (int i = 0; i < clients.size(); i++) {
                    System.out.println("ID [" + i + "] - " + clients.get(i).getClientInfo());
                }
            }
        }
    }

    private static void sendMonitorCommand(int id, String path) {
        synchronized (clients) {
            if (id >= 0 && id < clients.size()) {
                ClientHandler target = clients.get(id);
                target.sendMonitorRequest(path);
                System.out.println("-> Đã gửi lệnh tới Client ID [" + id + "]");
            } else {
                System.out.println("Lỗi: Không tìm thấy Client có ID [" + id + "]");
            }
        }
    }

    public static synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("Đã xóa một client khỏi danh sách. Tổng số: " + clients.size());
        System.out.print("Server-Admin> ");
    }
}