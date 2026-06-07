// ══════════════════════════════════════════════════════════════════════
//  EduCore — Frontend Models
//  مطابق بالكامل لـ Backend DTOs & Entities
// ══════════════════════════════════════════════════════════════════════

// ── Enums (مطابقة للـ Backend تماماً) ────────────────────────────────

export type StudentStatus     = 'PENDING' | 'ACTIVE' | 'BLOCKED' | 'REJECTED';
export type MaterialType      = 'PDF' | 'VIDEO' | 'YOUTUBE' | 'IMAGE' | 'DOC' | 'PPT' | 'AUDIO' | 'ARCHIVE' | 'OTHER';
export type EnrollmentStatus  = 'ACTIVE' | 'COMPLETED' | 'EXPIRED' | 'CANCELLED' | 'SUSPENDED';
export type CouponType        = 'PERCENTAGE' | 'FIXED_AMOUNT';
export type WeekLockType      = 'NEVER' | 'AFTER_DURATION' | 'ON_DATE';
export type TransactionType   = 'TOP_UP' | 'DEDUCTION' | 'REFUND' | 'PURCHASE' | 'TRANSFER';
export type TransactionStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'REVERSED';
export type DifficultyLevel   = 'EASY' | 'MEDIUM' | 'HARD';
export type NotificationType  = 'PAYMENT' | 'ENROLLMENT' | 'QUIZ' | 'SYSTEM' | 'INFO';
/** Backend enum: com.educore.attendance.group.AttendanceStatus */
export type AttendanceStatus  = 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED';
/** Backend enum: com.educore.attendance.group.ScanMethod */
export type ScanMethod        = 'QR_SCAN' | 'MANUAL_ID' | 'AUTO_ABSENT';
/** Backend enum: com.educore.attendance.group.GroupAlertType */
export type GroupAlertType    = 'WRONG_CENTER' | 'ONLINE_TO_CENTER';

// ── Auth ──────────────────────────────────────────────────────────────

/** POST /api/auth/teacher/login */
export interface LoginRequest {
  phone: string;
  password: string;
}

/** POST /api/auth/teacher/register */
export interface RegisterRequest {
  phone: string;
  password: string;
  name: string;       // NOT fullName
  subject?: string;
  email?: string;
}

/** TeacherAuthResponse من الباك */
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  name: string;
  phone: string;
  role: string;       // always "TEACHER"
  message: string;
}

// ── Teacher Profile — TeacherProfileResponse ──────────────────────────

export interface TeacherProfile {
  id: number;
  name: string;               // NOT fullName
  phone: string;
  email?: string;
  subject?: string;
  bio?: string;
  quote?: string;
  profileImageUrl?: string;   // NOT avatarUrl
  homeCardImageUrl?: string;  // full-design card image for student home
  logoUrl?: string;
  darkLogoUrl?: string;
  teacherCardUrl?: string;
  teacherCardDarkUrl?: string;
  facebookUrl?: string;
  youtubeUrl?: string;
  instagramUrl?: string;
  tiktokUrl?: string;
  whatsappNumber?: string;
  telegramUrl?: string;
  enabled?: boolean;
  createdAt?: string;
}

// ── Levels & Categories ───────────────────────────────────────────────

export interface Level {
  id: number;
  name: string;
}

export interface Category {
  id: number;
  name: string;
  description?: string;
  price?: number;
  levelId?: number;
  levelName?: string;
  active?: boolean;
  status?: 'ACTIVE' | 'INACTIVE';
  createdAt?: string;
  updatedAt?: string;
  coursesCount?: number;         // computed in frontend from courses list
  orderNumber?: number;
}

// ── Course ────────────────────────────────────────────────────────────

