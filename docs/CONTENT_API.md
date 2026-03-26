# Content API Documentation

Документация по контент-ручкам, добавленным в `ContentController`.

- Базовый префикс: `/api`
- Формат ответов: `application/json`
- Текущая версия: `v1`

## 1. Общие правила

### 1.1. Типы данных

- `UUID` -> строка в формате UUID (пример: `"a3b8d3e4-1c2b-4e77-ae6d-0abf12345678"`).
- `string` -> текстовая строка.
- `int` -> целое число.
- `long` -> целое число (в JSON также приходит числом).
- `nullable` -> поле может быть `null`.

### 1.2. Аутентификация

Для текущих контент-ручек в контроллере отдельная авторизация не требуется.

### 1.3. Пагинация

`GET /api/courses` принимает:

- `page` (`int`, по умолчанию `0`)
- `size` (`int`, по умолчанию `20`)

Ограничения на бэке:

- `page < 0` приводится к `0`
- `size < 1` приводится к `1`
- `size > 100` приводится к `100`

### 1.4. Сортировка в ответах

- Курсы: по `name` ASC (из репозитория).
- Модули внутри курса: `name` ASC, затем `moduleId`.
- Уроки внутри модуля: `name` ASC, затем `lessonId`.
- Вопросы: `createdAt` ASC, затем `questId`.
- Таски экзамена: `createdAt` ASC, затем `tasksId`.

## 2. Формат ошибок

### 2.1. Ошибки уровня API (бизнес-ошибки)

Возвращаются в формате:

```json
{
  "error": "course_not_found"
}
```

### 2.2. Коды ошибок, которые используются в контент-ручках

- `course_not_found`
- `module_not_found`
- `lesson_not_found`
- `quiz_not_found`
- `exam_not_found`
- `task_not_found`

### 2.3. Общие HTTP-коды

- `200 OK` -> успешный `GET`.
- `404 Not Found` -> сущность не найдена (ошибки выше).
- `400 Bad Request` -> некорректные входные данные (например, неверный UUID в path).
- `500 Internal Server Error` -> непредвиденная ошибка.

Для `500` в глобальном обработчике предусмотрен формат:

```json
{
  "error": "error"
}
```

## 3. Эндпоинты

## 3.1. Курсы

### `GET /api/courses`

Возвращает список курсов.

#### Query-параметры

- `page` (`int`, optional, default: `0`)
- `size` (`int`, optional, default: `20`)

#### Ответ `200`

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

---

### `GET /api/courses/{courseId}`

Возвращает детали курса + список модулей.

#### Path-параметры

- `courseId` (`UUID`, required)

#### Ответ `200`

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

#### Ошибки

- `404`: `course_not_found`

---

### `GET /api/courses/{courseId}/modules`

Возвращает только модули курса.

#### Path-параметры

- `courseId` (`UUID`, required)

#### Ответ `200`

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

#### Ошибки

- `404`: `course_not_found`

---

### `GET /api/courses/{courseId}/tree`

Возвращает дерево курса для фронта:
`курс -> модули -> уроки`.

#### Path-параметры

- `courseId` (`UUID`, required)

#### Ответ `200`

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
          "body": "/content/lessons/if.md",
          "quizId": "uuid",
          "taskId": "uuid"
        }
      ]
    }
  ]
}
```

#### Ошибки

- `404`: `course_not_found`

## 3.2. Модули

### `GET /api/modules/{moduleId}`

Детали модуля + краткая инфа об экзамене.

#### Path-параметры

- `moduleId` (`UUID`, required)

#### Ответ `200`

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

#### Ошибки

- `404`: `module_not_found`

---

### `GET /api/modules/{moduleId}/lessons`

Список уроков модуля.

#### Path-параметры

- `moduleId` (`UUID`, required)

#### Ответ `200`

```json
{
  "items": [
    {
      "lessonId": "uuid",
      "name": "if",
      "description": "Введение",
      "body": "/content/lessons/if.md",
      "xp": 10,
      "quizId": "uuid",
      "taskId": "uuid"
    }
  ]
}
```

#### Ошибки

- `404`: `module_not_found`

---

### `GET /api/modules/{moduleId}/exam`

Экзамен модуля (1:1).

#### Path-параметры

- `moduleId` (`UUID`, required)

#### Ответ `200`

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

#### Ошибки

- `404`: `exam_not_found`

## 3.3. Уроки

### `GET /api/lessons/{lessonId}`

Детали урока.

#### Path-параметры

- `lessonId` (`UUID`, required)

#### Ответ `200`

```json
{
  "lessonId": "uuid",
  "moduleId": "uuid",
  "name": "if",
  "description": "Введение",
  "body": "/content/lessons/if.md",
  "xp": 10,
  "quizId": "uuid",
  "taskId": "uuid"
}
```

#### Ошибки

- `404`: `lesson_not_found`

---

### `GET /api/lessons/{lessonId}/quiz`

Квиз урока (1:1).

#### Path-параметры

- `lessonId` (`UUID`, required)

#### Ответ `200`

```json
{
  "quizId": "uuid",
  "lessonId": "uuid",
  "name": "Квиз по уроку",
  "description": "Проверка темы",
  "questionsCount": 5
}
```

#### Ошибки

- `404`: `quiz_not_found`

---

### `GET /api/lessons/{lessonId}/task`

Таск урока (по текущей модели: максимум один).

#### Path-параметры

- `lessonId` (`UUID`, required)

#### Ответ `200`

```json
{
  "taskId": "uuid",
  "lessonId": "uuid",
  "examId": "uuid",
  "name": "Практическая задача",
  "description": "Решить задачу"
}
```

`examId` может быть `null` (если таск только урока).

#### Ошибки

- `404`: `task_not_found`

## 3.4. Квизы

### `GET /api/quizzes/{quizId}/questions`

Возвращает вопросы квиза.

#### Path-параметры

- `quizId` (`UUID`, required)

#### Ответ `200`

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

#### Ошибки

- `404`: `quiz_not_found`

## 3.5. Экзамены

### `GET /api/exams/{examId}`

Детали экзамена.

#### Path-параметры

- `examId` (`UUID`, required)

#### Ответ `200`

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

#### Ошибки

- `404`: `exam_not_found`

---

### `GET /api/exams/{examId}/questions`

Вопросы экзамена.

#### Path-параметры

- `examId` (`UUID`, required)

#### Ответ `200`

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

#### Ошибки

- `404`: `exam_not_found`

---

### `GET /api/exams/{examId}/tasks`

Таски экзамена.

#### Path-параметры

- `examId` (`UUID`, required)

#### Ответ `200`

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

`lessonId` может быть `null` (таск не привязан к уроку).

#### Ошибки

- `404`: `exam_not_found`

## 4. Быстрый список для фронтенда

Маршруты для подключения:

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

