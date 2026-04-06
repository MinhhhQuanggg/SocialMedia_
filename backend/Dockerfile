# === CÔNG THỨC DOCKERFILE CHO BACKEND (Dành cho việc giải thích trong báo cáo) ===

# Lớp (Stage) 1: Build source code bằng Maven
# Từ kho ảnh chuẩn của Maven, tải bản Maven 3.9 kèm JDK 17 (đây là base image)
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

# Chuyển thư mục hiện tại bên trong container thành /app
WORKDIR /app

# Ưu tiên copy file cấu hình pom.xml vào trước để tận dụng layer cache của Docker
COPY pom.xml .

# Copy toàn bộ thư mục mã nguồn (src) vào trong container
COPY src ./src

# Chạy lệnh biên dịch và đóng gói project thành file .jar, bỏ qua khâu chạy test unit
RUN mvn clean package -DskipTests

# Lớp (Stage) 2: Môi trường chạy siêu nhẹ (Chỉ dùng JRE thay vì JDK)
# Từ kho ảnh JRE 17 Alpine siêu gọn nhẹ (chỉ mười mấy MB)
FROM eclipse-temurin:17-jre-alpine

# Set lại working directory cho lớp chạy
WORKDIR /app

# Copy thành phẩm (file .jar) đã được build thành công từ lớp builder ở trên sang thư mục hiện tại của lớp 2
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Tạo thư mục gốc cho tính năng upload file nếu hệ thống có chức năng upload ảnh
RUN mkdir -p uploads

# Khai báo port 8080 ra bên ngoài để Nginx/Frontend kết nối
EXPOSE 8080

# Kích hoạt thực thi ứng dụng Spring Boot bằng lệnh terminal cơ bản
ENTRYPOINT ["java", "-jar", "app.jar"]
