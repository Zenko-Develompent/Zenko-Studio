# Content API Documentation

Документация по текущим read-ручкам контента и загрузке markdown для уроков.

- Базовый префикс: `/api`
- Формат ответов: `application/json`
- Аутентификация: для большинства read GET-ручек не требуется, но progress-ручки (`/progress`) требуют `Authorization: Bearer <access_token>`.

## 1. Общие правила

### 1.1 Типы

- `UUID`: строка UUID.
- `string`: строка.
- `int`: целое число.
- `long`: целое число.
- `nullable`: поле может быть `null`.

### 1.2 Пагинация

Для `GET /api/courses`:

- `page` (`int`, default `0`)
- `size` (`int`, default `20`)

Ограничения на бэке:

- `page < 0` -> `0`
- `size < 1` -> `1`
- `size > 100` -> `100`

### 1.3 Сортировки

- Курсы: `name ASC`.
- Модули в курсе: `name ASC`, затем `moduleId`.
- Уроки в модуле: `name ASC`, затем `lessonId`.
- Вопросы: `createdAt ASC`, затем `questId`.
- Таски экзамена: `createdAt ASC`, затем `tasksId`.

## 2. Ошибки

Формат бизнес-ошибки:

```json
{
  "error": "course_not_found"
}
```

Основные коды ошибок контента:

- `course_not_found`
- `module_not_found`
- `lesson_not_found`
- `quiz_not_found`
- `exam_not_found`
- `task_not_found`
- `answer_not_in_question`
- `quiz_question_out_of_order`
- `exam_not_completed`
- `module_locked`
- `lesson_locked`
- `lesson_quiz_not_completed`
- `exam_locked`
- `user_not_found`
- `unauthorized`
- `forbidden`
- `runner_language_not_supported`
- `task_expected_output_not_configured`
- `task_language_mismatch`
- `code_invalid`
- `code_too_large`
- `task_input_too_large`
- `runner_unavailable`
- `community_period_invalid`
- `community_metric_invalid`
- `social_not_available_for_parent`
- `chat_with_parent_forbidden`
- `parent_control_request_self`
- `parent_control_request_already_exists`
- `parent_control_already_active`
- `parent_control_request_not_found`
- `parent_control_request_not_pending`
- `parent_control_forbidden`
- `parent_control_parent_role_required`
- `parent_control_child_role_required`
- `invalid_username`
- `weak_password`
- `invalid_age`
- `invalid_role`
- `conflict`
- `too_many_attempts`
- `invalid_credentials`
- `invalid`
- `invalid_access_token`
- `access_token_expired`

Коды ошибок чтения markdown:

- `lesson_content_not_found`
- `lesson_content_not_markdown`
- `lesson_content_path_invalid`
- `lesson_content_ambiguous`
- `lesson_content_read_failed`

HTTP-коды:

- `200 OK`: успех.
- `400 Bad Request`: невалидный запрос/путь/тип файла.
- `401 Unauthorized`: отсутствует/невалидный access token.
- `403 Forbidden`: недостаточно прав (например, нужен admin).
- `404 Not Found`: сущность или markdown не найден.
- `409 Conflict`: для папки урока найдено более одного `.md`.
- `503 Service Unavailable`: раннер временно недоступен.
- `500 Internal Server Error`: непредвиденная ошибка.

Глобальный формат для `500`:

```json
{
  "error": "error"
}
```

## 3. Эндпоинты

### 3.0 Auth

Базовый префикс auth: `/api/auth`.

#### `POST /api/auth/register`

Регистрация пользователя.

Body:

```json
{
  "username": "student_1",
  "password": "Stud12345",
  "age": 12,
  "role": "student"
}
```

`role`: `student | parent`.

Ответ `201`:

```json
{
  "accessToken": "jwt",
  "accessExpiresAt": "2026-03-27T15:10:00Z",
  "refreshToken": "tokenId.raw",
  "refreshExpiresAt": "2026-04-26T15:10:00Z",
  "user": {
    "id": "uuid",
    "username": "student_1",
    "birthDate": "2014-03-27",
    "age": 12,
    "role": "student",
    "xp": 0,
    "level": 0,
    "coins": 0
  }
}
```

Дополнительно backend ставит HttpOnly-cookie `refresh` (path `/api`).

Ошибки:

- `400 invalid_username`
- `400 weak_password`
- `400 invalid_age`
- `400 invalid_role`
- `409 conflict` (логин уже занят)

#### `POST /api/auth/login`

Логин пользователя.

Body:

```json
{
  "id": "student_1",
  "password": "Stud12345"
}
```

Допустимо передавать `username` вместо `id`.

Ответ `200`:

```json
{
  "accessToken": "jwt",
  "accessExpiresAt": "2026-03-27T15:10:00Z",
  "refreshToken": "tokenId.raw",
  "refreshExpiresAt": "2026-04-26T15:10:00Z",
  "user": {
    "id": "uuid",
    "username": "student_1",
    "birthDate": "2014-03-27",
    "age": 12,
    "role": "student",
    "xp": 0,
    "level": 0,
    "coins": 0
  }
}
```

Дополнительно backend ставит HttpOnly-cookie `refresh` (path `/api`).

Ошибки:

- `400 bad_request` (пустые поля)
- `401 invalid_credentials`
- `429 too_many_attempts`

#### `POST /api/auth/refresh`

Обновление access-токена по refresh-токену.

Refresh можно передать:

- в body: `{"refreshToken":"..."}`
- или через cookie `refresh`

Body (optional):

```json
{
  "refreshToken": "tokenId.raw"
}
```

Ответ `200`: формат такой же, как у `POST /api/auth/login`.

При успехе refresh-cookie обновляется.

Ошибки:

- `401 invalid`

