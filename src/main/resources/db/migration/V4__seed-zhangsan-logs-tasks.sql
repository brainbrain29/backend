SET @zhangsan_id := (SELECT employee_id FROM employee WHERE employee_name = '张三' LIMIT 1);
SET @tech_lead_id := (SELECT employee_id FROM employee WHERE employee_name = '赵磊' LIMIT 1);

INSERT INTO task (title, content, start_time, end_time, task_status, task_priority, assignee_id, sender_id, task_type, milestone_id)
VALUES ('Spike: Agent report prompt tuning', 'Design prompt templates for ENTP-style reasoning and concise report output.', '2025-12-05 09:30:00', '2025-12-12 18:00:00', 1, 2, @zhangsan_id, @tech_lead_id, 1, NULL);
SET @task1 := LAST_INSERT_ID();

INSERT INTO task (title, content, start_time, end_time, task_status, task_priority, assignee_id, sender_id, task_type, milestone_id)
VALUES ('Prototype: Task board quick filter', 'Build a prototype for smart filtering (priority/status/assignee) and validate UX quickly.', '2025-12-06 10:00:00', '2025-12-10 18:00:00', 2, 1, @zhangsan_id, @tech_lead_id, 1, NULL);
SET @task2 := LAST_INSERT_ID();

INSERT INTO task (title, content, start_time, end_time, task_status, task_priority, assignee_id, sender_id, task_type, milestone_id)
VALUES ('Experiment: Cloudflare tunnel stability', 'Run a short stress test and document tunnel protocol options and failure patterns.', '2025-12-08 09:00:00', '2025-12-09 18:00:00', 2, 1, @zhangsan_id, @tech_lead_id, 1, NULL);
SET @task3 := LAST_INSERT_ID();

INSERT INTO task (title, content, start_time, end_time, task_status, task_priority, assignee_id, sender_id, task_type, milestone_id)
VALUES ('Refactor: Redis lock usage audit', 'Audit Redisson lock usage, remove dead code paths, and improve timeouts.', '2025-12-09 14:00:00', '2025-12-13 18:00:00', 2, 1, @zhangsan_id, @tech_lead_id, 1, NULL);
SET @task4 := LAST_INSERT_ID();

INSERT INTO task (title, content, start_time, end_time, task_status, task_priority, assignee_id, sender_id, task_type, milestone_id)
VALUES ('Bugfix: Task status edge cases', 'Fix status transitions for overdue tasks and ensure correct status mapping.', '2025-12-11 09:30:00', '2025-12-15 18:00:00', 2, 2, @zhangsan_id, @tech_lead_id, 1, NULL);
SET @task5 := LAST_INSERT_ID();

INSERT INTO task (title, content, start_time, end_time, task_status, task_priority, assignee_id, sender_id, task_type, milestone_id)
VALUES ('Draft: Tech debt cleanup proposal', 'Write a lightweight proposal: refactor priorities, risks, and incremental plan.', '2025-12-14 10:00:00', '2025-12-20 18:00:00', 1, 0, @zhangsan_id, @tech_lead_id, 1, NULL);
SET @task6 := LAST_INSERT_ID();

INSERT INTO task (title, content, start_time, end_time, task_status, task_priority, assignee_id, sender_id, task_type, milestone_id)
VALUES ('Research: LLM evaluation checklist', 'Create a checklist for testing output quality, hallucination, and safety for reports.', '2025-12-16 09:00:00', '2025-12-24 18:00:00', 1, 2, @zhangsan_id, @tech_lead_id, 1, NULL);
SET @task7 := LAST_INSERT_ID();

INSERT INTO task (title, content, start_time, end_time, task_status, task_priority, assignee_id, sender_id, task_type, milestone_id)
VALUES ('Polish: API docs grouping', 'Improve OpenAPI grouping and reduce noisy endpoints for faster onboarding.', '2025-12-18 10:30:00', '2025-12-26 18:00:00', 1, 1, @zhangsan_id, @tech_lead_id, 1, NULL);
SET @task8 := LAST_INSERT_ID();

INSERT INTO task (title, content, start_time, end_time, task_status, task_priority, assignee_id, sender_id, task_type, milestone_id)
VALUES ('Meeting: Cross-team release alignment', 'Align scope and trade-offs with market team and QA; decide what to cut.', '2025-12-19 13:30:00', '2025-12-19 16:30:00', 2, 0, @zhangsan_id, @tech_lead_id, 1, NULL);
SET @task9 := LAST_INSERT_ID();

INSERT INTO task (title, content, start_time, end_time, task_status, task_priority, assignee_id, sender_id, task_type, milestone_id)
VALUES ('Implement: Lightweight observability', 'Add minimal logging for SSE generation and persistence checkpoints.', '2025-12-21 09:30:00', '2025-12-28 18:00:00', 0, 2, @zhangsan_id, @tech_lead_id, 1, NULL);
SET @task10 := LAST_INSERT_ID();

