# Netty Spring Boot Starter

A comprehensive Spring Boot starter for building high-performance network applications with Netty.
Support TCP/UDP/HTTP/WebSocket protocols with declarative configuration and annotation-based routing.

## Features

- **Multi-Protocol Support**: TCP, UDP, HTTP/1.1, WebSocket
- **Declarative Configuration**: Configure servers via `application.yml`
- **Annotation-Based Routing**: Spring MVC-like handler development
- **Protocol Profiles**: Pre-built protocol stacks for common use cases
- **Extensible Features**: SSL/TLS, idle detection, logging, rate limiting
- **Production Ready**: Graceful shutdown, metrics, health checks

## Module Structure

```
netty-spring-boot/
├── netty-spring-boot-context/        # Core APIs, annotations, interfaces
├── netty-spring-boot-actuator/       # Metrics, health checks, endpoints
├── netty-spring-boot-autoconfigure/  # Auto-configuration
└── netty-spring-boot-starter/        # Starter dependency aggregation
```

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>com.childrengreens</groupId>
    <artifactId>netty-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Configure Servers

```yaml
netty:
  enabled: true
  servers:
    - name: tcp-server
      transport: TCP
      port: 9000
      profile: tcp-lengthfield-json
      routing:
        mode: MESSAGE_TYPE

    - name: http-server
      transport: HTTP
      port: 8080
      profile: http1-json
      routing:
        mode: PATH
```

### 3. Create Handlers

**TCP/UDP Message Handler:**

```java
@NettyMessageController
public class MessageHandler {

    @NettyMessageMapping("ping")
    public Map<String, Object> handlePing(NettyContext context) {
        return Map.of("type", "pong", "timestamp", System.currentTimeMillis());
    }

    @NettyMessageMapping("order")
    public OrderResponse handleOrder(OrderRequest request) {
        // Process order
        return new OrderResponse();
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
    public User getUser(@PathVar("id") Long id) {
        return userService.findById(id);
    }

    @NettyHttpPost("/users")
    public User createUser(@Body User user) {
        return userService.save(user);
    }
}
```

**WebSocket Handler:**

```java
@NettyController(path = "/ws")
public class WebSocketHandler {

    @NettyWsOnOpen("/chat")
    public void onOpen(NettyContext context) {
        // Handle connection opened
    }

    @NettyWsOnText("/chat")
    public String onMessage(String message, NettyContext context) {
        return "Echo: " + message;
    }

    @NettyWsOnClose("/chat")
    public void onClose(NettyContext context) {
        // Handle connection closed
    }
}
```

## Available Profiles

| Profile | Description |
|---------|-------------|
| `tcp-lengthfield-json` | TCP with 4-byte length prefix and JSON codec |
| `tcp-line` | TCP with line-based framing |
| `tcp-raw` | Raw TCP without framing |
| `http1-json` | HTTP/1.1 with JSON codec |
| `websocket` | WebSocket with JSON codec |
| `udp-json` | UDP with JSON codec |

## Features Configuration

```yaml
netty:
  servers:
    - name: my-server
      features:
        ssl:
          enabled: true
          certPath: /path/to/cert.pem
          keyPath: /path/to/key.pem
        idle:
          enabled: true
          readSeconds: 60
        logging:
          enabled: true
          level: DEBUG
        rateLimit:
          enabled: true
          requestsPerSecond: 100
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
public class MyCustomCodec implements NettyCodec {

    @Override
    public String getName() {
        return "protobuf";
    }

    @Override
    public byte[] encode(Object object) {
        // Encode to protobuf
    }

    @Override
    public <T> T decode(byte[] bytes, Class<T> targetType) {
        // Decode from protobuf
    }
}
```

### Pipeline Configurer

```java
@Component
public class MyPipelineConfigurer implements NettyPipelineConfigurer {

    @Override
    public void configure(ChannelPipeline pipeline, ServerSpec serverSpec) {
        // Add custom handlers
        pipeline.addBefore("dispatcherHandler", "myHandler", new MyHandler());
    }

    @Override
    public boolean supports(ServerSpec serverSpec) {
        return "my-server".equals(serverSpec.getName());
    }
}
```

## Actuator Integration

When `spring-boot-actuator` is on the classpath, the following endpoints are available:

- `GET /actuator/netty` - List all Netty servers
- `GET /actuator/netty/{serverName}` - Get specific server info
- `GET /actuator/health/netty` - Health check for all servers

## Requirements

- Java 17+
- Spring Boot 3.5+
- Netty 4.1+

## License

Apache License 2.0
