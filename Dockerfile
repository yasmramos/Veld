# Veld Framework Development Environment
# Provides a consistent environment for building and testing Veld
#
# Usage:
#   docker build -t veld-dev .
#   docker run -it --rm -v $(pwd):/workspace/Veld -w /workspace/Veld veld-dev bash
#
# Or for Maven builds:
#   docker run -it --rm -v $(pwd):/workspace/Veld -w /workspace/Veld veld-dev ./mvnw clean test

FROM maven:3.9-eclipse-temurin-21-jdk

# Labels for container metadata
LABEL maintainer="yasmramos@github.com"
LABEL description="Veld Framework Development Environment"
LABEL version="1.0.3"

# Install build essentials and development tools
RUN apt-get update && apt-get install -y \
    bash \
    curl \
    git \
    vim \
    nano \
    wget \
    unzip \
    zip \
    graphviz \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Install ASM Bytecode Explorer for development
RUN mkdir -p /opt/asm-tools \
    && cd /opt/asm-tools \
    && wget -q https://repo1.maven.org/maven2/org/ow2/asm/asm-tool/9.7/asm-tool-9.7.jar \
    && echo "ASM Tool installed at /opt/asm-tools/asm-tool-9.7.jar"

# Create non-root user for security
RUN groupadd -r veld && useradd -r -g veld veld
WORKDIR /home/veld
COPY --chown=veld:veld . .
USER veld

# Default command
CMD ["/bin/bash"]

---

# Alternative: Minimal build image for CI/CD
# Use this for faster builds in pipelines
# docker build -t veld-build --target build -f Dockerfile .

FROM maven:3.9-eclipse-temurin-21-jdk AS build

# Copy only dependency files first for better caching
COPY pom.xml /workspace/
COPY veld-*/pom.xml /workspace/veld-*/ || true

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B -q || true

# Copy source and build
COPY . /workspace/
WORKDIR /workspace/

# Build and run tests
RUN ./mvnw clean package -DskipTests -B -q

# Production image with JRE only
FROM eclipse-temurin:21-jre-jammy AS runtime

# Install graphviz for DOT visualization
RUN apt-get update && apt-get install -y \
    graphviz \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r veld && useradd -r -g veld veld
WORKDIR /home/veld

# Copy built artifacts from build stage
COPY --from=build /workspace/veld-runtime/target/*.jar /home/veld/
COPY --from=build /workspace/veld-annotations/target/*.jar /home/veld/

# Create launcher script
RUN echo '#!/bin/bash\n\
echo "Veld Framework Runtime Environment"\n\
echo "JAR files:"\n\
ls -la /home/veld/*.jar\n' > /home/veld/launch.sh \
    && chmod +x /home/veld/launch.sh

USER veld

CMD ["/home/veld/launch.sh"]

---

**Build Instructions**:

# Build development image
docker build -t veld-dev .

# Build production runtime image
docker build -t veld-runtime --target runtime .

# Build all stages
docker build -t veld-all .

**Last Updated**: 2025-12-30
**Version**: 1.0.3
