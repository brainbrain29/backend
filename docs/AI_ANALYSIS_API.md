# AI 工作分析模块 API 文档

## 概述

AI 工作分析模块基于员工近三周的工作日志和任务数据，使用 GLM-4 大语言模型实时生成个性化的工作分析报告，包括：
- **任务完成趋势**：总结任务完成率、优先级分布、延期情况等关键指标
- **工作节奏建议**：基于工作效率、任务完成情况和工作时间分布给出节奏调整建议
- **情绪健康提醒**：分析日志中的心情变化趋势，结合 MBTI 性格类型给出情绪管理建议

## 接口信息

### 1. 获取 AI 工作分析（流式响应）

**接口路径**
```
GET /glm/analysis/stream
```

**接口说明**
- 自动从 JWT Token 中获取当前登录员工的 ID
- 基于员工近三周（21天）的日志和任务数据生成分析
- 使用 Server-Sent Events (SSE) 实现流式响应，实时返回 AI 生成的内容
- 每个员工只保存最新的一份分析结果，旧的会被自动删除

**请求头**
```
Authorization: Bearer <JWT_TOKEN>
```

**请求参数**
无需传递参数，员工 ID 自动从 Token 中解析

**响应格式**
- Content-Type: `text/event-stream`
- 流式返回，每次返回一小段文本内容

**SSE 事件类型**

1. **message 事件**：返回 AI 生成的文本片段
```
event: message
data: 【工作节奏建议】
```

2. **done 事件**：表示流式响应完成
```
event: done
data: [DONE]
```

**响应内容格式**

AI 分析报告按照以下三个主题结构组织，每个主题不超过 50 字：

```
【工作节奏建议】
近期工作效率较高，但工作至深夜的情况较多。建议合理分配工作时间段，尝试在白天高效完成任务，保证充足的休息时间。

【情绪健康提醒】
日志显示压力和生气情绪出现较多，ENTP性格通常追求创新与自由，可能对重复或限制性任务感到不满。建议在面对压力时，适当调整任务类型，保持工作新鲜感，及时进行情绪释放。

【任务完成趋势】
已完成任务中，高优先级任务占比较高，且多数任务能按时完成。目前有一个紧急任务和一个普通任务正在进行中，注意紧急任务的截止时间，确保优先完成。
```

**错误响应**

1. **未登录或 Token 无效**
```json
{
  "error": "无法获取用户ID，请确保已登录"
}
```

2. **服务器错误**
- SSE 连接会自动关闭
- 前端需要监听 `error` 事件处理异常

## 前端集成示例

### JavaScript (原生)

```javascript
// 获取 AI 工作分析
function getAiAnalysis() {
  const token = localStorage.getItem('token'); // 从本地存储获取 token
  
  const eventSource = new EventSource('/glm/analysis/stream', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });

  let fullContent = '';

  // 监听消息事件
  eventSource.addEventListener('message', (event) => {
    const chunk = event.data;
    fullContent += chunk;
    
    // 实时更新 UI
    document.getElementById('analysis-content').textContent = fullContent;
  });

  // 监听完成事件
  eventSource.addEventListener('done', (event) => {
    console.log('AI 分析完成');
    eventSource.close();
    
    // 解析三个主题
    const analysis = parseAnalysis(fullContent);
    displayAnalysis(analysis);
  });

  // 监听错误事件
  eventSource.addEventListener('error', (error) => {
    console.error('SSE 错误:', error);
    eventSource.close();
    alert('获取 AI 分析失败，请稍后重试');
  });
}

// 解析分析内容
function parseAnalysis(content) {
  const rhythmMatch = content.match(/【工作节奏建议】\s*([\s\S]*?)(?=【|$)/);
  const emotionMatch = content.match(/【情绪健康提醒】\s*([\s\S]*?)(?=【|$)/);
  const trendMatch = content.match(/【任务完成趋势】\s*([\s\S]*?)(?=【|$)/);

  return {
    workRhythmAdvice: rhythmMatch ? rhythmMatch[1].trim() : '',
    emotionHealthReminder: emotionMatch ? emotionMatch[1].trim() : '',
    taskCompletionTrend: trendMatch ? trendMatch[1].trim() : ''
  };
}

// 显示分析结果
function displayAnalysis(analysis) {
  document.getElementById('work-rhythm').textContent = analysis.workRhythmAdvice;
  document.getElementById('emotion-health').textContent = analysis.emotionHealthReminder;
  document.getElementById('task-trend').textContent = analysis.taskCompletionTrend;
}
```

### React 示例

