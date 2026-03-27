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
- `unauthorized`
- `forbidden`
- `runner_language_not_supported`
- `task_expected_output_not_configured`
- `task_language_mismatch`
- `code_invalid`
- `code_too_large`
- `task_input_too_large`
- `runner_unavailable`

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
      "examId": "uuid"
    }
  ]
}
```

Ошибки:

- `404`: `course_not_found`

#### `GET /api/courses/{courseId}/modules`

Ответ `200`:

```json
{
  "items": [
    {
      "moduleId": "uuid",
      "name": "Условия",
      "description": "if/else",
      "lessonCount": 6,
      "examId": "uuid"
    }
  ]
}
```

Ошибки:

- `404`: `course_not_found`

#### `GET /api/courses/{courseId}/tree`

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
      "lessons": [
        {
          "lessonId": "uuid",
          "name": "if",
          "quizId": "uuid",
          "taskId": "uuid"
        }
      ]
    }
  ]
}
```

Ошибки:

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
      "taskId": "uuid"
    }
  ]
}
```

Ошибки:

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
  },
  "task": null
}
```

Если квиз уже завершен:

```json
{
  "completed": true,
  "question": null,
  "task": {
    "taskId": "uuid",
    "lessonId": "uuid",
    "examId": null,
    "name": "Практическая задача",
    "description": "..."
  }
}
```

Ошибки:

- `401`: `unauthorized`
- `404`: `quiz_not_found`

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
  },
  "task": null
}
```

Ошибки:

- `401`: `unauthorized`
- `404`: `quiz_not_found`
- `400`: `answer_not_in_question`
- `409`: `quiz_question_out_of_order`

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
19. `POST /api/tasks/{taskId}/complete`
20. `PUT /api/tasks/{taskId}/rewards`
21. `PUT /api/tasks/{taskId}/runner`
22. `POST /api/tasks/{taskId}/run`
23. `GET /api/quests/{questId}/answers`
24. `POST /api/quests/{questId}/check`
25. `GET /api/lessons/{lessonId}/progress`
26. `GET /api/modules/{moduleId}/progress`
27. `GET /api/courses/{courseId}/progress`
