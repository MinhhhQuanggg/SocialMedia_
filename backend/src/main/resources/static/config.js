// ========================================
// CONFIG.JS - Backend Configuration
// ========================================
// Cấu hình địa chỉ backend server
// 
// ⚙️ HƯỚNG DẪN:
// 1. Chạy trên 1 máy (local):
//    window.BACKEND_URL = 'http://localhost:8080';
//
// 2. Chạy trên 2 máy cùng WiFi:
//    - Lấy IP của máy chạy backend: chạy 'ipconfig' trong CMD
//    - Thay localhost bằng IP đó, ví dụ:
//    window.BACKEND_URL = 'http://192.168.1.100:8080';
//
// 3. Deploy lên server:
//    window.BACKEND_URL = 'http://your-server-domain.com:8080';
//
// 4. Deploy với Ngrok (expose ra internet):
//    - Chạy backend: cd backend && ./mvnw spring-boot:run
//    - Chạy ngrok: ngrok http 8080
//    - Copy URL từ ngrok (ví dụ: https://abcd-1234.ngrok-free.app)
//    - Thay vào đây:
//    window.BACKEND_URL = 'https://abcd-1234.ngrok-free.app';
//
// ========================================

// 🔧 Thay đổi URL này theo môi trường của bạn:
window.BACKEND_URL = 'http://localhost:8080';
//window.BACKEND_URL = 'http://192.168.1.53:8080'; // Thay bằng địa chỉ backend server của bạn