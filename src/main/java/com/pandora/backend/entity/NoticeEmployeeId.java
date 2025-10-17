package com.pandora.backend.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import java.util.Objects;

@Getter
@Setter
@Embeddable
public class NoticeEmployeeId implements Serializable {
    private Integer noticeId;
    private Integer receiverId;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NoticeEmployeeId that = (NoticeEmployeeId) o;
        return Objects.equals(noticeId, that.noticeId) &&
                Objects.equals(receiverId, that.receiverId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(noticeId, receiverId);
    }

}
