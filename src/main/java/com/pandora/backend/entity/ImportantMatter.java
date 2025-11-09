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

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String content;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "publish_time")
    private LocalDateTime publishTime;
}