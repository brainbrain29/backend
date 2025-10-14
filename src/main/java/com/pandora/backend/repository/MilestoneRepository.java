package com.pandora.backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.pandora.backend.entity.Milestone;

@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, Integer> {
    List<Milestone> findByProjectId(Integer projectId);
}
