package com.pandora.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "notice_employee")
public class NoticeEmployee {

    @EmbeddedId
    private NoticeEmployeeId id;

    @Column(length = 255)
    private String noticeStatus;

    @ManyToOne
    @MapsId("noticeId") // 与复合主键中的 noticeId 对应
    @JoinColumn(name = "notice_id")
    private Notice notice;

    @ManyToOne
    @MapsId("receiverId")
    @JoinColumn(name = "receiver_id")
    private Employee receiver;
}