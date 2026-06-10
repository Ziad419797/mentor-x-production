import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'forgot-password',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell/shell.component').then(m => m.ShellComponent),
    children: [
      { path: 'dashboard',         loadComponent: () => import('./pages/forgot-password/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'levels-categories', loadComponent: () => import('./pages/levels-categories/levels-categories.component').then(m => m.LevelsCategoriesComponent) },
      { path: 'level/:levelId/data',                           loadComponent: () => import('./pages/level-data/level-data.component').then(m => m.LevelDataComponent) },
      { path: 'level/:levelId/categories',                     loadComponent: () => import('./pages/level-categories/level-categories.component').then(m => m.LevelCategoriesComponent) },
      { path: 'level/:levelId/categories/:categoryId/courses', loadComponent: () => import('./pages/category-courses/category-courses.component').then(m => m.CategoryCoursesComponent) },
      { path: 'level/:levelId/add-category',                   loadComponent: () => import('./pages/add-category/add-category.component').then(m => m.AddCategoryComponent) },
      { path: 'level/:levelId/add-course',                     loadComponent: () => import('./pages/add-course/add-course.component').then(m => m.AddCourseComponent) },
      { path: 'sessions',          loadComponent: () => import('./pages/sessions/sessions.component').then(m => m.SessionsComponent) },
      { path: 'courses/:courseId/sessions',          loadComponent: () => import('./pages/course-sessions/course-sessions.component').then(m => m.CourseSessionsComponent) },
      { path: 'sessions/:id/content',                loadComponent: () => import('./pages/session-content/session-content.component').then(m => m.SessionContentComponent) },
      { path: 'sessions/:id/attendance',             loadComponent: () => import('./pages/session-attendance/session-attendance.component').then(m => m.SessionAttendanceComponent) },
      { path: 'courses/:courseId/enrolled-students', loadComponent: () => import('./pages/course-enrolled-students/course-enrolled-students.component').then(m => m.CourseEnrolledStudentsComponent) },
      { path: 'materials',         loadComponent: () => import('./pages/materials/materials.component').then(m => m.MaterialsComponent) },
      { path: 'quizzes',           loadComponent: () => import('./pages/quizzes/quizzes.component').then(m => m.QuizzesComponent) },
      { path: 'new-requests',      loadComponent: () => import('./pages/students/students.component').then(m => m.StudentsComponent) },
      { path: 'students',          loadComponent: () => import('./pages/students/students.component').then(m => m.StudentsComponent) },
      { path: 'level/:levelId/students', loadComponent: () => import('./pages/students/students.component').then(m => m.StudentsComponent) },
      { path: 'staff',             loadComponent: () => import('./pages/staff/staff.component').then(m => m.StaffComponent) },
      { path: 'wallet',            loadComponent: () => import('./pages/wallet/wallet.component').then(m => m.WalletComponent) },
      { path: 'coupons',           loadComponent: () => import('./pages/coupons/coupons.component').then(m => m.CouponsComponent) },
      { path: 'assignments',       loadComponent: () => import('./pages/assignments/assignments.component').then(m => m.AssignmentsComponent) },
      { path: 'question-bank',     loadComponent: () => import('./pages/question-bank/question-bank.component').then(m => m.QuestionBankComponent) },
      { path: 'create-codes',      loadComponent: () => import('./pages/access-codes/access-codes.component').then(m => m.AccessCodesComponent) },
      { path: 'codes',             loadComponent: () => import('./pages/codes/codes.component').then(m => m.CodesComponent) },
      { path: 'wallet-history',             loadComponent: () => import('./pages/wallet-history/wallet-history.component').then(m => m.WalletHistoryComponent) },
      { path: 'wallet-history/:studentId',  loadComponent: () => import('./pages/wallet-history/wallet-history.component').then(m => m.WalletHistoryComponent) },
      { path: 'student-cards',     loadComponent: () => import('./pages/student-cards/student-cards.component').then(m => m.StudentCardsComponent) },
      { path: 'banners',           loadComponent: () => import('./pages/banners/banners.component').then(m => m.BannersComponent) },
      { path: 'centers',           loadComponent: () => import('./pages/centers/centers.component').then(m => m.CentersComponent) },
      { path: 'attendance',        loadComponent: () => import('./pages/attendance/attendance.component').then(m => m.AttendanceComponent) },
      { path: 'profile',           loadComponent: () => import('./pages/profile/profile.component').then(m => m.ProfileComponent) },
      { path: 'notifications',     loadComponent: () => import('./pages/notifications/notifications.component').then(m => m.NotificationsComponent) },
      { path: 'activity-logs',     loadComponent: () => import('./pages/activity-logs/activity-logs.component').then(m => m.ActivityLogsComponent) },
      { path: 'center-schedule',   loadComponent: () => import('./pages/center-schedule/center-schedule.component').then(m => m.CenterScheduleComponent) },
      { path: 'books-codes',       loadComponent: () => import('./pages/books-codes/books-codes.component').then(m => m.BooksCodesComponent) },
      { path: 'support-channels',  loadComponent: () => import('./pages/support-channels/support-channels.component').then(m => m.SupportChannelsComponent) },
      { path: 'home-layout',       loadComponent: () => import('./pages/home-layout/home-layout.component').then(m => m.HomeLayoutComponent) },
      { path: 'topic-tree',        loadComponent: () => import('./pages/topic-tree/topic-tree.component').then(m => m.TopicTreeComponent) },
      { path: 'topic-analytics',   loadComponent: () => import('./pages/topic-analytics/topic-analytics.component').then(m => m.TopicAnalyticsComponent) },
      { path: '**', redirectTo: 'dashboard' }
    ]
  }
];
