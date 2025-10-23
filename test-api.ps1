# Task API 测试脚本
# 确保后端服务已启动在 http://localhost:8080

$baseUrl = "http://localhost:8080/tasks"

Write-Host "====== Task API 测试 ======" -ForegroundColor Green

# 1. 创建任务
Write-Host "`n1. 创建任务 (POST /tasks)" -ForegroundColor Yellow
$createTaskBody = @{
    title = "测试任务"
    content = "这是一个测试任务"
    startTime = "2025-10-21T09:00:00"
    endTime = "2025-10-25T18:00:00"
    taskStatus = 0
    taskPriority = 1
    senderId = 1
    assigneeId = 2
    taskType = 1
    createdByWho = 1
    milestoneId = 1
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri $baseUrl -Method Post -Body $createTaskBody -ContentType "application/json"
    Write-Host "成功创建任务，ID: $($response.taskId)" -ForegroundColor Green
    $taskId = $response.taskId
} catch {
    Write-Host "创建失败: $_" -ForegroundColor Red
    $taskId = 1  # 使用默认ID继续测试
}

# 2. 查询所有任务
Write-Host "`n2. 查询所有任务 (GET /tasks)" -ForegroundColor Yellow
try {
    $tasks = Invoke-RestMethod -Uri $baseUrl -Method Get
    Write-Host "成功查询，共 $($tasks.Count) 个任务" -ForegroundColor Green
} catch {
    Write-Host "查询失败: $_" -ForegroundColor Red
}

# 3. 查询单个任务
Write-Host "`n3. 查询单个任务 (GET /tasks/$taskId)" -ForegroundColor Yellow
try {
    $task = Invoke-RestMethod -Uri "$baseUrl/$taskId" -Method Get
    Write-Host "任务标题: $($task.title)" -ForegroundColor Green
} catch {
    Write-Host "查询失败: $_" -ForegroundColor Red
}

# 4. 更新任务
Write-Host "`n4. 更新任务 (PUT /tasks/$taskId)" -ForegroundColor Yellow
$updateTaskBody = @{
    title = "更新后的测试任务"
    taskStatus = 1
} | ConvertTo-Json

try {
    $updatedTask = Invoke-RestMethod -Uri "$baseUrl/$taskId" -Method Put -Body $updateTaskBody -ContentType "application/json"
    Write-Host "成功更新任务: $($updatedTask.title)" -ForegroundColor Green
} catch {
    Write-Host "更新失败: $_" -ForegroundColor Red
}

# 5. 按发送者查询
Write-Host "`n5. 按发送者查询 (GET /tasks/sender/1)" -ForegroundColor Yellow
try {
    $senderTasks = Invoke-RestMethod -Uri "$baseUrl/sender/1" -Method Get
    Write-Host "发送者1的任务数: $($senderTasks.Count)" -ForegroundColor Green
} catch {
    Write-Host "查询失败: $_" -ForegroundColor Red
}

# 6. 按执行者查询
Write-Host "`n6. 按执行者查询 (GET /tasks/assignee/2)" -ForegroundColor Yellow
try {
    $assigneeTasks = Invoke-RestMethod -Uri "$baseUrl/assignee/2" -Method Get
    Write-Host "执行者2的任务数: $($assigneeTasks.Count)" -ForegroundColor Green
} catch {
    Write-Host "查询失败: $_" -ForegroundColor Red
}

# 7. 删除任务（可选，取消注释以执行）
# Write-Host "`n7. 删除任务 (DELETE /tasks/$taskId)" -ForegroundColor Yellow
# try {
#     Invoke-RestMethod -Uri "$baseUrl/$taskId" -Method Delete
#     Write-Host "成功删除任务" -ForegroundColor Green
# } catch {
#     Write-Host "删除失败: $_" -ForegroundColor Red
# }

Write-Host "`n====== 测试完成 ======" -ForegroundColor Green


