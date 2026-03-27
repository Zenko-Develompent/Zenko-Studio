# Content API Documentation

Документация по текущим read-ручкам контента и загрузке markdown для уроков.

- Базовый префикс: `/api`
- Формат ответов: `application/json`
- Аутентификация: сейчас для этих GET-ручек не требуется

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

Коды ошибок чтения markdown:

- `lesson_content_not_found`
- `lesson_content_not_markdown`
- `lesson_content_path_invalid`
- `lesson_content_ambiguous`
- `lesson_content_read_failed`

HTTP-коды:

- `200 OK`: успех.
- `400 Bad Request`: невалидный запрос/путь/тип файла.
- `404 Not Found`: сущность или markdown не найден.
- `409 Conflict`: для папки урока найдено более одного `.md`.
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
  "description": "Решить задачу"
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

### 3.5 Экзамены

#### `GET /api/exams/{examId}`

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
      "description": "Описание таска"
    }
  ]
}
```

`lessonId` может быть `null`.

Ошибки:

- `404`: `exam_not_found`

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
12. `GET /api/exams/{examId}`
13. `GET /api/exams/{examId}/questions`
14. `GET /api/exams/{examId}/tasks`
