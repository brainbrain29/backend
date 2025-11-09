package com.pandora.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.pandora.backend.entity.Department;
import com.pandora.backend.entity.Employee;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
    Optional<Employee> findByPhone(String phone);
    Optional<Employee> findByEmail(String email);
    
    /**
     * 根据部门查询所有员工
     */
    List<Employee> findByDepartment(Department department);
    
    /**
     * 根据职位查询员工
     */
    List<Employee> findByPosition(Byte position);
    
    /**
     * 根据部门和职位查询员工
     */
    List<Employee> findByDepartmentAndPosition(Department department, Byte position);
}
