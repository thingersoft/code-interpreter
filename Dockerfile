FROM registry.gitlab.informatica.aci.it/ccsc/images/release/java/openjdk:21

LABEL org.opencontainers.image.title="code-interpreter"

RUN addgroup --system spring && useradd --system -g spring spring
USER spring
COPY --chown=spring:spring target/*.jar /apps/app.jar
EXPOSE 8080 9080
ENTRYPOINT ["java", "-jar", "/apps/app.jar"]
