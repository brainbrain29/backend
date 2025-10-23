package com.pandora.backend.repository;

import com.pandora.backend.entity.Project;

import org.springframework.data.jpa.repository.JpaRepository;

// TODO
public interface ProjectRepository extends JpaRepository<Project, Integer> {
    // List<Project> getProjectsBySender_EmployeeId(Integer sender_id);
}
