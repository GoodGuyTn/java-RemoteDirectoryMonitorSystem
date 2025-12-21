package client;

import common.Protocol;
import java.io.IOException;
import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;

public class FolderWatcher implements Runnable {
    private String pathStr;
    private MonitorClient client;
    private boolean isRunning = true;

    public FolderWatcher(String pathStr, MonitorClient client) {
        this.pathStr = pathStr;
        this.client = client;
    }

    public void stopWatching() {
        isRunning = false;
    }

    @Override
    public void run() {
        Path path = Paths.get(pathStr);

        // Kiểm tra thư mục có tồn tại không
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            System.err.println("Lỗi: Đường dẫn không tồn tại hoặc không phải thư mục: " + pathStr);
            return;
        }

        System.out.println(">>> Đang bắt đầu giám sát: " + pathStr);

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

            while (isRunning) {
                WatchKey key;
                try {
                    key = watchService.take();
                } catch (InterruptedException x) {
                    return;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    // Bỏ qua sự kiện lỗi hệ thống
                    if (kind == OVERFLOW) continue;

                    // Lấy tên file bị thay đổi
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();

                    // Xác định sự kiện và gửi báo cáo về cho Client chính xử lý
                    String actionCode = getActionCode(kind);

                    if (actionCode != null) {
                        client.sendNotify(actionCode, fileName.toString());
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Hàm chuyển đổi Enum của Java sang String trong Protocol
    private String getActionCode(WatchEvent.Kind<?> kind) {
        if (kind == ENTRY_CREATE) return Protocol.ACT_CREATE;
        if (kind == ENTRY_DELETE) return Protocol.ACT_DELETE;
        if (kind == ENTRY_MODIFY) return Protocol.ACT_MODIFY;
        return null;
    }
}