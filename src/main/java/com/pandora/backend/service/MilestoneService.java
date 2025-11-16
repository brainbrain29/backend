package com.pandora.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.pandora.backend.repository.MilestoneRepository;
import com.pandora.backend.repository.ProjectRepository;
import com.pandora.backend.dto.MilestoneDTO;
import com.pandora.backend.dto.MilestoneCreateDTO;
import com.pandora.backend.entity.Milestone;
import com.pandora.backend.entity.Project;

@Service
public class MilestoneService {

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private ProjectRepository projectRepository;

    // 返回任务里程碑
    public List<MilestoneDTO> getMilestonesByProjectId(Integer projectId) {
        List<Milestone> milestones = milestoneRepository.findByProjectProjectId(projectId);
        return milestones.stream()
                .map(m -> new MilestoneDTO(m.getMilestoneId(), m.getTitle()))
                .collect(Collectors.toList());
    }

    public MilestoneDTO createMilestone(MilestoneCreateDTO dto) {
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));

        Milestone m = new Milestone();
        m.setTitle(dto.getTitle());
        m.setContent(dto.getContent());
        m.setProject(project);

        Milestone saved = milestoneRepository.save(m);
        return new MilestoneDTO(saved.getMilestoneId(), saved.getTitle());
    }
}
