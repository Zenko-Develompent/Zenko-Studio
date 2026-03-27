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

## Деплой на сервер через auto ci/cd
для начло нужно заполнить .env пример заполнения находиться в .env.example, в принцепе можно полностью его скопировать и вставить в .env и всё запустится 
есть так же work для ci/cd через git action он в папке .github/workflows/deploy.yml с его помошью можно развернуть репозиторий прям на сервере, нужно только заполнить переменные в репозитории они заполнены, нужно изменить только 
![alt text](image.png) 
в deploy keys указать pub ssh сервера, в action 

ENV_SECRET - это env просто предать сюда env

SERVER_USER - это пользователь сервреа для разработки мы использовали root

SSH_PRIVATE_KEY - это приватный ключ при генерации у себя на клиенте выглядит примерно так 
``` bash
-----BEGIN OPENSSH PRIVATE KEY-----
...
-----END OPENSSH PRIVATE KEY-----
```

ZENKO1_HOST - сюда вводится ip серера 

и на сервере нужно указать 
```bash
mkdir -p ~/.ssh 
nano ~/.ssh/authorized_keys
```
сюда предаёться ключ ssh pub 

в итоге в этом случае клонирование репозитория произойдёт по ssh 

# Деплой ручной
Для ручного деплоя, если не учитывать что нужно установить на сервре докер то всё весьма просто 

нужно установить lfs
```bash
apt-get update
apt-get install -y git-lfs
```

Клонируем репозиторий 
```bash 
git clone https://github.com/Zenko-Develompent/Zenko-Studio.git
```
Переходим в репозиторий 
```bash
cd ./Zenko-Studio
```
И докачиваем тяжёлые образы с git 

```bash
git lfs install
git lfs pull --include="runner-images/*"
```

Затем создаём .env
```bash
nano .env
```
заполняем согласно .env.exapmple

затем поднимаем docker 

```bash
docker compose up -d --build
```

После запуска проверьте контейнеры:

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