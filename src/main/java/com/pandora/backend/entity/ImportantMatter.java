package com.pandora.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "important_matter")
public class ImportantMatter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer matterId;

    @Column(nullable = false, length = 255)
    private String content;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @ManyToOne(optional = false)
    @JoinColumn(name = "assignee_id", nullable = false)
    private Employee assignee;

    @Column(nullable = false)
    private Byte matterStatus;

    @Column(nullable = false)
    private Byte matterPriority;

    @Column(nullable = false)
    private Byte serialNum;

    @Column(nullable = false)
    private Byte visibleRange;
}
