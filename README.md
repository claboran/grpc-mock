# gRPC Mock Service

A Kotlin and Spring Boot based gRPC mock service that simulates asynchronous computation processes with streaming status updates.

## Overview

This project demonstrates a gRPC service implementation that processes calculation requests asynchronously and streams back status updates and results. It showcases server-streaming RPC patterns using Kotlin coroutines and Flow.

## Features

- Asynchronous computation simulation
- Server-streaming gRPC implementation
- Status updates via streaming
- Kotlin coroutines integration
- In-memory job repository

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
- **Development Environment**: [JetBrains Junie EAP](https://www.jetbrains.com/junie/) (Highly recommended!)

## Getting Started

### Prerequisites

- JDK 21
- Gradle (or use the included Gradle wrapper)
- IntelliJ IDEA (recommended for Kotlin development)

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

## Development

### Modifying the API

1. Edit the Protocol Buffer definitions in `src/main/proto/`
2. Rebuild the project to regenerate code:
```bash
./gradlew clean build
```

### Testing

The project uses JUnit 5 for testing with Spring Boot test support.

```bash
./gradlew test
```

## Project Structure

```
src/
├── main/
│   ├── kotlin/de/laboranowitsch/poc/grpcmock/
│   │   ├── backend/                  # Service implementations
│   │   │   ├── controller/           # gRPC controllers
│   │   │   ├── repository/           # Data repositories
│   │   │   ├── service/              # Business logic services
│   │   │   └── ComputationServiceMock.kt  # Legacy implementation
│   │   └── GrpcMockApplication.kt    # Main application entry point
│   ├── proto/                        # Protocol Buffer definitions
│   └── resources/                    # Configuration files
└── test/
    └── kotlin/de/laboranowitsch/poc/grpcmock/
        └── backend/                  # Service tests
            ├── controller/           # Controller tests
            ├── repository/           # Repository tests
            ├── service/              # Service tests
            ├── integration/          # Integration tests
            └── ComputationServiceMockTest.kt  # Legacy integration test
```

## Best Practices

For detailed development guidelines and best practices, please refer to our [Developer Guidelines](.junie/guidelines.md).

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is open-source and available under the MIT License.

## Acknowledgements

This project was developed with JetBrains Junie EAP, which provided an excellent development experience for Kotlin and Spring Boot applications.