export interface Course {
  id: number;
  title: string;
  description?: string;
  imageUrl?: string;
  price: number;
  discountedPrice?: number;
  discountPercentage?: number;
  accessDays?: number;
  accessExpiresAt?: string;
  active?: boolean;
  status?: 'ACTIVE' | 'INACTIVE';
  sessionsCount?: number;
  enrolledCount?: number;
  enrolledStudentsCount?: number;
  orderNumber?: number;
  categoryIds?: number[];
  createdAt?: string;
  name?: string; // legacy compat
  // ── حقول جديدة ──
  courseType?: string;           // RECORDED | LIVE | PACKAGE | HYBRID
  teachingType?: string;         // ONLINE | CENTER
  studentPoints?: number;
  contentOrder?: string;         // NONE | LOCK_BY_SESSION | LOCK_BY_ELEMENT | LOCK_BY_ELEMENT_IN_SESSION
  trackAttendance?: boolean;
  featured?: boolean;
  pinned?: boolean;
}
export type Lesson = Course;

// ── Session ───────────────────────────────────────────────────────────

export interface Session {
  id: number;
  title: string;
  description?: string;
  teachingType?: 'ONLINE' | 'CENTER' | 'BOTH' | string;
  courseId?: number;
  courseIds?: number[];
  active?: boolean;
  status?: 'ACTIVE' | 'INACTIVE';
  orderNumber?: number;
  weeksCount?: number;
}

// ── Week ──────────────────────────────────────────────────────────────

export interface Week {
  id: number;
  title: string;
  description?: string;
  sessionId?: number;
  orderNumber?: number;
  active?: boolean;
  status?: 'ACTIVE' | 'INACTIVE';
  /** مطابق للـ Backend enum: NEVER | AFTER_DURATION | ON_DATE */
  lockType?: WeekLockType;
  lockAfterDays?: number;           // موجود في الباك
  lockDate?: string;                // موجود في الباك
  globallyLocked?: boolean;         // موجود في الباك
  requiresSequentialAccess?: boolean; // موجود في الباك
  hasQuiz?: boolean;                // موجود في الباك
}

// ── Material (LessonMaterial) ─────────────────────────────────────────

export interface Material {
  id: number;
  /** Backend enum: PDF|VIDEO|YOUTUBE|IMAGE|DOC|PPT|AUDIO|ARCHIVE|OTHER */
  materialType: MaterialType;   // NOT type
  fileName?: string;            // NOT title
  fileUrl?: string;             // NOT url
  youtubeVideoId?: string;      // موجود في الباك
  fileSize?: number;            // bytes — موجود في الباك
  durationSeconds?: number;     // موجود في الباك
  downloadCount?: number;       // موجود في الباك
  preview?: boolean;            // موجود في الباك
  orderNumber?: number;
  active?: boolean;
  status?: 'ACTIVE' | 'INACTIVE';
  weekId?: number;
  createdAt?: string;
  studentPoints?: number;
  maxViewCount?: number;
  cooldownMinutes?: number;
}

// ── Quiz ──────────────────────────────────────────────────────────────

export interface Quiz {
  id: number;
  title: string;
  weekId?: number;
  durationMinutes?: number;
  timeRestricted?: boolean;
  orderNumber?: number;
  active?: boolean;
  deleted?: boolean;
  attemptsCount?: number;
  passingScore?: number;
  attemptsAllowed?: number;
  questionsCount?: number;
  totalMarks?: number;
  quizType?: string;
  questionOrder?: string;
  points?: number;
  prizeName?: string;
  prizeScore?: number;
  improvable?: boolean;
  startDate?: string;
  endDate?: string;
  createdAt?: string;
}

export interface QuizQuestion {
  id: number;
  quizId?: number;
  description: string;
  options: string[];
  correctAnswer: string;
  imageUrl?: string;
  mark?: number;
  deleted?: boolean;
  explanation?: string;
  explanationUrl?: string;
}

export interface QuizAttempt {
  id: number;
  studentId: number;
  studentName: string;
  score: number;
  submitted?: boolean;
  startedAt?: string;
  expiresAt?: string;
  passed?: boolean;
  timeTakenMinutes?: number;
  createdAt?: string;
}

export interface QuizStatistics {
  totalAttempts: number;
  passedCount: number;
  failedCount: number;
  averageScore: number;
}

// -- Assignment -----------------------------------------------------------

export interface AssignmentQuestion {
  id: number;
  description: string;
  options?: string[];
  correctAnswer?: string;
  mark?: number;
  imageUrl?: string;
}

