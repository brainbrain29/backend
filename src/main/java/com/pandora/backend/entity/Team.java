package com.pandora.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "team")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Integer teamId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", referencedColumnName = "org_id", nullable = false, foreignKey = @ForeignKey(name = "fk_team_department"))
    private Department department;

    @Column(name = "team_name", nullable = false, length = 64)
    private String teamName;

    @OneToMany(mappedBy = "team")
    private List<Employee_Team> employeeTeams;
}
