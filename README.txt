# Petfy HealthCare

Petfy HealthCare é um sistema para gestão de vacinação digital de pets, clínicas veterinárias,
donos (owners) e profissionais de saúde animal. O projeto foi construído em Java 17 com Spring
Boot e PostgreSQL, com foco em uma futura arquitetura baseada em microserviços e comunicação
orientada a eventos.

## Tecnologias Utilizadas

- Java 17 + Spring Boot (2.7.18)
- PostgreSQL
- Lombok
- Hibernate / JPA
- Docker + Docker Compose (PostgreSQL)
- RabbitMQ (futuro uso com eventos)
- IntelliJ IDEA
- UUIDs como identificadores

## Estrutura Atual

- Entidades: Owner, Pet, Vaccine, Clinic, HealthRecord
- DTOs para requests e responses
- Services com lógica de negócio
- Controllers REST com @RestController
- Tratamento global de exceções
- Enum para mensagens de erro padronizadas

## Endpoints Principais (exemplos)

- POST /owners/include - Criação de dono
- GET /pets/{id} - Busca por pet
- PUT /clinics/{id} - Atualiza clínica
- DELETE /vaccines/{id} - Remove vacinação

## Arquitetura

Neste momento: Monolito estruturado com padrão de microsserviços.
Futuramente: Será quebrado em serviços independentes:
- Auth Service
- Owner Service
- Pet Service
- Vaccine Service
- Clinic Service
- HealthRecord Service
- API Gateway

## Observações

- Atualizações parciais (ex: update do pet) utilizam operadores ternários para evitar sobrescrita de campos não enviados.

- Campos de auditoria (creationDate, updateDate) estão presentes em todas as entidades.

- As mensagens de erro são centralizadas via ErrorMessageEnum.