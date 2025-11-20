package com.pandora.backend.repository;

import com.pandora.backend.entity.NoticeEmployee;
import com.pandora.backend.entity.NoticeEmployeeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface NoticeEmployeeRepository extends JpaRepository<NoticeEmployee, NoticeEmployeeId> {

    @Query("SELECT COUNT(ne) FROM NoticeEmployee ne WHERE ne.id.receiverId = :receiverId AND ne.noticeStatus = com.pandora.backend.enums.NoticeStatus.NOT_VIEWED")
    long countUnreadByReceiverId(@Param("receiverId") Integer receiverId);

    @Query("SELECT ne FROM NoticeEmployee ne JOIN FETCH ne.notice n JOIN FETCH n.sender s WHERE ne.id.receiverId = :receiverId ORDER BY n.createdTime DESC")
    List<NoticeEmployee> findAllByReceiverId(@Param("receiverId") Integer receiverId);

    @Query("SELECT ne FROM NoticeEmployee ne JOIN FETCH ne.notice n JOIN FETCH n.sender s WHERE ne.id.receiverId = :receiverId AND ne.noticeStatus = com.pandora.backend.enums.NoticeStatus.NOT_VIEWED ORDER BY n.createdTime DESC")
    List<NoticeEmployee> findUnreadByReceiverId(@Param("receiverId") Integer receiverId);

    @Query("SELECT ne FROM NoticeEmployee ne JOIN FETCH ne.notice n JOIN FETCH n.sender s WHERE ne.id.receiverId = :receiverId AND ne.noticeStatus = :status ORDER BY n.createdTime DESC")
    List<NoticeEmployee> findByIdReceiverIdAndNoticeStatus(@Param("receiverId") Integer receiverId,
            @Param("status") com.pandora.backend.enums.NoticeStatus status);
}
