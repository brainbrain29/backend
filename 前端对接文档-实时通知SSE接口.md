# 实时通知 SSE 接口对接文档

## 📡 接口概述

这是一个 **SSE (Server-Sent Events)** 实时推送接口，用于建立前端与后端的长连接，实现**实时通知推送**功能。

---

## 🎯 这个接口是做什么的？

这是一个**实时推送接口**，用于：
1. **建立长连接**：前端调用后，会与后端保持一个持久连接
2. **接收实时通知**：后端可以主动推送新通知给前端
3. **无需轮询**：不需要前端定时请求，后端有消息会自动推送

**类似于**：微信、钉钉的消息推送机制

### 应用场景

- 📬 新任务分配通知
- 📝 任务状态更新通知
- 💬 评论回复通知
- 📢 系统公告通知
- ⚠️ 重要事项提醒

---

## 🔌 接口信息

### 基本信息

```
方法: GET
路径: /notifications/stream
URL: http://localhost:8080/notifications/stream
认证: Authorization: Bearer {access_token}
响应类型: text/event-stream
超时时间: 30 分钟
```

### 请求头 (Headers)

| 参数 | 值 | 必填 | 说明 |
|------|-----|------|------|
| `Authorization` | `Bearer {access_token}` | ✅ 是 | JWT 访问令牌 |
| `Accept` | `text/event-stream` | 建议 | 指定接收 SSE 流 |

### 请求示例

```http
GET /notifications/stream HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Accept: text/event-stream
```

---

## 📤 响应说明

### 连接成功

**状态码**: `200 OK`

**响应类型**: `text/event-stream`

**首次连接消息**:
```
event: connected
data: SSE connection established
```

### 后续推送消息格式

**通知消息示例**:
```
event: notification
data: {"noticeId":123,"type":1,"content":"您有新的任务分配","senderId":2,"senderName":"李华","createdTime":"2025-11-12T15:30:00"}
```

**消息字段说明**:
```json
{
  "noticeId": 123,                      // 通知ID
  "type": 1,                            // 通知类型
  "content": "您有新的任务分配",          // 通知内容
  "senderId": 2,                        // 发送者ID
  "senderName": "李华",                  // 发送者姓名
  "createdTime": "2025-11-12T15:30:00"  // 创建时间
}
```

**通知类型说明**:
- `1` - 任务分配通知
- `2` - 团队通知
- `3` - 审核通知

---

## 📋 如何保持连接？

### ✅ 正确做法

#### 1. 用户登录后立即建立连接

```dart
// 登录成功后
String token = loginResponse.accessToken;
await sseService.connect(token);  // 建立 SSE 连接
```

#### 2. 保持连接不断开

- 连接建立后，**不要主动关闭**
- 让连接一直保持活跃状态
- 应用在前台时，连接应该始终存在

#### 3. 监听连接状态，断线自动重连

```dart
// 监听连接断开事件
sseService.onDisconnect = () {
  print('连接断开，3秒后重连...');
  Future.delayed(Duration(seconds: 3), () {
    sseService.connect(token);  // 自动重连
  });
};
```

#### 4. 应用退出或登出时断开

```dart
// 用户登出时
await sseService.disconnect();
```

---

## 💻 Flutter 实现示例

### 1. 添加依赖

```yaml
# pubspec.yaml
dependencies:
  http: ^1.1.0
```

### 2. 创建 SSE 服务类

