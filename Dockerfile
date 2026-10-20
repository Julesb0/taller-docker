# ==========================================================
# Etapa 1 (builder): compila eco-air-backend con Maven + JDK 17
# ==========================================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Primero solo el pom.xml. Mientras el pom no cambie, Docker reutiliza
# esta capa y no vuelve a descargar las dependencias.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Despues el codigo fuente, que es lo que cambia seguido.
COPY src ./src

# ID del estudiante. Va despues de las dependencias para que cambiarlo
# no invalide el cache de Maven. Si no llega, el build se detiene.
ARG STUDENT_ID
RUN if [ -z "$STUDENT_ID" ]; then echo "ERROR: falta --build-arg STUDENT_ID"; exit 1; fi; \
    echo "Compilando eco-air-backend para el estudiante: $STUDENT_ID"

# Empaqueta el .jar ejecutable sin correr las pruebas.
RUN mvn package -DskipTests -B

# ==========================================================
# Etapa 2 (runtime): solo el JRE 17 y el .jar
# ==========================================================
FROM eclipse-temurin:17-jre-alpine AS runtime

# Usuario del sistema sin privilegios (UID 10001) y carpeta /app a su nombre.
RUN addgroup -S -g 10001 sensorgroup \
 && adduser -S -D -H -u 10001 -G sensorgroup -s /sbin/nologin sensor_user \
 && mkdir -p /app \
 && chown sensor_user:sensorgroup /app

WORKDIR /app

# Del builder solo se copia el .jar: ni Maven, ni el JDK, ni el codigo fuente.
COPY --from=builder --chown=sensor_user:sensorgroup /build/target/*.jar app.jar

# Ergonomia de la JVM: que lea el limite de memoria del contenedor (cgroups)
# y calcule el heap como porcentaje de ese limite.
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+ExitOnOutOfMemoryError"

# Los ARG no pasan de una etapa a otra: hay que declararlo otra vez aca.
ARG STUDENT_ID
ENV OPERATOR_ID=${STUDENT_ID}

USER sensor_user
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