#### `POST /api/auth/logout`

Выход из сессии. Ревокация семейства refresh-токенов (если передан валидный refresh).

Body (optional):

```json
{
  "refreshToken": "tokenId.raw"
}
```

Также может использоваться cookie `refresh`.

Ответ `200` с пустым телом.  
Backend очищает auth-cookie (`refresh`).

#### `GET /api/auth/profile`

Приватный профиль текущего пользователя.

Headers:

- `Authorization: Bearer <access_token>` (required)

Ответ `200`:

```json
{
  "username": "student_1",
  "xp": 120,
  "level": 2,
  "coins": 35,
  "achievements": [
    {
      "achievementId": "uuid",
      "name": "Первый шаг"
    }
  ]
}
```

Ошибки:

- `401 unauthorized` (нет/битый Bearer)
- `401 invalid_access_token`
- `401 access_token_expired`

#### `GET /api/users/{userId}/profile`

Публичный профиль пользователя (без приватных полей).

Headers:

- не требуется

Ответ `200`:

```json
{
  "username": "student_1",
  "level": 2,
  "exp": 120,
  "achievements": [
    {
      "achievementId": "uuid",
      "name": "Первый шаг"
    }
  ]
}
```

В публичном профиле нет `coins`.

Ошибки:

- `404 user_not_found`

### 3.1 Курсы

#### `GET /api/courses`

Query:

- `page` (`int`, optional)
- `size` (`int`, optional)

Ответ `200`:

```json
{
  "items": [
    {
      "courseId": "uuid",
      "name": "Python Basic",
      "description": "Базовый курс",
      "category": "programming",
      "moduleCount": 5,
      "lessonCount": 24
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1
}
```

#### `GET /api/courses/{courseId}`

Headers (optional):

- `Authorization: Bearer <access_token>`

Если токен передан, в элементах `modules[]` заполняется `unlocked` для текущего пользователя.
Если токен не передан, `unlocked = null`.

Ответ `200`:

```json
{
  "courseId": "uuid",
  "name": "Python Basic",
  "description": "Базовый курс",
  "category": "programming",
  "modules": [
    {
      "moduleId": "uuid",
      "name": "Условия",
      "description": "if/else",
      "lessonCount": 6,
      "examId": "uuid",
      "unlocked": true
    }
  ]
}
```

Ошибки:

- `401`: `unauthorized` (только если передан невалидный токен)
- `404`: `course_not_found`

#### `GET /api/courses/{courseId}/modules`

Headers (optional):

- `Authorization: Bearer <access_token>`

Если токен передан, в элементах `items[]` заполняется `unlocked` для текущего пользователя.
Если токен не передан, `unlocked = null`.

Ответ `200`:

```json
{
  "items": [
    {
      "moduleId": "uuid",
      "name": "Условия",
      "description": "if/else",
      "lessonCount": 6,
      "examId": "uuid",
      "unlocked": true
    }
  ]
}
```

Ошибки:

- `401`: `unauthorized` (только если передан невалидный токен)
- `404`: `course_not_found`

#### `GET /api/courses/{courseId}/tree`

Headers (optional):

- `Authorization: Bearer <access_token>`

Если токен передан, заполняются `module.unlocked` и `lesson.unlocked` для текущего пользователя.
Если токен не передан, `unlocked = null`.

Ответ `200`:

```json
{
  "courseId": "uuid",
  "name": "Python Basic",
  "modules": [
    {
      "moduleId": "uuid",
      "name": "Условия",
      "examId": "uuid",
      "unlocked": true,
      "lessons": [
        {
          "lessonId": "uuid",
          "name": "if",
          "quizId": "uuid",
          "taskId": "uuid",
          "unlocked": true
        }
      ]
    }
  ]
}
```

Ошибки:

- `401`: `unauthorized` (только если передан невалидный токен)
- `404`: `course_not_found`

### 3.2 Модули

#### `GET /api/modules/{moduleId}`

Ответ `200`:

```json
{
  "moduleId": "uuid",
  "courseId": "uuid",
  "name": "Условия",
  "description": "if/else",
  "exam": {
    "examId": "uuid",
    "name": "Экзамен по модулю",
    "questionsCount": 10,
    "tasksCount": 2
  }
}
```

`exam` может быть `null`.

Ошибки:

- `404`: `module_not_found`

#### `GET /api/modules/{moduleId}/lessons`

Headers (optional):

- `Authorization: Bearer <access_token>`

Если токен передан, в элементах `items[]` заполняется `unlocked` для текущего пользователя.
Если токен не передан, `unlocked = null`.

Ответ `200`:

```json
{
  "items": [
    {
      "lessonId": "uuid",
      "name": "if",
      "description": "Введение",
      "xp": 10,
      "quizId": "uuid",
      "taskId": "uuid",
      "unlocked": true
    }
  ]
}
```

Ошибки:

- `401`: `unauthorized` (только если передан невалидный токен)
- `404`: `module_not_found`

#### `GET /api/modules/{moduleId}/exam`

Ответ `200`:

```json
{
  "examId": "uuid",
  "moduleId": "uuid",
  "name": "Экзамен по модулю",
  "description": "Финальная проверка",
  "questionsCount": 10,
  "tasksCount": 2
}
```

Ошибки:

- `404`: `exam_not_found`

### 3.3 Уроки

#### `GET /api/lessons/{lessonId}`

Возвращает метаданные урока и `content` с raw markdown-текстом.

Ответ `200`:

```json
{
  "lessonId": "uuid",
  "moduleId": "uuid",
  "name": "if",
  "description": "Введение",
  "content": "# Условные операторы\n\nТекст урока...",
  "xp": 10,
  "quizId": "uuid",
  "taskId": "uuid"
}
```

`content` может быть `null`, если `lesson.body` пустой.

