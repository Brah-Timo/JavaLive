# ☕ JavaLive

> **Write Java. Ship Vue. No REST. No DTOs. No TypeScript. No duplication.**

[![Java 21](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.x](https://img.shields.io/badge/Spring%20Boot-3.3.0-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue-3-42b883)](https://vuejs.org/)
[![Maven](https://img.shields.io/badge/Maven-3.9%2B-blue)](https://maven.apache.org/)

---

## What is JavaLive?

JavaLive is an annotation-driven framework that **eliminates the entire frontend/backend boundary** for Spring Boot developers. You write one Java class with annotations — JavaLive's compile-time Annotation Processor generates:

- ✅ A **Spring WebSocket Controller** (handles method calls from the browser)
- ✅ A **Vue 3 Component** (`.js` file with reactive state and method bindings)
- ✅ A **State Schema JSON** (for type-safe client-side state initialization)
- ✅ A **Vue Router entry** (for `@VuePage` classes)

```
Your Java Class
       ↓  (compile time)
JavaLive APT
       ↓
Spring WebSocket Controller   +   Vue 3 Component   +   State Schema
```

---

## Quick Start

### Prerequisites

| Tool | Version |
|------|---------|
| Java | 21+ |
| Maven | 3.9+ |

### Build & Run

```bash
# Clone the repository
git clone https://github.com/Brah-Timo/javalive.git
cd javalive

# Build all modules
./build.sh

# Run the example app
./run.sh

# Open in browser
open http://localhost:8080
```

Or using Maven directly:

```bash
# Build
mvn clean install -DskipTests

# Run the example
mvn -pl javalive-example spring-boot:run
```

---

## Your First Component

This is **all** you write:

```java
@Controller
@VueComponent("counter-widget")
public class CounterWidget {

    @Reactive
    public int count = 0;

    @Reactive
    public int step = 1;

    @VueMethod
    public void increment() {
        this.count += this.step;
    }

    @VueMethod
    public void decrement() {
        this.count -= this.step;
    }

    @VueMethod
    public void reset() {
        this.count = 0;
    }

    @VueComputed
    public boolean isZero() {
        return this.count == 0;
    }

    @VueTemplate
    static final String template = """
        <div class="counter">
            <span>{{ state.count }}</span>
            <button @click="decrement">−</button>
            <button @click="reset" :disabled="isZero">Reset</button>
            <button @click="increment">+</button>
        </div>
    """;
}
```

JavaLive generates at compile time:

**Spring Controller:**
```java
// Generated: CounterWidgetLiveController.java
@Controller
public class CounterWidgetLiveController {

    @MessageMapping("/counter-widget.increment")
    public void handle_increment(ClientMessage message,
                                 @Header("simpSessionId") String sessionId) {
        SecurityGuard.verify("CounterWidget", "increment");
        // dispatch → update state → send diff to browser
    }
    // ... other methods
}
```

**Vue Component:**
```javascript
// Generated: static/javalive/components/counter-widget.js
export default defineComponent({
  name: 'counter-widget',
  setup() {
    const { state, dispatch, isConnected } = useLiveState('counter-widget', {
      count: 0,
      step: 1
    });
    const increment = async () => await dispatch('increment', []);
    const decrement = async () => await dispatch('decrement', []);
    const reset     = async () => await dispatch('reset', []);
    const isZero    = computed(() => dispatch('isZero', []));
    return { state, increment, decrement, reset, isZero, isConnected };
  },
  template: `...` // from your @VueTemplate
});
```

---

## All Annotations

| Annotation | Target | Description |
|-----------|--------|-------------|
| `@VueComponent` | Class | Marks the class as a JavaLive component |
| `@VuePage` | Class | Adds Vue Router route for this component |
| `@Reactive` | Field | Reactive state (session or global scope) |
| `@VueProp` | Field | Vue prop (passed in from parent template) |
| `@VueMethod` | Method | Callable from Vue template via WebSocket |
| `@VueComputed` | Method | Vue computed property |
| `@VueWatch` | Method | Vue watcher (watches a `@Reactive` field) |
| `@VueLifecycle` | Method | Vue lifecycle hook (onMounted, etc.) |
| `@VueTemplate` | Field | Inline HTML template |
| `@VueFile` | Class | External `.vue` file template |
| `@VueEmit` | Method | Emit a Vue event from the method |
| `@VueLayout` | Class | Mark class as a layout component |

### Annotation Details

```java
// @Reactive — reactive state field
@Reactive(scope = "session")  // "session" (default) or "global"
public int count = 0;

@Reactive(scope = "global")   // Shared across ALL users in real-time
public int onlineUsers = 0;

// @VueMethod — callable from browser
@VueMethod(loading = true)           // Shows isLoading while running
@VueMethod(debounce = 300)           // 300ms debounce
@VueMethod(confirm = true,           // Confirmation dialog
           confirmMessage = "Sure?")
public void myMethod() { }

// @VuePage — turn component into a page with routing
@VuePage(path = "/dashboard",
         name = "Dashboard",
         requiresAuth = false,
         layout = "default")

// @VueWatch — watch a reactive field
@VueWatch(value = "searchQuery",
          deep = false,
          immediate = false,
          debounce = 300)
public void onQueryChange(String newVal, String oldVal) { }

// @VueLifecycle — lifecycle hooks
@VueLifecycle(hook = "onMounted")
public void onMount() { }

@VueLifecycle(hook = "onUnmounted")
public void onDestroy() { }
```

---

## Module Structure

```
javalive/
├── javalive-annotations/         # @VueComponent, @Reactive, etc.
│                                   (No dependencies — use anywhere)
│
├── javalive-processor/           # Compile-time Annotation Processor
│   ├── parser/                   # Reads Java class → ComponentModel
│   ├── generator/                # Generates Spring + Vue + Schema + Router
│   └── validator/                # Validates annotation usage
│
├── javalive-core/                # Server-side runtime
│   ├── config/                   # Spring Boot AutoConfiguration
│   ├── session/                  # WebSocket session management
│   ├── state/                    # Reactive state store + schemas
│   ├── dispatch/                 # Method dispatcher + security guard
│   ├── websocket/                # STOMP message handling
│   ├── rendering/                # SSR + HTML composition
│   └── routing/                  # Page registry + route loader
│
├── javalive-spring-boot-starter/ # Spring Boot starter (one dependency)
│
└── javalive-example/             # Complete working example app
    ├── components/CounterWidget.java
    ├── pages/Dashboard.java
    └── pages/UserManagement.java
```

---

## Using JavaLive in Your Project

### 1. Add the starter dependency

```xml
<dependency>
    <groupId>io.javalive</groupId>
    <artifactId>javalive-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Register the annotation processor

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>io.javalive</groupId>
                <artifactId>javalive-processor</artifactId>
                <version>1.0.0</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

### 3. Configure application.properties

```properties
javalive.enabled=true
spring.datasource.url=jdbc:h2:mem:myapp
```

### 4. Write your component

```java
@Controller
@VueComponent
@VuePage(path = "/home")
public class HomePage {

    @Reactive
    public String message = "Hello from JavaLive!";

    @VueMethod
    public void updateMessage(String newMsg) {
        this.message = newMsg;
    }

    @VueTemplate
    static final String template = """
        <div>
            <h1>{{ state.message }}</h1>
            <input v-model="state.message" @change="updateMessage(state.message)" />
        </div>
    """;
}
```

---

## How It Works

### Compile Time

```
javac compiles your project
      ↓
JavaLive APT intercepts @VueComponent
      ↓
ClassParser → ComponentModel
      ↓
┌─────────────────────────────┐
│  SpringControllerGenerator  │ → YourClassLiveController.java
│  VueComponentGenerator      │ → static/javalive/components/your-class.js
│  StateSchemaGenerator       │ → static/javalive/schemas/your-class.schema.json
│  RouterGenerator             │ → static/javalive/router.js
└─────────────────────────────┘
```

### Runtime

```
Browser → GET /dashboard
              ↓
    JavaLivePageController
              ↓
    PageRegistry.findByPath("/dashboard")
              ↓
    PageComposer.composeHtmlString()
              ↓
    SsrRenderer: HTML + embedded state JSON
              ↓
Browser ← Full HTML with hydration data
              ↓
    Vue mounts + WebSocket connects
              ↓
User clicks button
              ↓
    dispatch → STOMP → /app/dashboard.toggleDarkMode
              ↓
    DashboardLiveController (generated)
              ↓
    SecurityGuard.verify(...)
              ↓
    MethodDispatcher → Java method executes
              ↓
    StateDiff.compute(old, new)
              ↓
    messagingTemplate → /user/topic/dashboard.state
              ↓
Browser ← { type: "patch", changed: { darkMode: true } }
              ↓
    Vue state updates → DOM updates
```

---

## WebSocket Protocol

### Client → Server (method call)
```json
{
  "destination": "/app/dashboard.toggleDarkMode",
  "body": {
    "method": "toggleDarkMode",
    "args": [],
    "currentState": { "darkMode": false }
  }
}
```

### Server → Client (state patch)
```json
{
  "destination": "/user/topic/dashboard.state",
  "body": {
    "type": "patch",
    "changed": { "darkMode": true },
    "removed": []
  }
}
```

Only **changed fields** are sent — minimizing bandwidth.

---

## Example Application

The `javalive-example` module includes:

| File | Demonstrates |
|------|-------------|
| `CounterWidget.java` | `@Reactive`, `@VueMethod`, `@VueComputed`, `@VueTemplate` |
| `Dashboard.java` | `@VuePage`, `@VueLifecycle`, `@VueComputed` |
| `UserManagement.java` | `@VueWatch`, `@VueMethod(loading=true)`, `@VueMethod(confirm=true)`, pagination |

After `./run.sh`:

- **http://localhost:8080** — Landing page
- **http://localhost:8080/dashboard** — Dashboard page
- **http://localhost:8080/users** — User Management CRUD
- **http://localhost:8080/javalive/status** — Route registry status
- **http://localhost:8080/h2-console** — H2 database console (JDBC: `jdbc:h2:mem:javalive`)

---

## Security

`SecurityGuard` prevents calling any Java method that is not explicitly annotated with `@VueMethod`. This means you cannot call `deleteAllData()` or any internal method from the browser — only annotated methods are exposed.

---

## License

MIT License — see [LICENSE](LICENSE) for details.

---

*JavaLive — Bridging Java and Vue, one annotation at a time.*
