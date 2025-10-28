package com.pandora.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.pandora.backend.entity.ImportantPersonTask;

@Repository
public interface ImportantPersonTaskRepository extends JpaRepository<ImportantPersonTask, Integer> {
}