Ошибки:

- `404`: `lesson_not_found`
- `404`: `lesson_content_not_found`
- `400`: `lesson_content_not_markdown`
- `400`: `lesson_content_path_invalid`
- `409`: `lesson_content_ambiguous`
- `500`: `lesson_content_read_failed`

#### `GET /api/lessons/{lessonId}/quiz`

Ответ `200`:

```json
{
  "quizId": "uuid",
  "lessonId": "uuid",
  "name": "Квиз по уроку",
  "description": "Проверка темы",
  "questionsCount": 5
}
```

Ошибки:

- `404`: `quiz_not_found`

#### `GET /api/lessons/{lessonId}/task`

Ответ `200`:

```json
{
  "taskId": "uuid",
  "lessonId": "uuid",
  "examId": "uuid",
  "name": "Практическая задача",
  "description": "Решить задачу",
  "runnerLanguage": "java",
  "xpReward": 20,
  "coinReward": 10
}
```

`examId` может быть `null`.

Ошибки:

- `404`: `task_not_found`

### 3.4 Квизы

#### `GET /api/quizzes/{quizId}/questions`

Ответ `200`:

```json
{
  "items": [
    {
      "questId": "uuid",
      "quizId": "uuid",
      "examId": null,
      "name": "Вопрос 1",
      "description": "Текст вопроса"
    }
  ]
}
```

Ошибки:

- `404`: `quiz_not_found`

#### `POST /api/quizzes/{quizId}/start`

Старт или продолжение прохождения квиза для текущего пользователя.

Headers:

- `Authorization: Bearer <access_token>` (required)

Body: не требуется.

Ответ `200`:

```json
{
  "completed": false,
  "question": {
    "questionId": "uuid",
    "name": "Вопрос 1",
    "description": "Текст вопроса",
    "index": 1,
    "total": 5,
    "options": [
      {
        "answerId": "uuid",
        "name": "Вариант A",
        "description": "..."
      }
    ]
  }
}
```

Если квиз уже завершен:

```json
{
  "completed": true,
  "question": null
}
```

Запуск практики выполняется отдельным API: `POST /api/tasks/{taskId}/start`.

Ошибки:

- `401`: `unauthorized`
- `404`: `quiz_not_found`
- `409`: `module_locked`
- `409`: `lesson_locked`

#### `POST /api/quizzes/{quizId}/answer`

Отправка ответа на текущий вопрос (строго по порядку).

Headers:

- `Authorization: Bearer <access_token>` (required)

Body:

```json
{
  "questionId": "uuid",
  "answerId": "uuid"
}
```

Ответ `200`:

```json
{
  "correct": true,
  "completed": false,
  "xpGranted": 0,
  "coinGranted": 0,
  "question": {
    "questionId": "uuid",
    "name": "Вопрос 2",
    "description": "Текст вопроса",
    "index": 2,
    "total": 5,
    "options": [
      {
        "answerId": "uuid",
        "name": "Вариант A",
        "description": "..."
      }
    ]
  }
}
```

Ошибки:

- `401`: `unauthorized`
- `404`: `quiz_not_found`
- `400`: `answer_not_in_question`
- `409`: `quiz_question_out_of_order`
- `409`: `module_locked`
- `409`: `lesson_locked`

### 3.5 Экзамены

#### `GET /api/exams/{examId}`

Ответ `200`:

```json
{
  "examId": "uuid",
  "moduleId": "uuid",
  "name": "Экзамен по модулю",
  "description": "Финальная проверка",
  "xpReward": 100,
  "coinReward": 50,
  "questionsCount": 10,
  "tasksCount": 2
}
```

Ошибки:

- `404`: `exam_not_found`

#### `GET /api/exams/{examId}/questions`

Ответ `200`:

```json
{
  "items": [
    {
      "questId": "uuid",
      "quizId": null,
      "examId": "uuid",
      "name": "Экзаменационный вопрос",
      "description": "Текст вопроса"
    }
  ]
}
```

Ошибки:

- `404`: `exam_not_found`

#### `GET /api/exams/{examId}/tasks`

Ответ `200`:

```json
{
  "items": [
    {
      "taskId": "uuid",
      "examId": "uuid",
      "lessonId": "uuid",
      "name": "Экзаменационный таск",
      "description": "Описание таска",
      "runnerLanguage": "bash",
      "xpReward": 30,
      "coinReward": 15
    }
  ]
}
```

`lessonId` может быть `null`.

Ошибки:

- `404`: `exam_not_found`

#### `POST /api/exams/{examId}/complete`

Помечает экзамен завершенным для текущего пользователя и выдает награду только при первом полном завершении.

Headers:

- `Authorization: Bearer <access_token>` (required)

Body: не требуется.

Ответ `200`:

```json
{
  "completed": true,
  "firstCompletion": true,
  "xpGranted": 100,
  "coinGranted": 50,
  "questionsDone": 10,
  "questionsTotal": 10,
  "tasksDone": 2,
  "tasksTotal": 2
}
```

Ошибки:

- `401`: `unauthorized`
- `404`: `exam_not_found`
- `409`: `exam_not_completed`
- `409`: `module_locked`
- `409`: `exam_locked`

#### `PUT /api/exams/{examId}/rewards`

Обновляет награды экзамена (только для admin).

Headers:

- `Authorization: Bearer <access_token>` (required, role `admin`)

Body:

```json
{
  "xpReward": 100,
  "coinReward": 50
}
```

Ответ `200`:

```json
{
  "examId": "uuid",
  "moduleId": "uuid",
  "name": "Экзамен по модулю",
  "description": "Финальная проверка",
  "xpReward": 100,
  "coinReward": 50,
  "questionsCount": 10,
  "tasksCount": 2
}
```

