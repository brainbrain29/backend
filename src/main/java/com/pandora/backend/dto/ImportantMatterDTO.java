package com.pandora.backend.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class ImportantMatterDTO {
    private Integer matterId;
    private String content;
    private LocalDateTime deadline;
    private Integer assigneeId;
    private String assigneeName;
    private Byte matterStatus;
    private Byte matterPriority;
    private Byte serialNum;
    private Byte visibleRange;

    // Constructors
    public ImportantMatterDTO() {}

    public ImportantMatterDTO(Integer matterId, String content, LocalDateTime deadline,
                            Integer assigneeId, String assigneeName, Byte matterStatus,
                            Byte matterPriority, Byte serialNum, Byte visibleRange) {
        this.matterId = matterId;
        this.content = content;
        this.deadline = deadline;
        this.assigneeId = assigneeId;
        this.assigneeName = assigneeName;
        this.matterStatus = matterStatus;
        this.matterPriority = matterPriority;
        this.serialNum = serialNum;
        this.visibleRange = visibleRange;
    }
}


