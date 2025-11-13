package com.pandora.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import jakarta.persistence.Entity; // <-- 确保导入了这个
import jakarta.persistence.Table;  // <-- 如果你用了 @Table，也确保导入

@Getter
@Setter
@Entity
@Table(name = "notice")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer noticeId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private Employee sender;

    @Column(nullable = false)
    private Byte noticeType;

    @Column(length = 255)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdTime;
}
