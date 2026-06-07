import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { AppNotification } from '../../models/models';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="space-y-6 animate-fade-in max-w-4xl pb-10">
      
      <!-- Header -->
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-white font-bold text-2xl">الإشعارات</h2>
          <p class="text-slate-500 text-sm mt-1">تابع آخر التحديثات والنشاطات على المنصة</p>
        </div>

        <div class="flex gap-3">
          <button (click)="markAllAsRead()" class="btn-secondary py-2 text-xs h-auto">
            تم قراءة الكل
          </button>

          <button (click)="clearAll()" class="btn-icon h-9 w-9 text-red-400">
            <span class="material-icons-round text-sm">delete_sweep</span>
          </button>
        </div>
      </div>

      <!-- Notifications List -->
      <div class="space-y-3">

        <div
          *ngFor="let n of notifications()"
          (click)="markAsRead(n)"
          [ngClass]="{
            'bg-indigo-500/5 border-indigo-500/20': !n.read
          }"
          class="edu-card p-5 flex items-start gap-4 transition-all cursor-pointer group hover:bg-slate-800/60 border border-slate-800"
        >

          <!-- Icon -->
          <div
            class="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0"
            [ngClass]="getIconClass(n.type || '')"
          >
            <span class="material-icons-round text-xl">
              {{ getIcon(n.type || '') }}
            </span>
          </div>

          <!-- Content -->
          <div class="flex-1 space-y-1">

            <div class="flex items-center justify-between">
              <h4
                class="text-white font-bold text-sm"
                [class.text-indigo-400]="!n.read"
              >
                {{ n.title }}
              </h4>

              <span class="text-[9px] text-slate-500 font-bold uppercase tracking-wider">
                {{ n.createdAt | date:'shortTime' }}
              </span>
            </div>

            <p class="text-xs text-slate-400 leading-relaxed">
              {{ n.body }}
            </p>

            <p class="text-[9px] text-slate-600 pt-1">
              {{ n.createdAt | date:'mediumDate' }}
            </p>
          </div>

          <!-- unread dot -->
          <div
            *ngIf="!n.read"
            class="w-2 h-2 rounded-full bg-indigo-500 mt-2 shadow-lg shadow-indigo-500/50"
          ></div>
        </div>

        <!-- Empty State -->
        <div
          *ngIf="notifications().length === 0"
          class="py-32 text-center text-slate-700 bg-slate-900/20 border-2 border-dashed border-slate-800 rounded-3xl"
        >
          <span class="material-icons-round text-5xl mb-4 opacity-10">
            notifications_off
          </span>

          <p class="text-sm">لا توجد إشعارات حالياً</p>
        </div>
      </div>

      <!-- Pagination -->
      <div *ngIf="totalPages() > 1" class="flex justify-center gap-2 pt-6">

        <button
          *ngFor="let p of [].constructor(totalPages()); let i = index"
          (click)="loadNotifications(i)"
          [class.bg-indigo-600]="currentPage() === i"
          class="w-10 h-10 rounded-lg bg-slate-800 border border-slate-700 text-white font-bold hover:bg-indigo-600 transition-colors"
        >
          {{ i + 1 }}
        </button>

      </div>

    </div>
  `
})
export class NotificationsComponent implements OnInit {

  notifications = signal<AppNotification[]>([]);
  totalPages = signal(0);
  currentPage = signal(0);

  constructor(
    private api: ApiService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.loadNotifications();
  }

  loadNotifications(page = 0) {
    this.api.getNotifications(false, page, 15).subscribe({
      next: (res: any) => {
        const data = res?.data;
        this.notifications.set(data?.content || []);
        this.totalPages.set(data?.totalPages || 0);
        this.currentPage.set(data?.number || 0);
      },
      error: (err) => {
        console.error('Notifications Error:', err);
      }
    });
  }

  getIcon(type: string | undefined) {
    switch (type) {
      case 'PAYMENT': return 'payments';
      case 'ENROLLMENT': return 'person_add';
      case 'QUIZ': return 'quiz';
      case 'SYSTEM': return 'settings_suggest';
      case 'ATTENDANCE_CENTER': return 'how_to_reg';
      default: return 'notifications';
    }
  }

  getIconClass(type: string | undefined) {
    switch (type) {
      case 'PAYMENT': return 'bg-emerald-500/10 text-emerald-400';
      case 'ENROLLMENT': return 'bg-blue-500/10 text-blue-400';
      case 'QUIZ': return 'bg-amber-500/10 text-amber-400';
      case 'SYSTEM': return 'bg-purple-500/10 text-purple-400';
      case 'ATTENDANCE_CENTER': return 'bg-indigo-500/10 text-indigo-400';
      default: return 'bg-slate-500/10 text-slate-400';
    }
  }

  markAsRead(n: AppNotification) {
    if (n.read) return;
    this.api.markNotificationRead(n.id).subscribe({
      next: () => {
        n.read = true;
        this.notifications.set([...this.notifications()]);
      }
    });
  }

  markAllAsRead() {
    this.api.markAllRead().subscribe({
      next: () => {
        this.notifications().forEach(n => n.read = true);
        this.notifications.set([...this.notifications()]);
        this.toastr.success('تم تحديد الكل كمقروء');
      }
    });
  }

  clearAll() {
    if (!confirm('هل أنت متأكد من حذف جميع الإشعارات؟')) return;
    this.api.clearAllNotifications().subscribe({
      next: () => {
        this.notifications.set([]);
        this.toastr.success('تم حذف الإشعارات');
      }
    });
  }
}
