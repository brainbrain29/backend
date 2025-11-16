package com.pandora.backend.entity;

import com.pandora.backend.enums.Emoji;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString; 
import java.time.LocalDateTime;

import java.util.Set;
import java.util.HashSet;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Getter
@Setter
// 2. (推荐) 添加 @ToString 并排除所有关联对象
// 这可以防止日志(logging)时因循环引用导致堆栈溢出
@ToString(exclude = {"employee", "task", "attachments"}) 
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
    @Enumerated(EnumType.ORDINAL)
    private Emoji emoji;

    // 4. --- 新增的附件集合 ---
    @OneToMany(
        mappedBy = "log", // 对应 LogAttachment 实体中的 "log" 字段
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private Set<LogAttachment> attachments = new HashSet<>();
}