# 任务附件功能 API 文档

## 概述

任务附件功能与日志附件功能类似，支持文件的上传、下载和预览。通过复用 `AttachmentService`，避免了代码重复。

## 数据库设计

### task_attachment 表

```sql
CREATE TABLE task_attachment (
    attachment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id INT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL UNIQUE,
    file_type VARCHAR(100),
    file_size BIGINT,
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    uploaded_by INT,
    FOREIGN KEY (task_id) REFERENCES task(task_id) ON DELETE CASCADE
);
```

## API 接口

### 1. 下载任务附件

**请求**
```
GET /tasks/attachments/{id}/download
```

**参数**
- `id` (路径参数): 附件 ID
- `token` (查询参数，可选): JWT Token，也可以通过 `Authorization: Bearer <token>` 头传递

**权限**
- 任务的发送者（创建者）可以下载
- 任务的执行者（assignee）可以下载

**响应**
- 成功: 200 OK，返回文件流
- 未授权: 401 UNAUTHORIZED
- 无权限: 403 FORBIDDEN
- 未找到: 404 NOT_FOUND

**示例**
```bash
# 使用查询参数传递 token
curl -O "http://localhost:8080/tasks/attachments/1/download?token=YOUR_JWT_TOKEN"

# 使用 Authorization 头
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     -O "http://localhost:8080/tasks/attachments/1/download"
```

---

### 2. 预览任务附件

**请求**
```
GET /tasks/attachments/{id}/preview
```

**参数**
- `id` (路径参数): 附件 ID
- `token` (查询参数，可选): JWT Token

**权限**
- 与下载接口相同

**响应**
- 成功: 200 OK，返回文件流（Content-Disposition: inline）
- 其他错误码同下载接口

**示例**
```bash
# 在浏览器中预览
http://localhost:8080/tasks/attachments/1/preview?token=YOUR_JWT_TOKEN
```

---

## 代码架构

### 1. 实体层
- `TaskAttachment.java`: 任务附件实体，与 `Task` 关联

### 2. Repository 层
- `TaskAttachmentRepository.java`: 任务附件数据访问接口

### 3. Service 层
- `AttachmentService.java`: **通用附件服务**，处理日志附件和任务附件的下载、预览逻辑
  - `downloadLogAttachment()`: 下载日志附件
  - `previewLogAttachment()`: 预览日志附件
  - `downloadTaskAttachment()`: 下载任务附件
  - `previewTaskAttachment()`: 预览任务附件

### 4. Controller 层
- `TaskController.java`: 任务附件接口
  - `GET /tasks/attachments/{id}/download`
  - `GET /tasks/attachments/{id}/preview`

---

## 代码复用说明

通过创建通用的 `AttachmentService`，我们实现了以下代码复用：

1. **Token 验证逻辑**: `extractToken()` 和 `validateAndExtractUserId()`
2. **文件响应构建**: `buildDownloadResponse()` 和 `buildPreviewResponse()`
3. **中文文件名编码**: 统一使用 RFC 5987 标准

这样避免了在 `LogController` 和 `TaskController` 中重复相同的代码。

---

## 权限控制

### 日志附件
- 只有日志的创建者（employee）可以访问

### 任务附件
- 任务的发送者（sender）可以访问
- 任务的执行者（assignee）可以访问

---

## 文件存储

所有附件（日志和任务）都存储在同一个目录中：
```
uploads/
```

文件名使用 UUID 生成，避免冲突和安全问题。

---

## 注意事项

1. **文件大小限制**: 在 `application.properties` 中配置
   ```properties
   spring.servlet.multipart.max-file-size=10MB
   spring.servlet.multipart.max-request-size=10MB
   ```

2. **支持的文件类型**: 无限制，但建议在前端进行验证

3. **中文文件名**: 已支持，使用 URL 编码

4. **级联删除**: 删除任务时，相关附件会自动删除（数据库和文件）

---

## 未来扩展

如需添加任务附件上传功能，可以参考 `LogService.createLogWithAttachments()` 方法，创建类似的 `TaskService.createTaskWithAttachments()` 方法。
