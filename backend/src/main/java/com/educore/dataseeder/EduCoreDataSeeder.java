package com.educore.dataseeder;

import com.educore.admin.Admin;
import com.educore.assignment.Assignment;
import com.educore.assignment.assignmentQuestion.AssignmentQuestion;
import com.educore.category.Category;
import com.educore.center.Center;
import com.educore.copon.AccessCode;
import com.educore.copon.AccessCodeUsage;
import com.educore.copon.CodeTargetType;
import com.educore.course.Course;
import com.educore.enrollment.Enrollment;
import com.educore.enrollment.EnrollmentStatus;
import com.educore.enrollment.EnrollmentType;
import com.educore.lesson.Week;
import org.springframework.core.annotation.Order;  // ✅ هذا هو الـ import الصحيح
import com.educore.lesson.WeekLockType;
import com.educore.lessonmaterial.LessonMaterial;
import com.educore.lessonmaterial.MaterialType;
import com.educore.level.Level;
import com.educore.parent.Parent;
import com.educore.payment.order.OrderItem;
import com.educore.payment.order.OrderStatus;
import com.educore.payment.payment.Payment;
import com.educore.payment.payment.PaymentMethod;
import com.educore.payment.payment.PaymentStatus;
import com.educore.question.Question;
import com.educore.questionbank.BankQuestion;
import com.educore.questionbank.DifficultyLevel;
import com.educore.questionbank.QuestionTopic;
import com.educore.quiz.Quiz;
import com.educore.quiz.StudentQuizAttempt;
import com.educore.student.Student;
import com.educore.student.StudentStatus;
import com.educore.studentcard.StudentCard;
import com.educore.unit.Session;
import com.educore.wallet.Wallet;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * ═══════════════════════════════════════════════════════════════
 *   EduCore Data Seeder — Complete & Fixed
 *
 *   Seeding order (FK-safe):
 *   Admin → Center → Level → Category → Course → Session →
 *   Week → LessonMaterial → Quiz → Question → Assignment →
 *   AssignmentQuestion → QuestionTopic → BankQuestion →
 *   Parent → Student → StudentCard → Wallet → Enrollment →
 *   AccessCode → Order → Payment → StudentQuizAttempt
 * ═══════════════════════════════════════════════════════════════
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class EduCoreDataSeeder  implements CommandLineRunner {

    @PersistenceContext
    private EntityManager em;

    // Injected via constructor (Spring auto-wires BCryptPasswordEncoder bean)
    private final PasswordEncoder passwordEncoder;

    private final Random random = new Random(42); // fixed seed → reproducible runs

    // ── Collected references shared across sections ────────────
    private final List<Level>      levels      = new ArrayList<>();
    private final List<Category>   categories  = new ArrayList<>();
    private final List<Course>     courses     = new ArrayList<>();
    private final List<Session>    sessions    = new ArrayList<>();
    private final List<Week>       weeks       = new ArrayList<>();
    private final List<Quiz>       quizzes     = new ArrayList<>();
    private final List<Assignment> assignments = new ArrayList<>();
    private final List<Student>    students    = new ArrayList<>();
    private final List<Parent>     parents     = new ArrayList<>();
    private final List<Center>     centers     = new ArrayList<>();
    private final List<Admin>      admins      = new ArrayList<>();

    // ── Real YouTube educational video IDs (Arabic-friendly) ──
    private static final String[] YOUTUBE_IDS = {
            "dQw4w9WgXcQ", "9bZkp7q19f0", "hT_nvWreIhg", "JGwWNGJdvx8",
            "kJQP7kiw5Fk", "OPf0YbXqDm0", "YqeW9_5kURI", "fJ9rUzIMcZQ",
            "pRpeEdMmmQ0", "tgbNymZ7vqY", "PT2_F-1esPk", "60ItHLz5WEA",
            "gdZLi9oWNZg", "09R8_2nJtjg", "7wtfhZwyrcc", "uelHwf8o7_U",
            "CevxZvSJLk8", "u9Dg-g7t2l4", "2vjPBrBU-TM", "qk1nnAHC76g",
            "BQ0mxQoEzvI", "3JZ_D3ELwOQ", "M7lc1UVf-VE", "oHg5SJYRHA0",
            "dGpVRO4Hvlg", "S2hrnAMUkPU", "YkADj0TPrJA", "LsoLEjrDogU"
    };

    // ══════════════════════════════════════════════════════════════
    //   ENTRY POINT
    // ══════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public void run(String... args) {
        long adminCount = (long) em.createQuery("SELECT COUNT(a) FROM Admin a").getSingleResult();

        if (adminCount > 0) {
            log.info("⚠️ [EduCore Seeder] Data already exists in database. Skipping seeding process...");
            return; // توقف هنا ولا تنفذ أي شيء
        }
        log.info("╔══════════════════════════════════════════════╗");
        log.info("║        EduCore Data Seeder Starting          ║");
        log.info("╚══════════════════════════════════════════════╝");

        seedAdmins();
        seedCenters();
        seedLevels();
        seedCategoriesAndCourses();
        seedSessionsAndWeeks();
        seedLessonMaterials();
        seedQuizzesAndQuestions();
        seedAssignments();
        seedQuestionTopicsAndBankQuestions();
        seedParentsAndStudents();
        seedStudentCardsAndWallets();
        seedEnrollments();
        seedAccessCodes();
        seedOrdersAndPayments();
        seedStudentQuizAttempts();

        printSummary();
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPER METHODS
    // ══════════════════════════════════════════════════════════════

    /** Returns a picsum.photos URL with deterministic ID */
    private String randomImageUrl(String category, int id) {
        int seed = Math.abs((category + id).hashCode()) % 1000;
        return "https://picsum.photos/seed/" + category + seed + "/400/300";
    }

    /** Returns a random YouTube video ID from the pool */
    private String randomYouTubeId(int idx) {
        return YOUTUBE_IDS[idx % YOUTUBE_IDS.length];
    }

    /** Returns a plausible PDF storage URL */
    private String randomPdfUrl(String label) {
        return "https://storage.educore.com/materials/" + label.replace(" ", "_")
                + "_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
    }

    /** Generates a unique 6-digit student code like 100001 */
    private String generateStudentCode(int idx) {
        return String.format("%06d", 100000 + idx + 1);
    }

    // ══════════════════════════════════════════════════════════════
    //  SECTION 1 — Admins (4 total)
    // ══════════════════════════════════════════════════════════════
    private void seedAdmins() {
        log.info("── [1/15] Seeding Admins ───────────────────────");

        String[][] adminData = {
                {"01000000001", "أحمد محمد السيد",        "Admin@Pass1"},
                {"01000000002", "سارة عبدالرحمن خليل",    "Admin@Pass2"},
                {"01000000003", "خالد إبراهيم النجار",    "Admin@Pass3"},
                {"01000000004", "منى طارق الشريف",        "Admin@Pass4"}
        };

        for (String[] d : adminData) {
            Admin a = Admin.builder()
                    .phone(d[0])
                    .name(d[1])
                    .password(passwordEncoder.encode(d[2]))
                    .enabled(true)
                    .build();
            em.persist(a);
            admins.add(a);
            log.info("  ✓ Admin: {} ({})", d[1], d[0]);
        }
        em.flush();
    }

    // ══════════════════════════════════════════════════════════════
    //  SECTION 2 — Centers (8 total)
    // ══════════════════════════════════════════════════════════════
    private void seedCenters() {
        log.info("── [2/15] Seeding Centers ──────────────────────");

        // [name, governorate, address, phone, whatsapp, instagram, facebook, telegram, youtube, tiktok, lat, lng]
        Object[][] centerData = {
                {"القاهرة - المقطم",      "القاهرة",    "شارع الأهرام، بجوار مول ستار، المقطم",         "0220001111",
                        "https://wa.me/g/cairo-mokattam",  "https://instagram.com/educore_mokattam",
                        "https://facebook.com/educore.mokattam", "https://t.me/educore_mokattam",
                        "https://youtube.com/@educore",    "https://tiktok.com/@educore_mokattam",
                        30.0131, 31.7667},

                {"الجيزة - الدقي",        "الجيزة",     "شارع التحرير، برج النيل، الدقي",               "0235551234",
                        "https://wa.me/g/giza-dokki",      "https://instagram.com/educore_dokki",
                        "https://facebook.com/educore.dokki",    "https://t.me/educore_dokki",
                        "https://youtube.com/@educore",    "https://tiktok.com/@educore_dokki",
                        30.0444, 31.2003},

                {"الإسكندرية - سموحة",    "الإسكندرية", "شارع فوزي معاذ، برج سموحة",                    "0341112222",
                        "https://wa.me/g/alex-smouha",     "https://instagram.com/educore_alex",
                        "https://facebook.com/educore.alex",     "https://t.me/educore_alex",
                        "https://youtube.com/@educore",    "https://tiktok.com/@educore_alex",
                        31.2001, 29.9187},

                {"المنصورة - الجامعة",    "الدقهلية",   "شارع الجمهورية، بجوار جامعة المنصورة",         "0502223333",
                        "https://wa.me/g/mansoura-univ",   "https://instagram.com/educore_mansoura",
                        "https://facebook.com/educore.mansoura", "https://t.me/educore_mansoura",
                        "https://youtube.com/@educore",    "https://tiktok.com/@educore_mansoura",
                        31.0409, 31.3785},

                {"أسيوط - المركز",       "أسيوط",      "شارع صلاح الدين، بجوار المحكمة، أسيوط",        "0884445555",
                        "https://wa.me/g/assiut-center",   "https://instagram.com/educore_assiut",
                        "https://facebook.com/educore.assiut",   "https://t.me/educore_assiut",
                        "https://youtube.com/@educore",    "https://tiktok.com/@educore_assiut",
                        27.1783, 31.1859},

                {"طنطا - المركز",        "الغربية",    "شارع البحر، برج الفردوس، طنطا",                "0403336666",
                        "https://wa.me/g/tanta-center",    "https://instagram.com/educore_tanta",
                        "https://facebook.com/educore.tanta",    "https://t.me/educore_tanta",
                        "https://youtube.com/@educore",    "https://tiktok.com/@educore_tanta",
                        30.7870, 31.0004},

                {"الزقازيق - الشرقية",   "الشرقية",   "شارع الحرية، بجوار ميدان البلد، الزقازيق",     "0552227777",
                        "https://wa.me/g/zagazig-center",  "https://instagram.com/educore_zagazig",
                        "https://facebook.com/educore.zagazig",  "https://t.me/educore_zagazig",
                        "https://youtube.com/@educore",    "https://tiktok.com/@educore_zagazig",
                        30.5877, 31.5021},

                {"بني سويف - المركز",    "بني سويف",   "شارع الجامعة، برج القاهرة الجديد، بني سويف",   "0822228888",
                        "https://wa.me/g/benisuef-center", "https://instagram.com/educore_benisuef",
                        "https://facebook.com/educore.benisuef",  "https://t.me/educore_benisuef",
                        "https://youtube.com/@educore",    "https://tiktok.com/@educore_benisuef",
                        29.0661, 31.0994}
        };

        for (Object[] d : centerData) {
            Center c = Center.builder()
                    .name((String) d[0])
                    .governorate((String) d[1])
                    .address((String) d[2])
                    .phone((String) d[3])
                    .whatsappGroupLink((String) d[4])
                    .instagramLink((String) d[5])
                    .facebookLink((String) d[6])
                    .telegramLink((String) d[7])
                    .youtubeLink((String) d[8])
                    .tiktokLink((String) d[9])
                    .active(true)
                    .createdBy("SEEDER")
                    .build();
            em.persist(c);
            centers.add(c);
            log.info("  ✓ Center: {}", d[0]);
        }
        em.flush();
    }

    // ══════════════════════════════════════════════════════════════
    //  SECTION 3 — Levels (5 total)
    // ══════════════════════════════════════════════════════════════
    private void seedLevels() {
        log.info("── [3/15] Seeding Levels ───────────────────────");

        String[] levelNames = {
                "الصف الأول الثانوي",
                "الصف الثاني الثانوي",
                "الصف الثالث الثانوي",
                "الصف الأول الإعدادي",
                "الصف الثاني الإعدادي"
        };

        for (String name : levelNames) {
            Level l = Level.builder()
                    .name(name)
                    .categories(new ArrayList<>())
                    .build();
            em.persist(l);
            levels.add(l);
            log.info("  ✓ Level: {}", name);
        }
        em.flush();
    }

    // ══════════════════════════════════════════════════════════════
    //  SECTION 4 — Categories & Courses
    //  3 categories per level × 2 courses each = 30 courses total
    // ══════════════════════════════════════════════════════════════
    private void seedCategoriesAndCourses() {
        log.info("── [4/15] Seeding Categories & Courses ─────────");

        // Per level: [catName, catPrice, course1Title, course1Price, course2Title, course2Price]
        String[][][] subjectData = {
                // ── Level 0: أولى ثانوي ─────────────────────────────
                {
                        {"رياضيات - أولى ثانوي",    "350.00",
                                "الجبر والمعادلات",          "150.00",
                                "الهندسة التحليلية",         "150.00"},
                        {"فيزياء - أولى ثانوي",     "320.00",
                                "الحركة والديناميكا",        "140.00",
                                "الكهرباء الساكنة",          "140.00"},
                        {"كيمياء - أولى ثانوي",     "300.00",
                                "الذرة والجدول الدوري",      "130.00",
                                "التفاعلات الكيميائية",     "130.00"}
                },
                // ── Level 1: تانية ثانوي ─────────────────────────────
                {
                        {"رياضيات - تانية ثانوي",   "380.00",
                                "التفاضل والتكامل",          "170.00",
                                "الإحصاء والاحتمالات",       "170.00"},
                        {"فيزياء - تانية ثانوي",    "350.00",
                                "المغناطيسية",               "160.00",
                                "الموجات والصوت",            "160.00"},
                        {"أحياء - تانية ثانوي",     "330.00",
                                "علم الخلية",                "150.00",
                                "الوراثة",                  "150.00"}
                },
                // ── Level 2: تالتة ثانوي ─────────────────────────────
                {
                        {"رياضيات - تالتة ثانوي",   "450.00",
                                "التفاضل المتقدم",           "200.00",
                                "الفضاء والمتجهات",          "200.00"},
                        {"فيزياء - تالتة ثانوي",    "420.00",
                                "الفيزياء الحديثة",          "190.00",
                                "الديناميكا الحرارية",       "190.00"},
                        {"كيمياء - تالتة ثانوي",    "400.00",
                                "الكيمياء العضوية",          "180.00",
                                "الكيمياء الكهربية",         "180.00"}
                },
                // ── Level 3: أولى إعدادي ─────────────────────────────
                {
                        {"رياضيات - أولى إعدادي",   "200.00",
                                "الأعداد والعمليات",         "90.00",
                                "الهندسة المستوية",          "90.00"},
                        {"علوم - أولى إعدادي",      "180.00",
                                "المادة وخصائصها",           "80.00",
                                "الكائنات الحية",            "80.00"},
                        {"لغة عربية - أولى إعدادي", "160.00",
                                "النحو والصرف",              "70.00",
                                "البلاغة والأدب",            "70.00"}
                },
                // ── Level 4: تانية إعدادي ────────────────────────────
                {
                        {"رياضيات - تانية إعدادي",  "220.00",
                                "المعادلات التربيعية",       "100.00",
                                "الإحصاء الأساسي",           "100.00"},
                        {"علوم - تانية إعدادي",     "200.00",
                                "الضوء والصوت",              "90.00",
                                "الكيمياء التطبيقية",       "90.00"},
                        {"جغرافيا - تانية إعدادي",  "150.00",
                                "جغرافيا مصر",              "65.00",
                                "الجغرافيا الاقتصادية",    "65.00"}
                }
        };

        int catImageSeed = 10;
        int crsImageSeed = 100;

        for (int li = 0; li < levels.size(); li++) {
            Level level = levels.get(li);
            String[][] levelSubjects = subjectData[li];

            for (String[] sub : levelSubjects) {
                Category cat = Category.builder()
                        .name(sub[0])
                        .description("باقة " + sub[0] + " — تشمل جميع الكورسات المرتبطة بهذا المستوى")
                        .price(new BigDecimal(sub[1]))
                        .active(true)
                        .level(level)
                        .courses(new HashSet<>())
                        .build();
                em.persist(cat);
                categories.add(cat);
                catImageSeed++;

                // Course 1
                Course c1 = buildCourse(sub[2], sub[3], crsImageSeed++);
                em.persist(c1);
                courses.add(c1);
                // Bidirectional: category ↔ course
                cat.getCourses().add(c1);
                c1.getCategories().add(cat);

                // Course 2
                Course c2 = buildCourse(sub[4], sub[5], crsImageSeed++);
                em.persist(c2);
                courses.add(c2);
                cat.getCourses().add(c2);
                c2.getCategories().add(cat);

                log.info("  ✓ Category: {} → [{}, {}]", sub[0], sub[2], sub[4]);
            }
        }
        em.flush();
        log.info("  Total — Categories: {}, Courses: {}", categories.size(), courses.size());
    }

    private Course buildCourse(String title, String price, int imageSeed) {
        return Course.builder()
                .title(title)
                .description("كورس " + title
                        + " — مصمم لمساعدة الطلاب على فهم المادة بعمق وتحقيق أعلى الدرجات.")
                .price(new BigDecimal(price))
                .active(true)
                .categories(new HashSet<>())
                .sessions(new HashSet<>())
                .build();
    }

    // ══════════════════════════════════════════════════════════════
    //  SECTION 5 — Sessions & Weeks
    //  3 sessions per course, 4 weeks per session
    // ══════════════════════════════════════════════════════════════
    private void seedSessionsAndWeeks() {
        log.info("── [5/15] Seeding Sessions & Weeks ─────────────");

        String[] sessionTitles = {"الترم الأول", "الترم الثاني", "المراجعة النهائية"};
        String[] sessionDescs  = {
                "يغطي الفصل الدراسي الأول بالكامل مع شرح تفصيلي لكل درس",
                "يغطي الفصل الدراسي الثاني بالكامل مع تدريبات متعددة",
                "مراجعة شاملة لجميع المناهج مع نماذج امتحانات وحلول"
        };
        String[] weekTitles = {
                "الأسبوع الأول — مقدمة وأساسيات",
                "الأسبوع الثاني — التعمق في المفاهيم",
                "الأسبوع الثالث — التطبيق والتدريب",
                "الأسبوع الرابع — مراجعة واختبار"
        };
        String[] weekDescs = {
                "نبدأ بمقدمة شاملة ونضع الأساس العلمي الصحيح للمادة",
                "نتعمق في المفاهيم الأساسية ونحلل الأمثلة بالتفصيل",
                "نطبق ما تعلمناه على مسائل متنوعة المستوى",
                "نراجع كل ما سبق ونختبر الفهم بامتحان شامل"
        };

        for (Course course : courses) {
            for (int si = 0; si < 3; si++) {
                Session session = Session.builder()
                        .title(sessionTitles[si] + " — " + course.getTitle())
                        .description(sessionDescs[si])
                        .active(true)
                        .courses(new HashSet<>())
                        .weeks(new HashSet<>())
                        .build();
                em.persist(session);
                sessions.add(session);

                // Bidirectional: course ↔ session
                session.getCourses().add(course);
                course.getSessions().add(session);

                for (int wi = 0; wi < 4; wi++) {
                    Week week = Week.builder()
                            .title(weekTitles[wi])
                            .description(weekDescs[wi])
                            .orderNumber(wi + 1)
                            .active(true)
                            .lockType(wi < 2 ? WeekLockType.NEVER : WeekLockType.AFTER_DURATION)
                            .lockAfterDays(wi < 2 ? null : 30)
                            .globallyLocked(false)
                            .hasQuiz(true)
                            .sessions(new HashSet<>())
                            .materials(new HashSet<>())
                            .quizzes(new HashSet<>())
                            .assignments(new HashSet<>())
                            .build();
                    em.persist(week);
                    weeks.add(week);

                    // Bidirectional: session ↔ week
                    week.getSessions().add(session);
                    session.getWeeks().add(week);
                }
            }
        }
        em.flush();
        log.info("  ✓ Sessions: {}, Weeks: {}", sessions.size(), weeks.size());
    }

    // ══════════════════════════════════════════════════════════════
    //  SECTION 6 — Lesson Materials (2 per week: 1 YouTube, 1 PDF)
    // ══════════════════════════════════════════════════════════════
    private void seedLessonMaterials() {
        log.info("── [6/15] Seeding Lesson Materials ─────────────");

        int matCount = 0;
        int yIdx     = 0;

        for (Week week : weeks) {
            String weekLabel = week.getTitle().replaceAll("[^\\w\\u0600-\\u06FF]", "_");

            // ── YouTube Video ──────────────────────────────────
            String videoId = randomYouTubeId(yIdx++);
            LessonMaterial video = LessonMaterial.builder()
                    .materialType(MaterialType.YOUTUBE)
                    .fileUrl("https://www.youtube.com/watch?v=" + videoId)
                    .youtubeVideoId(videoId)
                    .fileName("شرح_" + weekLabel + ".mp4")
                    .fileSize(null)                              // YouTube: no local size
                    .downloadCount(random.nextInt(300))
                    .preview(false)
                    .durationSeconds((long) (20 + random.nextInt(40)) * 60L)
                    .active(true)
                    .weeks(new HashSet<>())
                    .build();
            em.persist(video);
            // Bidirectional: week ↔ material
            week.getMaterials().add(video);
            video.getWeeks().add(week);
            matCount++;

            // ── PDF Material ───────────────────────────────────
            LessonMaterial pdf = LessonMaterial.builder()
                    .materialType(MaterialType.PDF)
                    .fileUrl(randomPdfUrl(weekLabel))
                    .youtubeVideoId(null)
                    .fileName("ملزمة_" + weekLabel + ".pdf")
                    .fileSize((long) (512 + random.nextInt(4096)) * 1024L)
                    .downloadCount(random.nextInt(200))
                    .preview(false)
                    .durationSeconds(null)
                    .active(true)
                    .weeks(new HashSet<>())
                    .build();
            em.persist(pdf);
            week.getMaterials().add(pdf);
            pdf.getWeeks().add(week);
            matCount++;
        }
        em.flush();
        log.info("  ✓ LessonMaterials: {} ({}v + {}p)", matCount, matCount / 2, matCount / 2);
    }

    // ══════════════════════════════════════════════════════════════
    //  SECTION 7 — Quizzes & Questions (1 quiz/week, 5 questions)
    // ══════════════════════════════════════════════════════════════
    private void seedQuizzesAndQuestions() {
        log.info("── [7/15] Seeding Quizzes & Questions ──────────");

        // [description, opt1, opt2, opt3, opt4, correctAnswer]
        String[][] templates = {
                {"ما هي قيمة س في المعادلة: ٢س + ٤ = ١٠؟",
                        "٣","٢","٤","٥","٣"},
                {"أي من التالي يمثل قانون نيوتن الثاني؟",
                        "F = ma","E = mc²","PV = nRT","F = kq₁q₂/r²","F = ma"},
                {"كم عدد بروتونات ذرة الأكسجين؟",
                        "٨","٦","١٠","١٢","٨"},
                {"ما هو مشتق الدالة f(x) = x³؟",
                        "3x²","x²","3x","2x³","3x²"},
                {"ما هي وحدة قياس القوة في SI؟",
                        "نيوتن","جول","واط","باسكال","نيوتن"},
                {"ما اسم العملية التي تحول الضوء إلى طاقة في النباتات؟",
                        "البناء الضوئي","التنفس الخلوي","الأكسدة","التخمر","البناء الضوئي"},
                {"ما هي نتيجة ٧ × ٩؟",
                        "٦٣","٥٦","٧٢","٤٩","٦٣"},
                {"أي من الآتي هو عنصر لا فلزي؟",
                        "الكلور","الحديد","النحاس","الألومنيوم","الكلور"},
                {"ما قيمة sin(90°)؟",
                        "١","٠","½","√2/2","١"},
                {"ما هو أكبر عدد أولي أقل من ٢٠؟",
                        "١٩","١٧","١٣","١٦","١٩"},
                {"ما هي سرعة الضوء في الفراغ تقريباً؟",
                        "3×10⁸ م/ث","3×10⁶ م/ث","3×10¹⁰ م/ث","3×10⁴ م/ث","3×10⁸ م/ث"},
                {"ما هو الرمز الكيميائي للذهب؟",
                        "Au","Ag","Fe","Cu","Au"},
                {"ما هو ناتج ١٥ ÷ ٣؟",
                        "٥","٣","٧","٤","٥"},
                {"أين يحدث تركيب البروتين في الخلية؟",
                        "الريبوسوم","النواة","الميتوكوندريا","الشبكة الإندوبلازمية","الريبوسوم"},
                {"ما مساحة مثلث قاعدته ٦ سم وارتفاعه ٤ سم؟",
                        "١٢ سم²","٢٤ سم²","١٠ سم²","٨ سم²","١٢ سم²"},
                {"ما الوحدة الأساسية للحرارة في SI؟",
                        "جول","كالوري","واط","نيوتن","جول"},
                {"أي من العناصر يقع في الدورة الثالثة من الجدول الدوري؟",
                        "الصوديوم","الكربون","الأكسجين","الكالسيوم","الصوديوم"},
                {"ما هو تكامل الدالة f(x) = 2x؟",
                        "x²+C","2x²+C","x+C","2+C","x²+C"},
                {"ما نوع الشحنة التي يحملها الإلكترون؟",
                        "سالبة","موجبة","محايدة","متذبذبة","سالبة"},
                {"ما هي قيمة cos(0°)؟",
                        "١","٠","-١","½","١"}
        };

        int quizCount = 0;
        int qCount    = 0;
        int tIdx      = 0;
        int imgSeed   = 200;

        for (Week week : weeks) {
            Quiz quiz = Quiz.builder()
                    .title("اختبار — " + week.getTitle())
                    .active(true)
                    .week(week)
                    .questions(new HashSet<>())
                    .durationMinutes(30)
                    .timeRestricted(true)
                    .deleted(false)
                    .build();
            em.persist(quiz);
            quizzes.add(quiz);
            week.getQuizzes().add(quiz);
            quizCount++;

            for (int qi = 0; qi < 5; qi++) {
                String[] t = templates[tIdx % templates.length];
                tIdx++;

                Question q = Question.builder()
                        .imageUrl(randomImageUrl("question", imgSeed++))
                        .description(t[0])
                        .mark(2)
                        .quiz(quiz)
                        .options(Arrays.asList(t[1], t[2], t[3], t[4]))
                        .correctAnswer(t[5])
                        .deleted(false)
                        .build();
                em.persist(q);
                quiz.getQuestions().add(q);
                qCount++;
            }

            if (quizCount % 100 == 0) em.flush();
        }
        em.flush();
        log.info("  ✓ Quizzes: {}, Questions: {}", quizCount, qCount);
    }

    // ══════════════════════════════════════════════════════════════
    //  SECTION 8 — Assignments & AssignmentQuestions
    //  1 per week, 2 questions each
    // ══════════════════════════════════════════════════════════════
    private void seedAssignments() {
        log.info("── [8/15] Seeding Assignments ───────────────────");

        String[] titles = {
                "واجب مراجعة الأسبوع",   "تدريب على المسائل",
                "ورقة عمل أسبوعية",     "واجب التطبيق العملي",
                "مسائل تحدي",            "أسئلة التفكير الناقد"
        };

        // [description, opt1, opt2, opt3, opt4, correctAnswer]
        String[][] aqTemplates = {
                {"اشرح خطوات حل المعادلة x² - 5x + 6 = 0 مع التوضيح الكامل.",
                        "x=2 أو x=3","x=1 أو x=6","x=-2 أو x=-3","x=3 أو x=4","x=2 أو x=3"},
                {"طائر يطير بسرعة ٦٠ كم/ساعة لمدة ٢.٥ ساعة. ما المسافة؟",
                        "١٥٠ كم","١٢٠ كم","١٨٠ كم","٩٠ كم","١٥٠ كم"},
                {"ما الفرق بين التفاعل الطارد والماص للحرارة؟ أعطِ مثالاً.",
                        "الطارد يطلق حرارة والماص يمتصها","العكس","لا فرق","كلاهما يطلق",
                        "الطارد يطلق حرارة والماص يمتصها"},
                {"متوازيان: الأول قاعدة ٨ × ارتفاع ٥، الثاني قاعدة ٦ × ارتفاع ٧. أيهما أكبر مساحة؟",
                        "الثاني (٤٢ وحدة²)","الأول (٤٠ وحدة²)","متساويان","لا يمكن التحديد",
                        "الثاني (٤٢ وحدة²)"}
        };

        int aCount  = 0;
        int aqCount = 0;
        int imgSeed = 500;

        for (Week week : weeks) {
            String title = titles[aCount % titles.length];
            Assignment assignment = Assignment.builder()
                    .title(title + " — " + week.getTitle())
                    .description("حل الأسئلة التالية بعناية وأرسل الحل في الموعد المحدد. "
                            + "احرص على كتابة جميع خطوات الحل بوضوح.")
                    .week(week)
                    .questions(new HashSet<>())
                    .deadline(LocalDateTime.now().plusDays(7))
                    .active(true)
                    .deleted(false)
                    .build();
            em.persist(assignment);
            assignments.add(assignment);
            week.getAssignments().add(assignment);
            aCount++;

            for (int qi = 0; qi < 2; qi++) {
                String[] t = aqTemplates[(aCount + qi) % aqTemplates.length];
                AssignmentQuestion aq = AssignmentQuestion.builder()
                        .imageUrl(randomImageUrl("assignment", imgSeed++))
                        .description(t[0])
                        .mark(5)
                        .assignment(assignment)
                        .options(Arrays.asList(t[1], t[2], t[3], t[4]))
                        .correctAnswer(t[5])
                        .deleted(false)
                        .build();
                em.persist(aq);
                assignment.getQuestions().add(aq);
                aqCount++;
            }

            if (aCount % 100 == 0) em.flush();
        }
        em.flush();
        log.info("  ✓ Assignments: {}, AssignmentQuestions: {}", aCount, aqCount);
    }

    // ══════════════════════════════════════════════════════════════
    //  SECTION 9 — QuestionTopics & BankQuestions
    //  Seeded for first 20 weeks (manageable volume)
    // ══════════════════════════════════════════════════════════════
    private void seedQuestionTopicsAndBankQuestions() {
        log.info("── [9/15] Seeding Question Bank ─────────────────");

        String[][] topicData = {
                {"المفاهيم الأساسية",  "الجزء التمهيدي من المنهج"},
                {"التطبيقات المتقدمة", "أسئلة تطبيقية ذات مستوى عالٍ"},
                {"المسائل الحسابية",   "مسائل تعتمد على الحساب والاستنتاج"}
        };
        DifficultyLevel[] diffs = DifficultyLevel.values();

        int topicCount = 0;
        int bqCount    = 0;
        int imgSeed    = 700;
        int orderIdx   = 1;

        List<Week> sampleWeeks = weeks.subList(0, Math.min(20, weeks.size()));

        for (Week week : sampleWeeks) {
            // الجزئيات بقت مرتبطة بالمحاضرة (Session) مش بالدرس (Week)
            // فبناخد أول محاضرة مرتبطة بالدرس ده، ولو مفيش نتخطاه
            com.educore.unit.Session topicSession = (week.getSessions() == null || week.getSessions().isEmpty())
                    ? null
                    : week.getSessions().iterator().next();
            if (topicSession == null) continue;

            for (String[] td : topicData) {
                // Parent topic
                QuestionTopic parent = QuestionTopic.builder()
                        .name(td[0])
                        .description(td[1])
                        .orderNumber(orderIdx++)
                        .session(topicSession)
                        .parentTopic(null)
                        .subTopics(new ArrayList<>())
                        .active(true)
                        .build();
                em.persist(parent);
                topicCount++;

                // Child topic
                QuestionTopic child = QuestionTopic.builder()
                        .name("جزئية — " + td[0])
                        .description("تفاصيل " + td[1])
                        .orderNumber(1)
                        .session(topicSession)
                        .parentTopic(parent)
                        .subTopics(new ArrayList<>())
                        .active(true)
                        .build();
                em.persist(child);
                parent.getSubTopics().add(child);
                topicCount++;

                // 3 BankQuestions per child topic
                for (int bqi = 0; bqi < 3; bqi++) {
                    BankQuestion bq = BankQuestion.builder()
                            .topic(child)
                            .week(week)
                            .conceptTag(td[0].replace(" ", "_").toLowerCase() + "_concept_" + bqi)
                            .imageUrl(randomImageUrl("bankq", imgSeed++))
                            .description("سؤال بنك الأسئلة — " + td[0] + " — نموذج " + (bqi + 1))
                            .mark(2 + bqi)
                            .options(Arrays.asList("الخيار أ", "الخيار ب", "الخيار ج", "الخيار د"))
                            .correctAnswer("الخيار أ")
                            .difficulty(diffs[bqi % diffs.length])
                            .active(true)
                            .build();
                    em.persist(bq);
                    bqCount++;
                }
            }
        }
        em.flush();
        log.info("  ✓ QuestionTopics: {}, BankQuestions: {}", topicCount, bqCount);
    }

    // ══════════════════════════════════════════════════════════════
    //  SECTION 10 — Parents & Students (30 total)
    // ══════════════════════════════════════════════════════════════
    private void seedParentsAndStudents() {
        log.info("── [10/15] Seeding Parents & Students ──────────");

        // 15 parents (each covers 2 students)
        String[][] parentRows = {
                {"01022222201","أحمد علي حسن"},          {"01022222202","محمود إبراهيم سالم"},
                {"01022222203","حسين عبدالله الشريف"},    {"01022222204","عمر عبدالعزيز فاروق"},
                {"01022222205","مصطفى محمد الأمير"},      {"01022222206","خالد سمير الطاهر"},
                {"01022222207","سعيد محمد البشير"},       {"01022222208","وائل أحمد الحلبي"},
                {"01022222209","محمد علي الغامدي"},       {"01022222210","أشرف محمود الجوهري"},
                {"01022222211","طارق حسن المصري"},        {"01022222212","رامي كريم القرشي"},
                {"01022222213","ياسر سمير الحسيني"},      {"01022222214","حاتم يوسف البدوي"},
                {"01022222215","ماهر عادل الزهراني"}
        };

        for (String[] pd : parentRows) {
            Parent p = new Parent();
            p.setPhone(pd[0]);
            p.setName(pd[1]);
            p.setStudents(new HashSet<>());
            em.persist(p);
            parents.add(p);
        }
        em.flush();

        // ── 30 students ────────────────────────────────────────
        // [firstName, secondName, thirdName, fourthName, phone]
        String[][] studentRows = {
                // ── ACTIVE students (20) ──────────────────────────
                {"محمد",    "أحمد",       "علي",       "حسن",         "01011111101"},
                {"أحمد",    "محمود",      "إبراهيم",   "سالم",        "01011111102"},
                {"علي",     "حسين",       "عبدالله",   "الشريف",      "01011111103"},
                {"خالد",    "عمر",        "عبدالعزيز", "فاروق",       "01011111104"},
                {"يوسف",    "مصطفى",      "محمد",      "الأمير",      "01011111105"},
                {"عمر",     "خالد",       "سمير",      "الطاهر",      "01011111106"},
                {"إبراهيم", "سعيد",       "محمد",      "البشير",      "01011111107"},
                {"سامي",    "وائل",       "أحمد",      "الحلبي",      "01011111108"},
                {"حسن",     "محمد",       "علي",       "الغامدي",     "01011111109"},
                {"كريم",    "أشرف",       "محمود",     "الجوهري",     "01011111110"},
                {"نور",     "محمد",       "صالح",      "السيد",       "01111111101"},
                {"سارة",    "أحمد",       "الطيب",     "النجار",      "01111111102"},
                {"مريم",    "عبدالرحمن",  "حسن",       "الشافعي",     "01111111103"},
                {"فاطمة",   "خالد",       "محمود",     "الأنصاري",    "01111111104"},
                {"ليلى",    "عمر",        "إبراهيم",   "التميمي",     "01111111105"},
                {"رنا",     "سمير",       "عبدالله",   "المصري",      "01211111101"},
                {"دينا",    "محمود",      "طارق",      "الزهراني",    "01211111102"},
                {"هناء",    "وائل",       "حسام",      "القرشي",      "01211111103"},
                {"شيماء",   "أشرف",       "ياسر",      "البدوي",      "01211111104"},
                {"أسماء",   "كريم",       "رامي",      "الحسيني",     "01211111105"},
                // ── PENDING students (6) ──────────────────────────
                {"عبدالله", "يوسف",       "سالم",      "المنصوري",    "01311111101"},
                {"عبدالرحمن","طارق",      "حمدي",      "الصعيدي",     "01311111102"},
                {"بشير",    "ماهر",       "أيمن",      "الشوربجي",    "01311111103"},
                {"زياد",    "حاتم",       "نبيل",      "الجندي",      "01311111104"},
                {"ريم",     "صلاح",       "محمد",      "العسقلاني",   "01311111105"},
                {"نادية",   "وليد",       "كمال",      "الطباخ",      "01311111106"},
                // ── REJECTED students (4) ─────────────────────────
                {"لقاء",    "أسامة",      "فاروق",     "الدسوقي",     "01411111101"},
                {"عمار",    "شريف",       "منير",      "العوضي",      "01411111102"},
                {"ضياء",    "جمال",       "بهاء",      "النعيمي",     "01411111103"},
                {"آية",     "رفعت",       "صابر",      "القيسي",      "01411111104"}
        };

        StudentStatus[] statuses = {
                // 20 ACTIVE
                StudentStatus.ACTIVE,StudentStatus.ACTIVE,StudentStatus.ACTIVE,StudentStatus.ACTIVE,
                StudentStatus.ACTIVE,StudentStatus.ACTIVE,StudentStatus.ACTIVE,StudentStatus.ACTIVE,
                StudentStatus.ACTIVE,StudentStatus.ACTIVE,StudentStatus.ACTIVE,StudentStatus.ACTIVE,
                StudentStatus.ACTIVE,StudentStatus.ACTIVE,StudentStatus.ACTIVE,StudentStatus.ACTIVE,
                StudentStatus.ACTIVE,StudentStatus.ACTIVE,StudentStatus.ACTIVE,StudentStatus.ACTIVE,
                // 6 PENDING
                StudentStatus.PENDING,StudentStatus.PENDING,StudentStatus.PENDING,
                StudentStatus.PENDING,StudentStatus.PENDING,StudentStatus.PENDING,
                // 4 REJECTED
                StudentStatus.REJECTED,StudentStatus.REJECTED,
                StudentStatus.REJECTED,StudentStatus.REJECTED
        };

        String[] grades       = {
                "الصف الأول الثانوي","الصف الثاني الثانوي","الصف الثالث الثانوي",
                "الصف الأول الإعدادي","الصف الثاني الإعدادي"
        };
        String[] governorates = {"القاهرة","الجيزة","الإسكندرية","الدقهلية","أسيوط",
                "الغربية","الشرقية","بني سويف"};
        String[] areas        = {"المقطم","الدقي","سموحة","المنصورة","المركز",
                "طنطا","الزقازيق","بني سويف"};
        String[] schools      = {
                "مدرسة القاهرة الثانوية","مدرسة النيل للغات",
                "مدرسة الجمهورية الثانوية","مدرسة المنصورة التجريبية",
                "مدرسة أسيوط الثانوية","مدرسة طنطا التجريبية",
                "مدرسة الزقازيق الثانوية","مدرسة بني سويف للبنات"
        };
        String[] centerNames = centers.stream().map(Center::getName).toArray(String[]::new);

        for (int i = 0; i < studentRows.length; i++) {
            String[] sd      = studentRows[i];
            StudentStatus st = statuses[i];
            Parent parent    = parents.get(i / 2 % parents.size());

            int gi        = i % grades.length;
            int govIdx    = i % governorates.length;
            boolean online = i % 3 != 0;

            Student s = new Student();
            s.setPhone(sd[4]);
            s.setPassword(passwordEncoder.encode("Student@" + String.format("%04d", 1000 + i)));
            s.setFirstName(sd[0]);
            s.setSecondName(sd[1]);
            s.setThirdName(sd[2]);
            s.setFourthName(sd[3]);
            s.setGrade(grades[gi]);
            s.setGovernorate(governorates[govIdx]);
            s.setArea(areas[govIdx]);
            s.setSchoolName(schools[govIdx]);
            s.setEducationDepartment("إدارة " + areas[govIdx] + " التعليمية");
            s.setOnline(online);
            s.setCenterName(online ? null : centerNames[govIdx % centerNames.length]);
            s.setStatus(st);
            s.setEnabled(st == StudentStatus.ACTIVE);
            s.setProfileImageUrl("https://randomuser.me/api/portraits/"
                    + (i < 10 || (i >= 20 && i < 26) ? "men" : "women") + "/" + (i + 1) + ".jpg");
            s.setIdentityDocumentUrl(
                    "https://storage.educore.com/ids/id_" + String.format("%04d", i + 1) + ".jpg");
            s.setStudentCode(generateStudentCode(i));
            s.setParent(parent);
            s.setLatitude(30.0 + (govIdx * 0.5));
            s.setLongitude(31.0 + (govIdx * 0.3));
            s.setMapAddress(areas[govIdx] + "، " + governorates[govIdx]);
            s.setDevicesCount(st == StudentStatus.ACTIVE ? 1 : 0);
            s.setLogoutCount(0);
            s.setLoginCount(st == StudentStatus.ACTIVE ? random.nextInt(50) + 1 : 0);
            s.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(90) + 10));
            s.setUpdatedAt(LocalDateTime.now().minusDays(random.nextInt(5)));
            s.setCreatedBy("SEEDER");

            if (st == StudentStatus.ACTIVE) {
                s.setApprovedAt(s.getCreatedAt().plusDays(1));
                s.setApprovedBy("admin");
                s.setLastLoginAt(LocalDateTime.now().minusHours(random.nextInt(48)));
                s.setLastActivityAt(LocalDateTime.now().minusMinutes(random.nextInt(120)));
            } else if (st == StudentStatus.REJECTED) {
                s.setRejectedAt(s.getCreatedAt().plusDays(2));
                s.setRejectedBy("admin");
                s.setRejectionReason("البيانات المقدمة غير مكتملة أو غير صحيحة");
            }

            em.persist(s);
            students.add(s);
            parent.getStudents().add(s);
        }
        em.flush();
        log.info("  ✓ Parents: {}, Students: {}", parents.size(), students.size());
    }

    // ══════════════════════════════════════════════════════════════
    //  SECTION 11 — Student Cards & Wallets
    // ══════════════════════════════════════════════════════════════
    private void seedStudentCardsAndWallets() {
        log.info("── [11/15] Seeding Student Cards & Wallets ─────");

        int cardCount   = 0;
        int walletCount = 0;

        for (Student student : students) {
            boolean isActive = student.getStatus() == StudentStatus.ACTIVE;
            // الطالب اللي اتقبل قبل كده واتحظر لسه محتفظ برصيده (ممكن يرجع تاني) — عكس اللي لسه معلق أو مرفوض ومعمول قبوله أصلاً
            boolean wasAccepted = isActive || student.getStatus() == StudentStatus.BLOCKED;

            // ── StudentCard ─────────────────────────────────────
            StudentCard card = StudentCard.builder()
                    .student(student)
                    .cardCode("EDU" + String.format("%06d", cardCount + 1))
                    .qrToken(UUID.randomUUID().toString().replace("-", "").substring(0, 32))
                    .active(isActive)
                    .issuedBy("SEEDER")
                    .deactivatedAt(isActive ? null : LocalDateTime.now())
                    .build();
            em.persist(card);
            cardCount++;

            // ── Wallet ──────────────────────────────────────────
            // الطلاب غير المقبولين (PENDING/REJECTED/BLOCKED) لازم محفظتهم تكون فاضية —
            // مينفعش يكون عندهم رصيد قبل ما يتقبلوا فعلياً على المنصة
            BigDecimal balance   = wasAccepted ? BigDecimal.valueOf(random.nextInt(501)) : BigDecimal.ZERO;   // 0–500 لمن سبق قبولهم (نشط/محظور)
            BigDecimal deposited = wasAccepted ? balance.add(BigDecimal.valueOf(random.nextInt(200))) : BigDecimal.ZERO;
            BigDecimal spent     = deposited.subtract(balance);

            Wallet wallet = Wallet.builder()
                    .student(student)
                    .balance(balance)
                    .totalDeposited(deposited)
                    .totalSpent(spent)
                    .totalRefunded(BigDecimal.ZERO)
                    .isActive(isActive)
                    .isVerified(isActive)
                    .build();
            em.persist(wallet);
            walletCount++;
        }
        em.flush();
        log.info("  ✓ StudentCards: {}, Wallets: {}", cardCount, walletCount);
    }

    // ══════════════════════════════════════════════════════════════
    //  SECTION 12 — Enrollments (2-3 courses per active student)
    // ══════════════════════════════════════════════════════════════
    private void seedEnrollments() {
        log.info("── [12/15] Seeding Enrollments ──────────────────");

        List<Student> activeStudents = students.stream()
                .filter(s -> s.getStatus() == StudentStatus.ACTIVE)
                .toList();

        int enrCount = 0;

        for (Student student : activeStudents) {
            // Find level matching the student's grade
            Level matchedLevel = levels.stream()
                    .filter(l -> l.getName().equals(student.getGrade()))
                    .findFirst()
                    .orElse(levels.get(0));

            // Get up to 3 courses from this level's categories
            List<Course> levelCourses = categories.stream()
                    .filter(c -> c.getLevel() != null
                            && c.getLevel().getId().equals(matchedLevel.getId()))
                    .flatMap(c -> c.getCourses().stream())
                    .distinct()
                    .limit(3)
                    .toList();

            Set<Long> enrolled = new HashSet<>();
            for (Course course : levelCourses) {
                if (enrolled.contains(course.getId())) continue;
                enrolled.add(course.getId());

                boolean completed = random.nextBoolean();
                double  progress  = completed ? 100.0 : (20.0 + random.nextInt(80));

                Enrollment enr = Enrollment.builder()
                        .student(student)
                        .course(course)
                        .enrollmentType(EnrollmentType.COURSE_PURCHASE)
                        .status(completed ? EnrollmentStatus.COMPLETED : EnrollmentStatus.ACTIVE)
                        .progress(progress)
                        .completedAt(completed
                                ? LocalDateTime.now().minusDays(random.nextInt(30)) : null)
                        .enrolledAt(LocalDateTime.now().minusDays(random.nextInt(60) + 10))
                        .lastAccessedAt(LocalDateTime.now().minusHours(random.nextInt(72)))
                        .totalWatchTimeSeconds((long) (random.nextInt(7200) + 1800))
                        .completedLessonsCount(completed ? 20 : random.nextInt(20))
                        .totalLessonsCount(20)
                        .quizzesTaken(random.nextInt(5))
                        .quizzesPassed(random.nextInt(4))
                        .averageQuizScore(50.0 + random.nextInt(50))
                        .assignmentsSubmitted(random.nextInt(4))
                        .averageAssignmentScore(60.0 + random.nextInt(40))
                        .accessCount(random.nextInt(100) + 1)
                        .active(true)
                        .createdBy("SEEDER")
                        .build();
                em.persist(enr);
                enrCount++;
            }
        }
        em.flush();
        log.info("  ✓ Enrollments: {}", enrCount);
    }

    // ══════════════════════════════════════════════════════════════
    //  SECTION 13 — Access Codes
    //  10 course codes + 5 category codes
    // ══════════════════════════════════════════════════════════════
    private void seedAccessCodes() {
        log.info("── [13/15] Seeding Access Codes ─────────────────");

        Admin creator  = admins.get(0);
        int codeCount  = 0;

        // ── Per-course codes (first 10 courses) ────────────────
        List<Course> sampleCourses = courses.subList(0, Math.min(10, courses.size()));
        List<Student> activeStudents = students.stream()
                .filter(s -> s.getStatus() == StudentStatus.ACTIVE)
                .limit(3)
                .toList();

        for (int i = 0; i < sampleCourses.size(); i++) {
            Course course = sampleCourses.get(i);
            AccessCode code = AccessCode.builder()
                    .code("CRS" + String.format("%06d", i + 1))
                    .targetType(CodeTargetType.COURSE)
                    .course(course)
                    .category(null)
                    .createdById(creator.getId())
                    .createdByName(creator.getName())
                    .maxUses(30)
                    .usedCount(random.nextInt(10))
                    .expiresAt(LocalDateTime.now().plusMonths(3))
                    .active(true)
                    .batchLabel("دفعة يناير ٢٠٢٥ — " + course.getTitle())
                    .build();
            em.persist(code);
            codeCount++;

            // Record usage for 2 active students
            for (Student student : activeStudents.subList(0, Math.min(2, activeStudents.size()))) {
                AccessCodeUsage usage = AccessCodeUsage.builder()
                        .accessCode(code)
                        .studentId(student.getId())
                        .studentName(student.getFullName())
                        .enrollmentsCreated(1)
                        .build();
                em.persist(usage);
            }
        }

        // ── Per-category codes (first 5 categories) ────────────
        List<Category> sampleCats = categories.subList(0, Math.min(5, categories.size()));
        for (int i = 0; i < sampleCats.size(); i++) {
            Category cat = sampleCats.get(i);
            AccessCode code = AccessCode.builder()
                    .code("CAT" + String.format("%06d", i + 1))
                    .targetType(CodeTargetType.CATEGORY)
                    .course(null)
                    .category(cat)
                    .createdById(creator.getId())
                    .createdByName(creator.getName())
                    .maxUses(50)
                    .usedCount(random.nextInt(15))
                    .expiresAt(LocalDateTime.now().plusMonths(6))
                    .active(true)
                    .batchLabel("باقة كاملة — " + cat.getName())
                    .build();
            em.persist(code);
            codeCount++;
        }

        em.flush();
        log.info("  ✓ AccessCodes: {}", codeCount);
    }

    // ══════════════════════════════════════════════════════════════
    //  SECTION 14 — Orders & Payments
    // ══════════════════════════════════════════════════════════════
    private void seedOrdersAndPayments() {
        log.info("── [14/15] Seeding Orders & Payments ───────────");

        List<Student> activeStudents = students.stream()
                .filter(s -> s.getStatus() == StudentStatus.ACTIVE)
                .limit(10)
                .toList();

        PaymentMethod[] methods = {
                PaymentMethod.CASH, PaymentMethod.WALLET,
                PaymentMethod.FAWRY, PaymentMethod.VODAFONE_CASH
        };

        int orderCount = 0;

        for (int si = 0; si < activeStudents.size(); si++) {
            Student student = activeStudents.get(si);
            Course  course  = courses.get(si % courses.size());
            PaymentMethod method = methods[si % methods.length];
            BigDecimal price = course.getPrice();

            // ── Order ──────────────────────────────────────────
            com.educore.payment.order.Order order = com.educore.payment.order.Order.builder()
                    .orderNumber("ORD" + String.format("%08d", si + 1))
                    .student(student)
                    .status(OrderStatus.PAID)
                    .subtotal(price)
                    .discount(BigDecimal.ZERO)
                    .tax(BigDecimal.ZERO)
                    .total(price)
                    .paymentMethod(method)
                    .notes("طلب شراء كورس: " + course.getTitle())
                    .items(new ArrayList<>())
                    .build();
            em.persist(order);

            // ── OrderItem ──────────────────────────────────────
            // NOTE: adjust field names below to match your actual OrderItem fields.
            // The entity above has productType / productName instead of itemType / refId / refName.
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .course(course)
                    .category(null)
                    .productType("COURSE")
                    .productName(course.getTitle())
                    .unitPrice(price)
                    .quantity(1)
                    .subtotal(price)
                    .build();
            em.persist(item);
            order.getItems().add(item);
            order.markAsPaid(method);

            // ── Payment ────────────────────────────────────────
            String txnId = "TXN" + UUID.randomUUID().toString().replace("-","")
                    .substring(0,12).toUpperCase();
            String gwResp = "{\"status\":\"success\",\"code\":\"00\",\"method\":\""
                    + method.name() + "\"}";

            Payment payment = Payment.builder()
                    .order(order)
                    .transactionId(txnId)
                    .paymentMethod(method)
                    .status(PaymentStatus.COMPLETED)
                    .amount(price)
                    .currency("EGP")
                    .gatewayResponse(gwResp)
                    .approvedBy(method == PaymentMethod.CASH ? admins.get(0).getName() : null)
                    .build();
            payment.complete(txnId, gwResp);
            em.persist(payment);
            order.setPayment(payment);

            orderCount++;
        }
        em.flush();
        log.info("  ✓ Orders & Payments: {}", orderCount);
    }

    // ══════════════════════════════════════════════════════════════
    //  SECTION 15 — StudentQuizAttempts
    // ══════════════════════════════════════════════════════════════
    private void seedStudentQuizAttempts() {
        log.info("── [15/15] Seeding Student Quiz Attempts ────────");

        List<Student> activeStudents = students.stream()
                .filter(s -> s.getStatus() == StudentStatus.ACTIVE)
                .limit(15)
                .toList();

        List<Quiz> sampleQuizzes = quizzes.subList(0, Math.min(6, quizzes.size()));
        Set<String> attempted    = new HashSet<>(); // enforce unique(student, quiz)
        int attemptCount         = 0;

        for (Student student : activeStudents) {
            for (Quiz quiz : sampleQuizzes) {
                String key = student.getId() + "_" + quiz.getId();
                if (attempted.contains(key)) continue;
                attempted.add(key);

                LocalDateTime started   = LocalDateTime.now().minusDays(random.nextInt(30) + 1);
                LocalDateTime expiresAt = started.plusMinutes(quiz.getDurationMinutes());

                StudentQuizAttempt attempt = StudentQuizAttempt.builder()
                        .student(student)
                        .quiz(quiz)
                        .score(random.nextInt(11))       // 0–10
                        .submitted(true)
                        .startedAt(started)
                        .expiresAt(expiresAt)
                        .build();
                em.persist(attempt);
                attemptCount++;
            }
        }
        em.flush();
        log.info("  ✓ StudentQuizAttempts: {}", attemptCount);
    }

    // ══════════════════════════════════════════════════════════════
    //  SUMMARY
    // ══════════════════════════════════════════════════════════════
    private void printSummary() {
        log.info("╔══════════════════════════════════════════════╗");
        log.info("║         EduCore Seed Summary                 ║");
        log.info("╠══════════════════════════════════════════════╣");
        log.info("║  Admins:        {:>5}                        ║", admins.size());
        log.info("║  Centers:       {:>5}                        ║", centers.size());
        log.info("║  Levels:        {:>5}                        ║", levels.size());
        log.info("║  Categories:    {:>5}                        ║", categories.size());
        log.info("║  Courses:       {:>5}                        ║", courses.size());
        log.info("║  Sessions:      {:>5}                        ║", sessions.size());
        log.info("║  Weeks:         {:>5}                        ║", weeks.size());
        log.info("║  Materials:     {:>5}  (video+pdf per week)  ║", weeks.size() * 2);
        log.info("║  Quizzes:       {:>5}  (1 per week)          ║", quizzes.size());
        log.info("║  Assignments:   {:>5}  (1 per week)          ║", assignments.size());
        log.info("║  Parents:       {:>5}                        ║", parents.size());
        log.info("║  Students:      {:>5}                        ║", students.size());
        log.info("╚══════════════════════════════════════════════╝");
    }
}