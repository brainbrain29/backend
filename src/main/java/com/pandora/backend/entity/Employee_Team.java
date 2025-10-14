package com.pandora.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "employee_team")
public class Employee_Team {

    @EmbeddedId
    private EmployeeTeamId id; // 复合主键

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("employeeId")
    @JoinColumn(name = "employee_id", referencedColumnName = "employee_id", foreignKey = @ForeignKey(name = "fk_relation_employee"))
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("teamId")
    @JoinColumn(name = "team_id", referencedColumnName = "team_id", foreignKey = @ForeignKey(name = "fk_relation_team"))
    private Team team;

    @Column(name = "is_leader", nullable = false)
    private Byte isLeader;
}
