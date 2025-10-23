# Task API 测试指南

## 前提条件
1. 确保数据库已启动并正确配置
2. 确保数据库中有测试数据（至少有employeeId=1和2的员工记录）
3. 启动Spring Boot应用

## 快速开始

### 1️⃣ 启动应用
```powershell
# 在项目根目录运行
./mvnw spring-boot:run
```

### 2️⃣ 方法一：使用PowerShell脚本测试（最快）
```powershell
# 运行测试脚本
./test-api.ps1
```

### 3️⃣ 方法二：使用Maven运行测试
```powershell
# 运行所有测试
./mvnw test

# 只运行TaskControllerTest
./mvnw test -Dtest=TaskControllerTest
```

### 4️⃣ 方法三：使用Postman

下载并导入以下Postman集合：

#### 创建任务
```
POST http://localhost:8080/tasks
Content-Type: application/json

{
  "title": "完成用户登录功能",
  "content": "实现用户登录功能，包括验证和token生成",
  "startTime": "2025-10-21T09:00:00",
  "endTime": "2025-10-25T18:00:00",
  "taskStatus": 0,
  "taskPriority": 1,
  "senderId": 1,
  "assigneeId": 2,
  "taskType": 1,
  "createdByWho": 1,
  "milestoneId": 1
}
```

#### 查询所有任务
```
GET http://localhost:8080/tasks
```

#### 查询单个任务
```
GET http://localhost:8080/tasks/{id}
```

#### 更新任务
```
PUT http://localhost:8080/tasks/{id}
Content-Type: application/json

{
  "title": "更新后的标题",
  "taskStatus": 1
}
```

#### 删除任务
```
DELETE http://localhost:8080/tasks/{id}
```

#### 高级查询
```
GET http://localhost:8080/tasks/sender/{senderId}      # 按发送者查询
GET http://localhost:8080/tasks/assignee/{assigneeId}  # 按执行者查询
GET http://localhost:8080/tasks/milestone/{milestoneId} # 按里程碑查询
GET http://localhost:8080/tasks/status/{taskStatus}    # 按状态查询
```

## API 端点汇总

| 功能 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 创建任务 | POST | `/tasks` | 创建新任务 |
| 查询所有任务 | GET | `/tasks` | 获取所有任务列表 |
| 查询单个任务 | GET | `/tasks/{id}` | 根据ID获取任务详情 |
| 更新任务 | PUT | `/tasks/{id}` | 更新指定任务 |
| 删除任务 | DELETE | `/tasks/{id}` | 删除指定任务 |
| 按发送者查询 | GET | `/tasks/sender/{senderId}` | 获取某人发送的所有任务 |
| 按执行者查询 | GET | `/tasks/assignee/{assigneeId}` | 获取分配给某人的所有任务 |
| 按里程碑查询 | GET | `/tasks/milestone/{milestoneId}` | 获取某里程碑下的所有任务 |
| 按状态查询 | GET | `/tasks/status/{taskStatus}` | 获取某状态的所有任务 |

## 响应状态码

| 状态码 | 说明 |
|--------|------|
| 200 OK | 查询/更新成功 |
| 201 Created | 创建成功 |
| 204 No Content | 删除成功 |
| 400 Bad Request | 请求参数错误 |
| 404 Not Found | 资源不存在 |

## 字段说明

### TaskDTO 字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskId | Integer | - | 任务ID（创建时不需要） |
| title | String | ✓ | 任务标题（最长64字符） |
| content | String | - | 任务内容（最长255字符） |
| startTime | LocalDateTime | - | 开始时间 |
| endTime | LocalDateTime | - | 结束时间 |
| taskStatus | Byte | ✓ | 任务状态 |
| taskPriority | Byte | ✓ | 优先级 |
| assigneeId | Integer | - | 执行者ID |
| senderId | Integer | ✓ | 发送者ID |
| taskType | Byte | ✓ | 任务类型 |
| createdByWho | Byte | ✓ | 创建者 |
| milestoneId | Integer | - | 里程碑ID（可选） |

## 常见问题

### Q: 创建任务时返回400错误
A: 检查是否提供了必填字段（senderId, title, taskStatus, taskPriority, taskType, createdByWho），以及senderId对应的员工是否存在。

### Q: 创建任务时提示"Sender not found"
A: 数据库中不存在对应的员工记录，请先创建员工或使用已存在的employeeId。

### Q: 时间格式错误
A: 使用ISO-8601格式：`2025-10-21T09:00:00`

## 测试数据示例

确保数据库中至少有以下测试数据：

```sql
-- 员工数据（如果没有）
INSERT INTO employee (employee_id, employee_name, gender, email, position, emp_password, phone) 
VALUES 
(1, '张三', 0, 'zhangsan@example.com', 1, 'password123', '13800138000'),
(2, '李四', 1, 'lisi@example.com', 2, 'password456', '13900139000');

-- 里程碑数据（如果需要测试milestone关联）
INSERT INTO milestone (milestone_id, title, milestone_no, project_id) 
VALUES (1, '第一阶段', 1, 1);
```

## 使用curl测试（Linux/Mac/Git Bash）

```bash
# 创建任务
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "title": "测试任务",
    "content": "这是测试",
    "taskStatus": 0,
    "taskPriority": 1,
    "senderId": 1,
    "assigneeId": 2,
    "taskType": 1,
    "createdByWho": 1
  }'

# 查询所有任务
curl http://localhost:8080/tasks

# 查询单个任务
curl http://localhost:8080/tasks/1

# 更新任务
curl -X PUT http://localhost:8080/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{"title": "更新后的任务", "taskStatus": 1}'

# 删除任务
curl -X DELETE http://localhost:8080/tasks/1
```


