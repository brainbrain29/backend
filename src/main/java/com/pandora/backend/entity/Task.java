package com.pandora.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

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

    @Column(name = "task_type", nullable = false)
    private Byte taskType;

    @ManyToOne
    @JoinColumn(name = "milestone_id")
    private Milestone milestone;

    @OneToMany(mappedBy = "task", fetch = FetchType.LAZY)
    @JsonManagedReference(value = "task-logs")
    private List<Log> logs;
}