Ошибки:

- `401`: `unauthorized`
- `403`: `forbidden`
- `404`: `exam_not_found`

### 3.6 Таски

#### `POST /api/tasks/{taskId}/start`

Отдельный старт практической задачи.  
Для задач урока запуск доступен только после полного прохождения квиза этого урока.

Headers:

- `Authorization: Bearer <access_token>` (required)

Body: не требуется.

Ответ `200`:

```json
{
  "taskId": "uuid",
  "lessonId": "uuid",
  "examId": null,
  "completed": false
}
```

Ошибки:

- `401`: `unauthorized`
- `404`: `task_not_found`
- `409`: `module_locked`
- `409`: `lesson_locked`
- `409`: `lesson_quiz_not_completed`
- `409`: `exam_locked`

#### `POST /api/tasks/{taskId}/complete`

Помечает таск завершенным для текущего пользователя.
Награда выдается только при первом прохождении таска в рамках урока (`examId = null`).
Для экзаменационных тасков (`examId != null`) отдельная награда не выдается.

Headers:

- `Authorization: Bearer <access_token>` (required)

Body: не требуется.

Ответ `200`:

```json
{
  "taskId": "uuid",
  "lessonId": "uuid",
  "examId": null,
  "completed": true,
  "firstCompletion": true,
  "xpGranted": 20,
  "coinGranted": 10
}
```

Ошибки:

- `401`: `unauthorized`
- `404`: `task_not_found`
- `409`: `module_locked`
- `409`: `lesson_locked`
- `409`: `lesson_quiz_not_completed`
- `409`: `exam_locked`

#### `PUT /api/tasks/{taskId}/rewards`

Обновляет награды таска (только для admin).

Headers:

- `Authorization: Bearer <access_token>` (required, role `admin`)

Body:

```json
{
  "xpReward": 20,
  "coinReward": 10
}
```

Ответ `200`:

```json
{
  "taskId": "uuid",
  "lessonId": "uuid",
  "examId": null,
  "xpReward": 20,
  "coinReward": 10
}
```

Ошибки:

- `401`: `unauthorized`
- `403`: `forbidden`
- `404`: `task_not_found`

#### `PUT /api/tasks/{taskId}/runner`

Обновляет конфиг раннера для таска (только для admin).
Эталонный ответ и входные данные хранятся в БД и не отдаются в публичных API.

Headers:

- `Authorization: Bearer <access_token>` (required, role `admin`)

Body:

```json
{
  "runnerLanguage": "java",
  "expectedOutput": "Hello, world!",
  "inputData": ""
}
```

`runnerLanguage` может быть `null`/пустым (тогда ограничения по языку нет).
`inputData` может быть `null`/пустым или отсутствовать (тогда задача запускается без входных данных).

Ответ `200`:

```json
{
  "taskId": "uuid",
  "runnerLanguage": "java",
  "hasExpectedOutput": true,
  "hasInputData": true
}
```

Ошибки:

- `400`: `runner_language_not_supported`
- `401`: `unauthorized`
- `403`: `forbidden`
- `404`: `task_not_found`

#### `POST /api/tasks/{taskId}/run`

Запускает пользовательский код в sandbox Docker, сравнивает stdout с эталонным ответом таска.
Входные данные (`stdin`) подставляются сервером из `task.inputData`; если `inputData` не задан, используется пустой ввод.
Если результат корректный, таск отмечается завершенным (и при первом прохождении выдаются награды).

Headers:

- `Authorization: Bearer <access_token>` (required)

Body:

```json
{
  "language": "java",
  "code": "public class Main { public static void main(String[] args) { System.out.println(\"Hello, world!\"); } }"
}
```

Ответ `200`:

```json
{
  "taskId": "uuid",
  "language": "java",
  "status": "ok",
  "correct": true,
  "stdout": "Hello, world!\n",
  "stderr": "",
  "exitCode": 0,
  "timedOut": false,
  "durationMs": 284,
  "completed": true,
  "firstCompletion": true,
  "xpGranted": 20,
  "coinGranted": 10
}
```

`status`: `ok | compile_error | runtime_error | timeout`.

Ошибки:

- `400`: `runner_language_not_supported`
- `400`: `code_invalid`
- `400`: `code_too_large`
- `401`: `unauthorized`
- `404`: `task_not_found`
- `409`: `task_expected_output_not_configured`
- `409`: `task_language_mismatch`
- `409`: `task_input_too_large`
- `409`: `module_locked`
- `409`: `lesson_locked`
- `409`: `lesson_quiz_not_completed`
- `409`: `exam_locked`
- `503`: `runner_unavailable`

### 3.7 Вопросы

#### `GET /api/quests/{questId}/answers`

Ответ `200`:

```json
{
  "items": [
    {
      "answerId": "uuid",
      "name": "Вариант A",
      "description": "..."
    }
  ]
}
```

#### `POST /api/quests/{questId}/check`

Проверяет ответ на вопрос.
`Authorization` необязателен: если передан и ответ правильный для экзаменационного вопроса, фиксируется прогресс вопроса для пользователя.

Headers:

- `Authorization: Bearer <access_token>` (optional)

Body:

```json
{
  "answerId": "uuid"
}
```

Ответ `200`:

```json
{
  "correct": true
}
```

Ошибки:

- `400`: `answer_not_in_question`
- `401`: `unauthorized` (только если передан невалидный токен)
- `409`: `module_locked`
- `409`: `exam_locked`

### 3.8 Прогресс

#### `GET /api/lessons/{lessonId}/progress`

Возвращает прогресс пользователя по уроку.

Headers:

- `Authorization: Bearer <access_token>` (required)

Ответ `200`:

