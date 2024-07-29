# run this after: ./gradlew.bat :bootJar
FROM mcr.microsoft.com/openjdk/jdk:17-ubuntu

ARG TAG

# 将构建好的jar文件复制到镜像中
COPY build/libs/*.jar app.jar

# 暴露8080端口
EXPOSE 9886

# 设置环境变量
ENV APP_VERSION_CODE_NAME=$TAG

# 启动Spring Boot应用
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]