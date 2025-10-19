package com.pandora.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer taskId;

    @Column(nullable = false, length = 64)
    private String title;

    @Column(length = 255)
    private String content;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Byte taskStatus;

    @Column(nullable = false)
    private Byte taskPriority;

    @ManyToOne
    @JoinColumn(name = "assignee_id")
    private Employee assignee;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private Employee sender;

    @Column(nullable = false)
    private Byte taskType;

    @Column(nullable = false)
    private Byte createdByWho;

    @ManyToOne
    @JoinColumn(name = "milestone_id")
    private Milestone milestone; // 可为空
}