```json
{
  "targetId": "uuid",
  "targetType": "lesson",
  "percent": 50,
  "completed": false,
  "doneItems": 1,
  "totalItems": 2
}
```

`percent` возвращается всегда (`100` означает, что урок полностью пройден).

Ошибки:

- `401`: `unauthorized`
- `404`: `lesson_not_found`

#### `GET /api/modules/{moduleId}/progress`

Возвращает прогресс пользователя по модулю.

Headers:

- `Authorization: Bearer <access_token>` (required)

Ответ `200`:

```json
{
  "targetId": "uuid",
  "targetType": "module",
  "percent": 33,
  "completed": false,
  "doneItems": 1,
  "totalItems": 3
}
```

Для модуля учитываются уроки модуля и экзамен модуля.

Ошибки:

- `401`: `unauthorized`
- `404`: `module_not_found`

#### `GET /api/courses/{courseId}/progress`

Возвращает прогресс пользователя по курсу.

Headers:

- `Authorization: Bearer <access_token>` (required)

Ответ `200`:

```json
{
  "targetId": "uuid",
  "targetType": "course",
  "percent": 7,
  "completed": false,
  "doneItems": 1,
  "totalItems": 15
}
```

Для курса агрегируется прогресс по всем модулям курса.

Ошибки:

- `401`: `unauthorized`
- `404`: `course_not_found`

## 4. Файловая система уроков (.md)

### 4.1 Что хранится в БД

В `lesson.body` хранится служебный путь до markdown-контента.

- Рекомендуемо: относительный путь, например `python/module-1/if.md`.
- На фронт `lesson.body` больше не отдается.

### 4.2 Конфиг backend

`application.properties`:

```properties
app.content.lessons-root=${LESSON_CONTENT_ROOT:}
```

- Если `LESSON_CONTENT_ROOT` задан, относительный `lesson.body` резолвится от этого корня.
- Если не задан, путь резолвится как есть (лучше не использовать в Docker).

### 4.3 Docker и внешняя папка

`docker-compose.yml`:

```yaml
services:
  backend:
    environment:
      LESSON_CONTENT_ROOT: /data/lessons
    volumes:
      - ${LESSON_CONTENT_HOST_PATH:-./lesson-content}:/data/lessons:ro
```

- `LESSON_CONTENT_HOST_PATH`: реальная папка на хосте (вне контейнера).
- `/data/lessons`: точка монтирования внутри контейнера.
- `:ro`: backend читает файлы только на чтение.

Пример:

- `LESSON_CONTENT_HOST_PATH=/srv/edu-content/lessons`
- `LESSON_CONTENT_ROOT=/data/lessons`
- `lesson.body=python/module-1/if.md`

Итоговый файл для чтения:

- в контейнере: `/data/lessons/python/module-1/if.md`
- на хосте: `/srv/edu-content/lessons/python/module-1/if.md`

### 4.4 Правила для папки/файла

- Если `lesson.body` указывает на `.md` файл, читается этот файл.
- Если `lesson.body` указывает на папку:
  - если в папке 0 `.md` -> `lesson_content_not_found`
  - если в папке 1 `.md` -> читается этот файл
  - если в папке >1 `.md` -> `lesson_content_ambiguous`

## 5. Быстрый список ручек

1. `GET /api/courses?page=0&size=20`
2. `GET /api/courses/{courseId}`
3. `GET /api/courses/{courseId}/modules`
4. `GET /api/courses/{courseId}/tree`
5. `GET /api/modules/{moduleId}`
6. `GET /api/modules/{moduleId}/lessons`
7. `GET /api/modules/{moduleId}/exam`
8. `GET /api/lessons/{lessonId}`
9. `GET /api/lessons/{lessonId}/quiz`
10. `GET /api/lessons/{lessonId}/task`
11. `GET /api/quizzes/{quizId}/questions`
12. `POST /api/quizzes/{quizId}/start`
13. `POST /api/quizzes/{quizId}/answer`
14. `GET /api/exams/{examId}`
15. `GET /api/exams/{examId}/questions`
16. `GET /api/exams/{examId}/tasks`
17. `POST /api/exams/{examId}/complete`
18. `PUT /api/exams/{examId}/rewards`
19. `POST /api/tasks/{taskId}/start`
20. `POST /api/tasks/{taskId}/complete`
21. `PUT /api/tasks/{taskId}/rewards`
22. `PUT /api/tasks/{taskId}/runner`
23. `POST /api/tasks/{taskId}/run`
24. `GET /api/quests/{questId}/answers`
25. `POST /api/quests/{questId}/check`
26. `GET /api/lessons/{lessonId}/progress`
27. `GET /api/modules/{moduleId}/progress`
28. `GET /api/courses/{courseId}/progress`
29. `GET /api/community/leaderboard?period=day|week|month&metric=activity|xp&limit=9`
30. `GET /api/community/feed?limit=10`
31. `POST /api/social/friends/requests`
32. `GET /api/social/friends/requests/incoming?limit=<n>`
33. `GET /api/social/friends/requests/outgoing?limit=<n>`
34. `POST /api/social/friends/requests/{requestId}/accept`
35. `POST /api/social/friends/requests/{requestId}/reject`
36. `GET /api/social/friends?limit=<n>`
37. `DELETE /api/social/friends/{friendUserId}`
38. `GET /api/social/users/search?q=<text>&limit=<n>`
39. `POST /api/social/chats/private/{otherUserId}`
40. `GET /api/social/chats?limit=<n>`
41. `GET /api/social/chats/{chatId}/messages?beforeMessageId=<id>&limit=<n>`
42. `POST /api/social/chats/{chatId}/messages`
43. `PUT /api/social/chats/{chatId}/messages/{messageId}`
44. `DELETE /api/social/chats/{chatId}/messages/{messageId}`
45. `POST /api/social/chats/{chatId}/read`
46. `POST /api/parent/requests`
47. `GET /api/parent/requests/outgoing?limit=<n>`
48. `GET /api/parent/requests/incoming?limit=<n>`
49. `POST /api/parent/requests/{requestId}/accept`
50. `POST /api/parent/requests/{requestId}/reject`
51. `GET /api/parent/children`
52. `GET /api/parent/children/{childId}/dashboard`

