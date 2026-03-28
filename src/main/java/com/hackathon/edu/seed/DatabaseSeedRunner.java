package com.hackathon.edu.seed;

import com.hackathon.edu.config.AppSeedProperties;
import com.hackathon.edu.entity.AnswerEntity;
import com.hackathon.edu.entity.CourseEntity;
import com.hackathon.edu.entity.ExemEntity;
import com.hackathon.edu.entity.LessonEntity;
import com.hackathon.edu.entity.ModuleEntity;
import com.hackathon.edu.entity.QuestEntity;
import com.hackathon.edu.entity.QuizEntity;
import com.hackathon.edu.entity.TasksEntity;
import com.hackathon.edu.repository.CourseRepository;
import com.hackathon.edu.repository.ExemRepository;
import com.hackathon.edu.repository.LessonRepository;
import com.hackathon.edu.repository.ModuleRepository;
import com.hackathon.edu.repository.QuizRepository;
import com.hackathon.edu.repository.TasksRepository;
import com.hackathon.edu.service.AuthService;
import com.hackathon.edu.service.CodeRunnerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeedRunner implements CommandLineRunner {
    private static final List<TopicSeed> TOPICS = List.of(
            new TopicSeed(
                    "variables",
                    "Переменные",
                    "Хранение данных и базовые типы.",
                    "Что такое переменная в программировании?",
                    "Именованное место для хранения значения",
                    "Команда для запуска цикла",
                    "Зачем нужен тип данных переменной?",
                    "Чтобы понимать, какие значения можно хранить",
                    "Чтобы ускорить интернет"
            ),
            new TopicSeed(
                    "conditionals",
                    "Условные операторы",
                    "Ветвление программы через if/else.",
                    "Какой оператор обычно используют для ветвления?",
                    "if",
                    "while",
                    "Что делает блок else?",
                    "Выполняется, когда условие в if ложно",
                    "Создает новую переменную"
            ),
            new TopicSeed(
                    "loops",
                    "Циклы",
                    "Повторение действий через for/while.",
                    "Для чего нужен цикл?",
                    "Для повторения однотипных действий",
                    "Для объявления функции",
                    "Что важно менять внутри цикла while?",
                    "Состояние, влияющее на условие",
                    "Название файла"
            ),
            new TopicSeed(
                    "functions",
                    "Функции",
                    "Вынос кода в переиспользуемые блоки.",
                    "Зачем нужны функции?",
                    "Чтобы переиспользовать код",
                    "Чтобы удалить переменные",
                    "Что обычно передают в функцию?",
                    "Аргументы",
                    "Только комментарии"
            ),
            new TopicSeed(
                    "arrays_strings",
                    "Массивы и строки",
                    "Работа с наборами данных и текстом.",
                    "Что такое массив?",
                    "Набор элементов одного типа",
                    "Тип цикла",
                    "Что вернет длина строки " + '"' + "HELLO" + '"' + "?",
                    "5",
                    "4"
            )
    );

    private final AppSeedProperties props;
    private final AuthService authService;
    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final QuizRepository quizRepository;
    private final TasksRepository tasksRepository;
    private final ExemRepository examRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (!props.isEnabled()) {
            log.info("Seed skipped: app.seed.enabled=false (set SEED_ENABLED=true to enable)");
            return;
        }

        if (props.isOnlyIfEmpty()) {
            boolean alreadySeeded = courseRepository.existsByCategoryIgnoreCase("java")
                    && courseRepository.existsByCategoryIgnoreCase("bash");
            if (alreadySeeded) {
                log.info("Seed skipped: app.seed.only-if-empty=true and seed data already exists (set SEED_ONLY_IF_EMPTY=false to force)");
                return;
            }
        }

        log.info("Seed starting (onlyIfEmpty={})", props.isOnlyIfEmpty());
        seed();
    }

    void seed() {
        var admin = props.getAdmin();
        authService.ensureAdmin(admin.getUsername(), admin.getPassword(), admin.getAge());

        CourseEntity javaCourse = ensureCourse(
                "Java для школьников",
                "Пошаговый курс по основам программирования на Java.",
                "java"
        );
        CourseEntity bashCourse = ensureCourse(
                "Bash для школьников",
                "Основы алгоритмов и автоматизации в Bash.",
                "bash"
        );

        seedCourse(javaCourse, "java", CodeRunnerService.LANGUAGE_JAVA);
        seedCourse(bashCourse, "bash", CodeRunnerService.LANGUAGE_BASH);

        log.info(
                "Seed done: javaCourseId={}, bashCourseId={}, modulesPerCourse={}",
                javaCourse.getCourseId(),
                bashCourse.getCourseId(),
                TOPICS.size()
        );
    }

    private String loadLessonBody(String courseKey, String topicKey) {
    // Формируем путь к файлу относительно корня проекта
    Path path = Paths.get("lesson-content", "seed", courseKey, topicKey + ".md");

    try {
        // Читаем все строки и объединяем в одну строку с переносами
        return Files.readString(path);
    } catch (IOException e) {
        // Если файл не найден, можно логировать и вернуть null
        log.warn("Lesson file not found: {}", path.toAbsolutePath());
        return null;
    }
}

    private void seedCourse(CourseEntity course, String courseKey, String language) {
        int moduleIndex = 1;
        for (TopicSeed topic : TOPICS) {
            String moduleName = "Модуль " + moduleIndex + ": " + topic.title();
            ModuleEntity module = ensureModule(course, moduleName, topic.moduleDescription());

            module = moduleRepository.findWithLessonsByModuleId(module.getModuleId())
                    .orElseThrow();

            String lessonName = "Урок: " + topic.title();
            String lessonBodyPath = lessonBodyPath(courseKey, topic.key());
           LessonEntity lesson = ensureLesson(
                module,
                lessonName,
                "Базовый урок по теме «" + topic.title() + "» для курса " + languageLabel(language) + ".",
                lessonBodyPath,
                15 + moduleIndex * 3
        );

            moduleRepository.saveAndFlush(module);
            lesson = lessonRepository.findByModuleIdAndNameIgnoreCase(module.getModuleId(), lessonName)
                    .orElseThrow();

            ensureLessonQuiz(
                    lesson,
                    "Квиз: " + topic.title(),
                    "Быстрая проверка по теме «" + topic.title() + "».",
                    10 + moduleIndex,
                    5 + moduleIndex,
                    List.of(new SeedQuestion(
                            "Вопрос по теме",
                            topic.quizQuestion(),
                            buildFourAnswers(
                                    topic.quizCorrect(),
                                    topic.quizWrong(),
                                    "Команда для сборки проекта",
                                    "Настройка темы интерфейса IDE"
                            )
                    ))
            );

            TaskSeed lessonTask = buildLessonTask(topic);
            ensureLessonTask(
                    lesson,
                    lessonTask.name(),
                    lessonTask.description(),
                    language,
                    lessonTask.expectedOutput(),
                    lessonTask.inputData(),
                    18 + moduleIndex * 2,
                    8 + moduleIndex
            );

            ExemEntity exam = ensureModuleExam(
                    module,
                    "Экзамен: " + topic.title(),
                    "Итоговая проверка по теме «" + topic.title() + "».",
                    70 + moduleIndex * 5,
                    30 + moduleIndex * 2,
                    List.of(
                            new SeedQuestion(
                                    "Итоговый вопрос",
                                    topic.examQuestion(),
                                    buildFourAnswers(
                                            topic.examCorrect(),
                                            topic.examWrong(),
                                            "Только название переменной",
                                            "Случайный набор символов"
                                    )
                            ),
                            new SeedQuestion(
                                    "Статус экзамена",
                                    "Когда экзамен модуля считается завершенным?",
                                    List.of(
                                            new SeedAnswer("Когда отвечены вопросы и выполнены задачи экзамена", true),
                                            new SeedAnswer("Сразу после запуска экзамена", false),
                                            new SeedAnswer("После входа в аккаунт", false),
                                            new SeedAnswer("Только после истечения времени", false)
                                    )
                            )
                    )
            );

            TaskSeed examTask = buildExamTask(topic);
            ensureExamTask(
                    exam,
                    examTask.name(),
                    examTask.description(),
                    language,
                    examTask.expectedOutput(),
                    examTask.inputData(),
                    25 + moduleIndex * 2,
                    12 + moduleIndex
            );

            moduleIndex++;
        }
    }

    private TaskSeed buildLessonTask(TopicSeed topic) {
        return switch (topic.key()) {
            case "variables" -> new TaskSeed(
                    "Задача: вывести 42",
                    "Напишите программу, которая выводит число 42 в отдельной строке.",
                    "42\n",
                    null
            );
            case "conditionals" -> new TaskSeed(
                    "Задача: знак числа",
                    "Считайте целое число N. Выведите YES, если N > 0, иначе NO.",
                    "YES\n",
                    "5\n"
            );
            case "loops" -> new TaskSeed(
                    "Задача: 1..5",
                    "Выведите числа от 1 до 5, каждое с новой строки.",
                    "1\n2\n3\n4\n5\n",
                    null
            );
            case "functions" -> new TaskSeed(
                    "Задача: сумма двух",
                    "Считайте два целых числа и выведите их сумму.",
                    "15\n",
                    "7 8\n"
            );
            case "arrays_strings" -> new TaskSeed(
                    "Задача: длина строки",
                    "Считайте одно слово и выведите его длину.",
                    "5\n",
                    "HELLO\n"
            );
            default -> new TaskSeed(
                    "Задача: вывод",
                    "Выведите число 1.",
                    "1\n",
                    null
            );
        };
    }

    private TaskSeed buildExamTask(TopicSeed topic) {
        return switch (topic.key()) {
            case "variables" -> new TaskSeed(
                    "Экз задача: сумма",
                    "Считайте два целых числа и выведите сумму.",
                    "7\n",
                    "3 4\n"
            );
            case "conditionals" -> new TaskSeed(
                    "Экз задача: отрицательное",
                    "Считайте целое N. Выведите YES, если N > 0, иначе NO.",
                    "NO\n",
                    "-3\n"
            );
            case "loops" -> new TaskSeed(
                    "Экз задача: четные",
                    "Выведите числа 2, 4 и 6, каждое с новой строки.",
                    "2\n4\n6\n",
                    null
            );
            case "functions" -> new TaskSeed(
                    "Экз задача: квадрат",
                    "Считайте целое число N и выведите N*N.",
                    "81\n",
                    "9\n"
            );
            case "arrays_strings" -> new TaskSeed(
                    "Экз задача: склейка",
                    "Считайте два слова и выведите их без пробела.",
                    "AB\n",
                    "A B\n"
            );
            default -> new TaskSeed(
                    "Экз задача: вывод",
                    "Выведите число 1.",
                    "1\n",
                    null
            );
        };
    }

    private String languageLabel(String language) {
        if (CodeRunnerService.LANGUAGE_JAVA.equals(language)) {
            return "Java";
        }
        if (CodeRunnerService.LANGUAGE_BASH.equals(language)) {
            return "Bash";
        }
        return language;
    }

    private String lessonBodyPath(String courseKey, String topicKey) {
        return "seed/" + courseKey + "/" + topicKey + ".md";
    }

    private CourseEntity ensureCourse(String name, String description, String category) {
        CourseEntity course = courseRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    CourseEntity entity = new CourseEntity();
                    entity.setName(name);
                    return courseRepository.save(entity);
                });
        course.setDescription(description);
        course.setCategory(category);
        return courseRepository.save(course);
    }

    private ModuleEntity ensureModule(CourseEntity course, String name, String description) {
        ModuleEntity module = moduleRepository.findByCourse_CourseIdAndNameIgnoreCase(course.getCourseId(), name)
                .orElse(null);
        if (module == null) {
            ModuleEntity entity = new ModuleEntity();
            entity.setCourse(course);
            entity.setName(name);
            entity.setDescription(description);
            module = moduleRepository.save(entity);
        }
        if (module.getCourse() == null) {
            module.setCourse(course);
        }
        module.setName(name);
        module.setDescription(description);
        return moduleRepository.save(module);
    }

