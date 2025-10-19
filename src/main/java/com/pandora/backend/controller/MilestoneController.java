package com.pandora.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.pandora.backend.dto.MilestoneDTO;
import com.pandora.backend.service.MilestoneService;

@RestController
@RequestMapping("/milestone")
public class MilestoneController {

    @Autowired
    private MilestoneService milestoneService;

    @GetMapping("/byProject/{projectId}")
    public List<MilestoneDTO> getMilestonesByProjectId(@PathVariable Integer projectId) {
        return milestoneService.getMilestonesByProjectId(projectId);
    }
}
