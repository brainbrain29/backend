package com.pandora.backend.entity;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import java.io.Serializable;

@Embeddable
@EqualsAndHashCode
public class EmployeeTeamId implements Serializable {
    private Integer employeeId;
    private Integer teamId;
}
