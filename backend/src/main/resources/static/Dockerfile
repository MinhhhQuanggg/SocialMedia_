# === CÔNG THỨC DOCKERFILE CHO FRONTEND (Dùng giải thích báo cáo) ===

# Sử dụng Nginx (bản đồ sộ Alpine siêu nhẹ) làm máy chủ web chính
FROM nginx:alpine

# Xoá toàn bộ cấu hình mặc định trang chủ lỗi của Nginx (để làm sạch không gian)
RUN rm -rf /usr/share/nginx/html/*

# Copy toàn bộ nội dung HTML, JS, CSS ở thư mục frontend cục bộ đập vào thư mục phục vụ web của Nginx
COPY . /usr/share/nginx/html/

# Mở cổng 80 chuẩn HTTP ra ngoài để Docker có thể ánh xạ vào localhost trình duyệt
EXPOSE 80

# Chạy engine Nginx trên foreground để giữ cho container luôn hoạt động không bị tắt
CMD ["nginx", "-g", "daemon off;"]
