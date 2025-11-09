package com.pandora.backend.entity;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;

@Embeddable
@EqualsAndHashCode
@Getter
@Setter
public class EmployeeTeamId implements Serializable {
    private Integer employeeId;
    private Integer teamId;
}
