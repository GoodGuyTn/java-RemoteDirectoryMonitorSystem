package server;

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
            MonitorServer monitorServer = new MonitorServer();
        });
    }
}