```jsx
import { useState, useEffect } from 'react';

function AiAnalysis() {
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchAnalysis = () => {
    setLoading(true);
    setError(null);
    setContent('');

    const token = localStorage.getItem('token');
    const eventSource = new EventSource('/glm/analysis/stream', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });

    eventSource.addEventListener('message', (event) => {
      setContent(prev => prev + event.data);
    });

    eventSource.addEventListener('done', () => {
      setLoading(false);
      eventSource.close();
    });

    eventSource.addEventListener('error', (err) => {
      setError('获取分析失败');
      setLoading(false);
      eventSource.close();
    });
  };

  return (
    <div className="ai-analysis">
      <button onClick={fetchAnalysis} disabled={loading}>
        {loading ? '分析中...' : '获取 AI 工作分析'}
      </button>
      
      {error && <div className="error">{error}</div>}
      
      <div className="analysis-content">
        <pre>{content}</pre>
      </div>
    </div>
  );
}
```

### Vue 3 示例

```vue
<template>
  <div class="ai-analysis">
    <button @click="fetchAnalysis" :disabled="loading">
      {{ loading ? '分析中...' : '获取 AI 工作分析' }}
    </button>
    
    <div v-if="error" class="error">{{ error }}</div>
    
    <div class="analysis-content">
      <pre>{{ content }}</pre>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';

const content = ref('');
const loading = ref(false);
const error = ref(null);

const fetchAnalysis = () => {
  loading.value = true;
  error.value = null;
  content.value = '';

  const token = localStorage.getItem('token');
  const eventSource = new EventSource('/glm/analysis/stream', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });

  eventSource.addEventListener('message', (event) => {
    content.value += event.data;
  });

  eventSource.addEventListener('done', () => {
    loading.value = false;
    eventSource.close();
  });

  eventSource.addEventListener('error', () => {
    error.value = '获取分析失败';
    loading.value = false;
    eventSource.close();
  });
};
</script>
```

## 数据库存储

### 表结构：ai_analysis

| 字段名 | 类型 | 说明 |
|--------|------|------|
| analysis_id | INT | 主键，自增 |
| employee_id | INT | 员工ID（外键） |
| created_time | DATETIME | 分析生成时间 |
| period_start | DATETIME | 分析周期开始时间（近三周起始） |
| period_end | DATETIME | 分析周期结束时间 |
| work_rhythm_advice | TEXT | 工作节奏建议 |
| emotion_health_reminder | TEXT | 情绪健康提醒 |
| task_completion_trend | TEXT | 任务完成趋势 |
| full_content | TEXT | 完整的 AI 回复内容 |
| log_count | INT | 分析时使用的日志数量 |
| task_count | INT | 分析时使用的任务数量 |

### 存储规则

- **每个员工只保存一份最新的分析**：当生成新的分析时，会自动删除该员工的所有旧分析记录
- **自动保存**：流式响应完成后，系统会自动将完整内容保存到数据库
- **字段解析**：系统会尝试从完整内容中解析出三个主题，分别存储到对应字段

## 注意事项

1. **Token 认证**：所有请求必须携带有效的 JWT Token
2. **超时设置**：SSE 连接超时时间为 90 秒（1.5 分钟）
3. **数据要求**：分析基于近三周的日志和任务数据，如果数据不足，AI 会明确说明
4. **字符编码**：响应内容使用 UTF-8 编码，可能包含中文和 emoji 表情
5. **并发限制**：建议控制并发请求数量，避免对 AI API 造成过大压力
6. **错误处理**：前端需要妥善处理 SSE 连接错误和超时情况

## 测试接口

### 测试用接口（带用户ID参数）

```
GET /glm/test/stream/context?userId={employeeId}&message={message}
```

此接口用于开发测试，可以直接指定用户ID，无需 Token 认证。

**示例**
```
GET /glm/test/stream/context?userId=15&message=帮我分析一下最近的工作情况
```

## MySQL 显示问题说明

### 问题现象
在 MySQL 命令行中查询 `ai_analysis` 表时，可能看到很多"横线"或乱码。

### 原因
- 数据中包含 emoji 表情符号（如 📊、💡、📈）
- MySQL 命令行客户端的字符编码不支持这些特殊字符

### 解决方案

1. **使用 MySQL Workbench 或其他 GUI 工具**：这些工具通常能正确显示 UTF-8 字符

2. **设置 MySQL 客户端字符集**：
```sql
SET NAMES utf8mb4;
SELECT * FROM ai_analysis;
```

3. **查询时转换编码**：
```sql
SELECT 
  analysis_id,
  employee_id,
  created_time,
  CONVERT(work_rhythm_advice USING utf8mb4) as work_rhythm_advice,
  CONVERT(emotion_health_reminder USING utf8mb4) as emotion_health_reminder,
  CONVERT(task_completion_trend USING utf8mb4) as task_completion_trend
FROM ai_analysis;
```

4. **数据本身是正常的**：通过 API 返回给前端的数据完全正常，只是命令行显示问题

## 版本历史

- **v1.0** (2025-11-19)
  - 初始版本
  - 实现基于 Token 的 AI 工作分析接口
  - 支持流式响应
  - 每个员工只保存一份最新分析
