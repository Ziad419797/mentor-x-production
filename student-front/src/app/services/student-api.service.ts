import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { StudentAuthService } from './student-auth.service';
import { environment } from '../../environments/environment';

const API = environment.apiBase;

@Injectable({ providedIn: 'root' })
export class StudentApiService {
  constructor(private http: HttpClient, private auth: StudentAuthService) {}

  private get h() { return { headers: this.auth.headers() }; }
  private unwrap = (r: any) => r?.data ?? r;

  getMe():           Observable<any> { return this.http.get(`${API}/api/auth/me`, this.h).pipe(map(this.unwrap)); }
  getSupportChannels(): Observable<any> { return this.http.get(`${API}/api/student/support-channels`, this.h).pipe(map(this.unwrap)); }
  updateProfile(body: any): Observable<any> {
    return this.http.put(`${API}/api/student/profile`, body, this.h).pipe(map(this.unwrap));
  }
  getMyStats():    Observable<any> { return this.http.get(`${API}/api/enrollments/stats/my-stats`, this.h).pipe(map(this.unwrap)); }
  getMyWallet():   Observable<any> { return this.http.get(`${API}/api/wallet/my`, this.h).pipe(map(this.unwrap)); }
  getAnnouncements(): Observable<any[]> {
    return this.http.get(`${API}/api/announcements/active?size=5`, this.h).pipe(
      map((r: any) => (r?.data?.content ?? r?.content ?? r?.data ?? []) as any[])
    );
  }

  // Enrollments
  getMyEnrollments(): Observable<any[]> {
    return this.http.get(`${API}/api/enrollments/my-enrollments`, this.h).pipe(
      map((r: any) => r?.data?.content ?? r?.content ?? r?.data ?? r ?? [])
    );
  }

  // Teacher public profile
  getTeacherProfile(): Observable<any> {
    return this.http.get(`${API}/api/teacher/profile/public`, this.h).pipe(map(this.unwrap));
  }

  getTeacherHomeLayout(): Observable<any> {
    return this.http.get(`${API}/api/teacher/profile/public`, this.h).pipe(
      map((r: any) => (r?.data ?? r)?.homeLayoutConfig ?? null)
    );
  }

  // Featured courses
  getFeaturedCourses(limit = 6): Observable<any[]> {
    return this.http.get(`${API}/api/courses/featured?limit=${limit}`, this.h).pipe(
      map((r: any) => r?.data ?? r ?? [])
    );
  }

  // Courses
  getCoursesByLevel(levelId: number): Observable<any[]> {
    return this.http.get(`${API}/api/courses?levelId=${levelId}&size=50`, this.h).pipe(
      map((r: any) => r?.data?.content ?? r?.content ?? [])
    );
  }
  getCourseById(id: number): Observable<any> {
    return this.http.get(`${API}/api/courses/${id}`, this.h).pipe(map(this.unwrap));
  }
  purchaseCourse(courseId: number): Observable<any> {
    return this.http.post(`${API}/api/enrollments`, { courseId }, this.h).pipe(map(this.unwrap));
  }

  // Payment
  createOrder(courseId: number): Observable<any> {
    const body = { items: [{ productType: 'COURSE', productId: courseId }] };
    return this.http.post(`${API}/api/payment/orders`, body, this.h).pipe(map(this.unwrap));
  }
  payWithWallet(orderId: number, couponCode?: string): Observable<any> {
    const body: any = { orderId, paymentMethod: 'WALLET' };
    if (couponCode) body.couponCode = couponCode;
    return this.http.post(`${API}/api/payment/process`, body, this.h).pipe(map(this.unwrap));
  }
  payOnline(orderId: number): Observable<any> {
    return this.http.post(`${API}/api/payment/process`,
      { orderId, paymentMethod: 'CREDIT_CARD' }, this.h).pipe(map(this.unwrap));
  }
  redeemAccessCode(code: string, courseId?: number): Observable<any> {
    return this.http.post(`${API}/api/v1/access-codes/redeem`, { code, courseId }, this.h).pipe(map(this.unwrap));
  }
  walletDepositOnline(amount: number, paymentMethodId: number): Observable<any> {
    return this.http.post(`${API}/api/wallet/deposit/online`, { amount, paymentMethodId }, this.h).pipe(map(this.unwrap));
  }
  getWalletPaymentMethods(): Observable<any> {
    return this.http.get(`${API}/api/wallet/payment-methods`, this.h).pipe(map(this.unwrap));
  }
  getDepositStatus(txNumber: string): Observable<any> {
    return this.http.get(`${API}/api/wallet/deposit/status/${txNumber}`, this.h).pipe(map(this.unwrap));
  }
  previewCoupon(code: string, originalPrice: number): Observable<any> {
    return this.http.post(`${API}/api/coupons/preview`, { code, originalPrice }, this.h).pipe(map(this.unwrap));
  }
  getWalletBalance(): Observable<any> {
    return this.http.get(`${API}/api/payment/wallet/balance`, this.h).pipe(map(this.unwrap));
  }

