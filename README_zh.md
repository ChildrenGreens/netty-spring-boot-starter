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
- **生产就绪** - 内置可观测性（metrics/health），支持优雅关闭
- **高度可扩展** - 高级用户可通过 Configurer/Codec/RouteResolver 扩展

## 架构设计

核心设计理念：**统一运行模型（ServerSpec）+ Profile（协议栈模板）+ Feature（能力组件）+ 注解路由（Handler Registry）+ Dispatcher（协议无关分发）**

用户只需编写 YAML 配置和注解，无需直接接触 Bootstrap/ChannelInitializer/EventLoopGroup/Pipeline 细节。

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        用户侧：配置 + 注解                                 │
│  ┌─────────────────────────┐    ┌─────────────────────────────────────┐ │
│  │    application.yml      │    │  @NettyHttpGet / @NettyWsOnText     │ │
│  │    netty.servers[*]     │    │  @NettyMessageMapping ...           │ │
│  └─────────────────────────┘    └─────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      Netty Spring Boot Starter                          │
│  ┌────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐   │
│  │ NettyProperties│→ │ 自动配置         │→ │ ServerOrchestrator      │   │
│  │ ServerSpec     │  └─────────────────┘  │ (多服务器管理)            │   │
│  └────────────────┘                       └─────────────────────────┘   │
│          │                                          │                   │
│          ▼                                          ▼                   │
│  ┌────────────────┐  ┌─────────────────┐   ┌─────────────────────────┐  │
│  │TransportFactory│  │ PipelineAssembler│  │    ServerRuntime[*]     │  │
│  │ TCP/UDP/HTTP   │  │ Profile+Features │  └─────────────────────────┘  │
│  └────────────────┘  └─────────────────┘                                │
│          │                    │                                         │
│          ▼                    ▼                                         │
│  ┌────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐   │
│  │ ProfileRegistry│  │ FeatureRegistry │  │   AnnotationRegistry    │   │
│  │ (内置+可扩展)    │  │ ssl/idle/codec..│  │   (扫描注解)             │   │
│  └────────────────┘  └─────────────────┘  └─────────────────────────┘   │
│                              │                        │                 │
│                              ▼                        ▼                 │
│                      ┌─────────────────┐  ┌─────────────────────────┐   │
│                      │     Router      │→ │      Dispatcher         │   │
│                      │PATH/MESSAGE_TYPE│  │ 参数绑定/方法调用          │   │
│                      └─────────────────┘  └─────────────────────────┘   │
│                              │                        │                 │
│                              ▼                        ▼                 │
│                      ┌─────────────────┐  ┌─────────────────────────┐   │
│                      │     Codec       │  │    Observability        │   │
│                      │ JSON/Protobuf   │  │ Metrics+Health+Tracing  │   │
│                      └─────────────────┘  └─────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           Netty 运行时                                   │
│  ┌─────────────┐    ┌─────────────┐    ┌────────────────────────────┐   │
│  │  BossGroup  │ →  │ WorkerGroup │ →  │  ChannelPipeline           │   │
│  └─────────────┘    └─────────────┘    │  (阶段化 Handlers)          │   │
│                                        └────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

## 模块结构

```
netty-spring-boot/
├── netty-spring-boot-context/        # 核心 API、注解、接口
├── netty-spring-boot-actuator/       # 监控指标、健康检查、端点
├── netty-spring-boot-autoconfigure/  # 自动配置
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

## Actuator 集成

当 classpath 中存在 `spring-boot-actuator` 时：

| 端点 | 描述 |
|------|------|
| `GET /actuator/netty` | 列出所有 Netty 服务器详情 |
| `GET /actuator/netty/{name}` | 获取指定服务器信息 |
| `GET /actuator/health/netty` | 所有服务器健康检查 |

### 监控指标（Micrometer）

| 指标 | 描述 |
|------|------|
| `netty.connections.current` | 当前活跃连接数 |
| `netty.connections.total` | 启动以来总连接数 |
| `netty.bytes.in` | 接收的总字节数 |
| `netty.bytes.out` | 发送的总字节数 |
| `netty.requests.total` | 按服务器/路由/状态统计的请求总数 |
| `netty.request.latency` | 请求延迟直方图 |

## Pipeline 阶段

Pipeline 被组织为 6 个固定阶段，确保行为可预测：

1. **传输层/SSL** - SslHandler（如启用）
2. **连接治理** - 连接限制、IP 过滤、代理协议
3. **帧处理** - 帧解码器（TCP/UDP）
4. **编解码** - ByteBuf ↔ Message（JSON/Protobuf）
5. **业务分发** - DispatcherHandler（统一入口）
6. **出站处理** - 编码器、指标、刷新策略

## 注解体系

### 基础元注解
- `@NettyController` - HTTP/WebSocket 控制器
- `@NettyMessageController` - TCP/UDP 消息控制器

### TCP/UDP 注解
- `@NettyMessageMapping(type="xxx")` - 消息类型映射

### HTTP 注解
- `@NettyHttpGet` / `@NettyHttpPost` / `@NettyHttpPut` / `@NettyHttpDelete`
- 参数绑定：`@PathVar` / `@Query` / `@Header` / `@Body`

### WebSocket 注解
- `@NettyWsOnOpen` - 连接打开
- `@NettyWsOnText` - 文本消息
- `@NettyWsOnBinary` - 二进制消息
- `@NettyWsOnClose` - 连接关闭

## 系统要求

- Java 17+
- Spring Boot 3.5+
- Netty 4.1+

## 开源协议

Apache License 2.0
