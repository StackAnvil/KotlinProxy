FROM eclipse-temurin:25-jre-alpine
WORKDIR /app/run
COPY build/libs/ /app/libs/
RUN find /app/libs -maxdepth 1 -name 'KotlinProxy-*.jar' \
    ! -name '*java8.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' \
    -exec cp {} /app/KotlinProxy.jar \;
ENTRYPOINT ["java", "-jar", "/app/KotlinProxy.jar", "config", "viaproxy.yml"]
