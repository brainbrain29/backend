package com.pandora.backend.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pandora.backend.dto.NoticeDTO;
import com.pandora.backend.entity.Notice;
import com.pandora.backend.repository.NoticeRepository;

@Service
public class NoticeService {

    @Autowired
    private NoticeRepository noticeRepository;

    public List<NoticeDTO> searchNotices(String keyword) {
        if (keyword == null) {
            return Collections.emptyList();
        }

        String trimmedKeyword = keyword.trim();
        if (trimmedKeyword.isEmpty()) {
            return Collections.emptyList();
        }

        List<Notice> notices = noticeRepository.searchByKeyword(trimmedKeyword);
        return notices.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private NoticeDTO convertToDto(Notice notice) {
        NoticeDTO dto = new NoticeDTO();
        dto.setNoticeId(notice.getNoticeId());
        dto.setContent(notice.getContent());
        dto.setCreatedTime(notice.getCreatedTime());
        if (notice.getSender() != null) {
            dto.setSenderName(notice.getSender().getEmployeeName());
        }
        return dto;
    }
}
