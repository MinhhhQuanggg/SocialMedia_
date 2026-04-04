/**
 * Notification Manager
 * Quản lý hiển thị và cập nhật thông báo real-time
 */

(function(global) {
    'use strict';

    class NotificationManager {
        constructor() {
            this.wsConnection = null;
            this.dropdownVisible = false;
            this.notifications = [];
            this.unreadCount = 0;
            
            this.initElements();
            this.attachEventListeners();
            this.loadNotifications();
            this.loadUnreadCount();
        }

        initElements() {
            this.notificationIcon = document.querySelector('.notification-icon');
            this.notificationBadge = document.querySelector('.notification-badge');
            this.notificationDropdown = document.querySelector('.notification-dropdown');
            this.notificationList = document.querySelector('.notification-list');
            this.notificationWrapper = document.querySelector('.notification-wrapper');
        }

        attachEventListeners() {
            // Toggle dropdown khi click vào icon
            if (this.notificationWrapper) {
                this.notificationWrapper.addEventListener('click', (e) => {
                    e.stopPropagation();
                    this.toggleDropdown();
                });
            }

            // Đóng dropdown khi click bên ngoài
            document.addEventListener('click', (e) => {
                if (this.dropdownVisible && this.notificationDropdown && !this.notificationDropdown.contains(e.target)) {
                    this.hideDropdown();
                }
            });
        }

        toggleDropdown() {
            if (this.dropdownVisible) {
                this.hideDropdown();
            } else {
                this.showDropdown();
            }
        }

        async showDropdown() {
            if (this.notificationDropdown) {
                this.notificationDropdown.style.display = 'block';
                this.dropdownVisible = true;
                await this.loadNotifications();
                // Đánh dấu tất cả đã đọc khi mở dropdown
                await this.markAllAsRead();
            }
        }

        hideDropdown() {
            if (this.notificationDropdown) {
                this.notificationDropdown.style.display = 'none';
                this.dropdownVisible = false;
            }
        }

        async loadNotifications(page = 0, size = 20) {
            try {
                const response = await fetch(`${api.baseUrl}/api/notifications?page=${page}&size=${size}`, {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + api.getToken(),
                        'Content-Type': 'application/json'
                    }
                });
                
                if (response.ok) {
                    const data = await response.json();
                    if (data && Array.isArray(data)) {
                        this.notifications = data;
                        this.renderNotifications();
                    }
                } else {
                    console.error('Error loading notifications:', response.status);
                    this.renderError();
                }
            } catch (error) {
                console.error('Error loading notifications:', error);
                this.renderError();
            }
        }

        async loadUnreadCount() {
            try {
                const response = await fetch(`${api.baseUrl}/api/notifications/count`, {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + api.getToken(),
                        'Content-Type': 'application/json'
                    }
                });
                
                if (response.ok) {
                    const data = await response.json();
                    if (data && typeof data.unreadCount !== 'undefined') {
                        this.updateUnreadCount(data.unreadCount);
                    }
                }
            } catch (error) {
                console.error('Error loading unread count:', error);
            }
        }

        async markAllAsRead() {
            try {
                // Đánh dấu tất cả thông báo chưa đọc thành đã đọc
                const unreadNotifications = this.notifications.filter(n => !n.isRead);
                
                for (const notification of unreadNotifications) {
                    await fetch(`${api.baseUrl}/api/notifications/${notification.notificationId}/read`, {
                        method: 'PUT',
                        headers: {
                            'Authorization': 'Bearer ' + api.getToken(),
                            'Content-Type': 'application/json'
                        }
                    });
                    notification.isRead = true;
                }
                
                // Cập nhật UI
                this.updateUnreadCount(0);
                this.renderNotifications();
            } catch (error) {
                console.error('Error marking all as read:', error);
            }
        }

        updateUnreadCount(count) {
            this.unreadCount = count;
            if (this.notificationBadge) {
                if (count > 0) {
                    this.notificationBadge.textContent = count > 99 ? '99+' : count;
                    this.notificationBadge.style.display = 'flex';
                } else {
                    this.notificationBadge.style.display = 'none';
                }
            }
        }

        renderNotifications() {
            if (!this.notificationList) return;

            if (this.notifications.length === 0) {
                this.notificationList.innerHTML = `
                    <div class="notification-empty">
                        <i class="fas fa-bell-slash"></i>
                        <p>Chưa có thông báo nào</p>
                    </div>
                `;
                return;
            }

            this.notificationList.innerHTML = this.notifications.map(n => this.createNotificationHTML(n)).join('');
            
            // Attach click handlers
            this.notificationList.querySelectorAll('.notification-item').forEach((item, index) => {
                item.addEventListener('click', () => {
                    this.handleNotificationClick(this.notifications[index]);
                });
            });
        }

        createNotificationHTML(notification) {
            const isUnread = !notification.isRead;
            
            // Xử lý avatar URL
            let avatarUrl;
            const actorAvatar = notification.actor?.avatarUrl;
            
            // Check for NULL, undefined, empty string, or 'null' string
            if (!actorAvatar || 
                actorAvatar === 'null' || 
                actorAvatar === 'undefined' || 
                actorAvatar === 'NULL' ||
                (typeof actorAvatar === 'string' && actorAvatar.trim() === '')) {
                avatarUrl = 'images/avatars/default_avatar.png';
            } else if (actorAvatar.startsWith('http')) {
                avatarUrl = actorAvatar;
            } else if (actorAvatar.startsWith('/')) {
                avatarUrl = `${api.baseUrl}${actorAvatar}`;
            } else {
                avatarUrl = actorAvatar;
            }
            
            const timeAgo = this.getTimeAgo(notification.createdAt);
            
            return `
                <div class="notification-item ${isUnread ? 'unread' : ''}" data-id="${notification.notificationId}">
                    <div class="notification-avatar" style="background-image: url('${avatarUrl}')"></div>
                    <div class="notification-content">
                        <div class="notification-message">${this.escapeHtml(notification.message || '')}</div>
                        <div class="notification-time">${timeAgo}</div>
                    </div>
                </div>
            `;
        }

        async handleNotificationClick(notification) {
            // Không làm gì khi click vào thông báo
            // Thông báo đã được đánh dấu đã đọc khi mở dropdown
        }

        // Nhận notification mới từ WebSocket
        handleNewNotification(notificationData) {
            console.log('New notification received:', notificationData);
            
            // Thêm vào đầu danh sách
            this.notifications.unshift(notificationData);
            
            // Tăng số lượng chưa đọc
            this.unreadCount++;
            this.updateUnreadCount(this.unreadCount);
            
            // Cập nhật UI nếu dropdown đang mở
            if (this.dropdownVisible) {
                this.renderNotifications();
            }
            
            // Hiển thị toast notification
            this.showToast(notificationData);
        }

        showToast(notification) {
            // Tạo toast notification
            const toast = document.createElement('div');
            toast.className = 'notification-toast';
            toast.innerHTML = `
                <div class="toast-content">
                    <i class="fas fa-bell"></i>
                    <span>${this.escapeHtml(notification.message || '')}</span>
                </div>
            `;
            
            // Thêm CSS cho toast nếu chưa có
            if (!document.getElementById('notification-toast-style')) {
                const style = document.createElement('style');
                style.id = 'notification-toast-style';
                style.textContent = `
                    .notification-toast {
                        position: fixed;
                        top: 80px;
                        right: 20px;
                        background: white;
                        padding: 15px 20px;
                        border-radius: 12px;
                        box-shadow: 0 8px 24px rgba(0,0,0,0.2);
                        z-index: 9999;
                        animation: slideIn 0.3s ease-out;
                        max-width: 350px;
                    }
                    
                    .toast-content {
                        display: flex;
                        align-items: center;
                        gap: 12px;
                    }
                    
                    .toast-content i {
                        color: var(--gummy-pink);
                        font-size: 20px;
                    }
                    
                    @keyframes slideIn {
                        from {
                            transform: translateX(400px);
                            opacity: 0;
                        }
                        to {
                            transform: translateX(0);
                            opacity: 1;
                        }
                    }
                    
                    @keyframes slideOut {
                        from {
                            transform: translateX(0);
                            opacity: 1;
                        }
                        to {
                            transform: translateX(400px);
                            opacity: 0;
                        }
                    }
                `;
                document.head.appendChild(style);
            }
            
            document.body.appendChild(toast);
            
            // Tự động ẩn sau 4 giây
            setTimeout(() => {
                toast.style.animation = 'slideOut 0.3s ease-out';
                setTimeout(() => {
                    if (toast.parentNode) {
                        toast.parentNode.removeChild(toast);
                    }
                }, 300);
            }, 4000);
        }

        getTimeAgo(dateString) {
            if (!dateString) return '';
            
            const date = new Date(dateString);
            const now = new Date();
            const seconds = Math.floor((now - date) / 1000);
            
            if (seconds < 60) return 'Vừa xong';
            if (seconds < 3600) return Math.floor(seconds / 60) + ' phút trước';
            if (seconds < 86400) return Math.floor(seconds / 3600) + ' giờ trước';
            if (seconds < 604800) return Math.floor(seconds / 86400) + ' ngày trước';
            if (seconds < 2592000) return Math.floor(seconds / 604800) + ' tuần trước';
            return Math.floor(seconds / 2592000) + ' tháng trước';
        }

        escapeHtml(text) {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        }

        renderError() {
            if (this.notificationList) {
                this.notificationList.innerHTML = `
                    <div class="notification-empty">
                        <i class="fas fa-exclamation-circle"></i>
                        <p>Không thể tải thông báo</p>
                    </div>
                `;
            }
        }
    }

    // Export to global scope
    global.NotificationManager = NotificationManager;
    
    // Auto-initialize when DOM is ready
    function initNotificationManager() {
        if (!global.notificationManager) {
            global.notificationManager = new NotificationManager();
            console.log('Notification Manager initialized');
        }
        return global.notificationManager;
    }
    
    global.initNotificationManager = initNotificationManager;

})(window);
