FROM maven:3.9.12-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
COPY kanon-common/pom.xml kanon-common/pom.xml
COPY kanon-domain/pom.xml kanon-domain/pom.xml
COPY kanon-tenant/pom.xml kanon-tenant/pom.xml
COPY kanon-policy/pom.xml kanon-policy/pom.xml
COPY kanon-ingestion/pom.xml kanon-ingestion/pom.xml
COPY kanon-config/pom.xml kanon-config/pom.xml
COPY kanon-ai-routing/pom.xml kanon-ai-routing/pom.xml
COPY kanon-agent-runtime/pom.xml kanon-agent-runtime/pom.xml
COPY kanon-workflow/pom.xml kanon-workflow/pom.xml
COPY kanon-evidence/pom.xml kanon-evidence/pom.xml
COPY kanon-annotation/pom.xml kanon-annotation/pom.xml
COPY kanon-api/pom.xml kanon-api/pom.xml
COPY kanon-ui/pom.xml kanon-ui/pom.xml
COPY kanon-dataset/pom.xml kanon-dataset/pom.xml
COPY kanon-training/pom.xml kanon-training/pom.xml
COPY kanon-model-registry/pom.xml kanon-model-registry/pom.xml
COPY kanon-active-learning/pom.xml kanon-active-learning/pom.xml
COPY kanon-bootstrap/pom.xml kanon-bootstrap/pom.xml
RUN mvn -B -DskipTests dependency:go-offline
COPY . .
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN addgroup --system kanon && adduser --system --ingroup kanon kanon
COPY --from=build /workspace/kanon-bootstrap/target/kanon-bootstrap-*.jar /app/kanon-bootstrap.jar
USER kanon:kanon
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=cloud
ENTRYPOINT ["java", "-jar", "/app/kanon-bootstrap.jar"]
