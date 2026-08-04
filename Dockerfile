FROM openjdk:17-jdk-slim
VOLUME /tmp

# o tess4j e um binding JNA para a libtesseract nativa; sem ela instalada o
# endpoint de importacao por OCR quebra em runtime dentro do container
RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-por \
    && rm -rf /var/lib/apt/lists/* \
    && dirname "$(find /usr/share -name 'por.traineddata' | head -1)" > /tessdata-path

ENV SPRING_PROFILES_ACTIVE=docker
COPY target/*.jar app.jar

# o diretorio do tessdata muda conforme a versao empacotada pela distribuicao,
# entao ele e descoberto no build em vez de fixado no Dockerfile
ENTRYPOINT ["sh", "-c", "export TESSDATA_PREFIX=$(cat /tessdata-path) && exec java -jar /app.jar"]