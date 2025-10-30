# Naming System

A aplicação do Naming System é construída em **Spring Boot 3.5.7**, usando **Java 21**.  
Quando executada, estará disponível na porta `8080` para ser utilizada.

## Pré-requisitos
- Java 21
- Maven 3.8.7 ou superior
- Docker

## Como configurar e executar

> **Importante!**  
> Os passos a seguir usam:
> 
> - Linux (seja uma distro nativa, MacOS ou WSL)
> - Visual Studio Code
> 
> Não são pré-requisitos, mas as instruções abaixo podem precisar de adaptações para funcionarem com outras tecnologias.

### Definir variáveis de ambiente

Após clonar o projeto, crie um arquivo na raiz chamado `.env` e o preencha com o seguinte conteúdo:

```properties
DATABASE_PORT=5432
DATABASE_USER=app
DATABASE_PASSWORD=123456
DATABASE_NAME=naming-system
DATABASE_HOST=localhost

# Todos os valores acima podem ser personalizados
```

### Criar container do banco de dados

Usando o Docker Compose, crie o container do Postgres com o comando:

```bash
docker compose up -d database
```

Use esse comando sempre que precisar iniciar o servidor do Postgres.

### Executar o projeto Spring

Está bastante simplificado. Usando o Visual Studio Code, tecle **`F5`** para iniciar o projeto. Essa ação também inicia o depurador.

Caso tenha problemas ou esteja usando outra IDE, você pode executar o arquivo `run.sh`, presente na pasta `/run` usando o seguinte comando:

```bash
./run/run.sh
```

### Acessar o OpenAPI e o Swagger

O JSON no OpenAPI está disponível na URL <localhost:8080/api-docs>, enquanto o Swagger será encontrado em <http://localhost:8080/swagger-ui/index.html>.

### Encerrar a execução

Finalizados os trabalhos, tecle `Ctrl + C` do terminal onde estiver rodando a aplicação para encerrá-la.

Encerre também o container do Postgres com o comando:

```bash
docker stop database
```
