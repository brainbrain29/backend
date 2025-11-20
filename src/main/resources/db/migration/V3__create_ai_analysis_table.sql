-- 创建 AI 分析结果表
CREATE TABLE IF NOT EXISTS ai_analysis (
    analysis_id INT AUTO_INCREMENT PRIMARY KEY COMMENT 'AI分析记录ID',
    employee_id INT NOT NULL COMMENT '员工ID',
    created_time DATETIME NOT NULL COMMENT '分析生成时间',
    period_start DATETIME NOT NULL COMMENT '分析周期开始时间',
    period_end DATETIME NOT NULL COMMENT '分析周期结束时间',
    work_rhythm_advice TEXT COMMENT '工作节奏建议',
    emotion_health_reminder TEXT COMMENT '情绪健康提醒',
    task_completion_trend TEXT COMMENT '任务完成趋势',
    full_content TEXT COMMENT '完整的AI回复内容',
    log_count INT DEFAULT 0 COMMENT '分析时使用的日志数量',
    task_count INT DEFAULT 0 COMMENT '分析时使用的任务数量',
    FOREIGN KEY (employee_id) REFERENCES employee(employee_id) ON DELETE CASCADE,
    INDEX idx_employee_created (employee_id, created_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI工作分析结果表';
