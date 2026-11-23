# ============================================
# Stage 1: Build Spring Boot JAR (Java 21)
# ============================================
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Tải sẵn dependencies để tận dụng Docker cache
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy mã nguồn (đã kèm frontend build sẵn trong src/main/resources/static)
COPY src ./src
RUN mvn clean package -DskipTests -B

# ============================================
# Stage 2: Runtime nhẹ với JRE 21
# ============================================
FROM eclipse-temurin:21-jre
WORKDIR /app

ENV TZ=Asia/Ho_Chi_Minh

# Tạo sẵn thư mục lưu ảnh upload và sao lưu
RUN mkdir -p /app/uploads /app/backups

COPY --from=builder /build/target/*.jar /app/app.jar

# 8080: Web & REST API | 9090: TCP Socket Server đa luồng
EXPOSE 8080 9090

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-Duser.timezone=Asia/Ho_Chi_Minh", "-jar", "/app/app.jar"]
