# Build multi-stage: a imagem se constroi a partir do codigo, sem depender de um
# jar compilado antes. Isso e o que permite o provedor de hospedagem construir
# direto do repositorio, em vez de puxar imagem de registry privado - e mantem o
# resultado reproduzivel por quem tem so o repo em maos.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# pom primeiro, sozinho: enquanto ele nao mudar, a camada de dependencias fica
# em cache e o build nao rebaixa a internet toda a cada alteracao de codigo
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# sem testes de proposito: o gate e o job build-and-test do pipeline, que roda a
# suite inteira com Docker disponivel para os testes de container. Repetir aqui
# so tornaria o build da imagem mais lento, e sem Docker os ContainerTest
# pulariam de qualquer forma
RUN mvn -B -q clean package -DskipTests

# a imagem oficial openjdk foi descontinuada e as tags sairam do ar (17-jdk-slim
# responde 404 no Docker Hub). eclipse-temurin e a substituta e e a mesma
# distribuicao que o build usa no pipeline. jre basta: aqui so roda o jar
FROM eclipse-temurin:17-jre-jammy
VOLUME /tmp

# o tess4j e um binding JNA para a libtesseract nativa; sem ela instalada o
# endpoint de importacao por OCR quebra em runtime dentro do container
RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-por \
    && rm -rf /var/lib/apt/lists/* \
    && dirname "$(find /usr/share -name 'por.traineddata' | head -1)" > /tessdata-path

# default para rodar o container contra o banco da propria maquina. Ambiente
# hospedado sobrescreve com dev, stg ou prd
ENV SPRING_PROFILES_ACTIVE=docker
COPY --from=build /build/target/*.jar app.jar

# o diretorio do tessdata muda conforme a versao empacotada pela distribuicao,
# entao ele e descoberto no build em vez de fixado no Dockerfile
ENTRYPOINT ["sh", "-c", "export TESSDATA_PREFIX=$(cat /tessdata-path) && exec java -jar /app.jar"]
