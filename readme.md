## Step by Step Implementation
1. **HTTP Server & Ping Client**: Set up JDK `HttpServer` with `/hello` returning `PingResponse`; built `PingClient` to call it.
2. **Registration**: Added `/signup` with layered `SignupHandler → UserService → UserDao (Derby JDBC)`; returns `SignupResponse`.
3. **List Users**: Added `/listUsers` with `ListUsersHandler` returning `UserListResponse`; created `ListUsersClient`.
4. **Master Client**: Orchestrates `PingClient → SignupClient → ListUsersClient` in a loop for continuous testing.

## Design & Implementation Decisions

We are tracking decisions to keep the project clear and consistent.

| Module        | Decision                                        | Reasoning |
|---------------|--------------------------------------------------|-----------|
| HTTP          | JDK `com.sun.net.httpserver.HttpServer`         | Zero framework; simplest for step-by-step demo. |
| JSON          | Gson                                             | Lightweight for plain Java (no Spring). |
| DB            | Apache Derby **embedded (file-based)**          | Persists across restarts without external server. |
| DAO           | Plain JDBC (`UserDao`)                           | No ORM per requirement; full control. |
| Services      | `UserService` between Handler and DAO            | Separation of concerns; testable logic. |
| Endpoints     | `/hello`, `/signup`, `/listUsers`                | Covers ping, registration, and read-back quickly. |
| Concurrency   | `Executors.newFixedThreadPool(10)`               | Predictable thread usage for handlers. |
| Passwords     | (Next) Hash before storing                       | Security best practice for follow-up. |
| Clients       | Separate CLI clients + `MasterClient`            | Easy manual/system testing. |
| Persistence   | Derby URL `jdbc:derby:eagleDB;create=true`       | Shared on-disk DB between processes. |
| Naming        | `*Handler`, `*Service`, `*Response`              | Consistent, discoverable code structure. |
