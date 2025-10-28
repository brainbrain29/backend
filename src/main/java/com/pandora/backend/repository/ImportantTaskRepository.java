package com.pandora.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.pandora.backend.entity.ImportantTask;

@Repository
public interface ImportantTaskRepository extends JpaRepository<ImportantTask, Integer> {
}
