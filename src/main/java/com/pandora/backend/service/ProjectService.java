package com.pandora.backend.service;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;

import com.pandora.backend.entity.Project;
import com.pandora.backend.dto.ProjectDTO;
import com.pandora.backend.repository.ProjectRepository;

//TODO
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    private List<ProjectDTO> convertToDtoList(List<Project> projects) {
        return projects.stream()
                // .filter(project -> "ACTIVE".equals(project.getStatus()))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private ProjectDTO convertToDto(Project project) {
        ProjectDTO dto = new ProjectDTO();

        return dto;
    }
}
