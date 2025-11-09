package com.pandora.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 评论与发布者的关系：多对一
    // 多个评论可以由一个员工发布
    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Employee author;

    // 评论与事项的关系：多对一
    // 多个评论可以属于一个事项
    @ManyToOne(optional = false)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    // 评论的父子关系（用于实现引用回复）
    // 一个父评论可以有多个子回复
    @ManyToOne
    @JoinColumn(name = "parent_id") // parent_id 可以为 null，表示这不是一条回复
    private Comment parent;

    // 在创建时自动设置时间
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}