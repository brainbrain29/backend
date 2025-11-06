package com.pandora.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TeamDTO {
    private Integer teamId;
    private String teamName;
    private Integer orgId;
    private String orgName;
    private Integer memberCount;
    private List<String> memberNames;
    private Integer leaderId;
    private String leaderName;
    private List<Integer> memberIds;
}
