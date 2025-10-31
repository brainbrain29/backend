package com.pandora.backend.service;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pandora.backend.dto.ProjectCreateDTO;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.entity.Project;
import com.pandora.backend.dto.ProjectDTO;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.repository.ProjectRepository;

@Service
public class ProjectService { // TODO: Service for CEO project creation

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    public ProjectDTO createProject(ProjectCreateDTO dto, Integer senderId) { // TODO: Create project by CEO
        Employee sender = employeeRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        Project p = new Project();
        p.setTitle(dto.getTitle());
        p.setContent(dto.getContent());
        p.setStartTime(dto.getStartTime());
        p.setEndTime(dto.getEndTime());
        p.setProjectStatus(dto.getProjectStatus());
        p.setProjectPriority(dto.getProjectPriority());
        p.setProjectType(dto.getProjectType() == null ? (byte) 1 : dto.getProjectType());
        p.setSender(sender);

        Project saved = projectRepository.save(p);
        return convertToDto(saved);
    }

    private List<ProjectDTO> convertToDtoList(List<Project> projects) {
        return projects.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private ProjectDTO convertToDto(Project project) {
        ProjectDTO dto = new ProjectDTO();
        dto.setProjectId(project.getProjectId());
        dto.setTitle(project.getTitle());
        dto.setContent(project.getContent());
        dto.setStartTime(project.getStartTime());
        dto.setEndTime(project.getEndTime());
        dto.setProjectStatus(project.getProjectStatus());
        dto.setProjectPriority(project.getProjectPriority());
        dto.setProjectType(project.getProjectType());
        if (project.getSender() != null) {
            dto.setSenderId(project.getSender().getEmployeeId());
        }
        return dto;
    }
}
