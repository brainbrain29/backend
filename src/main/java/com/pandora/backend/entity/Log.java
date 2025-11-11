package com.pandora.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Getter
@Setter
@Entity
@Table(name = "log")
public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer logId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    @JsonBackReference(value = "emp-logs")
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "task_id")
    @JsonBackReference(value = "task-logs")
    private Task task;

    @Column(nullable = false)
    private LocalDateTime createdTime;

    @Column(nullable = false, length = 255)
    private String content;

    @Column(nullable = false)
    private Byte emoji;

    @Column(length = 255)
    private String attachment;

    @Column(length = 50)
    private String employeeLocation;

    @Column(name = "employee_position")
    private Byte employeePosition;
}
