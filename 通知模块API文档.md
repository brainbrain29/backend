# 通知模块 API 文档

> **更新时间**: 2025-11-18  
> **后端版本**: v2.0  
> **重要变更**: 新增批量确认接口，优化通知状态管理

---

## 📋 目录

1. [通知状态说明](#通知状态说明)
2. [SSE 实时推送](#sse-实时推送)
3. [批量确认收到通知（新增）](#批量确认收到通知新增)
4. [获取未读通知](#获取未读通知)
5. [获取所有通知](#获取所有通知)
6. [检查未读通知数量](#检查未读通知数量)
7. [标记单个通知为已读](#标记单个通知为已读)
8. [标记所有通知为已读](#标记所有通知为已读)
9. [删除通知](#删除通知)
10. [完整前端实现示例](#完整前端实现示例)

---

## 通知状态说明

### 状态流转

```
创建通知
    ↓
NOT_RECEIVED (未接收) - 用户离线或未确认
    ↓ 前端调用批量确认接口
NOT_VIEWED (未查看) - 已接收但未查看
    ↓ 用户点击查看
VIEWED (已查看) - 已查看
```

### 状态枚举

| 状态值 | 状态名称 | 中文描述 | 说明 |
|--------|---------|---------|------|
| `2` | `NOT_RECEIVED` | 未接收 | 通知已创建，但用户未确认收到 |
| `0` | `NOT_VIEWED` | 未查看 | 用户已确认收到，但未查看详情 |
| `1` | `VIEWED` | 已查看 | 用户已查看通知详情 |

---

## SSE 实时推送

### 接口信息

- **URL**: `GET /notifications/stream`
- **协议**: Server-Sent Events (SSE)
- **认证**: 需要 JWT Token

### 请求示例

```javascript
const eventSource = new EventSource('/notifications/stream', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
});
```

### 事件类型

#### 1. `notification` - 新通知推送

**触发时机**: 
- 用户上线时，推送所有未接收的通知
- 用户在线时，实时推送新通知

**数据格式**:
```json
{
  "noticeId": 123,
  "title": "任务分配",
  "content": "您被分配了新任务: 完成项目文档",
  "senderName": "张三",
  "createdTime": "2025-11-18T15:30:00",
  "status": "未接收",
  "relatedId": 456
}
```

### ⚠️ 重要：`relatedId` 字段说明

**`relatedId` 的作用**：关联业务对象的 ID，用于跳转到对应的详情页面。

#### **根据通知类型判断 `relatedId` 的含义**

| 通知标题/类型 | `relatedId` 含义 | 前端应调用的接口 | 跳转页面 |
|-------------|----------------|----------------|----------|
| **"任务分配"** / **"任务更新"** | 任务ID (`taskId`) | `GET /tasks/{taskId}` | 任务详情页 |
| **"重要事项提醒"** / **"重要事项更新"** | 重要事项ID (`importantMatterId`) | `GET /important-matters/{id}` | 重要事项详情页 |
| **"项目通知"** | 项目ID (`projectId`) | `GET /projects/{projectId}` | 项目详情页 |
| **其他系统通知** | `null` 或 `0` | 无需调用 | 通知列表页 |

#### **前端处理示例**

```javascript
eventSource.addEventListener('notification', (event) => {
  const notice = JSON.parse(event.data);
  
  // 1. 显示通知
  displayNotification(notice);
  
  // 2. 用户点击通知时，根据 relatedId 跳转
  notice.onClick = () => {
    if (notice.relatedId && notice.relatedId > 0) {
      // 根据通知标题判断类型
      if (notice.title.includes('任务')) {
        // 跳转到任务详情
        navigateTo(`/tasks/${notice.relatedId}`);
      } else if (notice.title.includes('重要事项')) {
        // 跳转到重要事项详情
        navigateTo(`/important-matters/${notice.relatedId}`);
      } else if (notice.title.includes('项目')) {
        // 跳转到项目详情
        navigateTo(`/projects/${notice.relatedId}`);
      }
    } else {
      // 没有关联对象，跳转到通知列表
      navigateTo('/notifications');
    }
  };
  
  // 3. 加入待确认队列
  pendingConfirmNotices.add(notice.noticeId);
  scheduleBatchConfirm();
});
```

#### **推荐做法**

1. **保存通知类型字段**（如果后端提供）：
   ```json
   {
     "noticeId": 123,
     "noticeType": "TASK",  // 或 "IMPORTANT_MATTER", "PROJECT"
     "relatedId": 456
   }
   ```

2. **根据 `noticeType` 而不是标题判断**：
   ```javascript
   switch (notice.noticeType) {
     case 'TASK':
       navigateTo(`/tasks/${notice.relatedId}`);
       break;
     case 'IMPORTANT_MATTER':
       navigateTo(`/important-matters/${notice.relatedId}`);
       break;
     case 'PROJECT':
       navigateTo(`/projects/${notice.relatedId}`);
       break;
     default:
       navigateTo('/notifications');
   }
   ```

3. **容错处理**：
   - 如果 `relatedId` 为 `null` 或 `0`，跳转到通知列表
   - 如果调用详情接口返回 404，提示"关联内容已删除"

**前端处理**:
```javascript
eventSource.addEventListener('notification', (event) => {
  const notice = JSON.parse(event.data);
  
  // 1. 显示通知
  displayNotification(notice);
  
  // 2. 加入待确认队列（重要！）
  pendingConfirmNotices.add(notice.noticeId);
  
  // 3. 延迟批量确认
  scheduleBatchConfirm();
});
```

#### 2. `heartbeat` - 心跳检测

**触发时机**: 每 30 秒一次

**数据**: `"ping"`

**前端处理**:
```javascript
eventSource.addEventListener('heartbeat', (event) => {
  console.log('💓 心跳:', event.data);
});
```

---

### ⚠️ SSE 连接失败和重连机制

#### **常见失败原因**

| 错误类型 | 后端日志 | 原因 | 解决方案 |
|---------|---------|------|----------|
| **Token 无效或已过期** | `JWT Token 无效或已过期` | Token 超过 1 小时有效期 | 刷新 Token 后重连 |
| **Token 为空** | `未提供 JWT Token` | 请求头未携带 Token | 检查 localStorage 中的 Token |
| **Token 格式错误** | `Token 格式错误` | Token 格式不正确 | 重新登录 |
| **网络断开** | 无日志 | 网络连接中断 | 等待网络恢复后重连 |
| **服务器重启** | 无日志 | 后端服务重启 | 自动重连 |

#### **后端返回的错误信息**

当 SSE 连接失败时，后端会返回 **401 Unauthorized** 并记录日志：

```
2025-11-18T19:46:35.006+08:00  WARN 48672 --- [backend] [0.0-8080-exec-8] 
c.pandora.backend.filter.JwtAuthFilter : 请求路径: /notifications/stream - JWT Token 无效或已过期
```

**这说明**：
- Token 已过期（超过 1 小时）
- Token 格式错误
- Token 签名验证失败

#### **完整的重连机制（重要！）**

```javascript
let eventSource = null;
let reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 5;
const BASE_RECONNECT_DELAY = 1000; // 1秒

async function initSSE() {
  const token = localStorage.getItem('accessToken');
  
  // 1. 检查 Token 是否存在
  if (!token) {
    console.error('❌ 未登录，无法建立 SSE 连接');
    navigateToLogin();
    return;
  }
  
  // 2. 关闭旧连接
  if (eventSource) {
    eventSource.close();
    eventSource = null;
  }
  
  console.log('📡 正在建立 SSE 连接...');
  
  try {
    // 3. 创建新连接
    eventSource = new EventSource('/notifications/stream', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    // 4. 连接成功
    eventSource.onopen = () => {
      console.log('✅ SSE 连接已建立');
      reconnectAttempts = 0; // 重置重连次数
    };
    
    // 5. 监听通知
    eventSource.addEventListener('notification', handleNotification);
    eventSource.addEventListener('heartbeat', handleHeartbeat);
    
    // 6. 连接错误处理（关键！）
    eventSource.onerror = async (error) => {
      console.error('❌ SSE 连接错误:', error);
      
      // 检查连接状态
      if (eventSource.readyState === EventSource.CLOSED) {
        console.log('⚠️ SSE 连接已关闭');
        
        // 尝试判断错误原因
        const errorReason = await diagnoseSSEError();
        console.log('🔍 错误原因:', errorReason);
        
        // 根据错误原因处理
        if (errorReason === 'TOKEN_EXPIRED') {
          console.log('🔄 Token 已过期，尝试刷新...');
          
          // 刷新 Token
          const refreshSuccess = await refreshToken();
          
          if (refreshSuccess) {
            console.log('✅ Token 刷新成功，重新建立连接');
            await initSSE();
          } else {
            console.error('❌ Token 刷新失败，需要重新登录');
            logout();
          }
        } else if (errorReason === 'NETWORK_ERROR') {
          console.log('🔄 网络错误，尝试重连...');
          scheduleReconnect();
        } else {
          console.log('🔄 未知错误，尝试重连...');
          scheduleReconnect();
        }
      }
    };
    
  } catch (error) {
    console.error('❌ 创建 SSE 连接失败:', error);
    scheduleReconnect();
  }
}

// 诊断 SSE 错误原因
async function diagnoseSSEError() {
  // 1. 检查网络连接
  if (!navigator.onLine) {
    return 'NETWORK_ERROR';
  }
  
  // 2. 尝试发送一个简单的请求验证 Token
  try {
    const response = await fetch('/notices/check', {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
      }
    });
    
    if (response.status === 401) {
      return 'TOKEN_EXPIRED';
    } else if (response.ok) {
      return 'SERVER_ERROR';
    } else {
      return 'UNKNOWN_ERROR';
    }
  } catch (error) {
    return 'NETWORK_ERROR';
  }
}

// 计划重连（指数退避）
function scheduleReconnect() {
  if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
    console.error('❌ 重连次数过多，请检查网络或重新登录');
    alert('通知连接失败，请重新登录');
    logout();
    return;
  }
  
  reconnectAttempts++;
  
  // 指数退避：1秒、2秒、4秒、8秒、16秒
  const delay = Math.min(
    BASE_RECONNECT_DELAY * Math.pow(2, reconnectAttempts - 1),
    30000 // 最多 30 秒
  );
  
  console.log(`🔄 ${delay/1000}秒后尝试第 ${reconnectAttempts} 次重连...`);
  
  setTimeout(() => {
    initSSE();
  }, delay);
}
```

#### **重连策略总结**

| 错误原因 | 处理方式 | 重连延迟 |
|---------|---------|----------|
| **Token 过期** | 刷新 Token → 立即重连 | 0 秒 |
| **网络错误** | 指数退避重连 | 1s → 2s → 4s → 8s → 16s |
| **服务器错误** | 指数退避重连 | 1s → 2s → 4s → 8s → 16s |
| **重连失败 5 次** | 提示用户重新登录 | 停止重连 |

#### **最佳实践**

1. **优先刷新 Token**：如果怀疑是 Token 过期，先尝试刷新
2. **使用指数退避**：避免频繁重连浪费资源
3. **限制重连次数**：超过 5 次失败后提示用户
4. **监听网络状态**：网络恢复时立即重连
   ```javascript
   window.addEventListener('online', () => {
     console.log('🌐 网络已恢复，重新建立 SSE 连接');
     reconnectAttempts = 0;
     initSSE();
   });
   ```
5. **应用恢复时检查连接**：
   ```javascript
   document.addEventListener('visibilitychange', () => {
     if (document.visibilityState === 'visible') {
       if (!eventSource || eventSource.readyState === EventSource.CLOSED) {
         console.log('📱 应用恢复，重新建立 SSE 连接');
         initSSE();
       }
     }
   });
   ```

---

## 批量确认收到通知（新增）

### ⭐ 重要接口

> **这是新增的核心接口！前端收到通知后必须调用此接口确认。**

### 接口信息

- **URL**: `POST /notices/batch-confirm-received`
- **方法**: `POST`
- **认证**: 需要 JWT Token（userId 从 Token 中自动解析）
- **Content-Type**: `application/json`

### 调用时机

**前端通过 SSE 收到通知后，延迟 500ms 批量确认**

```javascript
// 收到通知
eventSource.addEventListener('notification', (event) => {
  const notice = JSON.parse(event.data);
  
  // 1. 显示通知
  displayNotification(notice);
  
  // 2. 加入待确认队列
  pendingConfirmNotices.add(notice.noticeId);
  
  // 3. 延迟 500ms 批量确认（收集这段时间内的所有通知）
  scheduleBatchConfirm();
});

// 延迟批量确认函数
function scheduleBatchConfirm() {
  if (confirmTimer) clearTimeout(confirmTimer);
  
  confirmTimer = setTimeout(() => {
    if (pendingConfirmNotices.size > 0) {
      batchConfirmReceived(Array.from(pendingConfirmNotices));
      pendingConfirmNotices.clear();
    }
  }, 500);
}
```

### 请求参数

**Headers**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Body**:
```json
{
  "noticeIds": [1, 2, 3, 4, 5]
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `noticeIds` | `Array<Integer>` | 是 | 需要确认的通知ID列表 |

**注意**: 
- ❌ **不需要传递 `userId`**，后端会从 JWT Token 中自动解析
- ✅ 只需要传递通知ID列表

### 返回数据

**成功响应** (200):
```json
{
  "success": true,
  "confirmedCount": 3,
  "failedNoticeIds": [4, 5]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `success` | `Boolean` | 操作是否成功 |
| `confirmedCount` | `Integer` | 成功确认的通知数量 |
| `failedNoticeIds` | `Array<Integer>` | 失败的通知ID列表（空数组表示全部成功） |

**失败响应** (400/401/500):
```json
{
  "success": false,
  "error": "错误信息"
}
```

### 完整示例

```javascript
async function batchConfirmReceived(noticeIds) {
  if (!noticeIds || noticeIds.length === 0) return;
  
  console.log('📤 批量确认通知:', noticeIds);
  
  try {
    const response = await fetch('/notices/batch-confirm-received', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: JSON.stringify({ noticeIds })
    });
    
    if (response.ok) {
      const result = await response.json();
      console.log(`✅ 成功确认 ${result.confirmedCount} 条通知`);
      
      if (result.failedNoticeIds.length > 0) {
        console.warn(`⚠️ 失败的通知ID: ${result.failedNoticeIds}`);
      }
      
      return result;
    } else {
      console.error('❌ 批量确认失败:', response.status);
    }
    
  } catch (error) {
    console.error('❌ 网络错误:', error);
    // 可以保存到本地存储，下次重试
    saveToRetryQueue(noticeIds);
  }
}
```

---

### ⚠️ 批量确认的调用策略（重要！）

#### **推荐策略：适度重试，避免浪费资源**

**原则**：
- ✅ **鼓励重试失败的通知**：确保通知状态正确更新
- ❌ **避免过度调用**：不要无限重试或频繁调用

#### **重试机制**

```javascript
const MAX_RETRY_ATTEMPTS = 3;  // 最多重试 3 次
const RETRY_DELAY = 5000;      // 每次重试间隔 5 秒
const retryQueue = new Map();  // 存储失败的通知和重试次数

async function batchConfirmReceived(noticeIds) {
  if (!noticeIds || noticeIds.length === 0) return;
  
  console.log('📤 批量确认通知:', noticeIds);
  
  try {
    const response = await fetch('/notices/batch-confirm-received', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
      },
      body: JSON.stringify({ noticeIds })
    });
    
    if (response.ok) {
      const result = await response.json();
      console.log(`✅ 成功确认 ${result.confirmedCount} 条通知`);
      
      // 处理失败的通知
      if (result.failedNoticeIds && result.failedNoticeIds.length > 0) {
        console.warn(`⚠️ ${result.failedNoticeIds.length} 条通知确认失败:`, result.failedNoticeIds);
        
        // 加入重试队列
        result.failedNoticeIds.forEach(noticeId => {
          const attempts = retryQueue.get(noticeId) || 0;
          
          if (attempts < MAX_RETRY_ATTEMPTS) {
            retryQueue.set(noticeId, attempts + 1);
            console.log(`🔄 通知 ${noticeId} 将在 ${RETRY_DELAY/1000} 秒后重试（第 ${attempts + 1} 次）`);
          } else {
            console.error(`❌ 通知 ${noticeId} 已重试 ${MAX_RETRY_ATTEMPTS} 次，放弃`);
            retryQueue.delete(noticeId);
          }
        });
        
        // 延迟重试
        if (retryQueue.size > 0) {
          setTimeout(() => {
            retryFailedNotices();
          }, RETRY_DELAY);
        }
      }
      
      return result;
    } else if (response.status === 401) {
      console.error('❌ Token 已过期，刷新后重试');
      const refreshSuccess = await refreshToken();
      if (refreshSuccess) {
        // Token 刷新成功，立即重试
        return batchConfirmReceived(noticeIds);
      }
    } else {
      console.error('❌ 批量确认失败:', response.status);
      // 服务器错误，加入重试队列
      noticeIds.forEach(noticeId => {
        const attempts = retryQueue.get(noticeId) || 0;
        if (attempts < MAX_RETRY_ATTEMPTS) {
          retryQueue.set(noticeId, attempts + 1);
        }
      });
    }
    
  } catch (error) {
    console.error('❌ 网络错误:', error);
    // 网络错误，加入重试队列
    noticeIds.forEach(noticeId => {
      const attempts = retryQueue.get(noticeId) || 0;
      if (attempts < MAX_RETRY_ATTEMPTS) {
        retryQueue.set(noticeId, attempts + 1);
      }
    });
  }
}

// 重试失败的通知
function retryFailedNotices() {
  if (retryQueue.size === 0) return;
  
  const noticeIds = Array.from(retryQueue.keys());
  console.log(`🔄 重试 ${noticeIds.length} 条失败的通知`);
  
  batchConfirmReceived(noticeIds);
}
```

#### **调用频率限制**

**避免过度调用的策略**：

1. **延迟批量确认**（推荐）：
   ```javascript
   // 收到通知后延迟 500ms 批量确认
   // 这样可以收集这段时间内的所有通知，减少调用次数
   let confirmTimer = null;
   const pendingConfirmNotices = new Set();
   
   function scheduleBatchConfirm() {
     if (confirmTimer) clearTimeout(confirmTimer);
     
     confirmTimer = setTimeout(() => {
       if (pendingConfirmNotices.size > 0) {
         batchConfirmReceived(Array.from(pendingConfirmNotices));
         pendingConfirmNotices.clear();
       }
     }, 500);  // 延迟 500ms
   }
   ```

2. **限制重试次数**：
   - 每个通知最多重试 **3 次**
   - 超过 3 次后放弃，记录到日志

3. **增加重试间隔**：
   - 第 1 次重试：5 秒后
   - 第 2 次重试：10 秒后
   - 第 3 次重试：20 秒后
   ```javascript
   const retryDelay = RETRY_DELAY * Math.pow(2, attempts - 1);
   ```

4. **避免重复确认**：
   ```javascript
   const confirmedNotices = new Set(); // 已确认的通知
   
   function batchConfirmReceived(noticeIds) {
     // 过滤掉已确认的通知
     const newNoticeIds = noticeIds.filter(id => !confirmedNotices.has(id));
     
     if (newNoticeIds.length === 0) {
       console.log('⚠️ 所有通知已确认，跳过');
       return;
     }
     
     // 调用接口...
     // 成功后加入已确认集合
     newNoticeIds.forEach(id => confirmedNotices.add(id));
   }
   ```

#### **失败原因分析**

| 失败原因 | `failedNoticeIds` 包含的通知 | 是否重试 | 处理方式 |
|---------|---------------------------|---------|----------|
| **通知不存在** | 该通知已被删除 | ❌ 不重试 | 记录日志，忽略 |
| **状态不是 NOT_RECEIVED** | 该通知已被确认过 | ❌ 不重试 | 记录日志，忽略 |
| **不属于当前用户** | 通知不属于该用户 | ❌ 不重试 | 记录日志，忽略 |
| **网络错误** | 所有通知 | ✅ 重试 | 延迟重试 |
| **服务器错误** | 所有通知 | ✅ 重试 | 延迟重试 |

#### **最佳实践总结**

| 实践 | 说明 | 好处 |
|------|------|------|
| **延迟批量确认** | 收到通知后延迟 500ms 确认 | 减少调用次数 |
| **限制重试次数** | 每个通知最多重试 3 次 | 避免无限重试 |
| **指数退避** | 重试间隔逐渐增加 | 减轻服务器压力 |
| **去重** | 避免重复确认同一通知 | 节省资源 |
| **本地持久化** | 失败的通知保存到 localStorage | 应用重启后继续重试 |

#### **示例：完整的重试队列管理**

```javascript
// 从 localStorage 加载重试队列
function loadRetryQueue() {
  const saved = localStorage.getItem('noticeRetryQueue');
  if (saved) {
    const data = JSON.parse(saved);
    data.forEach(([noticeId, attempts]) => {
      retryQueue.set(noticeId, attempts);
    });
    console.log(`📥 加载了 ${retryQueue.size} 条待重试的通知`);
  }
}

// 保存重试队列到 localStorage
function saveRetryQueue() {
  const data = Array.from(retryQueue.entries());
  localStorage.setItem('noticeRetryQueue', JSON.stringify(data));
}

// 应用启动时加载重试队列
window.addEventListener('DOMContentLoaded', () => {
  loadRetryQueue();
  if (retryQueue.size > 0) {
    console.log('🔄 发现待重试的通知，5秒后重试');
    setTimeout(retryFailedNotices, 5000);
  }
});

// 应用关闭时保存重试队列
window.addEventListener('beforeunload', () => {
  saveRetryQueue();
});
```

**推荐配置**：
- 延迟批量确认：**500ms**
- 最大重试次数：**3 次**
- 重试间隔：**5s → 10s → 20s**（指数退避）
- 本地持久化：**保存到 localStorage**

这样既能保证通知状态正确更新，又不会过度消耗后端资源。

### 使用场景

#### 场景1: 全部成功
```javascript
// 前端发送
{ "noticeIds": [1, 2, 3] }

// 后端返回
{
  "success": true,
  "confirmedCount": 3,
  "failedNoticeIds": []  // 空数组
}
```

#### 场景2: 部分失败
```javascript
// 前端发送
{ "noticeIds": [1, 2, 3, 4, 5] }

// 后端返回
{
  "success": true,
  "confirmedCount": 3,
  "failedNoticeIds": [4, 5]  // 通知4和5失败（可能不属于该用户）
}
```

---

## 获取未读通知

### 接口信息

- **URL**: `GET /notices/me/unread`
- **方法**: `GET`
- **认证**: 需要 JWT Token

### 调用时机

- 用户打开通知列表页面
- 需要显示未读通知时

### 请求示例

```javascript
const response = await fetch('/notices/me/unread', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
});

const notices = await response.json();
```

### 返回数据

**成功响应** (200):
```json
[
  {
    "noticeId": 123,
    "title": "任务分配",
    "content": "您被分配了新任务: 完成项目文档",
    "senderName": "张三",
    "createdTime": "2025-11-18T15:30:00",
    "status": "未查看",
    "relatedId": 456
  },
  {
    "noticeId": 124,
    "title": "重要事项",
    "content": "公司发布了新的重要事项",
    "senderName": "李四",
    "createdTime": "2025-11-18T16:00:00",
    "status": "未查看",
    "relatedId": 789
  }
]
```

---

## 获取所有通知

### 接口信息

- **URL**: `GET /notices/me/all`
- **方法**: `GET`
- **认证**: 需要 JWT Token

### 调用时机

- 用户查看所有通知（包括已读和未读）

### 请求示例

```javascript
const response = await fetch('/notices/me/all', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
});

const notices = await response.json();
```

### 返回数据

与"获取未读通知"相同，但包含所有状态的通知。

---

## 检查未读通知数量

### 接口信息

- **URL**: `GET /notices/check`
- **方法**: `GET`
- **认证**: 需要 JWT Token

### 调用时机

- 页面加载时
- 定时轮询（建议间隔 30-60 秒）
- 用户操作后需要更新未读数量时

### 请求示例

```javascript
const response = await fetch('/notices/check', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
});

const status = await response.json();
console.log(`未读通知数量: ${status.unreadCount}`);
```

### 返回数据

**成功响应** (200):
```json
{
  "unreadCount": 5,
  "hasUnread": true
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `unreadCount` | `Integer` | 未读通知数量 |
| `hasUnread` | `Boolean` | 是否有未读通知 |

---

## 标记单个通知为已读

### 接口信息

- **URL**: `PUT /notices/mark-read/{noticeId}`
- **方法**: `PUT`
- **认证**: 需要 JWT Token

### 调用时机

- 用户点击查看通知详情时
- 自动调用，无需用户手动操作

### 请求示例

```javascript
// 用户点击通知
function viewNoticeDetail(noticeId) {
  // 1. 标记为已读
  markAsRead(noticeId);
  
  // 2. 跳转到详情页
  navigateToDetail(noticeId);
}

async function markAsRead(noticeId) {
  await fetch(`/notices/mark-read/${noticeId}`, {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  });
}
```

### 返回数据

**成功响应** (200): 无返回内容

---

## 标记所有通知为已读

### 接口信息

- **URL**: `PUT /notices/mark-all-read`
- **方法**: `PUT`
- **认证**: 需要 JWT Token

### 调用时机

- 用户点击"全部已读"按钮

### 请求示例

```javascript
async function markAllAsRead() {
  await fetch('/notices/mark-all-read', {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  });
  
  // 刷新通知列表
  refreshNoticeList();
}
```

### 返回数据

**成功响应** (200): 无返回内容

---

## 删除通知

### 接口信息

- **URL**: `DELETE /notices/{noticeId}`
- **方法**: `DELETE`
- **认证**: 需要 JWT Token

### 调用时机

- 用户点击删除通知按钮

### 请求示例

```javascript
async function deleteNotice(noticeId) {
  const confirmed = confirm('确定要删除这条通知吗？');
  if (!confirmed) return;
  
  await fetch(`/notices/${noticeId}`, {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
  });
  
  // 刷新通知列表
  refreshNoticeList();
}
```

### 返回数据

**成功响应** (200): 无返回内容

---

## 完整前端实现示例

### 1. 初始化 SSE 连接

```javascript
// ========== 全局变量 ==========
let eventSource = null;
const pendingConfirmNotices = new Set();
let confirmTimer = null;

// ========== 建立 SSE 连接 ==========
function initSSE() {
  const token = localStorage.getItem('token');
  if (!token) {
    console.error('未登录，无法建立 SSE 连接');
    return;
  }
  
  eventSource = new EventSource('/notifications/stream', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  // 监听通知事件
  eventSource.addEventListener('notification', handleNotification);
  
  // 监听心跳事件
  eventSource.addEventListener('heartbeat', (event) => {
    console.log('💓 心跳:', event.data);
  });
  
  // 连接打开
  eventSource.onopen = () => {
    console.log('✅ SSE 连接已建立');
  };
  
  // 连接错误
  eventSource.onerror = (error) => {
    console.error('❌ SSE 连接错误:', error);
    // 可以实现重连逻辑
  };
}

// ========== 处理通知 ==========
function handleNotification(event) {
  try {
    const notice = JSON.parse(event.data);
    console.log('📥 收到通知:', notice);
    
    // 1. 显示通知（UI 更新）
    displayNotification(notice);
    
    // 2. 加入待确认队列
    pendingConfirmNotices.add(notice.noticeId);
    
    // 3. 延迟批量确认
    scheduleBatchConfirm();
    
    // 4. 显示桌面通知（可选）
    if (Notification.permission === 'granted') {
      new Notification(notice.title, {
        body: notice.content,
        icon: '/logo.png'
      });
    }
    
  } catch (error) {
    console.error('❌ 解析通知失败:', error);
  }
}

// ========== 延迟批量确认 ==========
function scheduleBatchConfirm() {
  // 清除之前的定时器
  if (confirmTimer) {
    clearTimeout(confirmTimer);
  }
  
  // 设置新的定时器：500ms 后执行批量确认
  confirmTimer = setTimeout(() => {
    if (pendingConfirmNotices.size > 0) {
      const noticeIds = Array.from(pendingConfirmNotices);
      batchConfirmReceived(noticeIds);
      pendingConfirmNotices.clear();
    }
  }, 500);
}

// ========== 批量确认接口 ==========
async function batchConfirmReceived(noticeIds) {
  if (!noticeIds || noticeIds.length === 0) return;
  
  console.log('📤 批量确认通知:', noticeIds);
  
  try {
    const response = await fetch('/notices/batch-confirm-received', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: JSON.stringify({ noticeIds })
    });
    
    if (response.ok) {
      const result = await response.json();
      console.log(`✅ 成功确认 ${result.confirmedCount} 条通知`);
      
      if (result.failedNoticeIds.length > 0) {
        console.warn(`⚠️ 失败的通知ID: ${result.failedNoticeIds}`);
        // 可以选择重试
      }
      
      // 更新未读数量
      updateUnreadCount();
      
      return result;
    } else {
      console.error('❌ 批量确认失败:', response.status);
      // 保存到本地存储，下次重试
      saveToRetryQueue(noticeIds);
    }
    
  } catch (error) {
    console.error('❌ 网络错误:', error);
    saveToRetryQueue(noticeIds);
  }
}

// ========== 显示通知（UI 更新）==========
function displayNotification(notice) {
  // 在通知列表中显示
  const noticeList = document.getElementById('notice-list');
  const noticeItem = document.createElement('div');
  noticeItem.className = 'notice-item unread';
  noticeItem.dataset.noticeId = notice.noticeId;
  noticeItem.innerHTML = `
    <div class="notice-title">${notice.title}</div>
    <div class="notice-content">${notice.content}</div>
    <div class="notice-time">${formatTime(notice.createdTime)}</div>
  `;
  
  // 点击查看详情
  noticeItem.addEventListener('click', () => {
    viewNoticeDetail(notice.noticeId, notice.relatedId);
  });
  
  noticeList.prepend(noticeItem);
  
  // 更新未读数量
  updateUnreadCount();
}

// ========== 查看通知详情 ==========
async function viewNoticeDetail(noticeId, relatedId) {
  // 1. 标记为已读
  await markAsRead(noticeId);
  
  // 2. 根据 relatedId 跳转到对应页面
  // 例如：任务详情、重要事项详情等
  navigateToDetail(relatedId);
}

// ========== 标记为已读 ==========
async function markAsRead(noticeId) {
  try {
    await fetch(`/notices/mark-read/${noticeId}`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    });
    
    // 更新 UI
    const noticeItem = document.querySelector(`[data-notice-id="${noticeId}"]`);
    if (noticeItem) {
      noticeItem.classList.remove('unread');
      noticeItem.classList.add('read');
    }
    
    // 更新未读数量
    updateUnreadCount();
    
  } catch (error) {
    console.error('❌ 标记已读失败:', error);
  }
}

// ========== 更新未读数量 ==========
async function updateUnreadCount() {
  try {
    const response = await fetch('/notices/check', {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    });
    
    const status = await response.json();
    
    // 更新 UI 上的未读数量徽章
    const badge = document.getElementById('unread-badge');
    if (status.unreadCount > 0) {
      badge.textContent = status.unreadCount;
      badge.style.display = 'block';
    } else {
      badge.style.display = 'none';
    }
    
  } catch (error) {
    console.error('❌ 获取未读数量失败:', error);
  }
}

// ========== 重试队列（容错机制）==========
function saveToRetryQueue(noticeIds) {
  const retryQueue = JSON.parse(localStorage.getItem('retry_confirm_queue') || '[]');
  retryQueue.push(...noticeIds);
  localStorage.setItem('retry_confirm_queue', JSON.stringify(retryQueue));
  console.log('💾 已保存到重试队列:', noticeIds);
}

// 页面加载时，重试之前失败的确认
window.addEventListener('load', () => {
  const retryQueue = JSON.parse(localStorage.getItem('retry_confirm_queue') || '[]');
  if (retryQueue.length > 0) {
    console.log('🔄 重试之前失败的确认:', retryQueue);
    batchConfirmReceived(retryQueue);
    localStorage.removeItem('retry_confirm_queue');
  }
});

// ========== 页面关闭前，确认所有待确认的通知 ==========
window.addEventListener('beforeunload', () => {
  if (pendingConfirmNotices.size > 0) {
    const noticeIds = Array.from(pendingConfirmNotices);
    const data = JSON.stringify({ noticeIds });
    
    // 使用 sendBeacon 发送（即使页面关闭也能发送）
    navigator.sendBeacon('/notices/batch-confirm-received', data);
  }
});

// ========== 初始化 ==========
document.addEventListener('DOMContentLoaded', () => {
  // 1. 建立 SSE 连接
  initSSE();
  
  // 2. 更新未读数量
  updateUnreadCount();
  
  // 3. 定时更新未读数量（可选）
  setInterval(updateUnreadCount, 60000); // 每分钟更新一次
});
```

---

## 🔔 重要提醒

### 1. **必须调用批量确认接口**

前端收到通知后，**必须调用** `POST /notices/batch-confirm-received` 接口，否则通知状态会一直保持 `NOT_RECEIVED`（未接收）。

### 2. **批量确认的时机**

- ✅ **推荐**: 收到通知后延迟 500ms 批量确认
- ❌ **不推荐**: 每收到一条通知就立即确认（会产生大量请求）

### 3. **userId 不需要传递**

所有接口的 `userId` 都从 JWT Token 中自动解析，前端**不需要**也**不应该**传递 `userId` 参数。

### 4. **错误处理**

- 网络错误时，应该保存到本地存储，下次重试
- 使用 `beforeunload` 事件确保页面关闭前确认所有通知

### 5. **SSE 连接管理**

- 页面加载时建立 SSE 连接
- 页面关闭时自动断开
- 连接错误时可以实现重连逻辑

---

## 📞 联系方式

如有疑问，请联系后端开发团队。

**文档版本**: v2.0  
**最后更新**: 2025-11-18
