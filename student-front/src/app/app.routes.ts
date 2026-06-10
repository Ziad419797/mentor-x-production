import { Routes } from '@angular/router';
import { studentAuthGuard, studentGuestGuard } from './guards/student-auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  {
    path: 'login',
    canActivate: [studentGuestGuard],
    loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    canActivate: [studentGuestGuard],
    loadComponent: () => import('./pages/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'forgot-password',
    canActivate: [studentGuestGuard],
    loadComponent: () => import('./pages/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)
  },
  {
    path: '',
    canActivate: [studentAuthGuard],
    loadComponent: () => import('./layout/shell.component').then(m => m.ShellComponent),
    children: [
      { path: 'home',            loadComponent: () => import('./pages/home/home.component').then(m => m.HomeComponent) },
      { path: 'profile',         loadComponent: () => import('./pages/profile/profile.component').then(m => m.ProfileComponent) },
      { path: 'courses',         loadComponent: () => import('./pages/courses/courses.component').then(m => m.CoursesComponent) },
      { path: 'courses/:id',     loadComponent: () => import('./pages/course-detail/course-detail.component').then(m => m.CourseDetailComponent) },
      { path: 'wallet',          loadComponent: () => import('./pages/wallet/wallet.component').then(m => m.WalletPageComponent) },
      { path: 'quiz/:id',        loadComponent: () => import('./pages/quiz/quiz.component').then(m => m.QuizComponent) },
      { path: 'assignment/:id',  loadComponent: () => import('./pages/assignment/assignment.component').then(m => m.AssignmentComponent) },
      { path: 'session/:id',     loadComponent: () => import('./pages/session-content/session-content.component').then(m => m.SessionContentComponent) },
      { path: 'video/:id',       loadComponent: () => import('./pages/video-player/video-player.component').then(m => m.VideoPlayerComponent) },
      { path: 'store',           loadComponent: () => import('./pages/store/store.component').then(m => m.StoreComponent) },
      { path: 'center-schedule', loadComponent: () => import('./pages/center-schedule/center-schedule.component').then(m => m.CenterScheduleComponent) },
      { path: 'books-codes',     loadComponent: () => import('./pages/books-codes/books-codes.component').then(m => m.BooksCodesComponent) },
      { path: 'ai-chat',         loadComponent: () => import('./pages/ai-chat/ai-chat.component').then(m => m.AiChatComponent) },
      { path: 'my-courses',     loadComponent: () => import('./pages/my-courses/my-courses.component').then(m => m.MyCoursesComponent) },
      { path: 'analytics',      loadComponent: () => import('./pages/analytics/analytics.component').then(m => m.StudentAnalyticsComponent) },
      { path: '**', redirectTo: 'home' }
    ]
  }
];
