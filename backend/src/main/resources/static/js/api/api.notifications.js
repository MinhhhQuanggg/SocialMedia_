/**
 * API cho Notifications
 */

import { apiRequest } from './api.base.js';

export const notificationAPI = {
    /**
     * Lấy danh sách thông báo
     */
    async getNotifications(page = 0, size = 20) {
        return await apiRequest(`/api/notifications?page=${page}&size=${size}`, {
            method: 'GET'
        });
    },

    /**
     * Đánh dấu thông báo đã đọc
     */
    async markAsRead(notificationId) {
        return await apiRequest(`/api/notifications/${notificationId}/read`, {
            method: 'PUT'
        });
    },

    /**
     * Đếm số thông báo chưa đọc
     */
    async countUnread() {
        return await apiRequest('/api/notifications/count', {
            method: 'GET'
        });
    }
};
