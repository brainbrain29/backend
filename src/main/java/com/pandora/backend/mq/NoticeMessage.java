package com.pandora.backend.mq;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Message object for notification queue.
 * Contains all data needed to push a notification to a user.
 */
public class NoticeMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer noticeId;
    private Integer receiverId;
    private String content;
    private String senderName;
    private LocalDateTime createdTime;
    private Integer relatedId;
    private String noticeType;
    private String status;

    public NoticeMessage() {
    }

    public Integer getNoticeId() {
        return noticeId;
    }

    public void setNoticeId(Integer noticeId) {
        this.noticeId = noticeId;
    }

    public Integer getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Integer receiverId) {
        this.receiverId = receiverId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public Integer getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(Integer relatedId) {
        this.relatedId = relatedId;
    }

    public String getNoticeType() {
        return noticeType;
    }

    public void setNoticeType(String noticeType) {
        this.noticeType = noticeType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "NoticeMessage{" +
                "noticeId=" + noticeId +
                ", receiverId=" + receiverId +
                ", noticeType='" + noticeType + '\'' +
                '}';
    }
}