## 6. Community (Leaderboard + Feed)

### `GET /api/community/leaderboard`

Query:

- `period` (`day | week | month`, optional, default `week`)
- `metric` (`activity | xp`, optional, default `activity`)
- `limit` (`int`, optional, default `9`, min `1`, max `9`)

Response `200`:

```json
{
  "period": "week",
  "metric": "activity",
  "fromInclusive": "2026-03-20T10:00:00Z",
  "toInclusive": "2026-03-27T10:00:00Z",
  "items": [
    {
      "rank": 1,
      "userId": "uuid",
      "username": "student_1",
      "score": 145
    }
  ]
}
```

Errors:

- `400`: `community_period_invalid`
- `400`: `community_metric_invalid`

### `GET /api/community/feed`

Query:

- `limit` (`int`, optional, default `10`, min `1`, max `100`)

Response `200`:

```json
{
  "items": [
    {
      "eventId": "uuid",
      "createdAt": "2026-03-27T10:02:00Z",
      "userId": "uuid",
      "username": "student_1",
      "eventType": "quiz_completed",
      "activityScore": 10,
      "xpGranted": 12,
      "coinGranted": 6,
      "progressPercent": 100,
      "lessonId": "uuid",
      "quizId": "uuid",
      "taskId": null,
      "examId": null,
      "details": null
    }
  ]
}
```

Current event types written by backend:

- `quiz_completed`
- `task_completed`
- `exam_completed`
- `level_up`

Anti-farming rule for ranking:

- Completion events are written only on `firstCompletion=true` (no repeated farming from reruns/retries).

Reserved event types (for next steps):

- `achievement_unlocked`
- `streak_day`

## 7. Social (друзья + чат)

Базовый префикс: `/api/social`.

Для всех ручек social:

- Header: `Authorization: Bearer <access_token>` (required)
- При отсутствии/невалидном токене: `401 unauthorized`
- Для пользователей роли `parent` social недоступен: `403 social_not_available_for_parent`

### 7.1 Друзья

#### `POST /api/social/friends/requests`

Отправить заявку в друзья.

Body:

```json
{
  "userId": "uuid"
}
```

Ответ `200`:

```json
{
  "requestId": "uuid",
  "status": "PENDING"
}
```

Ошибки:

- `400 friend_request_self`
- `404 user_not_found`
- `409 friendship_already_exists`
- `409 friend_request_already_exists`
- `409 friend_request_incoming_exists`

#### `GET /api/social/friends/requests/incoming?limit=<n>`

Входящие заявки в друзья текущего пользователя (только `PENDING`).

Параметры query:

- `limit` (`int`, optional)

Ответ `200`:

```json
{
  "items": [
    {
      "requestId": "uuid",
      "requesterUserId": "uuid",
      "requesterUsername": "student_1",
      "receiverUserId": "uuid",
      "receiverUsername": "student_2",
      "status": "PENDING",
      "createdAt": "2026-03-27T12:00:00Z",
      "respondedAt": null
    }
  ]
}
```

#### `GET /api/social/friends/requests/outgoing?limit=<n>`

Исходящие заявки в друзья текущего пользователя (только `PENDING`).

Параметры query:

- `limit` (`int`, optional)

Ответ `200`: формат такой же, как у incoming.

#### `POST /api/social/friends/requests/{requestId}/accept`

Принять входящую заявку.

Ответ `200`:

```json
{
  "requestId": "uuid",
  "status": "ACCEPTED"
}
```

Ошибки:

- `404 friend_request_not_found`
- `403 forbidden` (если заявка не адресована текущему пользователю)
- `409 friend_request_not_pending`

#### `POST /api/social/friends/requests/{requestId}/reject`

Отклонить входящую заявку.

Ответ `200`:

```json
{
  "requestId": "uuid",
  "status": "REJECTED"
}
```

Ошибки:

- `404 friend_request_not_found`
- `403 forbidden`
- `409 friend_request_not_pending`

#### `GET /api/social/friends?limit=<n>`

Список друзей текущего пользователя.

Параметры query:

- `limit` (`int`, optional)

Ответ `200`:

```json
{
  "items": [
    {
      "userId": "uuid",
      "username": "friend_name",
      "since": "2026-03-27T12:03:00Z"
    }
  ]
}
```

#### `DELETE /api/social/friends/{friendUserId}`

Удалить дружбу (в обе стороны).

Ответ `200`: пустое тело.

#### `GET /api/social/users/search?q=<text>&limit=<n>`

Поиск пользователей по `username` (кроме самого себя).

Параметры query:

- `q` (`string`, optional)
- `limit` (`int`, optional)

Поведение:

- если длина `q < 2`, возвращается пустой список
- если длина `q > 100`, ошибка `400 search_query_too_long`

Ответ `200`:

```json
{
  "users": [
    {
      "userId": "uuid",
      "username": "student_3",
      "friendStatus": "OUTGOING_REQUEST"
    }
  ]
}
```

`friendStatus`: `ACCEPTED | OUTGOING_REQUEST | INCOMING_REQUEST | null`.

### 7.2 Чат

Ограничение:

- чат с участием пользователя роли `parent` запрещён: `403 chat_with_parent_forbidden`

#### `POST /api/social/chats/private/{otherUserId}`

Создать приватный чат с пользователем.
Если чат уже есть, возвращается существующий.