```dart
import 'package:http/http.dart' as http;
import 'dart:convert';

class NotificationSseService {
  static const String baseUrl = 'http://localhost:8080';
  
  String? _accessToken;
  http.Client? _client;
  
  /// 建立 SSE 连接
  Future<void> connect(String accessToken) async {
    _accessToken = accessToken;
    _client = http.Client();
    
    final request = http.Request(
      'GET',
      Uri.parse('$baseUrl/notifications/stream'),
    );
    
    // 添加认证头
    request.headers['Authorization'] = 'Bearer $_accessToken';
    request.headers['Accept'] = 'text/event-stream';
    
    try {
      final response = await _client!.send(request);
      
      if (response.statusCode == 200) {
        print('✅ SSE 连接成功');
        
        // 监听消息流
        response.stream
          .transform(utf8.decoder)
          .transform(LineSplitter())
          .listen(
            (line) {
              if (line.startsWith('data: ')) {
                String data = line.substring(6);
                _handleMessage(data);
              }
            },
            onError: (error) {
              print('❌ 连接错误: $error');
              _reconnect();  // 自动重连
            },
            onDone: () {
              print('⚠️ 连接断开');
              _reconnect();  // 自动重连
            },
          );
      } else {
        print('❌ 连接失败: ${response.statusCode}');
        _reconnect();
      }
    } catch (e) {
      print('❌ 连接异常: $e');
      _reconnect();
    }
  }
  
  /// 处理接收到的消息
  void _handleMessage(String data) {
    if (data == 'SSE connection established') {
      print('✅ 收到连接确认消息');
      return;
    }
    
    try {
      final notification = json.decode(data);
      print('📬 收到新通知: ${notification['content']}');
      
      // 显示通知 UI
      _showNotification(notification);
    } catch (e) {
      print('解析消息失败: $e');
    }
  }
  
  /// 显示通知
  void _showNotification(Map<String, dynamic> notification) {
    // 在这里显示通知 UI
    // 例如：弹出 SnackBar、更新通知列表、显示角标等
    
    // 示例：显示 SnackBar
    // ScaffoldMessenger.of(context).showSnackBar(
    //   SnackBar(content: Text(notification['content'])),
    // );
  }
  
  /// 自动重连
  void _reconnect() {
    Future.delayed(Duration(seconds: 3), () {
      if (_accessToken != null) {
        print('🔄 正在重连...');
        connect(_accessToken!);
      }
    });
  }
  
  /// 断开连接
  void disconnect() {
    _client?.close();
    _client = null;
    _accessToken = null;
    print('🔌 连接已断开');
  }
}
```

---

## 📱 使用流程

### 完整示例

```dart
class NotificationManager {
  final sseService = NotificationSseService();
  
  /// 登录成功后建立连接
  Future<void> onLoginSuccess(String token) async {
    await sseService.connect(token);
  }
  
  /// 应用在前台时保持连接
  void onAppResumed() {
    // 不需要额外操作，连接会自动保持
    // 如果之前断开了，会自动重连
  }
  
  /// 应用进入后台
  void onAppPaused() {
    // 可以选择保持连接或断开
    // 建议：保持连接，以便接收后台通知
  }
  
  /// 登出时断开连接
  void onLogout() {
    sseService.disconnect();
  }
}
```

### 在主应用中集成

```dart
class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> with WidgetsBindingObserver {
  final notificationManager = NotificationManager();
  
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    
    // 如果已登录，建立连接
    _initializeConnection();
  }
  
  Future<void> _initializeConnection() async {
    // 从本地存储获取 token
    String? token = await getStoredToken();
    if (token != null) {
      await notificationManager.onLoginSuccess(token);
    }
  }
  
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      notificationManager.onAppResumed();
    } else if (state == AppLifecycleState.paused) {
      notificationManager.onAppPaused();
    }
  }
  
  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }
  
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: HomeScreen(),
    );
  }
}
```

---

## ⚠️ 注意事项

### 1. 认证要求
- ✅ 必须携带有效的 JWT Token
- ✅ Token 过期后需要重新登录并建立新连接

### 2. 连接管理
- ✅ 不要频繁断开重连，影响性能
- ✅ 网络断开后，3-5秒后自动重连
- ✅ 超时时间为 30 分钟，超时后会自动断开，需要重连

### 3. 错误处理
- ✅ 监听连接错误，自动重连
- ✅ 解析消息失败时，记录日志但不影响连接

### 4. 生命周期管理
- ✅ 登录成功：立即建立连接
- ✅ 应用前台：保持连接
- ✅ 应用后台：可选择保持或断开
- ✅ 用户登出：必须断开连接

---

## 🎯 核心要点总结

| 时机 | 操作 | 说明 |
|------|------|------|
| 用户登录成功 | ✅ 立即建立 SSE 连接 | 调用 `connect(token)` |
| 应用在前台 | ✅ 保持连接不断开 | 无需额外操作 |
| 网络断开 | ✅ 自动重连（3秒后） | 自动处理 |
| 连接超时 | ✅ 自动重连 | 30分钟超时 |
| 用户登出 | ✅ 断开连接 | 调用 `disconnect()` |
| 应用关闭 | ✅ 断开连接 | 自动处理 |

---

## 🔍 调试建议

### 查看连接状态

可以调用监控接口查看在线用户数：

```http
GET /notifications/online-count
Authorization: Bearer {token}
```

**响应示例**:
```json
{
  "onlineCount": 5,
  "onlineUsers": [1, 2, 3, 4, 5]
}
```

### 日志输出

建议在以下时机输出日志：
- ✅ 连接建立成功
- ✅ 收到消息
- ✅ 连接断开
- ✅ 自动重连
- ✅ 连接错误

---

## 📞 联系方式

如有问题，请联系后端开发团队。

---

**简单来说**：登录后连上，一直保持连接，断了就重连，登出才断开！📡✨
