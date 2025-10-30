package com.pandora.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.pandora.backend.entity.ImportantMatter;

@Repository
public interface ImportantMatterRepository extends JpaRepository<ImportantMatter, Integer> {
}


