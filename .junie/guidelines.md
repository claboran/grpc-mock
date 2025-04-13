# gRPC Mock Service - Developer Guidelines

## Project Overview
This project is a gRPC mock service built with Kotlin and Spring Boot. It simulates a computation service that processes calculation requests asynchronously and streams back status updates and results. The service demonstrates server-streaming RPC patterns using Kotlin coroutines and Flow.

## Tech Stack
- **Languages**: Kotlin 1.9.25, Java 21
- **Frameworks**: Spring Boot 3.4.4
- **API**: gRPC with Protocol Buffers
- **Libraries**:
  - gRPC (1.61.1)
  - Protobuf (3.25.6)
  - gRPC Spring Boot Starter (3.1.0.RELEASE)
  - Kotlin Coroutines
- **Build Tool**: Gradle

## Project Structure
```
src/
├── main/
│   ├── kotlin/de/laboranowitsch/poc/grpcmock/
│   │   ├── backend/                  # Service implementations
│   │   │   ├── controller/           # gRPC controllers
│   │   │   │   └── ComputationGrpcController.kt
│   │   │   ├── repository/           # Data repositories
│   │   │   │   └── JobRepository.kt
│   │   │   ├── service/              # Business logic services
│   │   │   │   └── ComputationService.kt
│   │   │   └── ComputationServiceMock.kt  # Legacy implementation (delegates to new components)
│   │   └── GrpcMockApplication.kt    # Main application entry point
│   ├── proto/                        # Protocol Buffer definitions
│   │   └── calculation.proto
│   └── resources/                    # Configuration files
│       └── application.properties
└── test/
    └── kotlin/de/laboranowitsch/poc/grpcmock/
        └── backend/                  # Service tests
            ├── controller/           # Controller tests
            │   └── ComputationGrpcControllerTest.kt
            ├── repository/           # Repository tests
            │   └── JobRepositoryTest.kt
            ├── service/              # Service tests
            │   └── ComputationServiceTest.kt
            ├── integration/          # Integration tests
            │   └── ComputationServiceIntegrationTest.kt
            └── ComputationServiceMockTest.kt  # Legacy integration test
```

## Development Workflow

### Setup
1. Clone the repository
2. Ensure you have JDK 21 installed
3. Open the project in your IDE (IntelliJ IDEA recommended for Kotlin)

### Building
```bash
./gradlew build
```

### Running the Application
```bash
./gradlew bootRun
```

### Modifying the API
1. Edit the Protocol Buffer definitions in `src/main/proto/`
2. Rebuild the project to regenerate code:
```bash
./gradlew clean build
```

## Testing
The project uses JUnit 5 for testing with Spring Boot test support.

### Running Tests
```bash
./gradlew test
```

### Test Structure
- Tests use Spring Boot's testing framework with in-process gRPC server/client
- The `@GrpcClient` annotation injects client stubs for testing
- Kotlin coroutines are used for asynchronous testing

## Best Practices

### Protocol Buffers
- Keep message definitions focused and reusable
- Use appropriate field numbers and types
- Document message fields and RPC methods

### gRPC Service Implementation
- Use Kotlin coroutines for asynchronous processing
- Handle errors gracefully and provide meaningful status messages
- Implement proper validation for incoming requests
- Use Flow for streaming responses

### Testing
- Test both happy path and error scenarios
- Verify status transitions and response content
- Use timeouts to prevent hanging tests
- Mock external dependencies when necessary

### Code Organization
- Keep service implementations in the `backend` package
- Store Protocol Buffer definitions in `src/main/proto`
- Follow Kotlin coding conventions