Ответ `200`:

```json
{
  "chatId": "uuid",
  "otherUserId": "uuid",
  "created": true
}
```

`created=false` означает, что чат уже существовал.

Ошибки:

- `400 chat_self`
- `404 user_not_found`

#### `GET /api/social/chats?limit=<n>`

Список чатов текущего пользователя.

Параметры query:

- `limit` (`int`, optional)

Ответ `200`:

```json
{
  "items": [
    {
      "chatId": "uuid",
      "otherUserId": "uuid",
      "otherUsername": "student_2",
      "lastMessage": {
        "messageId": 12,
        "senderUserId": "uuid",
        "text": "Привет",
        "createdAt": "2026-03-27T12:10:00Z"
      },
      "unreadCount": 3,
      "updatedAt": "2026-03-27T12:10:00Z"
    }
  ]
}
```

#### `GET /api/social/chats/{chatId}/messages?beforeMessageId=<id>&limit=<n>`

Постраничная загрузка сообщений чата.

Параметры query:

- `beforeMessageId` (`long`, optional)
- `limit` (`int`, optional)

Ответ `200`:

```json
{
  "items": [
    {
      "messageId": 11,
      "chatId": "uuid",
      "senderUserId": "uuid",
      "text": "Текст сообщения",
      "replyToMessageId": null,
      "createdAt": "2026-03-27T12:09:00Z"
    }
  ]
}
```

Ошибки:

- `404 chat_not_found`
- `403 chat_access_denied`

#### `POST /api/social/chats/{chatId}/messages`

Отправить сообщение в чат.

Body:

```json
{
  "text": "Привет!",
  "replyToMessageId": null
}
```

Ответ `200`:

```json
{
  "message": {
    "messageId": 13,
    "chatId": "uuid",
    "senderUserId": "uuid",
    "text": "Привет!",
    "replyToMessageId": null,
    "createdAt": "2026-03-27T12:11:00Z"
  }
}
```

Ошибки:

- `400 message_empty`
- `400 reply_message_not_found`
- `404 chat_not_found`
- `403 chat_access_denied`

#### `PUT /api/social/chats/{chatId}/messages/{messageId}`

Редактировать собственное сообщение.

Body:

```json
{
  "text": "Исправленный текст"
}
```

Ответ `200`:

```json
{
  "message": {
    "messageId": 13,
    "chatId": "uuid",
    "senderUserId": "uuid",
    "text": "Исправленный текст",
    "replyToMessageId": null,
    "createdAt": "2026-03-27T12:11:00Z"
  }
}
```

Ошибки:

- `400 message_empty`
- `404 message_not_found`
- `403 forbidden` (если сообщение не принадлежит текущему пользователю)

#### `DELETE /api/social/chats/{chatId}/messages/{messageId}`

Удалить собственное сообщение.

Ответ `200`:

```json
{
  "deleted": true,
  "messageId": 13
}
```

Ошибки:

- `404 message_not_found`
- `403 forbidden`

#### `POST /api/social/chats/{chatId}/read`

Обновить последнюю прочитанную позицию в чате.

Body (optional):

```json
{
  "lastReadMessageId": 13
}
```

Ответ `200`:

```json
{
  "ok": true,
  "lastReadMessageId": 13
}
```

Ошибки:

- `400 last_read_message_invalid`
- `400 message_not_found`
- `404 chat_not_found`
- `403 chat_access_denied`

## 8. Social WebSocket (на том же порту backend)

### 8.1 Подключение

- Endpoint: `ws(s)://<host>:<backend_port>${CHAT_WS_ENDPOINT}` (default `/ws/social`)
- Протокол: STOMP

Авторизация на `CONNECT`:

- `Authorization: Bearer <access_token>`
- или `access_token: <access_token>`

Если токен невалиден: соединение отклоняется.

### 8.2 Подписки

Подписываться нужно на user-destination:

- чат: `/user${CHAT_WS_USER_CHAT_DESTINATION}` (по умолчанию `/user/queue/social/chat`)
- социальные события (друзья): `/user${CHAT_WS_USER_FRIEND_DESTINATION}` (по умолчанию `/user/queue/social/friends`)
- события родительского контроля: `/user${CHAT_WS_USER_PARENT_CONTROL_DESTINATION}` (по умолчанию `/user/queue/social/parent-control`)

### 8.3 Формат событий

Все события имеют общий envelope:

```json
{
  "type": "message_sent",
  "timestamp": "2026-03-27T12:34:56Z",
  "data": {}
}
```

#### 8.3.1 События чата

`chat_created`:

```json
{
  "type": "chat_created",
  "timestamp": "2026-03-27T12:34:56Z",
  "data": {
    "chatId": "uuid",
    "otherUserId": "uuid",
    "initiatorUserId": "uuid"
  }
}
```

`message_sent` и `message_edited`:

```json
{
  "type": "message_edited",
  "timestamp": "2026-03-27T12:34:56Z",
  "data": {
    "chatId": "uuid",
    "message": {
      "messageId": 13,
      "chatId": "uuid",
      "senderUserId": "uuid",
      "text": "Текст",
      "replyToMessageId": null,
      "createdAt": "2026-03-27T12:11:00Z"
    }
  }
}
```

`message_deleted`:

```json
{
  "type": "message_deleted",
  "timestamp": "2026-03-27T12:34:56Z",
  "data": {
    "chatId": "uuid",
    "messageId": 13,
    "actorUserId": "uuid"
  }
}
```

`message_read`:

```json
{
  "type": "message_read",
  "timestamp": "2026-03-27T12:34:56Z",
  "data": {
    "chatId": "uuid",
    "lastReadMessageId": 13,
    "readerUserId": "uuid"
  }
}
```

#### 8.3.2 События друзей

