package com.pandora.backend.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class NoticeEmployeeId implements Serializable {
    private Integer noticeId;
    private Integer receiverId;

    // equals() 和 hashCode() 方法建议由 IDE 自动生成
}
