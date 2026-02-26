# Netty Spring Boot Starter Architecture

## Module Structure

```
netty-spring-boot/
├── netty-spring-boot-context/        # Core APIs, annotations, interfaces (Server + Client)
├── netty-spring-boot-actuator/       # Metrics, health checks, endpoints
├── netty-spring-boot-autoconfigure/  # Auto-configuration (Server + Client)
└── netty-spring-boot-starter/        # Starter dependency aggregation
```

## Auto-Configuration Structure

The auto-configuration is split into three independent classes for better modularity:

| Configuration Class | Responsibility | Condition |
|---------------------|----------------|-----------|
| `NettyAutoConfiguration` | Shared beans (CodecRegistry, TransportFactory) | `spring.netty.enabled=true` (default) |
| `NettyServerAutoConfiguration` | Server beans (Router, Dispatcher, ProfileRegistry, FeatureRegistry, PipelineAssembler, NettyServerOrchestrator, AnnotationRegistry) | `spring.netty.server.enabled=true` (default) + CodecRegistry exists |
| `NettyClientAutoConfiguration` | Client beans (ClientProfileRegistry, ClientPipelineAssembler, NettyClientOrchestrator, ClientProxyFactory) | `spring.netty.client.enabled=true` (default) + CodecRegistry exists |

### Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `spring.netty.enabled` | `true` | Enable/disable all Netty functionality |
| `spring.netty.server.enabled` | `true` | Enable/disable server components only |
| `spring.netty.client.enabled` | `true` | Enable/disable client components only |

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           application.yml                               │
│  ┌─────────────────────────────┐    ┌─────────────────────────────────┐ │
│  │ spring.netty.servers[*]      │    │ spring.netty.clients[*]         │ │
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

## Server Architecture

### Data Flow

```
Client Request
      │
      ▼
┌─────────────────┐
│ChannelPipeline  │
│  ├─ SslHandler  │ (if enabled)
│  ├─ IdleHandler │
│  ├─ FrameDecoder│
│  ├─ CodecHandler│
│  └─ Dispatcher  │
│      Handler    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐     ┌─────────────────┐
│   Dispatcher    │ ──► │     Router      │
│                 │     │  - exactRoutes  │
│                 │     │  - patternRoutes│
└────────┬────────┘     └─────────────────┘
         │
         ▼
┌─────────────────┐
│ ArgumentResolver│
│  - @PathVar     │
│  - @Query       │
│  - @Body        │
│  - @Header      │
└────────┬────────┘
         │
         ▼
┌─────────────────────┐
│   Handler Method    │
│ @NettyMessageMapping│
└──────────┬──────────┘
          │
          ▼
┌─────────────────┐
│ OutboundMessage │
│   (Response)    │
└─────────────────┘
```

### Server Components

| Component | Description |
|-----------|-------------|
| `ServerSpec` | Server configuration specification |
| `ServerOrchestrator` | Manages server lifecycle (start/stop) |
| `DispatcherHandler` | Netty handler that converts messages and dispatches |
| `Dispatcher` | Routes messages to handler methods |
| `Router` | Matches routes (exact + pattern with path variables) |
| `ArgumentResolverComposite` | Resolves method parameters |

### Server Annotations

| Annotation | Description |
|------------|-------------|
| `@NettyController` | HTTP/WebSocket controller |
| `@NettyMessageController` | TCP/UDP message controller |
| `@NettyMessageMapping` | Message type mapping |
| `@NettyHttpGet/Post/Put/Delete` | HTTP method mapping |
| `@NettyWsOnOpen/Text/Binary/Close` | WebSocket event mapping |
| `@PathVar` / `@Query` / `@Body` / `@Header` | Parameter binding |

## Client Architecture

### Data Flow

```
Application Code
      │
      ▼
┌─────────────────┐
│ @NettyClient    │
│ Interface Proxy │
└────────┬────────┘
         │
         ▼
┌─────────────────┐     ┌─────────────────┐
│ RequestInvoker  │ ──► │ ConnectionPool  │
│  - serialize    │     │  - getChannel() │
│  - send request │     │  - returnChannel│
│  - wait response│     └─────────────────┘
└────────┬────────┘              │
         │                       ▼
         │              ┌─────────────────┐
         │              │ReconnectManager │
         │              │  - exponential  │
         │              │    backoff      │
         │              └─────────────────┘
         │                       │
         │                       ▼
         │              ┌─────────────────┐
         │              │HeartbeatManager │
         │              │  - keep-alive   │
         │              │  - health check │
         │              └─────────────────┘
         │
         ▼
┌─────────────────┐
│ChannelPipeline  │  (Client Side)
│  ├─ SslHandler  │  (if enabled)
│  ├─ IdleHandler │
│  ├─ FrameEncoder│  (outbound)
│  ├─ FrameDecoder│  (inbound)
│  ├─ CodecHandler│
│  └─ Response    │
│      Handler    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ ResponseFuture  │
│  - correlationId│
│  - timeout      │
│  - callback     │
└────────┬────────┘
         │
         ▼
   Response Data
```

### Client Pipeline Stages

