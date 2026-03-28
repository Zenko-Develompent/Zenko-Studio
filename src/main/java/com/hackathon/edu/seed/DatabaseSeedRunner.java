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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeedRunner implements CommandLineRunner {
    private static final List<TopicSeed> TOPICS = List.of(
            new TopicSeed("variables", "Переменные", "Хранение данных, типы и базовые операции."),
            new TopicSeed("conditionals", "Условные операторы", "Ветвление программы через if/else и логические выражения."),
            new TopicSeed("loops", "Циклы", "Повторение действий через for/while и контроль итераций."),
            new TopicSeed("functions", "Функции", "Разбиение решения на переиспользуемые блоки."),
            new TopicSeed("arrays_strings", "Массивы и строки", "Работа с коллекциями данных и текстом.")
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
                "Пошаговый курс по основам программирования на Java с задачами и экзаменами.",
                "java"
        );
        CourseEntity bashCourse = ensureCourse(
                "Bash для школьников",
                "Курс по алгоритмическому мышлению и автоматизации на Bash.",
                "bash"
        );

        seedCourse(javaCourse, "java", CodeRunnerService.LANGUAGE_JAVA);
        seedCourse(bashCourse, "bash", CodeRunnerService.LANGUAGE_BASH);

        log.info(
                "Seed done: javaCourseId={}, bashCourseId={}, modulesPerCourse={}, lessonsPerModule={}",
                javaCourse.getCourseId(),
                bashCourse.getCourseId(),
                TOPICS.size(),
                2
        );
    }

    private void seedCourse(CourseEntity course, String courseKey, String language) {
        int moduleIndex = 1;
        for (TopicSeed topic : TOPICS) {
            String moduleName = "Модуль " + moduleIndex + ": " + topic.title();
            ModuleEntity module = ensureModule(course, moduleName, topic.moduleDescription());
            module = moduleRepository.findWithLessonsByModuleId(module.getModuleId()).orElseThrow();

            List<LessonPlan> lessonPlans = buildLessonPlans(topic, courseKey);
            List<PlannedLesson> plannedLessons = new ArrayList<>(lessonPlans.size());
            for (int i = 0; i < lessonPlans.size(); i++) {
                LessonPlan plan = lessonPlans.get(i);
                String lessonName = "Урок " + moduleIndex + "." + (i + 1) + ": " + plan.title();
                plannedLessons.add(new PlannedLesson(lessonName, plan));
            }

            syncModuleLessons(module, plannedLessons);

            for (PlannedLesson planned : plannedLessons) {
                LessonPlan plan = planned.plan();
                LessonEntity lesson = ensureLesson(
                        module,
                        planned.lessonName(),
                        plan.description(),
                        plan.bodyPath(),
                        plan.lessonXpReward()
                );

                ensureLessonQuiz(
                        lesson,
                        "Квиз: " + plan.title(),
                        "Проверка ключевых идей урока «" + plan.title() + "».",
                        plan.quizXpReward(),
                        plan.quizCoinReward(),
                        plan.quizQuestions()
                );

                ensureLessonTask(
                        lesson,
                        plan.lessonTask().name(),
                        plan.lessonTask().description(),
                        language,
                        plan.lessonTask().expectedOutput(),
                        plan.lessonTask().inputData(),
                        plan.taskXpReward(),
                        plan.taskCoinReward()
                );
            }

            ExemEntity exam = ensureModuleExam(
                    module,
                    "Экзамен: " + topic.title(),
                    "Итоговая проверка по модулю «" + topic.title() + "».",
                    100 + moduleIndex * 8,
                    45 + moduleIndex * 3,
                    buildExamQuestions(topic)
            );
            syncExamTasks(exam, buildExamTasks(topic), language);

            moduleIndex++;
        }
    }

    private void syncModuleLessons(ModuleEntity module, List<PlannedLesson> plannedLessons) {
        Set<String> requiredNames = new HashSet<>();
        for (PlannedLesson planned : plannedLessons) {
            requiredNames.add(planned.lessonName().toLowerCase(Locale.ROOT));
        }

        List<LessonEntity> existingLessons = new ArrayList<>(safeList(module.getLessons()));
        for (LessonEntity lesson : existingLessons) {
            String currentName = lesson.getName() == null ? "" : lesson.getName().toLowerCase(Locale.ROOT);
            if (requiredNames.contains(currentName)) {
                continue;
            }
            deleteLessonWithRelations(module, lesson);
        }
    }

    private void deleteLessonWithRelations(ModuleEntity module, LessonEntity lesson) {
        QuizEntity quiz = lesson.getQuiz();
        if (quiz != null && quiz.getQuizId() != null) {
            quizRepository.delete(quiz);
        }

        TasksEntity task = lesson.getTask();
        if (task != null && task.getTasksId() != null) {
            tasksRepository.delete(task);
        }

        module.getLessons().remove(lesson);
        lessonRepository.delete(lesson);
    }

    private List<LessonPlan> buildLessonPlans(TopicSeed topic, String courseKey) {
        String introPath = lessonBodyPath(courseKey, topic.key());
        String practicePath = lessonPracticeBodyPath(courseKey, topic.key());

        return switch (topic.key()) {
            case "variables" -> List.of(
                    new LessonPlan(
                            "Переменные: основы",
                            "Что такое переменные, типы и правила именования.",
                            introPath,
                            20, 12, 6, 20, 9,
                            List.of(
                                    q("Суть переменной", "Что такое переменная в программировании?",
                                            "Именованная область памяти для хранения значения",
                                            "Команда запуска программы",
                                            "Графический элемент интерфейса",
                                            "Способ подключения к интернету"),
                                    q("Роль типа данных", "Зачем у переменной есть тип (или ожидаемый формат) данных?",
                                            "Чтобы понимать допустимые значения и операции",
                                            "Чтобы ускорить установку редактора",
                                            "Чтобы отключить ввод с клавиатуры",
                                            "Чтобы код работал только на Windows"),
                                    q("Корректное имя", "Какое имя переменной лучше выбрать?",
                                            "Осмысленное и читаемое, например studentAge",
                                            "Имя с пробелами: student age",
                                            "Только символы вроде @@@",
                                            "Одну букву для всех переменных")
                            ),
                            new TaskSeed(
                                    "Задача: вывести 42",
                                    "Напишите программу, которая выводит число 42 в отдельной строке.",
                                    "42\n",
                                    null
                            )
                    ),
                    new LessonPlan(
                            "Переменные: ввод и вычисления",
                            "Как считывать входные данные и выполнять простые вычисления.",
                            practicePath,
                            24, 14, 7, 24, 10,
                            List.of(
                                    q("Изменение значения", "Что произойдет после операций x = 5; x = x + 2;?",
                                            "Значение x станет 7",
                                            "Значение x станет 52",
                                            "Переменная удалится",
                                            "Программа всегда завершится ошибкой"),
                                    q("Ввод данных", "Зачем программе читать значения из stdin?",
                                            "Чтобы работать с разными входными данными пользователя",
                                            "Чтобы убрать переменные из кода",
                                            "Чтобы запретить условия и циклы",
                                            "Чтобы программа не выводила результат"),
                                    q("Вывод переменной", "Как обычно выводят значение переменной?",
                                            "Передают переменную в print/println/echo",
                                            "Пишут имя переменной в комментарии",
                                            "Удаляют переменную перед выводом",
                                            "Переименовывают файл программы")
                            ),
                            new TaskSeed(
                                    "Задача: сумма двух чисел",
                                    "Считайте два целых числа и выведите их сумму.",
                                    "15\n",
                                    "7 8\n"
                            )
                    )
            );
            case "conditionals" -> List.of(
                    new LessonPlan(
                            "Условные операторы: базовые ветки",
                            "Как принимать решения через if/else.",
                            introPath,
                            20, 12, 6, 22, 9,
                            List.of(
                                    q("Оператор ветвления", "Какой оператор чаще всего используют для ветвления?",
                                            "if", "loop", "input", "module"),
                                    q("Назначение else", "Когда выполняется блок else?",
                                            "Когда условие в if ложно",
                                            "Всегда до if",
                                            "Только при запуске программы",
                                            "Только в цикле for"),
                                    q("Логический результат", "Что должно быть внутри скобок if (...) ?",
                                            "Логическое выражение true/false",
                                            "Название файла",
                                            "Случайный текст",
                                            "Только число 0")
                            ),
                            new TaskSeed(
                                    "Задача: знак числа",
                                    "Считайте целое число N. Выведите YES, если N > 0, иначе NO.",
                                    "YES\n",
                                    "5\n"
                            )
                    ),
                    new LessonPlan(
                            "Условные операторы: составные условия",
                            "Сравнения, логические операторы и порядок проверок.",
                            practicePath,
                            24, 14, 7, 24, 10,
                            List.of(
                                    q("Логическое И", "Какой оператор означает «И»?",
                                            "&&", "||", "==", "+="),
                                    q("Порядок проверок", "Почему важен порядок условий в цепочке if/else if?",
                                            "Чтобы более точные проверки не перекрывались общими",
                                            "Чтобы компилятор выбирал случайный вариант",
                                            "Чтобы убрать переменные из программы",
                                            "Порядок вообще не влияет"),
                                    q("Равенство", "Как обычно проверяют равенство двух значений?",
                                            "Через оператор сравнения == (или эквивалент для языка)",
                                            "Через оператор присваивания =",
                                            "Через комментарий",
                                            "Через имя файла")
                            ),
                            new TaskSeed(
                                    "Задача: чётное или нечётное",
                                    "Считайте целое число N. Выведите EVEN, если N чётное, иначе ODD.",
                                    "ODD\n",
                                    "11\n"
                            )
                    )
            );
            case "loops" -> List.of(
                    new LessonPlan(
                            "Циклы: for и while",
                            "Основы повторения действий в программе.",
                            introPath,
                            20, 12, 6, 22, 9,
                            List.of(
                                    q("Назначение цикла", "Для чего обычно используют цикл?",
                                            "Для повторения однотипных действий",
                                            "Для объявления переменной типа String",
                                            "Для создания изображения",
                                            "Для авторизации пользователя"),
                                    q("Обновление состояния", "Что важно менять внутри while, чтобы цикл не стал бесконечным?",
                                            "Переменную, от которой зависит условие",
                                            "Название программы",
                                            "Цвет терминала",
                                            "Права доступа к файлу"),
                                    q("for или while", "Когда часто удобно использовать цикл for?",
                                            "Когда заранее известен диапазон итераций",
                                            "Только для чтения строк",
                                            "Только для if/else",
                                            "Никогда, while всегда лучше")
                            ),
                            new TaskSeed(
                                    "Задача: числа от 1 до 10",
                                    "Выведите числа от 1 до 10, каждое с новой строки.",
                                    "1\n2\n3\n4\n5\n6\n7\n8\n9\n10\n",
                                    null
                            )
                    ),
                    new LessonPlan(
                            "Циклы: накопление результата",
                            "Суммы, счётчики и шаблоны решения задач с циклами.",
                            practicePath,
                            24, 14, 7, 24, 10,
                            List.of(
                                    q("Счётчик", "Зачем в цикле нужен счётчик?",
                                            "Чтобы отслеживать номер шага и контролировать диапазон",
                                            "Чтобы хранить пароль",
                                            "Чтобы объявить функцию",
                                            "Чтобы отключить ввод данных"),
                                    q("Накопление", "Как обычно считают сумму в цикле?",
                                            "На каждом шаге добавляют значение в accumulator",
                                            "Переименовывают переменную на каждом шаге",
                                            "Удаляют результат перед каждым шагом",
                                            "Используют только условный оператор"),
                                    q("Границы", "Почему важно правильно задавать границы цикла?",
                                            "Чтобы не пропустить нужные значения и не выйти за пределы",
                                            "Чтобы отключить вывод",
                                            "Чтобы цикл работал только один раз",
                                            "Это влияет только на цвет текста")
                            ),
                            new TaskSeed(
                                    "Задача: сумма от 1 до N",
                                    "Считайте целое N и выведите сумму чисел от 1 до N.",
                                    "15\n",
                                    "5\n"
                            )
                    )
            );
            case "functions" -> List.of(
                    new LessonPlan(
                            "Функции: зачем они нужны",
                            "Параметры, возвращаемое значение и повторное использование кода.",
                            introPath,
                            20, 12, 6, 22, 9,
                            List.of(
                                    q("Цель функций", "Зачем выносить код в функцию?",
                                            "Чтобы переиспользовать логику и упростить чтение кода",
                                            "Чтобы запретить переменные",
                                            "Чтобы убрать входные данные",
                                            "Чтобы программа работала только онлайн"),
                                    q("Параметры", "Что обычно передают в функцию?",
                                            "Аргументы — значения для вычислений",
                                            "Только название файла",
                                            "Только комментарии",
                                            "Только кодировку текста"),
                                    q("Возврат результата", "Как функция сообщает результат вычисления?",
                                            "Через return (или эквивалентный механизм)",
                                            "Через удаление переменной",
                                            "Через создание нового модуля",
                                            "Она не может возвращать результат")
                            ),
                            new TaskSeed(
                                    "Задача: квадрат числа",
                                    "Считайте число N и выведите N * N.",
                                    "49\n",
                                    "7\n"
                            )
                    ),
                    new LessonPlan(
                            "Функции: декомпозиция решения",
                            "Как разбивать программу на небольшие функции и тестировать их.",
                            practicePath,
                            24, 14, 7, 24, 10,
                            List.of(
                                    q("Декомпозиция", "Что такое декомпозиция в программировании?",
                                            "Разбиение большой задачи на маленькие функции",
                                            "Удаление всех комментариев",
                                            "Запуск программы без входных данных",
                                            "Случайное переименование переменных"),
                                    q("Тестирование функции", "Почему удобно тестировать маленькие функции отдельно?",
                                            "Проще найти и исправить ошибку в изолированной логике",
                                            "Потому что большие функции всегда запрещены",
                                            "Чтобы программа не использовала циклы",
                                            "Это нужно только для веб-дизайна"),
                                    q("Повторное использование", "Что даёт повторное использование одной функции в нескольких местах?",
                                            "Меньше дублирования и проще поддержка",
                                            "Обязательное ускорение в 100 раз",
                                            "Отказ от входных данных",
                                            "Невозможность изменить код")
                            ),
                            new TaskSeed(
                                    "Задача: максимум из двух",
                                    "Считайте два целых числа и выведите большее из них.",
                                    "9\n",
                                    "5 9\n"
                            )
                    )
            );
            case "arrays_strings" -> List.of(
                    new LessonPlan(
                            "Массивы и строки: основы",
                            "Индексы, длина, обход коллекций и текстовых данных.",
                            introPath,
                            20, 12, 6, 22, 9,
                            List.of(
                                    q("Суть массива", "Что такое массив?",
                                            "Набор элементов одного типа с доступом по индексу",
                                            "Тип цикла",
                                            "Файл настроек",
                                            "Команда запуска программы"),
                                    q("Индексация", "С какого индекса обычно начинается массив?",
                                            "С нуля",
                                            "С единицы всегда",
                                            "С минус одного",
                                            "С последнего элемента"),
                                    q("Длина строки", "Как узнать длину строки?",
                                            "Использовать свойство/метод длины строки",
                                            "Прибавить 1 к последнему символу",
                                            "Сравнить строку с массивом",
                                            "Это невозможно в программе")
                            ),
                            new TaskSeed(
                                    "Задача: длина слова",
                                    "Считайте одно слово и выведите его длину.",
                                    "5\n",
                                    "HELLO\n"
                            )
                    ),
                    new LessonPlan(
                            "Массивы и строки: практика",
                            "Типичные операции: поиск, сумма и обработка текста.",
                            practicePath,
                            24, 14, 7, 24, 10,
                            List.of(
                                    q("Обход массива", "Как чаще всего проходят по всем элементам массива?",
                                            "С помощью цикла по индексам или for-each",
                                            "Только через if/else",
                                            "Только через функцию ввода",
                                            "Массивы нельзя обходить"),
                                    q("Сумма элементов", "Как посчитать сумму элементов массива?",
                                            "Инициализировать sum = 0 и добавлять каждый элемент",
                                            "Сравнивать элементы только попарно",
                                            "Удалять массив перед подсчётом",
                                            "Использовать только комментарии"),
                                    q("Строковые операции", "Что полезно делать перед сравнением пользовательского текста?",
                                            "Нормализовать регистр и убрать лишние пробелы при необходимости",
                                            "Всегда заменять текст числом",
                                            "Удалять все символы",
                                            "Ничего, сравнение всегда одинаковое")
                            ),
                            new TaskSeed(
                                    "Задача: сумма трёх чисел",
                                    "Считайте три целых числа и выведите их сумму.",
                                    "12\n",
                                    "3 4 5\n"
                            )
                    )
            );
            default -> List.of();
        };
    }

    private List<SeedQuestion> buildExamQuestions(TopicSeed topic) {
        return switch (topic.key()) {
            case "variables" -> List.of(
                    q("Переменная и значение", "Что хранит переменная?",
                            "Значение выбранного типа",
                            "Список всех файлов проекта",
                            "Только комментарии",
                            "Пароль от базы данных"),
                    q("Присваивание", "Что делает оператор присваивания?",
                            "Записывает новое значение в переменную",
                            "Запускает цикл",
                            "Создаёт новый модуль",
                            "Удаляет переменную"),
                    q("Ввод", "Почему важно уметь считывать входные данные?",
                            "Для решения универсальных задач, а не только примеров с константами",
                            "Чтобы убрать вывод результата",
                            "Чтобы не использовать условия",
                            "Это нужно только в браузере"),
                    q("Имена", "Какой стиль имени переменной обычно самый читаемый?",
                            "Осмысленное имя вроде totalScore",
                            "Случайный набор символов",
                            "Только одна буква везде",
                            "Имя с пробелами"),
                    q("Формат вывода", "Что важно при сравнении ответа с ожидаемым выводом?",
                            "Текст и переносы строк должны совпадать",
                            "Важен только первый символ",
                            "Достаточно вывести любое число",
                            "Пробелы и переносы никогда не важны")
            );
            case "conditionals" -> List.of(
                    q("if/else", "Когда выполняется else?",
                            "Когда условие if ложно",
                            "Всегда перед if",
                            "Только при ошибке компиляции",
                            "Только если есть цикл"),
                    q("Сравнение", "Что делает оператор > ?",
                            "Проверяет, больше ли левое значение правого",
                            "Склеивает строки",
                            "Считывает ввод",
                            "Печатает результат"),
                    q("Логика", "Что означает оператор && ?",
                            "Оба условия должны быть истинны",
                            "Достаточно одного истинного",
                            "Меняет местами переменные",
                            "Прерывает программу"),
                    q("Порядок", "Почему в цепочке if/else if важен порядок веток?",
                            "Раньше стоящая ветка может перехватить условие",
                            "Порядок не влияет вообще",
                            "Потому что иначе не работает println",
                            "Из-за ограничений файловой системы"),
                    q("Граничные случаи", "Что нужно сделать перед сдачей решения с условиями?",
                            "Проверить граничные значения (0, 1, отрицательные и т.д.)",
                            "Удалить все проверки",
                            "Оставить только одну ветку",
                            "Всегда выводить YES")
            );
            case "loops" -> List.of(
                    q("Назначение", "Для чего нужен цикл?",
                            "Для повторения действий, пока выполняется условие",
                            "Только для объявления переменных",
                            "Для изменения темы редактора",
                            "Для создания комментариев"),
                    q("Счётчик", "Что обычно делает счётчик цикла?",
                            "Управляет количеством повторений",
                            "Хранит пароль",
                            "Переименовывает функцию",
                            "Запрещает ввод данных"),
                    q("while", "Когда цикл while завершится?",
                            "Когда условие станет ложным",
                            "Никогда не завершается",
                            "После первой итерации всегда",
                            "Только по нажатию Enter"),
                    q("Накопление", "Как вычислить сумму в цикле?",
                            "Добавлять текущее значение к sum на каждом шаге",
                            "Перезаписывать sum нулём каждый раз",
                            "Использовать только if без цикла",
                            "Нельзя посчитать сумму в цикле"),
                    q("Ошибки", "Что часто вызывает бесконечный цикл?",
                            "Отсутствие изменения переменной в условии",
                            "Слишком длинное имя переменной",
                            "Наличие комментария в коде",
                            "Вывод результата через println")
            );
            case "functions" -> List.of(
                    q("Функция", "Что такое функция?",
                            "Именованный блок кода, который выполняет задачу",
                            "Только комментарий",
                            "Тип переменной",
                            "Формат файла"),
                    q("Параметры", "Для чего функции параметры?",
                            "Передавать данные внутрь функции",
                            "Отключать return",
                            "Печатать только текст",
                            "Запрещать циклы"),
                    q("Return", "Зачем нужен return?",
                            "Вернуть результат вычисления из функции",
                            "Начать новый модуль",
                            "Считать данные из файла",
                            "Ускорить интернет"),
                    q("Переиспользование", "Почему функции уменьшают дублирование?",
                            "Один алгоритм можно вызвать в нескольких местах",
                            "Они автоматически пишут тесты",
                            "Потому что удаляют переменные",
                            "Потому что заменяют все циклы"),
                    q("Читаемость", "Как функции влияют на читаемость программы?",
                            "Код становится структурированнее и понятнее",
                            "Код всегда становится длиннее и хуже",
                            "Они убирают необходимость в именах",
                            "Никак не влияют")
            );
            case "arrays_strings" -> List.of(
                    q("Массив", "Что хранит массив?",
                            "Набор элементов одного типа",
                            "Только один символ",
                            "Только имя файла",
                            "Только условие if"),
                    q("Индекс", "Что означает индекс массива?",
                            "Позицию элемента в массиве",
                            "Тип данных элемента",
                            "Имя модуля",
                            "Количество файлов в проекте"),
                    q("Строка", "Что такое строка в программировании?",
                            "Последовательность символов",
                            "Числовой цикл",
                            "Условный оператор",
                            "Системная переменная"),
                    q("Длина", "Для чего нужна длина строки/массива?",
                            "Чтобы знать границы при обходе",
                            "Чтобы менять тип данных",
                            "Чтобы удалить элемент автоматически",
                            "Она нужна только в Bash"),
                    q("Ошибки", "Что часто вызывает ошибку при работе с индексами?",
                            "Выход за границы массива",
                            "Слишком короткое имя функции",
                            "Использование пробела в строке",
                            "Наличие вывода в консоль")
            );
            default -> List.of();
        };
    }

    private List<TaskSeed> buildExamTasks(TopicSeed topic) {
        return switch (topic.key()) {
            case "variables" -> List.of(
                    new TaskSeed(
                            "Экз задача: сумма двух",
                            "Считайте два целых числа и выведите их сумму.",
                            "20\n",
                            "8 12\n"
                    ),
                    new TaskSeed(
                            "Экз задача: приветствие",
                            "Считайте имя и выведите: Привет, <имя>!",
                            "Привет, Аня!\n",
                            "Аня\n"
                    )
            );
            case "conditionals" -> List.of(
                    new TaskSeed(
                            "Экз задача: возрастная группа",
                            "Если age < 13 -> CHILD, иначе если age < 18 -> TEEN, иначе ADULT.",
                            "TEEN\n",
                            "15\n"
                    ),
                    new TaskSeed(
                            "Экз задача: максимум из трёх",
                            "Считайте три целых числа и выведите максимальное.",
                            "9\n",
                            "3 9 4\n"
                    )
            );
            case "loops" -> List.of(
                    new TaskSeed(
                            "Экз задача: сумма квадратов",
                            "Считайте N и выведите сумму квадратов от 1 до N.",
                            "14\n",
                            "3\n"
                    ),
                    new TaskSeed(
                            "Экз задача: таблица на 3",
                            "Выведите 3, 6, 9, 12, 15 каждое с новой строки.",
                            "3\n6\n9\n12\n15\n",
                            null
                    )
            );
            case "functions" -> List.of(
                    new TaskSeed(
                            "Экз задача: модуль числа",
                            "Считайте целое N и выведите его модуль.",
                            "7\n",
                            "-7\n"
                    ),
                    new TaskSeed(
                            "Экз задача: минимум из двух",
                            "Считайте два целых числа и выведите меньшее.",
                            "4\n",
                            "4 9\n"
                    )
            );
            case "arrays_strings" -> List.of(
                    new TaskSeed(
                            "Экз задача: первый и последний",
                            "Считайте слово и выведите первый и последний символ через пробел.",
                            "H O\n",
                            "HELLO\n"
                    ),
                    new TaskSeed(
                            "Экз задача: сумма массива",
                            "Считайте N, затем N целых чисел. Выведите их сумму.",
                            "10\n",
                            "4\n1 2 3 4\n"
                    )
            );
            default -> List.of();
        };
    }

    private void syncExamTasks(ExemEntity exam, List<TaskSeed> requiredTasks, String language) {
        List<TasksEntity> existing = tasksRepository.findByExam_ExemIdOrderByCreatedAtAsc(exam.getExemId());
        Set<String> names = new HashSet<>();
        for (TaskSeed task : requiredTasks) {
            names.add(task.name().toLowerCase(Locale.ROOT));
        }

        for (TasksEntity task : existing) {
            String currentName = task.getName() == null ? "" : task.getName().toLowerCase(Locale.ROOT);
            if (!names.contains(currentName)) {
                tasksRepository.delete(task);
            }
        }

        for (TaskSeed task : requiredTasks) {
            ensureExamTask(
                    exam,
                    task.name(),
                    task.description(),
                    language,
                    task.expectedOutput(),
                    task.inputData(),
                    0,
                    0
            );
        }
    }

    private String lessonBodyPath(String courseKey, String topicKey) {
        return "seed/" + courseKey + "/" + topicKey + ".md";
    }

    private String lessonPracticeBodyPath(String courseKey, String topicKey) {
        return "seed/" + courseKey + "/" + topicKey + "_practice.md";
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

        lesson.setName(limit50(name));
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

        quiz.setName(limit50(quizName));
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

    private SeedQuestion q(String name, String description, String correct, String wrong1, String wrong2, String wrong3) {
        return new SeedQuestion(name, description, buildFourAnswers(correct, wrong1, wrong2, wrong3));
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

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record TopicSeed(
            String key,
            String title,
            String moduleDescription
    ) {
    }

    private record LessonPlan(
            String title,
            String description,
            String bodyPath,
            int lessonXpReward,
            int quizXpReward,
            int quizCoinReward,
            int taskXpReward,
            int taskCoinReward,
            List<SeedQuestion> quizQuestions,
            TaskSeed lessonTask
    ) {
    }

    private record PlannedLesson(
            String lessonName,
            LessonPlan plan
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
