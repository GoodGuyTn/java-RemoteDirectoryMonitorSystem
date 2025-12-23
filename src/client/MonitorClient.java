package client;

import common.Protocol;
import java.io.*;
import java.net.Socket;
import java.nio.file.*;

public class MonitorClient {
    private Socket socket;
    private PrintWriter out;
    private Thread watcherThread;
    private FolderWatcher currentWatcher;

    // Chuyển logic vào constructor/method để dễ quản lý biến instance
    public void start() {
        String serverIP = Protocol.DEFAULT_SERVER_IP;
        int port = Protocol.DEFAULT_PORT;

        System.out.println("Client đang kết nối tới " + serverIP + ":" + port + "...");

        try {
            socket = new Socket(serverIP, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Đã kết nối! Gửi lời chào...");
            out.println(Protocol.CMD_HELLO + Protocol.SEPARATOR + "Client_Console");

            // Vòng lặp lắng nghe lệnh từ Server
            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                System.out.println("[Server CMD]: " + serverMessage);
                processCommand(serverMessage);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processCommand(String message) {
        if (message.startsWith(Protocol.CMD_MONITOR_REQ)) {
            // Cấu trúc: MONITOR_REQ;D:/TestFolder
            // Xử lý lệnh yêu cầu theo dõi thư mục
            String[] parts = message.split(Protocol.SEPARATOR);
            if (parts.length > 1) {
                String path = parts[1];
                startWatcher(path);
            }
        }
    }

    private void startWatcher(String path) {
        Path pathStr = Paths.get(path);
        // Kiểm tra xem thư mục có tồn tại không
        if (!Files.exists(pathStr) || !Files.isDirectory(pathStr)) {
            System.err.println("Lỗi: Đường dẫn không hợp lệ: " + path);
            // Gửi LỖI về Server
            out.println(Protocol.CMD_MONITOR_RES + Protocol.SEPARATOR + "FAIL" + Protocol.SEPARATOR + "Thu muc khong ton tai");
            return;
        }

        if (currentWatcher != null) {
            currentWatcher.stopWatching();
        }

        // Tạo watcher mới
        currentWatcher = new FolderWatcher(path, this);
        watcherThread = new Thread(currentWatcher);
        watcherThread.start();

        // Gửi thông báo THÀNH CÔNG về Server
        out.println(Protocol.CMD_MONITOR_RES + Protocol.SEPARATOR + "OK");
        System.out.println("-> Đã bắt đầu giám sát và báo OK về Server.");
    }

    public synchronized void sendNotify(String action, String filename) {
        // Protocol: NOTIFY;CREATE;file.txt
        String msg = Protocol.CMD_NOTIFY + Protocol.SEPARATOR + action + Protocol.SEPARATOR + filename;
        out.println(msg);
        System.out.println("-> Đã báo cáo: " + msg);
    }

    public static void main(String[] args) {
        new MonitorClient().start();
    }
}