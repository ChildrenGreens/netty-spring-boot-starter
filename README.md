# Netty Spring Boot Starter

[![CircleCI](https://dl.circleci.com/status-badge/img/circleci/Uy51fPbZSgy15X8yEZTik4/YY1zfzyrviv8C9brtrcgbs/tree/main.svg?style=shield)](https://dl.circleci.com/status-badge/redirect/circleci/Uy51fPbZSgy15X8yEZTik4/YY1zfzyrviv8C9brtrcgbs/tree/main)
[![codecov](https://codecov.io/github/ChildrenGreens/netty-spring-boot-starter/graph/badge.svg?token=1HN5FLYOZ8)](https://codecov.io/github/ChildrenGreens/netty-spring-boot-starter)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5+-green.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

A comprehensive Spring Boot starter for building high-performance network applications with Netty.
Support TCP/UDP/HTTP/WebSocket protocols with declarative configuration and annotation-based routing.

[中文文档](README_zh.md)

## Highlights

- **One Configuration, Multiple Protocols** - Start TCP/HTTP/WebSocket/UDP servers with a single YAML configuration
- **Annotation-Based Routing** - Develop handlers like Spring MVC / Spring Messaging
- **Profile-Based Protocol Stacks** - One-click protocol setup, with Feature-based capability overlay
- **Client Support** - Declarative client interfaces with connection pooling, reconnection, and heartbeat
- **Production Ready** - Built-in observability (metrics/health), graceful shutdown support
- **Highly Extensible** - Advanced users can extend via Configurer/Codec/RouteResolver

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           application.yml                               │
│  ┌─────────────────────────────┐    ┌─────────────────────────────────┐ │
│  │ spring.netty.servers[*]    │    │ spring.netty.clients[*]         │ │
│  │    Server Configuration     │    │    Client Configuration         │ │
│  └─────────────────────────────┘    └─────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
                    │                              │
                    ▼                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      netty-spring-boot-context                          │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                       Shared Components                           │  │
│  │  Codec │ Profile │ InboundMessage │ OutboundMessage │ NettyContext │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  ┌────────────────────────┐        ┌────────────────────────────────┐  │
│  │    Server Components    │        │       Client Components        │  │
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
│                           Netty Runtime                                 │
│  ┌─────────────┐    ┌─────────────┐    ┌────────────────────────────┐  │
│  │  BossGroup  │ →  │ WorkerGroup │ →  │  ChannelPipeline           │  │
│  └─────────────┘    └─────────────┘    │  (Staged Handlers)         │  │
│                                         └────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

## Module Structure

```
netty-spring-boot/
├── netty-spring-boot-context/        # Core APIs, annotations, interfaces (Server + Client)
├── netty-spring-boot-actuator/       # Metrics, health checks, endpoints
├── netty-spring-boot-autoconfigure/  # Auto-configuration (Server + Client)
└── netty-spring-boot-starter/        # Starter dependency aggregation
```

## Enable/Disable Configuration

The auto-configuration is modular - you can enable/disable components independently:

| Property | Default | Description |
|----------|---------|-------------|
| `spring.netty.enabled` | `true` | Enable/disable all Netty functionality |
| `spring.netty.server.enabled` | `true` | Enable/disable server components only |
| `spring.netty.client.enabled` | `true` | Enable/disable client components only |

**Example - Client only mode:**
```yaml
spring:
  netty:
    server:
      enabled: false    # Disable server
    client:
      enabled: true     # Enable client (default)
```

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>com.childrengreens</groupId>
    <artifactId>netty-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 2. Configure Servers

```yaml
spring:
  netty:
    enabled: true
    defaults:
      threads:
        boss: 1
        worker: 0              # 0 = CPU cores * 2
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
          mode: MESSAGE_TYPE
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
        mode: PATH

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

### 3. Create Handlers

**TCP/UDP Message Handler:**

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
        // Async processing supported
        return orderService.processAsync(request);
    }
}
```

**HTTP Controller:**

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

**WebSocket Handler:**

```java
@NettyController(path = "/ws")
public class WebSocketHandler {

    @NettyWsOnOpen("/chat")
    public void onOpen(NettyContext context) {
        log.info("WebSocket connected: {}", context.getChannelId());
    }

    @NettyWsOnText("/chat")
    public String onMessage(String message, NettyContext context) {
        return "Echo: " + message;
    }

    @NettyWsOnBinary("/chat")
    public byte[] onBinary(byte[] data, NettyContext context) {
        return data; // Echo binary
    }

    @NettyWsOnClose("/chat")
    public void onClose(NettyContext context) {
        log.info("WebSocket disconnected: {}", context.getChannelId());
    }
}
```

## Client Usage

### 1. Configure Clients

```yaml
spring:
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
          maxRetries: -1             # -1 = infinite
        heartbeat:
          enabled: true
          intervalMs: 30000
          timeoutMs: 5000
          message: '{"type":"heartbeat"}'
        timeout:
          connectMs: 5000
          requestMs: 10000
```

### 2. Define Client Interface

```java
@NettyClient(name = "order-service")
public interface OrderClient {

    @NettyRequest(type = "ping")
    PongResponse ping();

    @NettyRequest(type = "order", timeout = 5000)
    CompletableFuture<OrderResponse> createOrder(OrderRequest request);
}
```

### 3. Enable Client Scanning

```java
@SpringBootApplication
@EnableNettyClients(basePackages = "com.example.clients")
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### 4. Use Client

```java
@Service
public class OrderService {

    @Autowired
    private OrderClient orderClient;  // Auto-injected proxy

    public void process() {
        PongResponse pong = orderClient.ping();

        orderClient.createOrder(request)
            .thenAccept(response -> log.info("Order created: {}", response));
    }
}
```

## Server Annotations

| Annotation | Description |
|------------|-------------|
| `@NettyController` | HTTP/WebSocket controller |
| `@NettyMessageController` | TCP/UDP message controller |
| `@NettyMessageMapping` | Message type mapping |
| `@NettyHttpGet/Post/Put/Delete` | HTTP method mapping |
| `@NettyWsOnOpen/Text/Binary/Close` | WebSocket event mapping |
| `@PathVar` / `@Query` / `@Body` / `@Header` | Parameter binding |

## Client Annotations

| Annotation | Description |
|------------|-------------|
| `@NettyClient` | Marks interface as Netty client |
| `@NettyRequest` | Marks method as request |
| `@Param` | Parameter binding |
| `@EnableNettyClients` | Enable client scanning |

## Available Profiles

| Profile | Transport | Description |
|---------|-----------|-------------|
| `tcp-lengthfield-json` | TCP | 4-byte length prefix + JSON codec |
| `tcp-line` | TCP | Line-based framing (CRLF) |
| `tcp-raw` | TCP | Raw TCP without framing |
| `http1-json` | HTTP | HTTP/1.1 with JSON codec |
| `websocket` | HTTP | WebSocket with JSON codec |
| `udp-json` | UDP | UDP datagram with JSON codec |

## Features Configuration

```yaml
spring:
  netty:
    servers:
      - name: my-server
        features:
          # SSL/TLS encryption
          ssl:
            enabled: true
            certPath: /path/to/cert.pem
            keyPath: /path/to/key.pem

          # Idle detection
          idle:
            enabled: true
            readSeconds: 60
            writeSeconds: 30
            allSeconds: 0

          # Logging
          logging:
            enabled: true
            level: DEBUG

          # Rate limiting (Token Bucket)
          rateLimit:
            enabled: true
            requestsPerSecond: 100
            burstSize: 150

          # Connection limiting
          connectionLimit:
            enabled: true
            maxConnections: 10000
```

## Extension Points

### Custom Profile

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

### Custom Codec

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
        // Decode protobuf message
    }
}
```

### Pipeline Configurer

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

### Custom Route Resolver

```java
@Component
public class MyRouteResolver implements NettyRouteResolver {

    @Override
    public String resolveRouteKey(InboundMessage message) {
        // Extract route key from custom field
        return message.getHeader("cmd");
    }
}
```

### Custom Client Profile

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

## Actuator Integration

When `spring-boot-actuator` is on the classpath:

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/netty` | List all Netty servers and clients |
| `GET /actuator/netty/servers` | List all server details |
| `GET /actuator/netty/servers/{name}` | Get specific server info |
| `GET /actuator/netty/clients` | List all client details |
| `GET /actuator/netty/clients/{name}` | Get specific client info |
| `GET /actuator/health/netty` | Health check for all |

### Metrics (Micrometer)

| Metric | Description |
|--------|-------------|
| `netty.server.connections.current` | Current server connections |
| `netty.server.connections.total` | Total server connections |
| `netty.server.bytes.in` | Server bytes received |
| `netty.server.bytes.out` | Server bytes sent |
| `netty.server.requests.total` | Server request count |
| `netty.server.request.latency` | Server request latency |
| `netty.client.connections.current` | Current client connections |
| `netty.client.pool.size` | Connection pool size |
| `netty.client.pool.pending` | Pending connection requests |
| `netty.client.requests.total` | Client request count |
| `netty.client.request.latency` | Client request latency |
| `netty.client.reconnect.count` | Reconnection attempts |

## Pipeline Stages

The pipeline is organized into 6 fixed stages for predictable behavior:

| Stage | Description | Handlers |
|-------|-------------|----------|
| 1. Transport/SSL | Encryption layer | `SslHandler` |
| 2. Connection Governance | Connection management | `ConnectionLimitHandler`, `IpFilterHandler` |
| 3. Framing | Message boundary detection | `LengthFieldBasedFrameDecoder`, `LineBasedFrameDecoder` |
| 4. Codec | Serialization/Deserialization | `JsonCodecHandler`, `ProtobufCodecHandler` |
| 5. Business Dispatch | Request routing | `DispatcherHandler` |
| 6. Outbound | Response encoding | `MessageEncoder`, `MetricsHandler` |

## Client Components

| Component | Description |
|-----------|-------------|
| `ClientSpec` | Client configuration specification |
| `ClientOrchestrator` | Manages client lifecycle |
| `ConnectionPool` | Manages connections (max, min idle, health check) |
| `ReconnectManager` | Auto-reconnect with exponential backoff |
| `HeartbeatManager` | Periodic heartbeat for keep-alive |
| `RequestInvoker` | Sends requests, matches responses |
| `ResponseFuture` | Async response handling with timeout |
| `ClientProxyFactory` | Generates dynamic proxies for interfaces |

## Requirements

- Java 17+
- Spring Boot 3.5+
- Netty 4.1+

## License

Apache License 2.0
