# Netty Spring Boot Starter

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
- **Production Ready** - Built-in observability (metrics/health), graceful shutdown support
- **Highly Extensible** - Advanced users can extend via Configurer/Codec/RouteResolver

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        User Side: Config + Annotations                   │
│  ┌─────────────────────────┐    ┌─────────────────────────────────────┐ │
│  │    application.yml      │    │  @NettyHttpGet / @NettyWsOnText     │ │
│  │    netty.servers[*]     │    │  @NettyMessageMapping ...           │ │
│  └─────────────────────────┘    └─────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      Netty Spring Boot Starter                           │
│  ┌────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐  │
│  │ NettyProperties│→ │AutoConfiguration│→ │ ServerOrchestrator      │  │
│  │ ServerSpec     │  └─────────────────┘  │ (Multi-Server Manager)  │  │
│  └────────────────┘                       └─────────────────────────┘  │
│          │                                          │                   │
│          ▼                                          ▼                   │
│  ┌────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐  │
│  │TransportFactory│  │ PipelineAssembler│  │    ServerRuntime[*]    │  │
│  │ TCP/UDP/HTTP   │  │ Profile+Features │  └─────────────────────────┘  │
│  └────────────────┘  └─────────────────┘                                │
│          │                    │                                         │
│          ▼                    ▼                                         │
│  ┌────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐  │
│  │ ProfileRegistry│  │ FeatureRegistry │  │   AnnotationRegistry    │  │
│  │ (Built-in+Ext) │  │ ssl/idle/codec..│  │   (Scan Annotations)    │  │
│  └────────────────┘  └─────────────────┘  └─────────────────────────┘  │
│                              │                        │                 │
│                              ▼                        ▼                 │
│                      ┌─────────────────┐  ┌─────────────────────────┐  │
│                      │     Router      │→ │      Dispatcher         │  │
│                      │PATH/MESSAGE_TYPE│  │ Param Binding/Invoke    │  │
│                      └─────────────────┘  └─────────────────────────┘  │
│                              │                        │                 │
│                              ▼                        ▼                 │
│                      ┌─────────────────┐  ┌─────────────────────────┐  │
│                      │     Codec       │  │    Observability        │  │
│                      │ JSON/Protobuf   │  │ Metrics+Health+Tracing  │  │
│                      └─────────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           Netty Runtime                                  │
│  ┌─────────────┐    ┌─────────────┐    ┌────────────────────────────┐  │
│  │  BossGroup  │ →  │ WorkerGroup │ →  │  ChannelPipeline           │  │
│  └─────────────┘    └─────────────┘    │  (Staged Handlers)         │  │
│                                         └────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

## Module Structure

```
netty-spring-boot/
├── netty-spring-boot-context/        # Core APIs, annotations, interfaces
├── netty-spring-boot-actuator/       # Metrics, health checks, endpoints
├── netty-spring-boot-autoconfigure/  # Auto-configuration
├── netty-spring-boot-starter/        # Starter dependency aggregation
└── samples/                          # Example applications
    └── sample-tcp-json/
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

## Actuator Integration

When `spring-boot-actuator` is on the classpath:

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/netty` | List all Netty servers with details |
| `GET /actuator/netty/{name}` | Get specific server information |
| `GET /actuator/health/netty` | Health check for all servers |

### Metrics (Micrometer)

| Metric | Description |
|--------|-------------|
| `netty.connections.current` | Current active connections |
| `netty.connections.total` | Total connections since startup |
| `netty.bytes.in` | Total bytes received |
| `netty.bytes.out` | Total bytes sent |
| `netty.requests.total` | Total requests by server/route/status |
| `netty.request.latency` | Request latency histogram |

## Pipeline Stages

The pipeline is organized into 6 fixed stages for predictable behavior:

1. **Transport/SSL** - SslHandler (if enabled)
2. **Connection Governance** - Connection limit, IP filter, Proxy protocol
3. **Framing** - Frame decoder (TCP/UDP)
4. **Codec** - ByteBuf ↔ Message (JSON/Protobuf)
5. **Business Dispatch** - DispatcherHandler (unified entry point)
6. **Outbound** - Encoder, metrics, flush strategy

## Requirements

- Java 17+
- Spring Boot 3.5+
- Netty 4.1+

## License

Apache License 2.0