`friend_request`, `friend_accept`, `friend_reject`:

```json
{
  "type": "friend_accept",
  "timestamp": "2026-03-27T12:34:56Z",
  "data": {
    "requestId": "uuid",
    "requesterUserId": "uuid",
    "receiverUserId": "uuid",
    "status": "ACCEPTED"
  }
}
```

`friend_removed`:

```json
{
  "type": "friend_removed",
  "timestamp": "2026-03-27T12:34:56Z",
  "data": {
    "friendUserId": "uuid"
  }
}
```

#### 8.3.3 События родительского контроля

`parent_control_request`, `parent_control_accept`, `parent_control_reject`:

```json
{
  "type": "parent_control_accept",
  "timestamp": "2026-03-27T12:34:56Z",
  "data": {
    "requestId": "uuid",
    "parentUserId": "uuid",
    "childUserId": "uuid",
    "status": "ACCEPTED"
  }
}
```

## 9. Родительский контроль

Базовый префикс: `/api/parent`.
Realtime-уведомления по заявкам приходят через WebSocket-события из раздела `8.3.3`.

Для всех ручек родконтроля:

- Header: `Authorization: Bearer <access_token>` (required)
- При отсутствии/невалидном токене: `401 unauthorized`

### 9.1 Заявки родительского контроля

#### `POST /api/parent/requests`

Отправить заявку на родконтроль (только роль `parent`).
Получатель заявки должен быть пользователем роли `student`.

Body:

```json
{
  "childUserId": "uuid"
}
```

Ответ `200`:

```json
{
  "requestId": "uuid",
  "status": "PENDING"
}
```

Ошибки:

- `400 parent_control_request_self`
- `403 parent_control_parent_role_required`
- `403 parent_control_child_role_required`
- `404 user_not_found`
- `409 parent_control_request_already_exists`
- `409 parent_control_already_active`

#### `GET /api/parent/requests/outgoing?limit=<n>`

Исходящие `PENDING` заявки текущего родителя.

Параметры query:

- `limit` (`int`, optional)

Ответ `200`:

```json
{
  "items": [
    {
      "requestId": "uuid",
      "parentUserId": "uuid",
      "parentUsername": "parent_1",
      "childUserId": "uuid",
      "childUsername": "student_1",
      "status": "PENDING",
      "createdAt": "2026-03-27T13:00:00Z",
      "respondedAt": null
    }
  ]
}
```

#### `GET /api/parent/requests/incoming?limit=<n>`

Входящие `PENDING` заявки текущего ребёнка (`student`).

Параметры query:

- `limit` (`int`, optional)

Ответ `200`: формат такой же, как у outgoing.

#### `POST /api/parent/requests/{requestId}/accept`

Принять заявку (выполняет ребёнок роли `student`).
После принятия создаётся активная связь `parent -> child`.

Ответ `200`:

```json
{
  "requestId": "uuid",
  "status": "ACCEPTED"
}
```

Ошибки:

- `403 parent_control_child_role_required`
- `403 parent_control_forbidden`
- `404 parent_control_request_not_found`
- `409 parent_control_request_not_pending`

#### `POST /api/parent/requests/{requestId}/reject`

Отклонить заявку (выполняет ребёнок роли `student`).

Ответ `200`:

```json
{
  "requestId": "uuid",
  "status": "REJECTED"
}
```

Ошибки:

- `403 parent_control_child_role_required`
- `403 parent_control_forbidden`
- `404 parent_control_request_not_found`
- `409 parent_control_request_not_pending`

### 9.2 Дети родителя

#### `GET /api/parent/children`

Список детей текущего родителя с активной связью.

Ответ `200`:

```json
{
  "items": [
    {
      "childUserId": "uuid",
      "childUsername": "student_1",
      "since": "2026-03-27T13:05:00Z"
    }
  ]
}
```

Ошибка:

- `403 parent_control_parent_role_required`

### 9.3 Дашборд ребёнка

#### `GET /api/parent/children/{childId}/dashboard`

Read-only дашборд ребёнка для родителя.
Доступ есть только при активной связи `parent -> child`.

Ответ `200`:

```json
{
  "child": {
    "userId": "uuid",
    "username": "student_1",
    "xp": 120,
    "level": 2,
    "coins": 35,
    "lastActivityAt": "2026-03-27T14:10:00Z"
  },
  "courses": [
    {
      "targetId": "uuid",
      "name": "Java",
      "courseId": "uuid",
      "moduleId": null,
      "percent": 40,
      "completed": false,
      "doneItems": 2,
      "totalItems": 5
    }
  ],
  "modules": [
    {
      "targetId": "uuid",
      "name": "Циклы",
      "courseId": "uuid",
      "moduleId": "uuid",
      "percent": 50,
      "completed": false,
      "doneItems": 1,
      "totalItems": 2
    }
  ],
  "lessons": [
    {
      "targetId": "uuid",
      "name": "while",
      "courseId": "uuid",
      "moduleId": "uuid",
      "percent": 100,
      "completed": true,
      "doneItems": 2,
      "totalItems": 2
    }
  ],
  "recentActivities": [
    {
      "eventId": "uuid",
      "createdAt": "2026-03-27T14:00:00Z",
      "eventType": "quiz_completed",
      "progressPercent": 100,
      "xpGranted": 12,
      "coinGranted": 5,
      "lessonId": "uuid",
      "quizId": "uuid",
      "taskId": null,
      "examId": null,
      "details": null
    }
  ]
}
```

`recentActivities` включает только типы:

- `quiz_completed`
- `task_completed`
- `exam_completed`

Ошибки:

- `403 parent_control_parent_role_required`
- `403 parent_control_forbidden`
- `403 parent_control_child_role_required`
- `404 user_not_found`

