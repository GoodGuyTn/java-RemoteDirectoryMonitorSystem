package client;

import common.Protocol;
import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import static java.nio.file.StandardWatchEventKinds.*;

public class FolderWatcher implements Runnable {
    private String rootPathStr;
    private MonitorClient client;
    private boolean isRunning = true;
    private WatchService watchService;

    private Map<WatchKey, Path> keys;

    public FolderWatcher(String pathStr, MonitorClient client) {
        this.rootPathStr = pathStr;
        this.client = client;
        this.keys = new HashMap<>();
    }

    public void stopWatching() {
        isRunning = false;
    }

    @Override
    public void run() {
        Path rootPath = Paths.get(rootPathStr);

        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            System.err.println("Lỗi: Đường dẫn không tồn tại: " + rootPathStr);
            return;
        }

        System.out.println(">>> Đang bắt đầu giám sát: " + rootPathStr);

        try {
            watchService = FileSystems.getDefault().newWatchService();

            registerAll(rootPath);

            // Vòng lặp lắng nghe
            while (isRunning) {
                WatchKey key;
                try {
                    key = watchService.take(); // Chờ sự kiện
                } catch (Exception e) {
                    return;
                }

                // Lấy ra đường dẫn của thư mục nơi sự kiện xảy ra
                Path dir = keys.get(key);
                if (dir == null) {
                    System.err.println("Không nhận diện được WatchKey!");
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == OVERFLOW) continue;

                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();

                    // Tạo đường dẫn đầy đủ: Thư mục xảy ra sự kiện + Tên file
                    Path fullPath = dir.resolve(fileName);

                    // Nếu sự kiện là TẠO MỚI (Create) và nó là THƯ MỤC
                    // -> Ta phải đăng ký giám sát nó luôn
                    if (kind == ENTRY_CREATE) {
                        try {
                            if (Files.isDirectory(fullPath, LinkOption.NOFOLLOW_LINKS)) {
                                registerAll(fullPath);
                            }
                        } catch (IOException e) {
                            // Bỏ qua lỗi
                        }
                    }
                    // Lấy đường dẫn tương đối
                    Path root = Paths.get(rootPathStr);
                    Path relativePath = root.relativize(fullPath);

                    // Gửi thông báo về Server
                    String actionCode = getActionCode(kind);
                    if (actionCode != null) {
                        client.sendNotify(actionCode, relativePath.toString());
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    keys.remove(key);
                    if (keys.isEmpty()) {
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

        keys.put(key, dir);
    }

    private void registerAll(final Path start) throws IOException {
        // Tham khảo
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir); // Gặp thư mục nào, đăng ký thư mục đó
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private String getActionCode(WatchEvent.Kind<?> kind) {
        if (kind == ENTRY_CREATE) return Protocol.ACT_CREATE;
        if (kind == ENTRY_DELETE) return Protocol.ACT_DELETE;
        if (kind == ENTRY_MODIFY) return Protocol.ACT_MODIFY;
        return null;
    }
}