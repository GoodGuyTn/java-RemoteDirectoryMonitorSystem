package client;

import common.Protocol;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class MonitorClient {

    public static void main(String[] args) {
        String serverIP = Protocol.DEFAULT_SERVER_IP;
        int port = Protocol.DEFAULT_PORT;

        System.out.println("Đang kết nối tới Server " + serverIP + ":" + port + "...");

        try (Socket socket = new Socket(serverIP, port)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Đã kết nối thành công!");

            String myComputerName = "Client_01";
            out.println(Protocol.CMD_HELLO + Protocol.SEPARATOR + myComputerName);
            System.out.println("Đã gửi tin nhắn cho Server.");

            // Vòng lặp lắng nghe lệnh từ Server
            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                System.out.println("[Server gửi]: " + serverMessage);

                // Xử lý các lệnh từ Server ở đây

            }

        } catch (UnknownHostException e) {
            System.err.println("Không tìm thấy Server: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Lỗi kết nối (Server có thể chưa bật): " + e.getMessage());
        }
    }
}