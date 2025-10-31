package com.pandora.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.pandora.backend.entity.Department;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {
}


