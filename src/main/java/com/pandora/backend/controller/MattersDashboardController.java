package com.pandora.backend.controller;

import com.pandora.backend.dto.*;
import com.pandora.backend.service.AdminService;
import com.pandora.backend.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 十大重要事项监控Dashboard控制器
 */
@RestController
@RequestMapping("/matters-dashboard")
@CrossOrigin(origins = "*")
public class MattersDashboardController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private DashboardService dashboardService;

    /**
     * 获取重要事项仪表板概览数据
     */
    @GetMapping("/overview")
    public ResponseEntity<?> getDashboardOverview() {
        try {
            List<ImportantMatterDTO> allMatters = adminService.getAllImportantMatters();

            Map<String, Object> overview = new HashMap<>();

            // 基础统计
            long totalCount = allMatters.size();
            long completedCount = allMatters.stream().mapToLong(m -> m.getMatterStatus() == 2 ? 1 : 0).sum();
            long inProgressCount = allMatters.stream().mapToLong(m -> m.getMatterStatus() == 1 ? 1 : 0).sum();
            long pendingCount = allMatters.stream().mapToLong(m -> m.getMatterStatus() == 0 ? 1 : 0).sum();

            overview.put("totalCount", totalCount);
            overview.put("completedCount", completedCount);
            overview.put("inProgressCount", inProgressCount);
            overview.put("pendingCount", pendingCount);
            overview.put("completionRate", totalCount > 0 ? (completedCount * 100.0 / totalCount) : 0.0);

            // 按部门统计
            Map<String, Long> departmentStats = allMatters.stream()
                    .collect(Collectors.groupingBy(
                            m -> m.getDepartmentName() != null ? m.getDepartmentName() : "未分配",
                            Collectors.counting()));
            overview.put("departmentStats", departmentStats);

            // 最近更新
            List<ImportantMatterDTO> recentlyUpdated = allMatters.stream()
                    .sorted((a, b) -> {
                        LocalDateTime aTime = a.getPublishTime();
                        LocalDateTime bTime = b.getPublishTime();
                        return bTime.compareTo(aTime);
                    })
                    .limit(5)
                    .collect(Collectors.toList());
            overview.put("recentlyUpdated", recentlyUpdated);

            // 即将到期事项
            List<ImportantMatterDTO> upcomingDeadlines = getUpcomingDeadlines(allMatters, 7);
            overview.put("upcomingDeadlines", upcomingDeadlines);

            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取重要事项趋势数据
     */
    @GetMapping("/trends")
    public ResponseEntity<?> getTrends(@RequestParam(defaultValue = "30") int days) {
        try {
            List<ImportantMatterDTO> allMatters = adminService.getAllImportantMatters();
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusDays(days);

            // 按日期统计创建和完成情况
            Map<String, Map<String, Long>> dailyStats = new HashMap<>();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            for (int i = 0; i < days; i++) {
                LocalDate date = startDate.plusDays(i).toLocalDate();
                String dateStr = date.format(dateFormatter);

                Map<String, Long> dayStats = new HashMap<>();
                dayStats.put("created", 0L);
                dayStats.put("completed", 0L);

                dailyStats.put(dateStr, dayStats);
            }

            // 统计创建情况
            allMatters.stream()
                    .filter(m -> m.getPublishTime() != null &&
                            !m.getPublishTime().isBefore(startDate) &&
                            !m.getPublishTime().isAfter(endDate))
                    .forEach(m -> {
                        String dateStr = m.getPublishTime().toLocalDate().format(dateFormatter);
                        dailyStats.get(dateStr).put("created",
                                dailyStats.get(dateStr).get("created") + 1);
                    });

            // 统计完成情况（使用publishTime作为完成时间）
            allMatters.stream()
                    .filter(m -> m.getMatterStatus() == 2 &&
                            !m.getPublishTime().isBefore(startDate) &&
                            !m.getPublishTime().isAfter(endDate))
                    .forEach(m -> {
                        String dateStr = m.getPublishTime().toLocalDate().format(dateFormatter);
                        dailyStats.get(dateStr).put("completed",
                                dailyStats.get(dateStr).get("completed") + 1);
                    });

            return ResponseEntity.ok(dailyStats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取实时监控数据
     */
    @GetMapping("/realtime")
    public ResponseEntity<?> getRealtimeData() {
        try {
            List<ImportantMatterDTO> allMatters = adminService.getAllImportantMatters();

            Map<String, Object> realtime = new HashMap<>();

            // 实时统计
            realtime.put("totalActive", allMatters.stream()
                    .mapToLong(m -> m.getMatterStatus() < 2 ? 1 : 0).sum());
            realtime.put("overdueCount", getOverdueCount(allMatters));
            realtime.put("todayUpdated", getTodayUpdatedCount(allMatters));
            realtime.put("currentTime", LocalDateTime.now());

            // 状态分布
            Map<String, Long> statusDistribution = allMatters.stream()
                    .collect(Collectors.groupingBy(
                            m -> {
                                switch (m.getMatterStatus()) {
                                    case 0:
                                        return "待处理";
                                    case 1:
                                        return "进行中";
                                    case 2:
                                        return "已完成";
                                    default:
                                        return "未知";
                                }
                            },
                            Collectors.counting()));
            realtime.put("statusDistribution", statusDistribution);

            // 紧急事项（截止日期在3天内且未完成）
            List<ImportantMatterDTO> urgentMatters = allMatters.stream()
                    .filter(m -> m.getMatterStatus() < 2 && m.getDeadline() != null)
                    .filter(m -> m.getDeadline().isBefore(LocalDateTime.now().plusDays(3)))
                    .sorted(Comparator.comparing(ImportantMatterDTO::getDeadline))
                    .collect(Collectors.toList());
            realtime.put("urgentMatters", urgentMatters);

            return ResponseEntity.ok(realtime);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取重要事项详情（带过滤和搜索）
     */
    @GetMapping("/matters")
    public ResponseEntity<?> getMatters(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            List<ImportantMatterDTO> allMatters = adminService.getAllImportantMatters();

            // 应用过滤
            List<ImportantMatterDTO> filteredMatters = allMatters.stream()
                    .filter(m -> status == null || m.getMatterStatus().equals(status))
                    .filter(m -> departmentId == null ||
                            (m.getAssigneeId() != null && m.getAssigneeId().equals(departmentId)))
                    .filter(m -> keyword == null ||
                            m.getContent().toLowerCase().contains(keyword.toLowerCase()))
                    .collect(Collectors.toList());

            // 分页
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, filteredMatters.size());
            List<ImportantMatterDTO> pagedMatters = filteredMatters.subList(
                    Math.min(fromIndex, filteredMatters.size()), toIndex);

            Map<String, Object> result = new HashMap<>();
            result.put("matters", pagedMatters);
            result.put("totalCount", filteredMatters.size());
            result.put("currentPage", page);
            result.put("totalPages", (int) Math.ceil((double) filteredMatters.size() / size));
            result.put("pageSize", size);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取单个重要事项详情
     */
    @GetMapping("/matters/{id}")
    public ResponseEntity<?> getMatter(@PathVariable Integer id) {
        try {
            List<ImportantMatterDTO> allMatters = adminService.getAllImportantMatters();
            ImportantMatterDTO matter = allMatters.stream()
                    .filter(m -> m.getMatterId().equals(id))
                    .findFirst()
                    .orElse(null);

            if (matter != null) {
                return ResponseEntity.ok(matter);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取单个重要任务详情
     */
    @GetMapping("/tasks/{id}")
    public ResponseEntity<?> getTask(@PathVariable Integer id) {
        try {
            List<ImportantTaskDTO> allTasks = adminService.getAllImportantTasks();
            ImportantTaskDTO task = allTasks.stream()
                    .filter(t -> t.getTaskId().equals(id))
                    .findFirst()
                    .orElse(null);

            if (task != null) {
                return ResponseEntity.ok(task);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取预警和提醒信息
     */
    @GetMapping("/alerts")
    public ResponseEntity<?> getAlerts() {
        try {
            List<ImportantMatterDTO> allMatters = adminService.getAllImportantMatters();

            Map<String, Object> alerts = new HashMap<>();

            // 逾期事项
            List<ImportantMatterDTO> overdueMatters = allMatters.stream()
                    .filter(m -> m.getMatterStatus() < 2 &&
                            m.getDeadline() != null &&
                            m.getDeadline().isBefore(LocalDateTime.now()))
                    .collect(Collectors.toList());
            alerts.put("overdueMatters", overdueMatters);
            alerts.put("overdueCount", overdueMatters.size());

            // 今日到期事项
            List<ImportantMatterDTO> todayDeadlines = allMatters.stream()
                    .filter(m -> m.getMatterStatus() < 2 &&
                            m.getDeadline() != null &&
                            m.getDeadline().toLocalDate().equals(LocalDate.now()))
                    .collect(Collectors.toList());
            alerts.put("todayDeadlines", todayDeadlines);
            alerts.put("todayDeadlineCount", todayDeadlines.size());

            // 长时间无更新事项（超过7天未更新）
            List<ImportantMatterDTO> stagnantMatters = allMatters.stream()
                    .filter(m -> m.getMatterStatus() < 2)
                    .filter(m -> {
                        LocalDateTime lastUpdate = m.getPublishTime();
                        return lastUpdate.isBefore(LocalDateTime.now().minusDays(7));
                    })
                    .collect(Collectors.toList());
            alerts.put("stagnantMatters", stagnantMatters);
            alerts.put("stagnantCount", stagnantMatters.size());

            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 辅助方法
    private List<ImportantMatterDTO> getUpcomingDeadlines(List<ImportantMatterDTO> matters, int days) {
        return matters.stream()
                .filter(m -> m.getMatterStatus() < 2 && m.getDeadline() != null)
                .filter(m -> m.getDeadline().isBefore(LocalDateTime.now().plusDays(days)))
                .sorted(Comparator.comparing(ImportantMatterDTO::getDeadline))
                .collect(Collectors.toList());
    }

    private long getOverdueCount(List<ImportantMatterDTO> matters) {
        return matters.stream()
                .filter(m -> m.getMatterStatus() < 2 &&
                        m.getDeadline() != null &&
                        m.getDeadline().isBefore(LocalDateTime.now()))
                .count();
    }

    private long getTodayUpdatedCount(List<ImportantMatterDTO> matters) {
        return matters.stream()
                .filter(m -> {
                    LocalDateTime lastUpdate = m.getPublishTime();
                    return lastUpdate.toLocalDate().equals(LocalDate.now());
                })
                .count();
    }
}