export interface Assignment {
  id: number;
  title: string;
  description?: string;
  deadline?: string;
  weekId?: number;
  maxScore?: number;
  submissionsCount?: number;
  questions?: AssignmentQuestion[];
  createdAt?: string;
}

export interface AssignmentAttempt {
  id: number;
  studentId: number;
  studentName?: string;
  assignmentId?: number;
  submittedAt?: string;
  score?: number;
  answers?: any[];
}

// -- Question Bank --------------------------------------------------------

export interface Topic {
  id: number;
  name: string;
  description?: string;
  courseId?: number;
}

export interface QuestionBankItem {
  id: number;
  description: string;
  options: string[];
  correctAnswer: string;
  mark?: number;
  difficulty?: DifficultyLevel;
  conceptTag?: string;
  topicId?: number;
  topicName?: string;
  createdAt?: string;
}

// -- Student & Staff ------------------------------------------------------

export interface Student {
  id: number;
  fullName: string;
  firstName?: string;
  secondName?: string;
  thirdName?: string;
  fourthName?: string;
  phone: string;
  parentPhone?: string;
  code?: string;
  studentCode?: string;
  status?: StudentStatus;
  profileImageUrl?: string;
  identityDocumentUrl?: string;
  walletBalance?: number;
  attendanceRate?: number;
  enrolledCoursesCount?: number;
  createdAt?: string;
  governorateName?: string;
  governorate?: string;
  area?: string;
  schoolName?: string;
  educationDepartment?: string;
  centerName?: string;
  educationalAdministrationName?: string;
  studyType?: string;
  grade?: string;
  hasCard?: boolean;
  online?: boolean;
  attendanceCount?: number;
  rejectionReason?: string;
  groupId?: number;
  groupName?: string;
}

export interface Staff {
  id: number;
  fullName: string;
  phone: string;
  role?: string;
  email?: string;
  enabled?: boolean;
  status?: string;
  notes?: string;
  permissions: string[];
  createdAt?: string;
}

// -- Wallet ---------------------------------------------------------------

export interface WalletTransaction {
  id: number;
  studentId?: number;
  studentName?: string;
  studentCode?: string;
  type: TransactionType;
  status?: TransactionStatus;
  amount: number;
  balanceBefore?: number;
  balanceAfter?: number;
  description?: string;
  referenceId?: string;
  expiresAt?: string;
  createdAt?: string;
  wallet?: { student?: { id?: number; fullName?: string } };
}

// -- Coupons --------------------------------------------------------------

export interface Coupon {
  id: number;
  code: string;
  type: CouponType;
  value: number;
  maxUses?: number;
  usedCount?: number;
  expiresAt?: string;
  status?: 'ACTIVE' | 'INACTIVE';
  createdAt?: string;
}

// -- Access Codes ---------------------------------------------------------

export interface AccessCode {
  id: number;
  code: string;
  courseId?: number;
  courseName?: string;
  isUsed?: boolean;
  usedByStudentName?: string;
  usedAt?: string;
  batchLabel?: string;
  createdAt?: string;
}

export interface AccessCodeBatch {
  id: number;
  batchLabel?: string;
  count?: number;
  courseId?: number;
  courseName?: string;
  createdAt?: string;
}

// -- Enrollments ----------------------------------------------------------

export interface Enrollment {
  id: number;
  studentId?: number;
  studentName?: string;
  studentPhone?: string;
  courseId?: number;
  courseTitle?: string;
  courseName?: string;
  status?: EnrollmentStatus;
  enrolledAt?: string;
  expiresAt?: string;
  expiryDate?: string;
  paymentMethod?: string;
  progress?: number;
  completedLessonsCount?: number;
  totalLessonsCount?: number;
}

// -- Attendance Records ---------------------------------------------------

export interface AttendanceRecord {
  id: number;
  studentId?: number;
  studentName?: string;
  studentCode?: string;
  sessionId?: number;
  sessionTitle?: string;
  weekId?: number;
  weekTitle?: string;
  courseName?: string;
  status?: AttendanceStatus;
  /** نوع الحضور كما يرجعه الباك إند: ONLINE | CENTER */
  type?: 'ONLINE' | 'CENTER' | AttendanceStatus;
  /** مصدر التسجيل: QR_SCAN | ONLINE_ACCESS | MANUAL */
  source?: 'QR_SCAN' | 'ONLINE_ACCESS' | 'MANUAL';
  attendedAt?: string;
  date?: string;
  scanTime?: string;
  notes?: string;
}