INSERT INTO log (employee_id, task_id, created_time, content, emoji, employee_location) VALUES
 (@zhangsan_id, @task1, '2025-12-05 10:20:00', '上午把周报提示词拆成三版骨架，先把结构钉死避免跑题。边写边想用户会怎么读，优先把信息密度做出来。整体心态很稳。', 3, '上海'),
 (@zhangsan_id, @task1, '2025-12-05 16:40:00', '下午突然灵感爆发：把长期记忆摘要塞进系统上下文可能很有效。赶紧画了一个最小验证流程，越想越兴奋。', 1, '上海'),
 (@zhangsan_id, @task2, '2025-12-06 11:15:00', '把任务看板快速筛选做了个可用原型，状态收敛后交互一下顺了。试了几条典型路径，反馈手感不错。', 1, '上海'),
 (@zhangsan_id, @task2, '2025-12-06 21:10:00', '晚上忍不住又改了一轮细节，边抠边觉得自己有点过度优化。眼睛开始酸了，先收工明天再看。', 4, '上海'),
 (@zhangsan_id, @task3, '2025-12-08 10:05:00', '上午压测隧道时发现 http2 偶发卡顿，数据不够干净让我有点焦躁。先把现象和复现条件记下来，准备补一轮对照。', 2, '上海'),
 (@zhangsan_id, @task3, '2025-12-08 15:30:00', '下午把协议自动协商的参数收敛到一个稳定基线。把下一轮要试的组合列成清单，心里踏实多了。', 3, '上海'),
 (@zhangsan_id, @task4, '2025-12-09 14:20:00', '开始盘点 Redisson 的加锁点位，逐个核对作用域和超时。删掉一个边界不清的锁后，整体逻辑清爽了不少。', 3, '上海'),
 (@zhangsan_id, @task4, '2025-12-10 18:05:00', '今天的重构手感很顺，像是在解一串纠结的绳结。每清掉一层分支就更有成就感，能量回来了。', 1, '上海'),
 (@zhangsan_id, @task5, '2025-12-11 10:10:00', '早上踩到一个状态流转的边界问题，直接把统计口径弄乱了。明明是小坑却很影响结果，我有点火大。先把问题切成最小复现再下手修。', 5, '上海'),
 (@zhangsan_id, @task5, '2025-12-11 17:50:00', '把映射逻辑补齐后顺手做了个自检清单，避免下次再被同类问题偷袭。修完那一刻感觉轻松不少。', 1, '上海'),
 (@zhangsan_id, @task5, '2025-12-12 22:40:00', '回归测出来新回归点，时间一紧压力就上来了。硬顶着把链路跑通，提醒自己别在疲劳状态做大改动。', 2, '上海'),
 (@zhangsan_id, @task1, '2025-12-12 18:20:00', '提示词调参终于收敛，输出更短也更一致了。看到模型按结构走的那一刻特别爽，值得。', 1, '上海'),
 (@zhangsan_id, @task6, '2025-12-14 10:35:00', '把技术债提案先写成故事线：为什么做、做什么、怎么渐进。权衡点很多，但写出来之后思路更清晰了。', 3, '上海'),
 (@zhangsan_id, @task6, '2025-12-15 16:15:00', '下午开脑洞把重构拆成多条可选路径，先发散再收敛是我的舒适区。把选项写给团队看，沟通成本立刻下降。', 1, '上海'),
 (@zhangsan_id, @task7, '2025-12-16 09:40:00', '开始整理 LLM 评测清单，先把幻觉高发点和可验证指标列出来。按模块归类后很顺，节奏稳。', 3, '上海'),
 (@zhangsan_id, @task7, '2025-12-16 19:20:00', '点子太多反而卡住，担心写成聪明但没用的文档。我给自己设了可执行的最小版本，压力才降一点。', 2, '上海'),
 (@zhangsan_id, @task8, '2025-12-18 11:00:00', '把 OpenAPI 文档按使用者画像分组，入口更直观了。看新人能更快找到接口，我挺开心。', 1, '上海'),
 (@zhangsan_id, @task9, '2025-12-19 14:10:00', '对齐发布范围时大家围绕取舍争得很凶，我在中间做方案对比。气氛紧张但必须把风险说透，压力不小。', 2, '上海'),
 (@zhangsan_id, @task9, '2025-12-19 16:20:00', '决定落地后反而平静了，确定性比讨论更治愈。把后续行动项写进任务里，心里就有底。', 3, '上海'),
 (@zhangsan_id, @task6, '2025-12-20 17:45:00', '把提案里的术语砍掉一半，尽量用人话讲清楚。今天更相信清晰胜过复杂，写完挺踏实。', 3, '上海'),
 (@zhangsan_id, @task10, '2025-12-21 11:25:00', '给 SSE 关键节点加了最小可观测性方案，只记录该记录的点。既能定位问题又不吵，刚刚好。', 3, '上海'),
 (@zhangsan_id, @task10, '2025-12-22 23:30:00', '夜里构建又飘了几次，盯到后面脑子开始发木。为了不在疲劳下乱改，我停下来先睡，明早再战。', 4, '上海'),
 (@zhangsan_id, @task7, '2025-12-24 15:05:00', '评测清单补上了红旗项和快速小测试，结构更像一套工具箱了。越写越顺，成品感出来了。', 1, '上海'),
 (@zhangsan_id, NULL, '2025-12-25 10:10:00', '昨天没写日志，今天补一条给周报喂数据。先把本周关键进展和风险点记下来，保持节奏。', 3, '上海');