```
                          Outbound (Request)                 Inbound (Response)
                                │                                   │
                                ▼                                   │
┌──────────────────────────────────────────────────────────────────────────────┐
│                         Client ChannelPipeline                               │
│                                                                              │
│  ┌─────────────┐                                                             │
│  │ SslHandler  │  ◄─────────────────────────────────────────────────────────►│
│  │ (optional)  │  TLS encryption/decryption                                  │
│  └─────────────┘                                                             │
│        │                                                                     │
│        ▼                                                                     │
│  ┌─────────────┐                                                             │
│  │ IdleHandler │  Connection idle detection, trigger heartbeat               │
│  └─────────────┘                                                             │
│        │                                                                     │
│        ▼                                                                     │
│  ┌─────────────┐                                           ┌─────────────┐  │
│  │FrameEncoder │  Object → ByteBuf (add length prefix)     │FrameDecoder │  │
│  │ (outbound)  │                                           │ (inbound)   │  │
│  └─────────────┘                                           └─────────────┘  │
│        │                                                          │         │
│        ▼                                                          ▼         │
│  ┌─────────────┐                                           ┌─────────────┐  │
│  │CodecEncoder │  Object → JSON/Protobuf bytes             │CodecDecoder │  │
│  │ (outbound)  │                                           │ (inbound)   │  │
│  └─────────────┘                                           └─────────────┘  │
│        │                                                          │         │
│        ▼                                                          ▼         │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                        ResponseHandler                                │   │
│  │  - Match response to request by correlationId                        │   │
│  │  - Complete ResponseFuture                                           │   │
│  │  - Handle timeout                                                    │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
                          Network I/O
```

### Client Components

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

### Client Annotations

| Annotation | Description |
|------------|-------------|
| `@NettyClient` | Marks interface as Netty client |
| `@NettyRequest` | Marks method as request |
| `@Param` | Parameter binding |

## Shared Components

### Message Abstraction

```java
// Inbound message (received)
InboundMessage.builder()
    .transport(TransportType.TCP)
    .routeKey("ping")
    .headers(headers)
    .rawPayload(bytes)
    .build();

// Outbound message (response/request)
OutboundMessage.ok(payload);
OutboundMessage.error(500, "error message");
```

### Codec System

```java
public interface NettyCodec {
    String getName();
    byte[] encode(Object object);
    <T> T decode(byte[] bytes, Class<T> targetType);
}
```

Built-in codecs: `json`, `protobuf` (extensible)

### Profile System

```java
public interface Profile {
    String getName();
    void configure(ChannelPipeline pipeline, ServerSpec serverSpec);
}
```

Built-in profiles:
- `tcp-lengthfield-json` - 4-byte length prefix + JSON
- `tcp-line` - Line-based framing (CRLF)
- `tcp-raw` - Raw TCP
- `http1-json` - HTTP/1.1 + JSON
- `websocket` - WebSocket + JSON
- `udp-json` - UDP + JSON

### Feature System

```java
public interface Feature {
    String getName();
    void apply(ChannelPipeline pipeline, FeatureConfig config);
    int order(); // Pipeline ordering
}
```

Built-in features: `ssl`, `idle`, `logging`, `rateLimit`, `connectionLimit`

## Configuration

### Full Example

```yaml
spring:
  netty:
    enabled: true

    defaults:
      threads:
        boss: 1
        worker: 0                    # 0 = CPU cores * 2
      transport:
        prefer: AUTO                 # AUTO/NIO/EPOLL/KQUEUE
      shutdown:
        graceful: true
        quietPeriodMs: 200
        timeoutMs: 3000

    # Server configuration
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

    # Client configuration
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

    observability:
      metrics: true
      health: true
```

## Usage Examples

### Server Side

```java
@NettyMessageController
public class MessageHandler {

    @NettyMessageMapping("ping")
    public Map<String, Object> handlePing(NettyContext context) {
        return Map.of(
            "type", "pong",
            "timestamp", System.currentTimeMillis()
        );
    }

    @NettyMessageMapping("order")
    public CompletableFuture<OrderResponse> handleOrder(
            OrderRequest request, NettyContext context) {
        return orderService.processAsync(request);
    }
}
```

### Client Side

```java
@NettyClient(name = "order-service")
public interface OrderClient {

    @NettyRequest(type = "ping")
    PongResponse ping();

    @NettyRequest(type = "order", timeout = 5000)
    CompletableFuture<OrderResponse> createOrder(OrderRequest request);
}

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

## Pipeline Stages

The pipeline is organized into 6 fixed stages:

| Stage | Description | Handlers |
|-------|-------------|----------|
| 1. Transport/SSL | Encryption layer | `SslHandler` |
| 2. Connection Governance | Connection management | `ConnectionLimitHandler`, `IpFilterHandler` |
| 3. Framing | Message boundary detection | `LengthFieldBasedFrameDecoder`, `LineBasedFrameDecoder` |
| 4. Codec | Serialization/Deserialization | `JsonCodecHandler`, `ProtobufCodecHandler` |
| 5. Business Dispatch | Request routing | `DispatcherHandler` |
| 6. Outbound | Response encoding | `MessageEncoder`, `MetricsHandler` |

## Actuator Integration

### Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/netty` | List all servers and clients |
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

## Extension Points

### Custom Profile

```java
@Component
public class MyProfile implements Profile {
    @Override
    public String getName() { return "my-profile"; }

    @Override
    public void configure(ChannelPipeline pipeline, ServerSpec spec) {
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
    public String getName() { return "protobuf"; }

    @Override
    public byte[] encode(Object obj) {
        return ((Message) obj).toByteArray();
    }

    @Override
    public <T> T decode(byte[] bytes, Class<T> type) {
        // Protobuf deserialization
    }
}
```

### Custom Feature

```java
@Component
public class MyFeature implements Feature {
    @Override
    public String getName() { return "my-feature"; }

    @Override
    public void apply(ChannelPipeline pipeline, FeatureConfig config) {
        pipeline.addLast("myHandler", new MyHandler());
    }

    @Override
    public int order() { return 100; }
}
```

## Requirements

- Java 17+
- Spring Boot 3.5+
- Netty 4.1+

## License

Apache License 2.0
