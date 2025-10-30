package com.pandora.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "important_task")
public class ImportantTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Integer taskId;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "task_content", nullable = false, length = 255)
    private String taskContent;

    @Column(name = "deadline", nullable = false)
    private LocalDateTime deadline;

    @Column(name = "task_status", nullable = false)
    private Byte taskStatus; // 0: 待处理, 1: 进行中, 2: 已完成

    @Column(name = "task_priority", nullable = false)
    private Byte taskPriority; // 0: 低, 1: 中, 2: 高

    @Column(name = "serial_num", nullable = false)
    private Byte serialNum; // 序号 1-10

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;
}
