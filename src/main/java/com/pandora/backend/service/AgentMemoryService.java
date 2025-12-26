package com.pandora.backend.service;

import com.pandora.backend.entity.AgentMemory;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.repository.AgentMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AgentMemoryService {

    private final AgentMemoryRepository agentMemoryRepository;

    public List<AgentMemory> getLatestMemories(final Integer employeeId) {
        return agentMemoryRepository.findTop10ByEmployeeEmployeeIdOrderByCreatedTimeDesc(employeeId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AgentMemory appendMemory(final Integer employeeId, final String memoryType, final String content) {
        final LocalDateTime now = LocalDateTime.now();

        log.info("[agent-plan] appendMemory start employeeId={} type={} contentLength={}", employeeId, memoryType,
                content == null ? 0 : content.length());

        final AgentMemory memory = new AgentMemory();
        final Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        memory.setEmployee(employee);
        memory.setMemoryType(memoryType);
        memory.setContent(content);
        memory.setCreatedTime(now);
        memory.setUpdatedTime(now);

        final AgentMemory saved = agentMemoryRepository.save(memory);
        log.info("[agent-plan] appendMemory done employeeId={} type={} memoryId={}", employeeId, memoryType,
                saved.getMemoryId());
        return saved;
    }
}
