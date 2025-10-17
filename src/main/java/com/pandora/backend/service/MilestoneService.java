package com.pandora.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.pandora.backend.repository.MilestoneRepository;
import com.pandora.backend.dto.MilestoneDTO;
import com.pandora.backend.entity.Milestone;

@Service
public class MilestoneService {

    @Autowired
    private MilestoneRepository milestoneRepository;

    // 返回任务里程碑
    public List<MilestoneDTO> getMilestonesByProjectId(Integer projectId) {
        List<Milestone> milestones = milestoneRepository.findByProjectProjectId(projectId);
        return milestones.stream()
                .map(m -> new MilestoneDTO(m.getTitle(), m.getMilestoneNo()))
                .collect(Collectors.toList());
    }
}