// -- Student Cards --------------------------------------------------------

export interface StudentCard {
  id: number;
  studentId?: number;
  studentName?: string;
  studentCode?: string;
  profileImageUrl?: string;
  level?: string;
  center?: string;
}

// -- Banners & Announcements ----------------------------------------------

export interface Banner {
  id: number;
  title?: string;
  imageUrl?: string | null;
  targetUrl?: string;
  linkUrl?: string;
  active?: boolean;
  orderNumber?: number;
  displayOrder?: number;
  startDate?: string;
  endDate?: string;
  createdAt?: string;
}

export interface Announcement {
  id: number;
  title: string;
  body?: string;
  targetRole?: string;
  createdAt?: string;
}

// -- Centers & Geo --------------------------------------------------------

export interface Center {
  id: number;
  name: string;
  address?: string;
  phone?: string;
  governorateId?: number;
  governorateName?: string;
  governorate?: string;
  area?: string;
  mapsLink?: string;
  sellsBooks?: boolean;
  sellsCodes?: boolean;
  enabled?: boolean;
  active?: boolean;
  latitude?: number;
  longitude?: number;
  studentsCount?: number;
}

export interface Governorate {
  id: number;
  name: string;
}

export interface Area {
  id: number;
  name: string;
  governorateId?: number;
}

// -- Notifications --------------------------------------------------------

export interface AppNotification {
  id: number;
  title: string;
  body?: string;
  type?: NotificationType;
  read?: boolean;
  createdAt?: string;
  targetUserId?: number;
}

// -- Pagination -----------------------------------------------------------

export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
}

// -- Dashboard ------------------------------------------------------------

export interface DashboardAnalytics {
  totalStudents?: number;
  activeStudents?: number;
  totalCourses?: number;
  totalRevenue?: number;
  recentEnrollments?: number;
  monthlyRevenue?: number;
  pendingStudents?: number;
  quizPassRate?: number;
  newStudentsThisWeek?: number;
  quizAttempts?: number;
}

// -- Attendance Groups ----------------------------------------------------

export interface AttendanceGroup {
  id: number;
  name: string;
  courseId?: number;
  courseName?: string;
  sessionId?: number;
  sessionTitle?: string;
  membersCount?: number;
  active?: boolean;
  createdAt?: string;
}

export interface AttendanceSession {
  id: number;
  groupId?: number;
  groupName?: string;
  weekId?: number;
  weekTitle?: string;
  status?: string;
  startedAt?: string;
  endedAt?: string;
  totalPresent?: number;
  totalAbsent?: number;
}

export interface AttendanceGroupMember {
  id: number;
  groupId?: number;
  studentId: number;
  studentName?: string;
  studentCode?: string;
  joinedAt?: string;
}

export interface AttendanceGroupRecord {
  id: number;
  attendanceSessionId?: number;
  studentId: number;
  studentName?: string;
  studentCode?: string;
  status?: AttendanceStatus;
  scanMethod?: ScanMethod;
  alertType?: GroupAlertType;
  scannedAt?: string;
  notes?: string;
}

export interface MarkAttendanceResult {
  studentId: number;
  studentName?: string;
  status: AttendanceStatus;
  alertType?: GroupAlertType;
  message?: string;
}

export interface StudentGroupBrief {
  groupId: number;
  groupName?: string;
  joinedAt?: string;
}

// -- Request Payloads -----------------------------------------------------

export interface CreateGroupRequest {
  name: string;
  courseId?: number;
  sessionId?: number;
  dayOfWeek?: string;
  meetingTime?: string;
  levelId?: number;
  maxCapacity?: number;
  centerId?: number;
}

export interface CreateSessionRequest {
  groupId: number;
  weekId: number;
}

export interface MarkAttendanceRequest {
  sessionId: number;
  records: { studentId: number; status: AttendanceStatus }[];
}

export interface AddStudentToGroupRequest {
  studentId: number;
  groupId: number;
}
