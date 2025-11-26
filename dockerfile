FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copiar o JAR compilado
COPY target/xliff-converter-1.0-SNAPSHOT.jar xliff-converter-1.0-SNAPSHOT.jar

# Copiar os arquivos de recursos necessários
COPY src/main/resource/ src/main/resource/

# Criar diretórios necessários para upload e output
RUN mkdir -p upload-dir files files/DOCX document/docx/

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "xliff-converter-1.0-SNAPSHOT.jar"]