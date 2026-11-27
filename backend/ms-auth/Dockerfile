# Imagen para publicar el microservicio en internet (Render u otro hosting con Docker).
# Etapa 1: compila el proyecto con el Maven Wrapper (sin correr las pruebas, que ya corren en el PC).
FROM eclipse-temurin:25-jdk AS compilacion
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && sh mvnw -q -B dependency:go-offline
COPY src src
RUN sh mvnw -q -B package -DskipTests

# Etapa 2: solo el .jar y Java (imagen liviana)
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=compilacion /app/target/*.jar app.jar
# El plan gratis tiene 512 MB de RAM: se limita la memoria de Java
ENV JAVA_TOOL_OPTIONS="-Xmx300m -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