  // Sessions
  getSessionById(id: number): Observable<any> {
    return this.http.get(`${API}/api/sessions/${id}`, this.h).pipe(map(this.unwrap));
  }

  getSessionsByCourse(courseId: number): Observable<any[]> {
    return this.http.get(`${API}/api/sessions/course/${courseId}`, this.h).pipe(
      map((r: any) => r?.data?.content ?? r?.content ?? r?.data ?? r ?? [])
    );
  }

  // Weeks & Content
  getWeeksBySession(sessionId: number): Observable<any[]> {
    return this.http.get(`${API}/api/weeks/session/${sessionId}?size=100`, this.h).pipe(
      map((r: any) => r?.data?.content ?? r?.content ?? r?.data ?? r ?? [])
    );
  }
  getMaterialById(id: number): Observable<any> {
    return this.http.get(`${API}/api/materials/${id}`, this.h).pipe(map(this.unwrap));
  }
  getMaterialsByWeek(weekId: number): Observable<any[]> {
    return this.http.get(`${API}/api/materials/lesson/${weekId}?size=100&sort=orderNumber,asc`, this.h).pipe(
      map((r: any) => r?.data?.content ?? r?.content ?? r?.data ?? r ?? [])
    );
  }
  getQuizzesByWeek(weekId: number): Observable<any[]> {
    return this.http.get(`${API}/api/quizzes/week/${weekId}?size=100&sort=orderNumber,asc`, this.h).pipe(
      map((r: any) => r?.data?.content ?? r?.content ?? r?.data ?? r ?? [])
    );
  }
  getAssignmentsByWeek(weekId: number): Observable<any[]> {
    return this.http.get(`${API}/api/assignments/week/${weekId}?size=100&sort=orderNumber,asc`, this.h).pipe(
      map((r: any) => r?.data?.content ?? r?.content ?? r?.data ?? r ?? [])
    );
  }

  // My attempts (for profile page)
  getMyQuizAttempts(studentId: number): Observable<any[]> {
    return this.http.get(`${API}/api/quiz-attempts/student/${studentId}?size=100`, this.h).pipe(
      map((r: any) => r?.data?.content ?? r?.data ?? r?.content ?? r?.content ?? [])
    );
  }
  getMyAssignmentAttempts(studentId: number): Observable<any[]> {
    return this.http.get(`${API}/api/assignment-attempts/student/${studentId}?size=100`, this.h).pipe(
      map((r: any) => r?.data?.content ?? r?.data ?? r?.content ?? r ?? [])
    );
  }
  getMyWalletTransactions(): Observable<any[]> {
    return this.http.get(`${API}/api/wallet/my/transactions?size=20&sort=createdAt,desc`, this.h).pipe(
      map((r: any) => r?.data?.content ?? r?.data ?? r?.content ?? r ?? [])
    );
  }

  // Quiz
  getQuizQuestions(quizId: number): Observable<any[]> {
    return this.http.get(`${API}/api/quiz-questions/quiz/${quizId}`, this.h).pipe(
      map((r: any) => r?.data ?? r ?? [])
    );
  }
  startQuiz(quizId: number): Observable<any> {
    return this.http.post(`${API}/api/quiz-attempts/start/${quizId}`, {}, this.h).pipe(map(this.unwrap));
  }
  submitQuiz(attemptId: number, answers: {questionId: number; answer: string}[]): Observable<any> {
    return this.http.post(`${API}/api/quiz-attempts/${attemptId}/submit`, { answers }, this.h).pipe(map(this.unwrap));
  }

