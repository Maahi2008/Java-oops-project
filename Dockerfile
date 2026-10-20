FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy project files
COPY . .

# Compile all Java sources
RUN javac -cp ".:lib/javax.servlet-api-4.0.1.jar" model/*.java service/*.java storage/*.java web/*.java ServerLauncher.java

# Expose standard port (dynamically overridden by cloud provider via $PORT)
EXPOSE 8080

# Run multi-threaded standalone server
CMD ["java", "-cp", ".:lib/javax.servlet-api-4.0.1.jar", "ServerLauncher"]
