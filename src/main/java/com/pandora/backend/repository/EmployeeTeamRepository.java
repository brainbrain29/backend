package com.pandora.backend.repository;

import com.pandora.backend.entity.EmployeeTeamId;
import com.pandora.backend.entity.Employee_Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeTeamRepository extends JpaRepository<Employee_Team, EmployeeTeamId> {
    List<Employee_Team> findByTeamTeamId(Integer teamId);
    long countByTeamTeamId(Integer teamId);
}
