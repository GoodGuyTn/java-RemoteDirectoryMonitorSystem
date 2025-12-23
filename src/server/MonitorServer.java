/*
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

 */

package server;

import client.MonitorClient;
import common.Protocol;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class MonitorServer extends JFrame {
    private JList<ClientHandler> listClients;
    private DefaultListModel<ClientHandler> listModel;
    private JTextArea txtLog;
    private JTextField txtPath;
    private JButton btnMonitor, btnStop, btnDisconnect;

    public MonitorServer() {
        setTitle("Server Giám Sát Thư Mục");
        setupGUI();
        startServerThread();
    }

    private void setupGUI() {
        setLayout(new BorderLayout(10, 10));

        listModel = new DefaultListModel<>();
        listClients = new JList<>(listModel);
        listClients.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        listClients.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ClientHandler selected = listClients.getSelectedValue();
                if (selected != null) {
                    txtPath.setText(selected.getMonitoredPath());
                } else {
                    txtPath.setText("");
                }
            }
        });

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JLabel("Danh sách Client:"), BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(listClients), BorderLayout.CENTER);
        leftPanel.setPreferredSize(new Dimension(200, 0));
        add(leftPanel, BorderLayout.WEST);

        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(txtLog), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.add(new JLabel("Đường dẫn giám sát:"));

        txtPath = new JTextField(50);
        bottomPanel.add(txtPath);

        btnMonitor = new JButton("Bắt đầu Giám sát");
        bottomPanel.add(btnMonitor);

        btnStop = new JButton("Dừng");
        bottomPanel.add(btnStop);

        btnDisconnect = new JButton("Ngắt kết nối");
        bottomPanel.add(btnDisconnect);

        add(bottomPanel, BorderLayout.SOUTH);

        btnMonitor.addActionListener(e -> {
            // Lấy client đang được chọn trong danh sách
            ClientHandler selectedClient = listClients.getSelectedValue();

            if (selectedClient == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một Client trong danh sách!");
                return;
            }

            String path = txtPath.getText().trim();
            if (path.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đường dẫn thư mục!");
                return;
            }

            // Gửi lệnh
            path = path.replace("\"", "");
            selectedClient.sendMonitorRequest(path);
        });

        btnStop.addActionListener(e -> {
            ClientHandler selected = listClients.getSelectedValue();
            if (selected != null) {
                selected.sendStopMonitor();
            } else {
                JOptionPane.showMessageDialog(this, "Chưa chọn Client!");
            }
        });

        btnDisconnect.addActionListener(e -> kickClient());

        setSize(1000, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void startServerThread() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(Protocol.DEFAULT_PORT)) {
                log("Server đang lắng nghe tại cổng " + Protocol.DEFAULT_PORT + "...");

                while (true) {
                    Socket socket = serverSocket.accept();

                    ClientHandler handler = new ClientHandler(socket, this);

                    // Cập nhật giao diện
                    SwingUtilities.invokeLater(() -> {
                        listModel.addElement(handler);
                        log("Kết nối mới: " + socket.getInetAddress());
                    });

                    new Thread(handler).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }


    public void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append(msg + "\n");
            // Tự cuộn xuống dòng cuối
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    public void removeClient(ClientHandler client) {
        SwingUtilities.invokeLater(() -> {
            // Kiểm tra xem client này có đang được chọn không
            ClientHandler selected = listClients.getSelectedValue();

            // Nếu client bị xóa chính là client đang được chọn -> Xóa trắng ô nhập
            if (selected != null && selected.equals(client)) {
                txtPath.setText("");
                listClients.clearSelection();
            }

            // Xóa khỏi Model dữ liệu
            boolean removed = listModel.removeElement(client);

            updateClientList();

            if (removed) {
                log(">>> Đã ngắt kết nối: " + client.getClientInfo());
            }
        });
    }

    private void kickClient() {
        ClientHandler selected = listClients.getSelectedValue();
        if (selected != null) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc muốn ngắt kết nối client này?", "Xác nhận", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                selected.closeConnection();
                removeClient(selected);
            }
        }
    }

    public void updateClientList() {
        SwingUtilities.invokeLater(() -> {
            listClients.revalidate();
            listClients.repaint();
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MonitorClient monitorClient = new MonitorClient();
        });
    }
}