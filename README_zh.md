# Netty Spring Boot Starter

[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5+-green.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

一个功能完善的 Spring Boot Starter，用于基于 Netty 构建高性能网络应用。
支持 TCP/UDP/HTTP/WebSocket 协议，采用声明式配置和注解路由。

[English](README.md)

## 特性亮点

- **一套配置启动多协议服务** - 通过 YAML 配置同时启动 TCP/HTTP/WebSocket/UDP 服务
- **注解式路由** - 像 Spring MVC / Spring Messaging 一样开发处理器
- **Profile 协议栈模板** - 一键启用协议栈，Feature 配置化叠加能力
- **客户端支持** - 声明式客户端接口，支持连接池、自动重连、心跳保活
- **生产就绪** - 内置可观测性（metrics/health），支持优雅关闭
- **高度可扩展** - 高级用户可通过 Configurer/Codec/RouteResolver 扩展

## 架构设计

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           application.yml                               │
│  ┌─────────────────────────────┐    ┌─────────────────────────────────┐ │
│  │    netty.servers[*]         │    │    netty.clients[*]             │ │
│  │    服务端配置                 │    │    客户端配置                    │ │
│  └─────────────────────────────┘    └─────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
                    │                              │
                    ▼                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      netty-spring-boot-context                          │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                         共享组件                                   │  │
│  │  Codec │ Profile │ InboundMessage │ OutboundMessage │ NettyContext │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  ┌────────────────────────┐        ┌────────────────────────────────┐  │
│  │      服务端组件          │        │         客户端组件              │  │
│  │  ┌──────────────────┐  │        │  ┌────────────────────────┐   │  │
│  │  │ ServerOrchestrator│  │        │  │ ClientOrchestrator     │   │  │
│  │  │ DispatcherHandler │  │        │  │ ConnectionPool         │   │  │
│  │  │ Router            │  │        │  │ ReconnectManager       │   │  │
│  │  │ Dispatcher        │  │        │  │ HeartbeatManager       │   │  │
│  │  │ @NettyController  │  │        │  │ RequestInvoker         │   │  │
│  │  └──────────────────┘  │        │  │ @NettyClient           │   │  │
│  └────────────────────────┘        │  └────────────────────────┘   │  │
│                                     └────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                    │                              │
                    ▼                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     netty-spring-boot-actuator                          │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  Metrics │ Health Check │ Endpoint │ Connection Stats │ Tracing   │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           Netty 运行时                                   │
│  ┌─────────────┐    ┌─────────────┐    ┌────────────────────────────┐  │
│  │  BossGroup  │ →  │ WorkerGroup │ →  │  ChannelPipeline           │  │
│  └─────────────┘    └─────────────┘    │  (阶段化 Handlers)          │  │
│                                         └────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

## 模块结构

```
netty-spring-boot/
├── netty-spring-boot-context/        # 核心 API、注解、接口（服务端 + 客户端）
├── netty-spring-boot-actuator/       # 监控指标、健康检查、端点
├── netty-spring-boot-autoconfigure/  # 自动配置（服务端 + 客户端）
├── netty-spring-boot-starter/        # Starter 依赖聚合
└── samples/                          # 示例应用
    └── sample-tcp-json/
```

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.childrengreens</groupId>
    <artifactId>netty-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 2. 配置服务器

```yaml
netty:
  enabled: true
  defaults:
    threads:
      boss: 1
      worker: 0              # 0 = CPU核心数 * 2
    transport:
      prefer: AUTO           # AUTO/NIO/EPOLL/KQUEUE
    shutdown:
      graceful: true
      quietPeriodMs: 200
      timeoutMs: 3000

  servers:
    - name: tcp-server
      transport: TCP
      host: 0.0.0.0
      port: 9000
      profile: tcp-lengthfield-json
      routing:
        mode: MESSAGE_TYPE   # 按消息类型路由
      features:
        idle:
          enabled: true
          readSeconds: 60
        logging:
          enabled: true
          level: DEBUG

    - name: http-server
      transport: HTTP
      port: 8080
      profile: http1-json
      routing:
        mode: PATH           # 按路径路由

    - name: ws-server
      transport: HTTP
      port: 8081
      profile: websocket
      routing:
        mode: WS_PATH

    - name: udp-server
      transport: UDP
      port: 7000
      profile: udp-json
      routing:
        mode: MESSAGE_TYPE

  observability:
    metrics: true
    health: true
```

### 3. 编写处理器

**TCP/UDP 消息处理器：**

```java
@NettyMessageController
public class MessageHandler {

    @NettyMessageMapping("ping")
    public Map<String, Object> handlePing(NettyContext context) {
        return Map.of(
            "type", "pong",
            "timestamp", System.currentTimeMillis(),
            "channelId", context.getChannelId()
        );
    }

    @NettyMessageMapping("order")
    public CompletableFuture<OrderResponse> handleOrder(OrderRequest request, NettyContext context) {
        // 支持异步处理
        return orderService.processAsync(request);
    }
}
```

**HTTP 控制器：**

```java
@NettyController(path = "/api")
public class HttpController {

    @NettyHttpGet("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }

    @NettyHttpGet("/users/{id}")
    public User getUser(@PathVar("id") Long id, @Query("fields") String fields) {
        return userService.findById(id);
    }

    @NettyHttpPost("/users")
    public User createUser(@Body User user) {
        return userService.save(user);
    }

    @NettyHttpPut("/users/{id}")
    public User updateUser(@PathVar("id") Long id, @Body User user) {
        return userService.update(id, user);
    }

    @NettyHttpDelete("/users/{id}")
    public void deleteUser(@PathVar("id") Long id) {
        userService.delete(id);
    }
}
```

**WebSocket 处理器：**

```java
@NettyController(path = "/ws")
public class WebSocketHandler {

    @NettyWsOnOpen("/chat")
    public void onOpen(NettyContext context) {
        log.info("WebSocket 已连接: {}", context.getChannelId());
    }

    @NettyWsOnText("/chat")
    public String onMessage(String message, NettyContext context) {
        return "回显: " + message;
    }

    @NettyWsOnBinary("/chat")
    public byte[] onBinary(byte[] data, NettyContext context) {
        return data; // 回显二进制数据
    }

    @NettyWsOnClose("/chat")
    public void onClose(NettyContext context) {
        log.info("WebSocket 已断开: {}", context.getChannelId());
    }
}
```

## 客户端使用

### 1. 配置客户端

```yaml
netty:
  clients:
    - name: order-service
      host: 127.0.0.1
      port: 9000
      profile: tcp-lengthfield-json
      pool:
        maxConnections: 10
        minIdle: 2
        maxIdleMs: 60000
        acquireTimeoutMs: 5000
      reconnect:
        enabled: true
        initialDelayMs: 1000
        maxDelayMs: 30000
        multiplier: 2.0
        maxRetries: -1             # -1 = 无限重试
      heartbeat:
        enabled: true
        intervalMs: 30000
        timeoutMs: 5000
        message: '{"type":"heartbeat"}'
      timeout:
        connectMs: 5000
        requestMs: 10000
```

### 2. 定义客户端接口

```java
@NettyClient(name = "order-service")
public interface OrderClient {

    @NettyRequest(type = "ping")
    PongResponse ping();

    @NettyRequest(type = "order", timeout = 5000)
    CompletableFuture<OrderResponse> createOrder(OrderRequest request);
}
```

### 3. 启用客户端扫描

```java
@SpringBootApplication
@EnableNettyClients(basePackages = "com.example.clients")
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### 4. 使用客户端

```java
@Service
public class OrderService {

    @Autowired
    private OrderClient orderClient;  // 自动注入代理

    public void process() {
        PongResponse pong = orderClient.ping();

        orderClient.createOrder(request)
            .thenAccept(response -> log.info("订单已创建: {}", response));
    }
}
```

## 服务端注解

| 注解 | 描述 |
|------|------|
| `@NettyController` | HTTP/WebSocket 控制器 |
| `@NettyMessageController` | TCP/UDP 消息控制器 |
| `@NettyMessageMapping` | 消息类型映射 |
| `@NettyHttpGet/Post/Put/Delete` | HTTP 方法映射 |
| `@NettyWsOnOpen/Text/Binary/Close` | WebSocket 事件映射 |
| `@PathVar` / `@Query` / `@Body` / `@Header` | 参数绑定 |

## 客户端注解

| 注解 | 描述 |
|------|------|
| `@NettyClient` | 标记接口为 Netty 客户端 |
| `@NettyRequest` | 标记方法为请求 |
| `@Param` | 参数绑定 |
| `@EnableNettyClients` | 启用客户端扫描 |

## 可用的 Profile（协议栈模板）

| Profile | 传输层 | 描述 |
|---------|--------|------|
| `tcp-lengthfield-json` | TCP | 4字节长度前缀 + JSON 编解码 |
| `tcp-line` | TCP | 基于行的帧处理（CRLF） |
| `tcp-raw` | TCP | 原始 TCP，无帧处理 |
| `http1-json` | HTTP | HTTP/1.1 + JSON 编解码 |
| `websocket` | HTTP | WebSocket + JSON 编解码 |
| `udp-json` | UDP | UDP 数据报 + JSON 编解码 |

## Feature（能力组件）配置

```yaml
netty:
  servers:
    - name: my-server
      features:
        # SSL/TLS 加密
        ssl:
          enabled: true
          certPath: /path/to/cert.pem
          keyPath: /path/to/key.pem

        # 空闲检测
        idle:
          enabled: true
          readSeconds: 60
          writeSeconds: 30
          allSeconds: 0

        # 日志记录
        logging:
          enabled: true
          level: DEBUG

        # 限流（令牌桶算法）
        rateLimit:
          enabled: true
          requestsPerSecond: 100
          burstSize: 150

        # 连接数限制
        connectionLimit:
          enabled: true
          maxConnections: 10000
```

## 扩展点

### 自定义 Profile

```java
@Component
public class MyCustomProfile implements Profile {

    @Override
    public String getName() {
        return "my-custom-profile";
    }

    @Override
    public void configure(ChannelPipeline pipeline, ServerSpec serverSpec) {
        pipeline.addLast("myDecoder", new MyDecoder());
        pipeline.addLast("myEncoder", new MyEncoder());
    }
}
```

### 自定义 Codec

```java
@Component
public class ProtobufCodec implements NettyCodec {

    @Override
    public String getName() {
        return "protobuf";
    }

    @Override
    public byte[] encode(Object object) {
        return ((Message) object).toByteArray();
    }

    @Override
    public <T> T decode(byte[] bytes, Class<T> targetType) {
        // 解码 protobuf 消息
    }
}
```

### Pipeline 配置器

```java
@Component
public class MyPipelineConfigurer implements NettyPipelineConfigurer {

    @Override
    public void configure(ChannelPipeline pipeline, ServerSpec serverSpec) {
        pipeline.addBefore("dispatcherHandler", "myHandler", new MyHandler());
    }

    @Override
    public boolean supports(ServerSpec serverSpec) {
        return "my-server".equals(serverSpec.getName());
    }
}
```

### 自定义路由解析器

```java
@Component
public class MyRouteResolver implements NettyRouteResolver {

    @Override
    public String resolveRouteKey(InboundMessage message) {
        // 从自定义字段提取路由键
        return message.getHeader("cmd");
    }
}
```

### 自定义客户端 Profile

```java
@Component
public class MyClientProfile implements ClientProfile {

    @Override
    public String getName() {
        return "my-client-profile";
    }

    @Override
    public void configure(ChannelPipeline pipeline, ClientSpec clientSpec) {
        pipeline.addLast("myDecoder", new MyDecoder());
        pipeline.addLast("myEncoder", new MyEncoder());
    }
}
```

## Actuator 集成

当 classpath 中存在 `spring-boot-actuator` 时：

| 端点 | 描述 |
|------|------|
| `GET /actuator/netty` | 列出所有 Netty 服务器和客户端 |
| `GET /actuator/netty/servers` | 列出所有服务器详情 |
| `GET /actuator/netty/servers/{name}` | 获取指定服务器信息 |
| `GET /actuator/netty/clients` | 列出所有客户端详情 |
| `GET /actuator/netty/clients/{name}` | 获取指定客户端信息 |
| `GET /actuator/health/netty` | 所有组件健康检查 |

### 监控指标（Micrometer）

| 指标 | 描述 |
|------|------|
| `netty.server.connections.current` | 当前服务器连接数 |
| `netty.server.connections.total` | 服务器总连接数 |
| `netty.server.bytes.in` | 服务器接收字节数 |
| `netty.server.bytes.out` | 服务器发送字节数 |
| `netty.server.requests.total` | 服务器请求数 |
| `netty.server.request.latency` | 服务器请求延迟 |
| `netty.client.connections.current` | 当前客户端连接数 |
| `netty.client.pool.size` | 连接池大小 |
| `netty.client.pool.pending` | 等待连接请求数 |
| `netty.client.requests.total` | 客户端请求数 |
| `netty.client.request.latency` | 客户端请求延迟 |
| `netty.client.reconnect.count` | 重连次数 |

## Pipeline 阶段

Pipeline 被组织为 6 个固定阶段，确保行为可预测：

| 阶段 | 描述 | Handlers |
|------|------|----------|
| 1. 传输层/SSL | 加密层 | `SslHandler` |
| 2. 连接治理 | 连接管理 | `ConnectionLimitHandler`, `IpFilterHandler` |
| 3. 帧处理 | 消息边界检测 | `LengthFieldBasedFrameDecoder`, `LineBasedFrameDecoder` |
| 4. 编解码 | 序列化/反序列化 | `JsonCodecHandler`, `ProtobufCodecHandler` |
| 5. 业务分发 | 请求路由 | `DispatcherHandler` |
| 6. 出站处理 | 响应编码 | `MessageEncoder`, `MetricsHandler` |

## 客户端组件

| 组件 | 描述 |
|------|------|
| `ClientSpec` | 客户端配置规范 |
| `ClientOrchestrator` | 管理客户端生命周期 |
| `ConnectionPool` | 连接池管理（最大连接数、最小空闲、健康检查） |
| `ReconnectManager` | 指数退避自动重连 |
| `HeartbeatManager` | 定期心跳保活 |
| `RequestInvoker` | 发送请求、匹配响应 |
| `ResponseFuture` | 带超时的异步响应处理 |
| `ClientProxyFactory` | 为接口生成动态代理 |

## 系统要求

- Java 17+
- Spring Boot 3.5+
- Netty 4.1+

## 开源协议

Apache License 2.0
