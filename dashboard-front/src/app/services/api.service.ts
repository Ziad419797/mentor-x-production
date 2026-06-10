import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { catchError, map, Observable, of } from 'rxjs';
import { extractList, extractPage } from '../core/api-response.model';
import { environment } from '../../environments/environment';
import {
  LoginRequest, RegisterRequest, LoginResponse, TeacherProfile,
  Level, Category, Course, Session, Week, Material,
  Quiz, QuizQuestion, QuizAttempt, QuizStatistics,
  Assignment, AssignmentQuestion, AssignmentAttempt,
  Topic, QuestionBankItem, Student, Staff,
  WalletTransaction, Coupon, AccessCodeBatch, AccessCode,
  Enrollment, AttendanceRecord, StudentCard,
  Banner, Announcement, Center, Governorate, Area,
  AppNotification, Page, DashboardAnalytics,
  AttendanceGroup, AttendanceSession, AttendanceGroupMember,
  AttendanceGroupRecord, MarkAttendanceResult, StudentGroupBrief,
  CreateGroupRequest, CreateSessionRequest,
  MarkAttendanceRequest, AddStudentToGroupRequest,
  AttendanceStatus
} from '../models/models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly base = environment.apiBase;

  constructor(private http: HttpClient) { }

  // -- Auth
  login(body: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.base}/api/auth/teacher/login`, body);
  }
  staffLogin(body: LoginRequest): Observable<any> {
    return this.http.post<any>(`${this.base}/api/auth/staff/login`, body);
  }
  getStaffMe(): Observable<any> {
    return this.http.get<any>(`${this.base}/api/auth/staff/me`).pipe(map(r => this.unwrap(r)));
  }
  register(body: RegisterRequest): Observable<any> {
    return this.http.post(`${this.base}/api/auth/teacher/register`, body);
  }
  forgotPassword(phone: string): Observable<any> {
    return this.http.post(`${this.base}/api/auth/teacher/forgot-password`, { phone }).pipe(
      catchError(() => this.http.post(`${this.base}/api/auth/staff/forgot-password`, { phone }))
    );
  }
  verifyOtp(phone: string, otp: string): Observable<any> {
    return this.http.post(`${this.base}/api/auth/teacher/verify-otp`, { phone, otp }).pipe(
      catchError(() => this.http.post(`${this.base}/api/auth/staff/verify-otp`, { phone, otp }))
    );
  }
  resendOtp(phone: string): Observable<any> {
    return this.http.post(`${this.base}/api/auth/teacher/resend-otp`, { phone }).pipe(
      catchError(() => this.http.post(`${this.base}/api/auth/staff/forgot-password`, { phone }))
    );
  }
  resetPassword(phone: string, newPassword: string): Observable<any> {
    return this.http.post(`${this.base}/api/auth/teacher/reset-password`, { phone, newPassword }).pipe(
      catchError(() => this.http.post(`${this.base}/api/auth/staff/reset-password`, { phone, newPassword }))
    );
  }

  // -- Profile
  private unwrap = (r: any): any => r?.data ?? r;

  getProfile(): Observable<TeacherProfile> {
    return this.http.get<any>(`${this.base}/api/teacher/profile`).pipe(map(this.unwrap));
  }
  getMe(): Observable<any> {
    return this.getProfile();
  }
  updateProfile(data: Partial<TeacherProfile>): Observable<TeacherProfile> {
    return this.http.put<any>(`${this.base}/api/teacher/profile`, data).pipe(map(this.unwrap));
  }
  updatePassword(newPassword: string): Observable<any> {
    return this.http.put(`${this.base}/api/teacher/profile/password`, { newPassword });
  }
  uploadProfileImage(file: File): Observable<TeacherProfile> {
    const fd = new FormData();
    fd.append('image', file);
    return this.http.post<any>(`${this.base}/api/teacher/profile/image`, fd).pipe(map(this.unwrap));
  }
  uploadHomeCardImage(file: File): Observable<TeacherProfile> {
    const fd = new FormData();
    fd.append('image', file);
    return this.http.post<any>(`${this.base}/api/teacher/profile/home-card-image`, fd).pipe(map(this.unwrap));
  }
  uploadLogo(file: File): Observable<TeacherProfile> {
    const fd = new FormData();
    fd.append('image', file);
    return this.http.post<any>(`${this.base}/api/teacher/profile/logo`, fd).pipe(map(this.unwrap));
  }
  uploadDarkLogo(file: File): Observable<TeacherProfile> {
    const fd = new FormData();
    fd.append('image', file);
    return this.http.post<any>(`${this.base}/api/teacher/profile/dark-logo`, fd).pipe(map(this.unwrap));
  }
  deleteDarkLogo(): Observable<TeacherProfile> {
    return this.http.put<any>(`${this.base}/api/teacher/profile`, { darkLogoUrl: '' }).pipe(map(this.unwrap));
  }

  uploadTeacherCard(file: File): Observable<TeacherProfile> {
    const fd = new FormData(); fd.append('image', file);
    return this.http.post<any>(`${this.base}/api/teacher/profile/teacher-card`, fd).pipe(map(this.unwrap));
  }

  uploadTeacherCardDark(file: File): Observable<TeacherProfile> {
    const fd = new FormData(); fd.append('image', file);
    return this.http.post<any>(`${this.base}/api/teacher/profile/teacher-card-dark`, fd).pipe(map(this.unwrap));
  }

  deleteTeacherCard(): Observable<TeacherProfile> {
    return this.http.put<any>(`${this.base}/api/teacher/profile`, { teacherCardUrl: '' }).pipe(map(this.unwrap));
  }

  deleteTeacherCardDark(): Observable<TeacherProfile> {
    return this.http.put<any>(`${this.base}/api/teacher/profile`, { teacherCardDarkUrl: '' }).pipe(map(this.unwrap));
  }

  // -- Support Channels
  getSupportChannels(): Observable<any> {
    return this.http.get<any>(`${this.base}/api/teacher/support-channels`).pipe(map(this.unwrap));
  }
  createSupportChannel(data: any): Observable<any> {
    return this.http.post<any>(`${this.base}/api/teacher/support-channels`, data).pipe(map(this.unwrap));
  }
  updateSupportChannel(id: number, data: any): Observable<any> {
    return this.http.put<any>(`${this.base}/api/teacher/support-channels/${id}`, data).pipe(map(this.unwrap));
  }
  deleteSupportChannel(id: number): Observable<any> {
    return this.http.delete<any>(`${this.base}/api/teacher/support-channels/${id}`).pipe(map(this.unwrap));
  }

    // -- Analytics
  getDashboardAnalytics(): Observable<DashboardAnalytics> {
    return this.http.get<DashboardAnalytics>(`${this.base}/api/analytics/teacher/dashboard`);
  }
  getTopicWeakness(): Observable<any[]> {
    return this.http.get<any>(`${this.base}/api/analytics/teacher/topic-weakness`)
      .pipe(map((r: any) => r?.data ?? r ?? []));
  }
  // Topic Tree CRUD
  getTopicTree(sessionId: number): Observable<any[]> {
    return this.http.get<any>(`${this.base}/api/question-bank/topics/session/${sessionId}`)
      .pipe(map((r: any) => r?.data ?? r ?? []));
  }
  createTopic(data: any): Observable<any> {
    return this.http.post<any>(`${this.base}/api/question-bank/topics`, data)
      .pipe(map((r: any) => r?.data ?? r));
  }
  updateTopic(id: number, data: any): Observable<any> {
    return this.http.put<any>(`${this.base}/api/question-bank/topics/${id}`, data)
      .pipe(map((r: any) => r?.data ?? r));
  }
  deleteTopic(id: number): Observable<any> {
    return this.http.delete<any>(`${this.base}/api/question-bank/topics/${id}`);
  }

  // -- Levels & Categories
  getLevelById(levelId: number): Observable<any> {
    return this.http.get<any>(`${this.base}/api/levels/${levelId}`);
  }
    getLevels(): Observable<Level[]> {
    return this.http.get<Level[]>(`${this.base}/api/levels`);
  }
  createLevel(name: string): Observable<Level> {
    return this.http.post<Level>(`${this.base}/api/levels`, { name });
  }
  updateLevel(id: number, name: string): Observable<Level> {
    return this.http.put<Level>(`${this.base}/api/levels/${id}`, { name });
  }
  deleteLevel(id: number): Observable<any> {
    return this.http.delete(`${this.base}/api/levels/${id}`);
  }

  getCategories(): Observable<Category[]> {
    return this.http.get<any>(`${this.base}/api/categories?size=200&sort=id,asc`)
      .pipe(map(r => this.L<Category>(r)), catchError(() => of([])));
  }
  getCategoriesByLevel(levelId: number, page = 0, size = 100): Observable<Page<Category>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<any>(`${this.base}/api/categories/level/${levelId}`, { params })
      .pipe(map(r => this.P<Category>(r)));
  }
  getCategoriesByLevelAdmin(levelId: number, page = 0, size = 200): Observable<Page<Category>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<any>(`${this.base}/api/categories/level/${levelId}/admin`, { params })
      .pipe(map(r => this.P<Category>(r)));
  }
  createCategory(data: { name: string; description?: string; levelId: number }): Observable<Category> {
    return this.http.post<Category>(`${this.base}/api/categories`, data);
  }
  updateCategory(id: number, data: Partial<Category>): Observable<Category> {
    return this.http.put<Category>(`${this.base}/api/categories/${id}`, data);
  }
  toggleCategoryStatus(id: number): Observable<any> {
    return this.http.patch(`${this.base}/api/categories/${id}/toggle-status`, {});
  }
  deleteCategory(id: number): Observable<any> {
    return this.http.delete(`${this.base}/api/categories/${id}`);
  }

  // -- Courses
  getCourses(page = 0, size = 20, categoryId?: number | null): Observable<Page<Course>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (categoryId) params = params.set('categoryId', categoryId);
    return this.http.get<any>(`${this.base}/api/courses`, { params })
      .pipe(map(r => this.P<Course>(r)));
  }
  createCourse(data: FormData): Observable<Course> {
    return this.http.post<Course>(`${this.base}/api/courses`, data);
  }
  updateCourse(id: number, data: FormData): Observable<Course> {
    return this.http.put<Course>(`${this.base}/api/courses/${id}/with-image`, data);
  }
  getCourseLevelId(courseId: number): Observable<any> {
    return this.http.get<any>(`${this.base}/api/courses/${courseId}/level-id`).pipe(
      catchError(() => of({ levelId: null }))
    );
  }

  getCoursesByCategory(categoryId: number, page = 0, size = 50): Observable<Page<Course>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<any>(`${this.base}/api/courses/category/${categoryId}`, { params })
      .pipe(map(r => this.P<Course>(r)));
  }
  changeCourseCategory(courseId: number, newCategoryId: number): Observable<any> {
    return this.http.patch(`${this.base}/api/courses/${courseId}/change-category`, { newCategoryId });
  }
  toggleCourseStatus(id: number): Observable<any> {
    return this.http.patch(`${this.base}/api/courses/${id}/toggle-status`, {});
  }
  deleteCourse(id: number): Observable<any> {
    return this.http.delete(`${this.base}/api/courses/${id}`);
  }

  // -- Response normalizer
  private L = extractList;
  private P = extractPage;

  // -- Sessions & Weeks
  getSessionsByCourse(courseId: number): Observable<Session[]> {
    return this.http.get<any>(`${this.base}/api/sessions/course/${courseId}`)
      .pipe(map(r => this.L<Session>(r)), catchError(() => of([])));
  }
  getAllSessions(page = 0, size = 200): Observable<Session[]> {
    return this.http.get<any>(`${this.base}/api/sessions?page=${page}&size=${size}`)
      .pipe(map(r => this.L<Session>(r)), catchError(() => of([])));
  }
  createSession(data: { title: string; description?: string; teachingType?: string | null; orderNumber?: number; courseIds: number[] }): Observable<Session> {
    return this.http.post<Session>(`${this.base}/api/sessions`, data);
  }
  updateSession(id: number, data: Partial<Session>): Observable<Session> {
    return this.http.put<Session>(`${this.base}/api/sessions/${id}`, data);
  }
  toggleSessionStatus(id: number): Observable<any> {
    return this.http.patch(`${this.base}/api/sessions/${id}/toggle-status`, {});
  }
  deleteSession(id: number): Observable<any> {
    return this.http.delete(`${this.base}/api/sessions/${id}`);
  }
  linkSessionToCourse(sessionId: number, courseId: number): Observable<Session> {
    return this.http.post<Session>(`${this.base}/api/sessions/${sessionId}/link-course/${courseId}`, {});
  }

  getWeeksBySession(sessionId: number): Observable<Week[]> {
    return this.http.get<any>(`${this.base}/api/weeks/session/${sessionId}`)
      .pipe(map(r => this.L<Week>(r)), catchError(() => of([])));
  }
  createWeek(data: Partial<Week>): Observable<Week> {
    return this.http.post<Week>(`${this.base}/api/weeks`, data);
  }
  updateWeek(id: number, data: Partial<Week>): Observable<Week> {
    return this.http.put<Week>(`${this.base}/api/weeks/${id}`, data);
  }
  toggleWeekStatus(id: number): Observable<any> {
    return this.http.patch(`${this.base}/api/weeks/${id}/toggle-status`, {});
  }
  deleteWeek(id: number): Observable<any> {
    return this.http.delete(`${this.base}/api/weeks/${id}`);
  }

  // -- Materials
  getMaterialsByWeek(weekId: number): Observable<Material[]> {
    return this.http.get<any>(`${this.base}/api/materials/lesson/${weekId}`)
      .pipe(map(r => this.L<Material>(r)), catchError(() => of([])));
  }
  getMaterialById(id: number): Observable<Material> {
    return this.http.get<Material>(`${this.base}/api/materials/${id}`);
  }
  createMaterial(data: Partial<Material>): Observable<Material> {
    return this.http.post<Material>(`${this.base}/api/materials`, data);
  }
  updateMaterial(id: number, data: Partial<Material>): Observable<Material> {
    return this.http.put<Material>(`${this.base}/api/materials/${id}`, data);
  }
  toggleMaterialStatus(id: number): Observable<any> {
    return this.http.patch(`${this.base}/api/materials/${id}/toggle-status`, {});
  }
  deleteMaterial(id: number): Observable<any> {
    return this.http.delete(`${this.base}/api/materials/${id}`);
  }
  reorderMaterials(items: {id: number; orderNumber: number}[]): Observable<any> {
    return this.http.put(`${this.base}/api/materials/reorder`, { items });
  }

  // -- Quizzes
  getQuizzesByWeek(weekId: number): Observable<Quiz[]> {
    return this.http.get<any>(`${this.base}/api/quizzes/week/${weekId}`)
      .pipe(map(r => this.L<Quiz>(r)), catchError(() => of([])));
  }
  createQuiz(data: Partial<Quiz>): Observable<Quiz> {
    return this.http.post<Quiz>(`${this.base}/api/quizzes`, data);
  }
  deleteQuiz(id: number): Observable<any> {
    return this.http.delete(`${this.base}/api/quizzes/${id}`);
  }
  getQuizQuestions(quizId: number): Observable<QuizQuestion[]> {
    return this.http.get<any>(`${this.base}/api/questions/quiz/${quizId}`)
      .pipe(map(r => this.L<QuizQuestion>(r)), catchError(() => of([])));
  }
  addQuizQuestion(quizId: number, data: Partial<QuizQuestion>): Observable<QuizQuestion> {
    return this.http.post<QuizQuestion>(`${this.base}/api/questions/quiz/${quizId}`, data);
  }
  updateQuizQuestion(id: number, data: Partial<QuizQuestion>): Observable<QuizQuestion> {
    return this.http.put<QuizQuestion>(`${this.base}/api/questions/${id}`, data);
  }
  deleteQuizQuestion(id: number): Observable<any> {
    return this.http.delete(`${this.base}/api/questions/${id}`);
  }
  deleteAllQuizQuestions(quizId: number): Observable<any> {
    return this.http.delete(`${this.base}/api/questions/quiz/${quizId}/all`);
  }
  getQuizAttempts(quizId: number): Observable<QuizAttempt[]> {
    return this.http.get<any>(`${this.base}/api/quiz-attempts/quiz/${quizId}`)
      .pipe(map(r => this.L<QuizAttempt>(r)));
  }
  getQuizStatistics(quizId: number): Observable<QuizStatistics> {
    return this.http.get<any>(`${this.base}/api/quiz-attempts/quiz/${quizId}/statistics`)
      .pipe(map(r => r?.data ?? r));
  }
  deleteQuizAttempt(attemptId: number): Observable<any> {
    return this.http.delete(`${this.base}/api/quiz-attempts/${attemptId}`);
  }

  // -- Assignments
  getAssignmentsByWeek(weekId: number): Observable<Assignment[]> {
    return this.http.get<any>(`${this.base}/api/assignments/week/${weekId}`)
      .pipe(map(r => this.L<Assignment>(r)), catchError(() => of([])));
  }
  createAssignment(data: Partial<Assignment>): Observable<Assignment> {
    return this.http.post<Assignment>(`${this.base}/api/assignments`, data);
  }
  updateAssignment(id: number, data: Partial<Assignment>): Observable<Assignment> {
    return this.http.put<Assignment>(`${this.base}/api/assignments/${id}`, data);
  }
  deleteAssignment(id: number): Observable<any> {
    return this.http.delete(`${this.base}/api/assignments/${id}`);
  }
  getAssignmentQuestions(assignmentId: number): Observable<AssignmentQuestion[]> {
    return this.http.get<any>(`${this.base}/api/assignment-questions/assignment/${assignmentId}`)
      .pipe(map(r => this.L<AssignmentQuestion>(r)), catchError(() => of([])));
  }
  addAssignmentQuestion(data: Partial<AssignmentQuestion>): Observable<AssignmentQuestion> {
    return this.http.post<AssignmentQuestion>(`${this.base}/api/assignment-questions`, data);
  }
  updateAssignmentQuestion(id: number, data: Partial<AssignmentQuestion>): Observable<AssignmentQuestion> {
    return this.http.put<AssignmentQuestion>(`${this.base}/api/assignment-questions/${id}`, data);
  }
  deleteAssignmentQuestion(id: number): Observable<any> {
    return this.http.delete(`${this.base}/api/assignment-questions/${id}`);
  }
  getAssignmentAttempts(assignmentId: number): Observable<AssignmentAttempt[]> {
    return this.http.get<any>(`${this.base}/api/assignment-attempts/assignment/${assignmentId}`)
      .pipe(map(r => this.L<AssignmentAttempt>(r)));
  }
  getAssignmentStatistics(assignmentId: number): Observable<any> {
    return this.http.get<any>(`${this.base}/api/assignment-attempts/assignment/${assignmentId}/statistics`)
      .pipe(map(r => r?.data ?? r));
  }
  deleteAssignmentAttempt(attemptId: number): Observable<any> {
    return this.http.delete(`${this.base}/api/assignment-attempts/${attemptId}`);
  }

  // -- Question Bank
  getTopicsByWeek(weekId: number): Observable<Topic[]> {
    return this.http.get<Topic[]>(`${this.base}/api/topics/week/${weekId}`);
  }
  getQuestionBank(weekId?: number, topicId?: number): Observable<QuestionBankItem[]> {
    let params = new HttpParams();
    if (weekId) params = params.set('weekId', weekId);
    if (topicId) params = params.set('topicId', topicId);
    return this.http.get<any>(`${this.base}/api/question-bank`, { params })
      .pipe(map(r => this.L<QuestionBankItem>(r)));
  }
  addQuestionToBank(data: Partial<QuestionBankItem>): Observable<QuestionBankItem> {
    return this.http.post<QuestionBankItem>(`${this.base}/api/question-bank`, data);
  }
  updateQuestionInBank(id: number, data: Partial<QuestionBankItem>): Observable<QuestionBankItem> {
    return this.http.put<QuestionBankItem>(`${this.base}/api/question-bank/${id}`, data);
  }
  deleteQuestionFromBank(id: number): Observable<any> {
    return this.http.delete(`${this.base}/api/question-bank/${id}`);
  }
  getBankTopics(): Observable<{id: number, name: string}[]> {
    return this.http.get<any>(`${this.base}/api/question-bank/topics/all`)
      .pipe(map(r => this.L<{id: number, name: string}>(r)), catchError(() => of([])));
  }
  getBankQuestionsByTopic(topicId: number): Observable<QuestionBankItem[]> {
    return this.http.get<any>(`${this.base}/api/question-bank/questions/topic/${topicId}`)
      .pipe(map(r => this.L<QuestionBankItem>(r)), catchError(() => of([])));
  }
  getBankQuestionsByWeek(weekId: number, page = 0, size = 20): Observable<Page<QuestionBankItem>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<any>(`${this.base}/api/question-bank/questions/week/${weekId}`, { params })
      .pipe(map(r => this.P<QuestionBankItem>(r)));
  }
  createBankQuestion(data: Partial<QuestionBankItem>): Observable<QuestionBankItem> {
    return this.http.post<QuestionBankItem>(`${this.base}/api/question-bank/questions`, data);
  }
  updateBankQuestion(id: number, data: Partial<QuestionBankItem>): Observable<QuestionBankItem> {
    return this.http.put<QuestionBankItem>(`${this.base}/api/question-bank/questions/${id}`, data);
  }
  deleteBankQuestion(id: number): Observable<any> {
    return this.http.delete(`${this.base}/api/question-bank/questions/${id}`);
  }
  generateExam(data: { topicIds: number[]; questionsPerTopic: number }): Observable<any> {
    return this.http.post(`${this.base}/api/question-bank/generate-exam`, data);
  }

  // -- Students
  getPendingStudents(page = 0, size = 10, grade = ''): Observable<Page<Student>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (grade) params = params.set('grade', grade);
    return this.http.get<any>(`${this.base}/api/teacher/students/pending`, { params })
      .pipe(map(r => this.P<Student>(r)));
  }
  getActiveStudents(page = 0, size = 10, search = '', grade = ''): Observable<Page<Student>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (search) params = params.set('search', search);
    if (grade) params = params.set('grade', grade);
    return this.http.get<any>(`${this.base}/api/teacher/students/active`, { params })
      .pipe(map(r => this.P<Student>(r)));
  }
  getBannedStudents(page = 0, size = 10, grade = ''): Observable<Page<Student>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (grade) params = params.set('grade', grade);
    return this.http.get<any>(`${this.base}/api/teacher/students/blocked`, { params })
      .pipe(map(r => this.P<Student>(r)));
  }
  getRejectedStudents(page = 0, size = 10): Observable<Page<Student>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<any>(`${this.base}/api/teacher/students/rejected`, { params })
      .pipe(map(r => this.P<Student>(r)));
  }
  approveStudent(id: number): Observable<any> {
    return this.http.post(`${this.base}/api/teacher/students/${id}/approve`, {});
  }
  rejectStudent(id: number, reason: string): Observable<any> {
    return this.http.post(`${this.base}/api/teacher/students/${id}/reject`, { reason });
  }
  blockStudent(id: number, reason = ''): Observable<any> {
    return this.http.post(`${this.base}/api/teacher/students/${id}/block`, { reason });
  }
  unblockStudent(id: number): Observable<any> {
    return this.http.post(`${this.base}/api/teacher/students/${id}/unblock`, {});
  }
  updateStudent(id: number, data: any): Observable<any> {
    return this.http.put(`${this.base}/api/teacher/students/${id}`, data);
  }
  deleteStudent(id: number): Observable<any> {
    return this.http.delete(`${this.base}/api/teacher/students/${id}`);
  }
  clearStudentDevice(id: number): Observable<any> {
    return this.http.post(`${this.base}/api/teacher/students/${id}/clear-device`, {});
  }
  getStudentActivity(id: number, page = 0, size = 20): Observable<any> {
    return this.http.get(`${this.base}/api/teacher/students/${id}/activity`, {
      params: { page: page.toString(), size: size.toString() }
    });
  }
  transferStudentToCenter(id: number, groupId?: number | null, centerName?: string): Observable<any> {
    const body: any = {};
    if (groupId) body['groupId'] = groupId;
    if (centerName) body['centerName'] = centerName;
    return this.http.post(`${this.base}/api/teacher/students/${id}/transfer-to-center`, body);
  }
  getFutureCenterStudents(page = 0, size = 200): Observable<any> {
    return this.http.get(`${this.base}/api/teacher/students/future-center`, { params: { page, size } });
  }
  getStudentWallet(studentId: number): Observable<any> {
    return this.http.get<any>(`${this.base}/api/wallet/student/${studentId}`)
      .pipe(map(r => r?.data ?? r));
  }

  getStudentWalletTransactions(studentId: number, size = 200): Observable<WalletTransaction[]> {
    const params = new HttpParams().set('size', String(size));
    return this.http.get<any>(`${this.base}/api/wallet/student/${studentId}/transactions`, { params })
      .pipe(map(r => {
        // GlobalResponse<Page<T>> → unwrap data → extract content
        const page = r?.data ?? r;
        const list = page?.content ?? page;
        return Array.isArray(list) ? list as WalletTransaction[] : [];
      }));
  }
  getStudentAttendance(studentId: number): Observable<AttendanceRecord[]> {
    return this.http.get<any>(`${this.base}/api/attendance/student/${studentId}`)
      .pipe(map(r => this.L<AttendanceRecord>(r)));
  }
  getStudentEnrollments(studentId: number): Observable<Enrollment[]> {
    return this.http.get<any>(`${this.base}/api/enrollments/admin/student/${studentId}`)
      .pipe(map(r => this.L<Enrollment>(r)));
  }
  getStudentQuizAttempts(studentId: number): Observable<QuizAttempt[]> {
    return this.http.get<any>(`${this.base}/api/quiz-attempts/student/${studentId}`)
      .pipe(map(r => this.L<QuizAttempt>(r)));
  }

  // -- Staff
  getStaff(): Observable<Staff[]> {
    return this.http.get<any>(`${this.base}/api/teacher/staff`)
      .pipe(map(r => this.L<Staff>(r)), catchError(() => of([])));
  }
  getStaffPermissionsList(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/api/teacher/staff/permissions`);
  }
  createStaff(data: any): Observable<Staff> {
    return this.http.post<Staff>(`${this.base}/api/teacher/staff`, data);
  }
  updateStaff(id: number, data: any): Observable<Staff> {
    return this.http.put<Staff>(`${this.base}/api/teacher/staff/${id}`, data);
  }
  updateStaffPermissions(id: number, permissions: string[]): Observable<any> {
    return this.http.put(`${this.base}/api/teacher/staff/${id}/permissions`, { permissions });
  }
  toggleStaffStatus(id: number): Observable<any> {
    return this.http.patch(`${this.base}/api/teacher/staff/${id}/toggle`, {});
  }
  deleteStaff(id: number): Observable<any> {
    return this.http.delete(`${this.base}/api/teacher/staff/${id}`);
  }

  // -- Wallet
  topUpWallet(data: { studentId: number; amount: number; validDays?: number | null; expiresAt?: string | null }): Observable<any> {
    return this.http.post(`${this.base}/api/wallet/top-up`, data);
  }
  getWalletTransactions(page = 0, size = 20): Observable<Page<WalletTransaction>> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', 'createdAt,desc');
    return this.http.get<any>(`${this.base}/api/wallet/transactions`, { params })
      .pipe(map(r => this.P<WalletTransaction>(r)));
  }
  getWalletStats(): Observable<any> {
    return this.http.get<any>(`${this.base}/api/wallet/stats`)
      .pipe(map(r => r?.data ?? r));
  }
  searchStudentByPhone(phone: string): Observable<Student> {
    return this.http.get<any>(`${this.base}/api/teacher/students/by-phone/${phone}`)
      .pipe(map(r => r?.data ?? r));
  }
  searchStudentByCode(studentCode: string): Observable<Student> {
    return this.http.get<any>(`${this.base}/api/teacher/students/by-code/${studentCode}`)
      .pipe(map(r => r?.data ?? r));
  }

  // -- Activity Logs
  getActivityLogs(page = 0, size = 30, actor?: string, action?: string, from?: string, to?: string): Observable<Page<any>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (actor) params = params.set('actor', actor);
    if (action) params = params.set('action', action);
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    return this.http.get<any>(`${this.base}/api/activity-logs`, { params })
      .pipe(map(r => this.P<any>(r)));
  }

  // -- Coupons
  getCoupons(): Observable<Coupon[]> {
    return this.http.get<any>(`${this.base}/api/coupons`)
      .pipe(map(r => this.L<Coupon>(r)), catchError(() => of([])));
  }
  getValidCoupons(): Observable<Coupon[]> {
    return this.http.get<any>(`${this.base}/api/coupons/valid`)
      .pipe(map(r => this.L<Coupon>(r)), catchError(() => of([])));
  }
  createCoupon(data: Partial<Coupon>): Observable<Coupon> {
    return this.http.post<Coupon>(`${this.base}/api/coupons`, data);
  }
  updateCoupon(id: number, data: Partial<Coupon>): Observable<Coupon> {
    return this.http.put<Coupon>(`${this.base}/api/coupons/${id}`, data);
  }
  toggleCoupon(id: number): Observable<any> {
    return this.http.patch(`${this.base}/api/coupons/${id}/toggle`, {});
  }
  deleteCoupon(id: number): Observable<any> {
    return this.http.delete(`${this.base}/api/coupons/${id}`);
  }
  previewCoupon(code: string, courseId: number): Observable<any> {
    return this.http.post(`${this.base}/api/coupons/preview`, { code, courseId });
  }

  // -- Access Codes (v1)
  generateAccessCodes(data: any): Observable<any> {
    return this.http.post(`${this.base}/api/v1/access-codes/generate`, data);
  }
  getMyCodes(page = 0, size = 30): Observable<any> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', 'createdAt,desc');
    return this.http.get<any>(`${this.base}/api/v1/access-codes/my-codes`, { params });
  }
  deactivateCode(codeId: number): Observable<any> {
    return this.http.delete(`${this.base}/api/v1/access-codes/${codeId}`);
  }
  deactivateBatch(batchLabel: string): Observable<any> {
    return this.http.delete(`${this.base}/api/v1/access-codes/batch/${batchLabel}`);
  }
  getCodeUsages(codeId: number): Observable<any[]> {
    return this.http.get<any>(`${this.base}/api/v1/access-codes/${codeId}/usages`)
      .pipe(map(r => Array.isArray(r) ? r : r?.data ?? []));
  }

  // -- Enrollments
  getEnrollmentsByCourse(courseId: number): Observable<Enrollment[]> {
    return this.http.get<any>(`${this.base}/api/enrollments/admin/course/${courseId}`, { params: { size: 1000 } })
      .pipe(map(r => this.L<Enrollment>(r)), catchError(() => of([])));
  }
  grantEnrollment(studentId: number, courseId: number): Observable<any> {
    return this.http.post(`${this.base}/api/enrollments/grant`, { studentId, courseId });
  }
  extendEnrollment(id: number, days: number): Observable<any> {
    return this.http.post(`${this.base}/api/enrollments/${id}/extend`, { days });
  }
  deleteEnrollment(id: number): Observable<any> {
    return this.http.delete(`${this.base}/api/enrollments/admin/${id}`);
  }
  deleteAllStudentEnrollments(studentId: number): Observable<any> {
    return this.http.delete(`${this.base}/api/enrollments/student/${studentId}/all`);
  }
  getEnrollments(page = 0, size = 20, courseId?: number, status?: string, search?: string): Observable<Page<Enrollment>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (courseId) params = params.set('courseId', courseId);
    if (status && status !== 'ALL') params = params.set('status', status);
    if (search) params = params.set('search', search);
    return this.http.get<any>(`${this.base}/api/enrollments/paged`, { params })
      .pipe(map(r => this.P<Enrollment>(r)));
  }
  manualEnroll(data: { studentId: number; courseId: number }): Observable<any> {
    const params = new HttpParams()
      .set('studentId', data.studentId)
      .set('courseId', data.courseId);
    return this.http.post(`${this.base}/api/enrollments/admin/grant`, {}, { params });
  }
  cancelEnrollment(id: number): Observable<any> {
    return this.http.post(`${this.base}/api/enrollments/${id}/cancel`, {});
  }
  expireEnrollments(): Observable<any> {
    return this.http.post(`${this.base}/api/enrollments/expire`, {});
  }

  // -- Attendance
  getAttendanceQrToken(): Observable<{ token: string }> {
    return this.http.get<{ token: string }>(`${this.base}/api/attendance/qr-token`);
  }
  getAttendanceByWeek(weekId: number): Observable<AttendanceRecord[]> {
    return this.http.get<any>(`${this.base}/api/attendance/lesson/${weekId}`)
      .pipe(map(r => this.L<AttendanceRecord>(r)), catchError(() => of([])));
  }
  recordOnlineAttendance(studentId: number, weekId: number): Observable<any> {
    return this.http.post(`${this.base}/api/attendance/online`, { studentId, weekId });
  }
  markAttendance(studentCode: string, weekId: number): Observable<AttendanceRecord> {
    return this.http.post<AttendanceRecord>(`${this.base}/api/attendance/mark`, { studentCode, weekId });
  }
  getRecentAttendance(page = 0, size = 20): Observable<Page<AttendanceRecord>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<any>(`${this.base}/api/attendance/recent`, { params })
      .pipe(map(r => this.P<AttendanceRecord>(r)));
  }

  // -- Student Cards
  issueStudentCard(studentId: number): Observable<any> {
    return this.http.post(`${this.base}/api/student-cards/issue`, { studentId });
  }
  verifyStudentCard(token: string): Observable<any> {
    return this.http.post(`${this.base}/api/student-cards/verify`, { token });
  }

  // -- Banners & Announcements
  getBanners(): Observable<Banner[]> {
    return this.http.get<any>(`${this.base}/api/banners/admin?page=0&size=100`)
      .pipe(map(r => this.L<Banner>(r)), catchError(() => of([])));
  }
  createBanner(formData: FormData): Observable<Banner> {
    return this.http.post<any>(`${this.base}/api/banners`, formData)
      .pipe(map(r => r?.data ?? r));
  }
  updateBanner(id: number, data: FormData | Record<string, unknown>): Observable<Banner> {
    const body = data instanceof FormData ? data : {
      title: (data as any).title,
      description: (data as any).description,
      linkUrl: (data as any).linkUrl,
      displayOrder: (data as any).displayOrder ?? (data as any).orderNumber,
      startDate: (data as any).startDate,
      endDate: (data as any).endDate,
      active: (data as any).active ?? ((data as any).status === 'ACTIVE'),
      removeImage: (data as any).removeImage ?? false
    };
    return this.http.put<any>(`${this.base}/api/banners/${id}`, body)
      .pipe(map(r => r?.data ?? r));
  }
  toggleBannerStatus(id: number): Observable<any> {
    return this.http.patch(`${this.base}/api/banners/${id}/toggle-status`, {});
  }
  deleteBanner(id: number): Observable<any> {
    return this.http.delete(`${this.base}/api/banners/${id}`);
  }

  getAnnouncements(): Observable<Announcement[]> {
    return this.http.get<any>(`${this.base}/api/announcements`)
      .pipe(map(r => this.L<Announcement>(r)), catchError(() => of([])));
  }
  createAnnouncement(text: string): Observable<Announcement> {
    return this.http.post<Announcement>(`${this.base}/api/announcements`, { text });
  }
  updateAnnouncement(id: number, text: string): Observable<Announcement> {
    return this.http.put<Announcement>(`${this.base}/api/announcements/${id}`, { text });
  }
  toggleAnnouncementStatus(id: number): Observable<any> {
    return this.http.patch(`${this.base}/api/announcements/${id}/toggle`, {});
  }
  deleteAnnouncement(id: number): Observable<any> {
    return this.http.delete(`${this.base}/api/announcements/${id}`);
  }

  // -- Centers
  getCenters(governorate?: string): Observable<Center[]> {
    let params = new HttpParams();
    if (governorate) params = params.set('governorate', governorate);
    return this.http.get<Center[]>(`${this.base}/api/centers`, { params });
  }
  createCenter(data: Partial<Center>): Observable<Center> {
    return this.http.post<Center>(`${this.base}/api/centers`, data);
  }
  updateCenter(id: number, data: Partial<Center>): Observable<Center> {
    return this.http.put<Center>(`${this.base}/api/centers/${id}`, data);
  }
  deleteCenter(id: number): Observable<any> {
    return this.http.delete(`${this.base}/api/centers/${id}`);
  }

  // -- Geo
  getGovernorates(): Observable<Governorate[]> {
    return this.http.get<Governorate[]>(`${this.base}/api/governorates`);
  }
  getAreas(governorateId: number): Observable<Area[]> {
    return this.http.get<Area[]>(`${this.base}/api/areas?governorateId=${governorateId}`);
  }
  getGovernorateNames(): Observable<string[]> {
    return this.http.get<any>(`${this.base}/api/student/register/governorates`)
      .pipe(map((r: any) => Array.isArray(r) ? r : (r?.data || r?.governorates || [])));
  }
  getAreaNames(governorate: string): Observable<string[]> {
    return this.http.get<any>(`${this.base}/api/student/register/areas/${encodeURIComponent(governorate)}`)
      .pipe(map((r: any) => Array.isArray(r) ? r : (r?.data || r?.areas || [])));
  }

  // -- Notifications
  getNotifications(unreadOnly = false, page = 0, size = 20): Observable<Page<AppNotification>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (unreadOnly) params = params.set('unreadOnly', 'true');
    return this.http.get<any>(`${this.base}/api/notifications`, { params })
      .pipe(map(r => this.P<AppNotification>(r)));
  }
  getUnreadCount(): Observable<number> {
    return this.http.get<any>(`${this.base}/api/notifications/unread-count`)
      .pipe(map(r => (r?.count ?? r?.data ?? 0)), catchError(() => of(0)));
  }
  markAllRead(): Observable<any> {
    return this.http.patch(`${this.base}/api/notifications/read-all`, {});
  }
  clearAllNotifications(): Observable<any> {
    return this.http.delete(`${this.base}/api/notifications/clear-all`);
  }
  markNotificationRead(id: number): Observable<any> {
    return this.http.patch(`${this.base}/api/notifications/${id}/read`, {});
  }

  // ─── Level Stats ───────────────────────────────────────────
  getLevelStats(levelId: number): Observable<any> {
    return this.http.get<any>(`${this.base}/api/levels/${levelId}/stats`)
      .pipe(catchError(() => of(null)));
  }

  // ─── Sessions by Level ─────────────────────────────────────
  getSessionsByLevel(levelId: number): Observable<Session[]> {
    return this.http.get<any>(`${this.base}/api/sessions/by-level/${levelId}`)
      .pipe(map(r => Array.isArray(r) ? r : (r?.content ?? [])), catchError(() => of([])));
  }

  // ─── Groups ───────────────────────────────────────────────────
  getGroupMembers(groupId: number): Observable<any[]> {
    return this.http.get<any>(`${this.base}/api/attendance/groups/${groupId}/members`)
      .pipe(map(r => r?.data?.content ?? r?.data ?? r?.content ?? (Array.isArray(r) ? r : [])), catchError(() => of([])));
  }
  getMyGroups(levelId?: number): Observable<any[]> {
    // كان بيكلم /api/groups/my اللي مش موجود — الصح هو /api/attendance/groups
    let url = `${this.base}/api/attendance/groups`;
    if (levelId != null) url += `?levelId=${levelId}`;
    return this.http.get<any>(url)
      .pipe(map(r => r?.data?.content ?? r?.data ?? r?.content ?? (Array.isArray(r) ? r : [])), catchError(() => of([])));
  }
  createGroup(data: any): Observable<any> {
    // كان /api/groups — الصح /api/attendance/groups
    return this.http.post<any>(`${this.base}/api/attendance/groups`, data).pipe(map(this.unwrap));
  }
  updateGroup(id: number, data: any): Observable<any> {
    return this.http.put<any>(`${this.base}/api/attendance/groups/${id}`, data).pipe(map(this.unwrap));
  }
  deleteGroup(id: number): Observable<any> {
    return this.http.delete<any>(`${this.base}/api/attendance/groups/${id}`);
  }

  // ─── Reorder Categories ────────────────────────────────────────
  reorderCategories(orders: {id: number; sortOrder: number}[]): Observable<any> {
    return this.http.put<any>(`${this.base}/api/categories/reorder`, { orders });
  }

  // ─── File Upload ───────────────────────────────────────────────
  uploadFile(file: File): Observable<any> {
    const fd = new FormData();
    fd.append('file', file);
    return this.http.post<any>(`${this.base}/api/files/upload`, fd).pipe(map(this.unwrap));
  }

  // ─── Center Schedule ──────────────────────────────────────────
  getCenterSchedule(): Observable<any[]> {
    return this.http.get<any>(`${this.base}/api/center-schedule`)
      .pipe(map(r => r?.data ?? r ?? []), catchError(() => of([])));
  }
  addCenterScheduleEntry(body: any): Observable<any> {
    return this.http.post<any>(`${this.base}/api/center-schedule`, body);
  }
  updateCenterScheduleEntry(id: number, body: any): Observable<any> {
    return this.http.put<any>(`${this.base}/api/center-schedule/${id}`, body);
  }
  deleteCenterScheduleEntry(id: number): Observable<any> {
    return this.http.delete<any>(`${this.base}/api/center-schedule/${id}`);
  }

  // ─── Books & Codes Locations ──────────────────────────────────
  getBooksCodesLocations(): Observable<any[]> {
    return this.http.get<any>(`${this.base}/api/books-codes`)
      .pipe(map(r => r?.data ?? r ?? []), catchError(() => of([])));
  }
  addBooksCodesLocation(body: any): Observable<any> {
    return this.http.post<any>(`${this.base}/api/books-codes`, body);
  }
  updateBooksCodesLocation(id: number, body: any): Observable<any> {
    return this.http.put<any>(`${this.base}/api/books-codes/${id}`, body);
  }
  deleteBooksCodesLocation(id: number): Observable<any> {
    return this.http.delete<any>(`${this.base}/api/books-codes/${id}`);
  }

  // ─── Home Layout Config ───────────────────────────────────────
  getHomeLayout(): Observable<any> {
    return this.http.get<any>(`${this.base}/api/teacher/profile/home-layout`)
      .pipe(map(r => r?.data ?? null), catchError(() => of(null)));
  }
  saveHomeLayout(config: string): Observable<any> {
    return this.http.post<any>(`${this.base}/api/teacher/profile/home-layout`, { config });
  }

  // ─── Delete card image / logo ────────────────────────────────
  deleteHomeCardImage(): Observable<any> {
    return this.http.delete<any>(`${this.base}/api/teacher/profile/home-card-image`).pipe(catchError(() => of(null)));
  }
  deleteLogo(): Observable<any> {
    return this.http.delete<any>(`${this.base}/api/teacher/profile/logo`).pipe(catchError(() => of(null)));
  }
}