private LessonEntity ensureLesson(
        ModuleEntity module,
        String name,
        String description,
        String lessonBodyPath,
        int xp
) {
    LessonEntity lesson = module.getLessons().stream()
            .filter(existing -> existing.getName() != null && existing.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElseGet(() -> {
                LessonEntity created = new LessonEntity();
                created.setModule(module);        
                module.getLessons().add(created); 
                return created;
            });

    lesson.setName(name);
    lesson.setDescription(description);
    lesson.setBody(lessonBodyPath);
    lesson.setXp(Math.max(0, xp));

    return lessonRepository.save(lesson);
}

private QuizEntity ensureLessonQuiz(
        LessonEntity lesson,
        String quizName,
        String description,
        int xpReward,
        int coinReward,
        List<SeedQuestion> questions
) {
    QuizEntity quiz = lesson.getQuiz();

    if (quiz == null) {
        quiz = new QuizEntity();
        quiz.setLesson(lesson);
        lesson.setQuiz(quiz); 
    }

    quiz.setName(quizName);
    quiz.setDescription(description);
    quiz.setXpReward(Math.max(0, xpReward));
    quiz.setCoinReward(Math.max(0, coinReward));

    if (quiz.getQuests() == null) {
        quiz.setQuests(new ArrayList<>());
    } else {
        quiz.getQuests().clear(); 
    }

    for (SeedQuestion seedQuestion : questions) {
        QuestEntity quest = new QuestEntity();
        quest.setQuiz(quiz);
        quest.setName(limit50(seedQuestion.name()));
        quest.setDescription(seedQuestion.description());

        quest.setAnswers(new ArrayList<>());

        for (SeedAnswer seedAnswer : seedQuestion.answers()) {
            AnswerEntity answer = new AnswerEntity();
            answer.setQuest(quest);
            answer.setName(limit50(seedAnswer.name()));
            answer.setCorrectly(seedAnswer.correct());
            quest.getAnswers().add(answer);
        }

        quiz.getQuests().add(quest);
    }

    return quizRepository.save(quiz);
}

 private TasksEntity ensureLessonTask(
        LessonEntity lesson,
        String taskName,
        String description,
        String language,
        String expectedOutput,
        String inputData,
        int xpReward,
        int coinReward
) {
    TasksEntity task = lesson.getTask();

    if (task == null) {
        task = new TasksEntity();
        task.setLesson(lesson);
        lesson.setTask(task); 
    }

    task.setExam(null);
    task.setName(limit50(taskName));
    task.setDescription(description);
    task.setRunnerLanguage(normalizeLanguage(language));
    task.setExpectedOutput(expectedOutput);
    task.setInputData(normalizeInputData(inputData));
    task.setXpReward(Math.max(0, xpReward));
    task.setCoinReward(Math.max(0, coinReward));

    return tasksRepository.save(task);
}
   private ExemEntity ensureModuleExam(
        ModuleEntity module,
        String examName,
        String description,
        int xpReward,
        int coinReward,
        List<SeedQuestion> questions
) {
    ExemEntity exam = module.getExam();

    if (exam == null) {
        exam = new ExemEntity();
        exam.setModule(module);
        module.setExam(exam); 
    }

    exam.setName(limit50(examName));
    exam.setDescription(description);
    exam.setXpReward(Math.max(0, xpReward));
    exam.setCoinReward(Math.max(0, coinReward));

    if (exam.getQuests() == null) {
        exam.setQuests(new ArrayList<>());
    } else {
        exam.getQuests().clear();
    }

    for (SeedQuestion seedQuestion : questions) {
        QuestEntity quest = new QuestEntity();
        quest.setExam(exam);
        quest.setName(limit50(seedQuestion.name()));
        quest.setDescription(seedQuestion.description());

        quest.setAnswers(new ArrayList<>());

        for (SeedAnswer seedAnswer : seedQuestion.answers()) {
            AnswerEntity answer = new AnswerEntity();
            answer.setQuest(quest);
            answer.setName(limit50(seedAnswer.name()));
            answer.setCorrectly(seedAnswer.correct());
            quest.getAnswers().add(answer);
        }

        exam.getQuests().add(quest);
    }

    return examRepository.save(exam);
}

    private TasksEntity ensureExamTask(
            ExemEntity exam,
            String taskName,
            String description,
            String language,
            String expectedOutput,
            String inputData,
            int xpReward,
            int coinReward
    ) {
        TasksEntity task = tasksRepository.findByExam_ExemIdOrderByCreatedAtAsc(exam.getExemId()).stream()
                .filter(existing -> existing.getName() != null && existing.getName().equalsIgnoreCase(taskName))
                .findFirst()
                .orElseGet(TasksEntity::new);

        task.setExam(exam);
        task.setLesson(null);
        task.setName(limit50(taskName));
        task.setDescription(description);
        task.setRunnerLanguage(normalizeLanguage(language));
        task.setExpectedOutput(expectedOutput);
        task.setInputData(normalizeInputData(inputData));
        task.setXpReward(Math.max(0, xpReward));
        task.setCoinReward(Math.max(0, coinReward));
        return tasksRepository.saveAndFlush(task);
    }

    private String normalizeLanguage(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (CodeRunnerService.LANGUAGE_JAVA.equals(value) || CodeRunnerService.LANGUAGE_BASH.equals(value)) {
            return value;
        }
        return null;
    }

    private String normalizeInputData(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        return raw;
    }

    private List<SeedAnswer> buildFourAnswers(String correct, String wrong1, String wrong2, String wrong3) {
        return List.of(
                new SeedAnswer(correct, true),
                new SeedAnswer(wrong1, false),
                new SeedAnswer(wrong2, false),
                new SeedAnswer(wrong3, false)
        );
    }

    private String limit50(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= 50) {
            return value;
        }
        return value.substring(0, 50);
    }

    private static <T> List<T> safeList(List<T> items) {
        return items == null ? List.of() : items;
    }

    private record TopicSeed(
            String key,
            String title,
            String moduleDescription,
            String quizQuestion,
            String quizCorrect,
            String quizWrong,
            String examQuestion,
            String examCorrect,
            String examWrong
    ) {
    }

    private record TaskSeed(
            String name,
            String description,
            String expectedOutput,
            String inputData
    ) {
    }

    private record SeedQuestion(
            String name,
            String description,
            List<SeedAnswer> answers
    ) {
    }

    private record SeedAnswer(
            String name,
            boolean correct
    ) {
    }
}
