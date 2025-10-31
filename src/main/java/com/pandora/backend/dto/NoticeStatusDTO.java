package com.pandora.backend.dto;

public class NoticeStatusDTO {
    private boolean hasUnreadNotice;
    private int unreadCount;

    public NoticeStatusDTO(boolean hasUnreadNotice, int unreadCount) {
        this.hasUnreadNotice = hasUnreadNotice;
        this.unreadCount = unreadCount;
    }

    public boolean isHasUnreadNotice() {
        return hasUnreadNotice;
    }

    public int getUnreadCount() {
        return unreadCount;
    }
}
