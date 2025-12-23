package common;

public class Protocol {

    // 1. Cấu hình mạng mặc định
    public static final int DEFAULT_PORT = 5000;
    public static final String DEFAULT_SERVER_IP = "127.0.0.1";

    // 2. Ký tự phân cách (Separator)
    public static final String SEPARATOR = ";";

    // 3. Các lệnh (Commands) - SERVER gửi CLIENT

    // Lệnh yêu cầu giám sát: Server gửi kèm đường dẫn
    // Cấu trúc: MONITOR_REQ;D:/ThuMucCanGiamSat
    public static final String CMD_MONITOR_REQ = "MONITOR_REQ";

    // Client trả lời kết quả giám sát (Thành công hay Thất bại)
    // Cấu trúc: MONITOR_RES;OK hoặc MONITOR_RES;FAIL;Lý do lỗi
    public static final String CMD_MONITOR_RES = "MONITOR_RES";

    // Lệnh dừng giám sát
    public static final String CMD_STOP_REQ = "STOP_REQ";

    // 4. Các lệnh (Commands) - CLIENT gửi SERVER

    // Client chào Server khi vừa kết nối
    // Cấu trúc: HELLO;Ten_May_Client
    public static final String CMD_HELLO = "HELLO";

    // Client báo cáo sự thay đổi file
    // Cấu trúc: NOTIFY;Loai_Thay_Doi;Ten_File
    // Ví dụ: NOTIFY;ENTRY_CREATE;bai_tap.txt
    public static final String CMD_NOTIFY = "NOTIFY";

    // 5. Các loại sự kiện thay đổi (Dùng cho lệnh NOTIFY)
    public static final String ACT_CREATE = "CREATE";
    public static final String ACT_DELETE = "DELETE";
    public static final String ACT_MODIFY = "MODIFY";


}