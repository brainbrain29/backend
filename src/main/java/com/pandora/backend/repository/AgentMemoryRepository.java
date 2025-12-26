package com.pandora.backend.repository;

import com.pandora.backend.entity.AgentMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentMemoryRepository extends JpaRepository<AgentMemory, Long> {

    List<AgentMemory> findTop10ByEmployeeEmployeeIdOrderByCreatedTimeDesc(Integer employeeId);

    List<AgentMemory> findTop5ByEmployeeEmployeeIdAndMemoryTypeOrderByCreatedTimeDesc(Integer employeeId,
            String memoryType);
}
