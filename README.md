## Комманда Zenko Studio 

# Руководство по Развёртыванию сервера 

## Стек

- Docker version 29.3.0, build 5927d80
- Docker Compose version v5.1.1
- git version 2.43.0
- git-lfs/3.4.1 (GitHub; linux amd64; go 1.22.2)
- Свободные порты 5432 бд
- Свободные порты 8080 backend
- Свободные потры 3000 front

# Деплой ручной
Для ручного деплоя, если не учитывать что docker уже установлен

1.нужно установить lfs
```bash
apt-get update
apt-get install -y git-lfs
```

2. делаем пространство и клонируем репозитории 
```bash
mkdir my-app && cd my-app
git clone https://github.com/Zenko-Develompent/Zenko-Studio.git
git clone https://github.com/Zenko-Develompent/hackaton-front.git
```

3. Переходим в репозиторий 
```bash
cd ./Zenko-Studio
```
И докачиваем тяжёлые образы с git 

```bash
git lfs install
git lfs pull --include="runner-images/*"
```

4. Затем создаём .env и в hackaton-front, и в Zenko-Studio, но, хоть это и не правильно мы не удаляли env из репозитория и недобавляли в .gitignore
```bash
nano .env
```
заполняем согласно .env.exapmple

5. Создаём docker-compose.yml его можно взять из репозитория Zenko-Studio, но не в коем случае не запускать его находясь в этой деректории, его нужно запустить из my-app, но пред этим нужно создать docker-compose.yml в my-app. 

На всякий случай снизу там где и env прикрепляю docker-compose.yml 

6.затем поднимаем docker  
### !!НУЖНО НАХОДИТЬСЯ В my-app!!
```bash
docker compose up -d --build
```

7. После запуска проверьте контейнеры:

```bash
docker ps
```


вот .env рабочий на всякий случай 

```m
BACKEND_PORT=8080
DB_URL=jdbc:postgresql://edu-db:5432/edu_db
DB_USER=postgres
DB_PASS=postgres
DB_NAME=edu_db

MASTER_KEY_B64=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=
ACCESS_TTL_SEC=900
REFRESH_TTL_DAYS=30
WEB_SESSION_TTL_SEC=86400

RL_LOGIN_MAX=8
RL_LOGIN_WINMS=300000

COOKIES_SECURE=false
COOKIES_DOMAIN=
CORS_ALLOW_ALL=true
LESSON_CONTENT_ROOT=
LESSON_CONTENT_HOST_PATH=./lesson-content
CODE_RUNNER_ENABLED=true
CODE_RUNNER_DOCKER_COMMAND=docker
CODE_RUNNER_TIMEOUT_MS=5000
CODE_RUNNER_MEMORY_MB=128
CODE_RUNNER_CPUS=0.5
CODE_RUNNER_PIDS_LIMIT=64
CODE_RUNNER_MAX_CODE_LENGTH=20000
CODE_RUNNER_MAX_INPUT_LENGTH=4000
CODE_RUNNER_MAX_OUTPUT_LENGTH=20000

# Social chat + websocket
CHAT_WS_ENDPOINT=/ws/social
CHAT_WS_ALLOWED_ORIGINS=*
CHAT_WS_BROKER_PREFIXES=/topic,/queue
CHAT_WS_APP_PREFIX=/app
CHAT_WS_USER_PREFIX=/user
CHAT_WS_USER_CHAT_DESTINATION=/queue/social/chat
CHAT_WS_USER_FRIEND_DESTINATION=/queue/social/friends
CHAT_WS_USER_PARENT_CONTROL_DESTINATION=/queue/social/parent-control
CHAT_CHATS_DEFAULT_LIMIT=20
CHAT_CHATS_MAX_LIMIT=100
CHAT_MESSAGES_DEFAULT_LIMIT=50
CHAT_MESSAGES_MAX_LIMIT=200
CHAT_FRIEND_REQUESTS_DEFAULT_LIMIT=20
CHAT_FRIEND_REQUESTS_MAX_LIMIT=50
CHAT_FRIENDS_DEFAULT_LIMIT=50
CHAT_FRIENDS_MAX_LIMIT=200
CHAT_USER_SEARCH_DEFAULT_LIMIT=20
CHAT_USER_SEARCH_MAX_LIMIT=50
CHAT_USER_SEARCH_MIN_QUERY_LENGTH=2
CHAT_USER_SEARCH_MAX_QUERY_LENGTH=100

# Seed
SEED_ENABLED=true
SEED_ONLY_IF_EMPTY=true
SEED_ADMIN_USERNAME=admin
SEED_ADMIN_PASSWORD=Admin1234
SEED_ADMIN_AGE=30
SEED_COURSE_NAME=Demo Course
SEED_COURSE_DESCRIPTION=Seeded course
SEED_COURSE_CATEGORY=demo
SEED_MODULE_NAME=Demo Module

PORT=
JWT_TIME=
REFRESH_TIME=
# mayd by zenko
```

вот docker-compose.yml

```m
version: '3.9'

services:
  backend:
    build:
      context: ./Zenko-Studio
    container_name: edu-backend
    ports:
      - "${BACKEND_PORT:-8080}:8080"
    env_file:
      - ./Zenko-Studio/.env
    environment:
      LESSON_CONTENT_ROOT: /data/lessons
      CODE_RUNNER_WORKSPACE_ROOT: ${CODE_RUNNER_WORKSPACE_ROOT:-/var/lib/edu-runner-work}
    volumes:
      - ${LESSON_CONTENT_HOST_PATH:-./Zenko-Studio/lesson-content}:/data/lessons:rw
      - ${CODE_RUNNER_WORKSPACE_HOST_PATH:-/var/lib/edu-runner-work}:${CODE_RUNNER_WORKSPACE_ROOT:-/var/lib/edu-runner-work}:rw
      - /var/run/docker.sock:/var/run/docker.sock
      - /usr/bin/docker:/usr/bin/docker:ro
    depends_on:
      - edu-db
    networks:
      - edu-network

  frontend:
    build:
      context: ./hackaton-front
      dockerfile: Dockerfile
    container_name: edu-frontend
    ports:
      - "3000:3000"
    env_file:
      - ./hackaton-front/.env
    environment:
      NEXT_PUBLIC_API_URL: http://backend:8080
      NEXT_PUBLIC_WS_URL: http://backend:8080
    depends_on:
      - backend
    networks:
      - edu-network

  edu-db:
    image: postgres:15
    container_name: edu-db
    env_file:
      - ./Zenko-Studio/.env
    environment:
      POSTGRES_DB: ${DB_NAME:-edu_db}
      POSTGRES_USER: ${DB_USER:-postgres}
      POSTGRES_PASSWORD: ${DB_PASS:-postgres}
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    networks:
      - edu-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER:-postgres} -d ${DB_NAME:-edu_db}"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  pgdata:

networks:
  edu-network:
    driver: bridge

```