  // Assignment
  getAssignmentById(assignmentId: number): Observable<any> {
    return this.http.get(`${API}/api/assignments/${assignmentId}`, this.h).pipe(map(this.unwrap));
  }
  getAssignmentQuestions(assignmentId: number): Observable<any[]> {
    return this.http.get(`${API}/api/assignment-questions/assignment/${assignmentId}`, this.h).pipe(
      map((r: any) => r?.data ?? r ?? [])
    );
  }
  submitAssignment(assignmentId: number, answers: {questionId: number; answer: string}[]): Observable<any> {
    return this.http.post(`${API}/api/assignment-attempts`, { assignmentId, answers }, this.h).pipe(map(this.unwrap));
  }

  // Levels
  getLevels(): Observable<any[]> {
    return this.http.get(`${API}/api/levels`, this.h).pipe(map((r: any) => r?.data ?? r ?? []));
  }

  // Categories by level
  getCategoriesByLevel(levelId: number): Observable<any[]> {
    return this.http.get(`${API}/api/categories/level/${levelId}?size=50&sort=sortOrder,asc`, this.h).pipe(
      map((r: any) => r?.data?.content ?? r?.content ?? r?.data ?? r ?? [])
    );
  }

  // Courses by category
  getCoursesByCategory(categoryId: number): Observable<any[]> {
    return this.http.get(`${API}/api/courses/category/${categoryId}?size=50`, this.h).pipe(
      map((r: any) => r?.data?.content ?? r?.content ?? r?.data ?? r ?? [])
    );
  }

  // ── Center Schedule ──────────────────────────────────────────
  getCenterSchedule(): Observable<any[]> {
    return this.http.get(`${API}/api/center-schedule`, this.h).pipe(
      map((r: any) => r?.data ?? r ?? [])
    );
  }
  addCenterScheduleEntry(body: any): Observable<any> {
    return this.http.post(`${API}/api/center-schedule`, body, this.h).pipe(map(this.unwrap));
  }
  updateCenterScheduleEntry(id: number, body: any): Observable<any> {
    return this.http.put(`${API}/api/center-schedule/${id}`, body, this.h).pipe(map(this.unwrap));
  }
  deleteCenterScheduleEntry(id: number): Observable<any> {
    return this.http.delete(`${API}/api/center-schedule/${id}`, this.h).pipe(map(this.unwrap));
  }

  // ── Books & Codes Locations ──────────────────────────────────
  getBooksCodesLocations(): Observable<any[]> {
    return this.http.get(`${API}/api/books-codes`, this.h).pipe(
      map((r: any) => r?.data ?? r ?? [])
    );
  }
  addBooksCodesLocation(body: any): Observable<any> {
    return this.http.post(`${API}/api/books-codes`, body, this.h).pipe(map(this.unwrap));
  }
  updateBooksCodesLocation(id: number, body: any): Observable<any> {
    return this.http.put(`${API}/api/books-codes/${id}`, body, this.h).pipe(map(this.unwrap));
  }
  deleteBooksCodesLocation(id: number): Observable<any> {
    return this.http.delete(`${API}/api/books-codes/${id}`, this.h).pipe(map(this.unwrap));
  }

  // ── AI Chat ──────────────────────────────────────────────────
  askAi(question: string, history: {role:string;content:string}[] = []): Observable<any> {
    return this.http.post(`${API}/api/ai/chat`, { question }, this.h).pipe(map(this.unwrap));
  }

  // ── Public branding (no auth required) ───────────────────────
  getPublicBranding(): Observable<any> {
    return this.http.get(`${API}/api/public/branding`).pipe(map(this.unwrap));
  }

  // ── Student Analytics ─────────────────────────────────────────
  getStudentAnalytics(): Observable<any> {
    return this.http.get(`${API}/api/analytics/student/all`, this.h).pipe(map(this.unwrap));
  }
}
