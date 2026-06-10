import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router, ActivatedRoute } from '@angular/router';
import { StudentApiService } from '../../services/student-api.service';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

type Tab = 'profile' | 'courses' | 'quizzes' | 'assignments' | 'wallet';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
<div *ngIf="loading()" class="flex justify-center py-20">
  <span class="material-symbols-outlined animate-spin text-[#8892a0]">refresh</span>
</div>

<div *ngIf="!loading()" class="flex gap-6 pb-10" dir="rtl">

  <!-- Left: Profile card -->
  <div class="w-72 flex-shrink-0 space-y-4">

    <div class="bg-white dark:bg-[#162033] border border-[#DDE1EA] dark:border-slate-800 rounded-2xl p-6 flex flex-col items-center text-center shadow-sm">
      <!-- Avatar -->
      <div class="w-24 h-24 rounded-full overflow-hidden mb-4 ring-4 ring-[#183764]/20 dark:ring-white/10">
        <img *ngIf="me()?.profileImageUrl" [src]="me()?.profileImageUrl" class="w-full h-full object-cover">
        <div *ngIf="!me()?.profileImageUrl"
             class="w-full h-full bg-gradient-to-br from-[#183764] to-[#4BBBA0] flex items-center justify-center text-white text-3xl font-black">
          {{ initial }}
        </div>
      </div>

      <h2 class="font-black text-lg text-[#183764] dark:text-white">{{ me()?.fullName || me()?.name }}</h2>

      <div *ngIf="me()?.email" class="flex items-center justify-center gap-1 text-xs text-[#8892a0] mt-1">
        <span class="material-symbols-outlined" style="font-size:13px">mail</span>
        {{ me()?.email }}
      </div>
      <div *ngIf="me()?.phone" class="flex items-center justify-center gap-1 text-xs text-[#8892a0] mt-0.5">
        <span class="material-symbols-outlined" style="font-size:13px">phone</span>
        {{ me()?.phone }}
      </div>
      <div class="flex items-center justify-center gap-1 text-xs font-mono font-bold text-[#183764] dark:text-white mt-0.5">
        <span class="material-symbols-outlined text-[#8892a0]" style="font-size:13px">badge</span>
        {{ me()?.studentCode || '—' }}
      </div>
      <div *ngIf="me()?.grade || me()?.level?.name" class="mt-1 px-3 py-0.5 rounded-full bg-[#e8edf7] dark:bg-white/10 text-[#183764] dark:text-white text-xs font-bold">
        {{ me()?.grade || me()?.level?.name }}
      </div>

      <!-- QR Code -->
      <div *ngIf="me()?.studentCode" class="mt-5 p-3 bg-white rounded-xl border border-[#DDE1EA]">
        <img [src]="qrUrl()" alt="QR Code" class="w-40 h-40 mx-auto">
        <p class="text-xs font-mono font-bold text-[#183764] mt-1 tracking-widest">{{ me()?.studentCode }}</p>
      </div>
    </div>

    <!-- Stats mini -->
    <div class="grid grid-cols-2 gap-3">
      <div class="bg-white dark:bg-[#162033] border border-[#DDE1EA] dark:border-slate-800 rounded-xl p-3 text-center">
        <div class="text-2xl font-black text-[#183764] dark:text-white">{{ enrollments().length }}</div>
        <div class="text-[10px] text-[#8892a0] mt-0.5">كورسات</div>
      </div>
      <div class="bg-white dark:bg-[#162033] border border-[#DDE1EA] dark:border-slate-800 rounded-xl p-3 text-center">
        <div class="text-2xl font-black text-[#4BBBA0]">{{ walletBalance }}</div>
        <div class="text-[10px] text-[#8892a0] mt-0.5">رصيد ج.م</div>
      </div>
    </div>
  </div>

  <!-- Right: Tabs -->
  <div class="flex-1 min-w-0">

    <!-- Tab bar -->
    <div class="bg-white dark:bg-[#162033] border border-[#DDE1EA] dark:border-slate-800 rounded-2xl overflow-hidden shadow-sm mb-4">
      <div class="flex overflow-x-auto">
        <button *ngFor="let t of tabs" (click)="loadTab(t.id)"
                class="flex items-center gap-2 px-5 py-3.5 text-sm font-bold whitespace-nowrap border-b-2 transition-all flex-shrink-0"
                [class.border-[#183764]]="activeTab() === t.id"
                [class.text-[#183764]]="activeTab() === t.id"
                [class.dark:border-white]="activeTab() === t.id"
                [class.dark:text-white]="activeTab() === t.id"
                [class.border-transparent]="activeTab() !== t.id"
                [class.text-[#8892a0]]="activeTab() !== t.id">
          <span class="material-symbols-outlined" style="font-size:16px">{{ t.icon }}</span>
          {{ t.label }}
        </button>
      </div>
    </div>

    <!-- Tab content -->
    <div class="bg-white dark:bg-[#162033] border border-[#DDE1EA] dark:border-slate-800 rounded-2xl p-6 shadow-sm">

      <!-- ═══ الملف الشخصي ═══ -->
      <div *ngIf="activeTab() === 'profile'">

        <!-- View mode -->
        <div *ngIf="!editMode()">
          <div class="flex items-center justify-between mb-4">
            <h3 class="font-bold text-[#183764] dark:text-white">البيانات الشخصية</h3>
            <button (click)="startEdit()"
                    class="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-[#183764] text-white text-xs font-bold hover:opacity-90 transition-opacity">
              <span class="material-symbols-outlined" style="font-size:14px">edit</span>
              تعديل البيانات
            </button>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-1">
              <label class="text-xs text-[#8892a0]">الاسم الكامل</label>
              <p class="font-semibold text-[#183764] dark:text-white">{{ me()?.fullName || me()?.name || '—' }}</p>
            </div>
            <div class="space-y-1">
              <label class="text-xs text-[#8892a0]">كود الطالب</label>
              <p class="font-mono font-bold text-[#183764] dark:text-white tracking-wider">{{ me()?.studentCode || '—' }}</p>
            </div>
            <div class="space-y-1">
              <label class="text-xs text-[#8892a0]">رقم الهاتف</label>
              <p class="font-semibold text-[#183764] dark:text-white">{{ me()?.phone || '—' }}</p>
            </div>
            <div class="space-y-1">
              <label class="text-xs text-[#8892a0]">البريد الإلكتروني</label>
              <p class="font-semibold text-[#183764] dark:text-white text-sm">{{ me()?.email || '—' }}</p>
            </div>
            <div class="space-y-1">
              <label class="text-xs text-[#8892a0]">الصف الدراسي</label>
              <p class="font-semibold text-[#183764] dark:text-white">{{ me()?.grade || me()?.level?.name || '—' }}</p>
            </div>
            <div class="space-y-1">
              <label class="text-xs text-[#8892a0]">المركز</label>
              <p class="font-semibold text-[#183764] dark:text-white">{{ me()?.centerName || me()?.center?.name || '—' }}</p>
            </div>
            <div class="space-y-1">
              <label class="text-xs text-[#8892a0]">المحافظة</label>
              <p class="font-semibold text-[#183764] dark:text-white">{{ me()?.governorate || '—' }}</p>
            </div>
            <div class="space-y-1">
              <label class="text-xs text-[#8892a0]">المدرسة</label>
              <p class="font-semibold text-[#183764] dark:text-white">{{ me()?.schoolName || '—' }}</p>
            </div>
            <div class="space-y-1">
              <label class="text-xs text-[#8892a0]">نوع الدراسة</label>
              <p class="font-semibold text-[#183764] dark:text-white">
                {{ me()?.online === true ? 'أونلاين' : me()?.online === false ? 'سنتر' : '—' }}
              </p>
            </div>
          </div>
        </div>

        <!-- Edit mode -->
        <div *ngIf="editMode()">
          <div class="flex items-center justify-between mb-4">
            <h3 class="font-bold text-[#183764] dark:text-white">تعديل البيانات</h3>
            <button (click)="editMode.set(false)"
                    class="text-xs text-[#8892a0] hover:text-[#183764] transition-colors">إلغاء</button>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-1">
              <label class="text-xs text-[#8892a0]">الاسم الأول</label>
              <input type="text" [(ngModel)]="editForm.firstName" class="w-full px-3 py-2 rounded-xl border border-[#DDE1EA] dark:border-slate-700 bg-[#F5F6FA] dark:bg-white/5 text-[#183764] dark:text-white text-sm outline-none focus:border-[#183764]">
            </div>
            <div class="space-y-1">
              <label class="text-xs text-[#8892a0]">الاسم الثاني</label>
              <input type="text" [(ngModel)]="editForm.secondName" class="w-full px-3 py-2 rounded-xl border border-[#DDE1EA] dark:border-slate-700 bg-[#F5F6FA] dark:bg-white/5 text-[#183764] dark:text-white text-sm outline-none focus:border-[#183764]">
            </div>
            <div class="space-y-1">
              <label class="text-xs text-[#8892a0]">الاسم الثالث</label>
              <input type="text" [(ngModel)]="editForm.thirdName" class="w-full px-3 py-2 rounded-xl border border-[#DDE1EA] dark:border-slate-700 bg-[#F5F6FA] dark:bg-white/5 text-[#183764] dark:text-white text-sm outline-none focus:border-[#183764]">
            </div>
            <div class="space-y-1">
              <label class="text-xs text-[#8892a0]">الاسم الرابع</label>
              <input type="text" [(ngModel)]="editForm.fourthName" class="w-full px-3 py-2 rounded-xl border border-[#DDE1EA] dark:border-slate-700 bg-[#F5F6FA] dark:bg-white/5 text-[#183764] dark:text-white text-sm outline-none focus:border-[#183764]">
            </div>
            <div class="space-y-1">
              <label class="text-xs text-[#8892a0]">الصف الدراسي</label>
              <input type="text" [(ngModel)]="editForm.grade" class="w-full px-3 py-2 rounded-xl border border-[#DDE1EA] dark:border-slate-700 bg-[#F5F6FA] dark:bg-white/5 text-[#183764] dark:text-white text-sm outline-none focus:border-[#183764]">
            </div>
            <div class="space-y-1">
              <label class="text-xs text-[#8892a0]">المحافظة</label>
              <input type="text" [(ngModel)]="editForm.governorate" class="w-full px-3 py-2 rounded-xl border border-[#DDE1EA] dark:border-slate-700 bg-[#F5F6FA] dark:bg-white/5 text-[#183764] dark:text-white text-sm outline-none focus:border-[#183764]">
            </div>
            <div class="space-y-1">
              <label class="text-xs text-[#8892a0]">المنطقة</label>
              <input type="text" [(ngModel)]="editForm.area" class="w-full px-3 py-2 rounded-xl border border-[#DDE1EA] dark:border-slate-700 bg-[#F5F6FA] dark:bg-white/5 text-[#183764] dark:text-white text-sm outline-none focus:border-[#183764]">
            </div>
            <div class="space-y-1">
              <label class="text-xs text-[#8892a0]">اسم المدرسة</label>
              <input type="text" [(ngModel)]="editForm.schoolName" class="w-full px-3 py-2 rounded-xl border border-[#DDE1EA] dark:border-slate-700 bg-[#F5F6FA] dark:bg-white/5 text-[#183764] dark:text-white text-sm outline-none focus:border-[#183764]">
            </div>
            <div class="space-y-1" *ngIf="editForm.online !== true">
              <label class="text-xs text-[#8892a0]">اسم المركز</label>
              <input type="text" [(ngModel)]="editForm.centerName" class="w-full px-3 py-2 rounded-xl border border-[#DDE1EA] dark:border-slate-700 bg-[#F5F6FA] dark:bg-white/5 text-[#183764] dark:text-white text-sm outline-none focus:border-[#183764]">
            </div>
          </div>
          <div *ngIf="saveError()" class="mt-3 p-3 rounded-xl bg-red-50 dark:bg-red-500/10 text-red-600 dark:text-red-400 text-sm">{{ saveError() }}</div>
          <div *ngIf="saveSuccess()" class="mt-3 p-3 rounded-xl bg-green-50 dark:bg-green-500/10 text-green-700 dark:text-green-400 text-sm">{{ saveSuccess() }}</div>
          <button (click)="saveProfile()" [disabled]="saving()"
                  class="mt-5 w-full py-3 rounded-xl bg-[#183764] text-white font-bold text-sm hover:opacity-90 transition-opacity disabled:opacity-50">
            {{ saving() ? 'جاري الحفظ...' : 'حفظ التعديلات' }}
          </button>
        </div>
      </div>

      <!-- ═══ كورساتي ═══ -->
      <div *ngIf="activeTab() === 'courses'">
        <h3 class="font-bold text-[#183764] dark:text-white mb-4">كورساتي ({{ enrollments().length }})</h3>
        <div *ngIf="enrollments().length === 0" class="text-center py-12 text-[#8892a0]">
          <span class="material-symbols-outlined" style="font-size:48px;opacity:.3">school</span>
          <p class="mt-2 text-sm">لم تنضم لأي كورس بعد</p>
        </div>
        <div class="space-y-3">
          <div *ngFor="let e of enrollments()"
               class="flex items-center gap-4 p-4 rounded-xl border border-[#DDE1EA] dark:border-slate-800 hover:bg-[#F5F6FA] dark:hover:bg-white/5 transition-colors cursor-pointer"
               [routerLink]="['/courses', e.courseId ?? e.course?.id]">
            <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-[#183764] to-[#4BBBA0] flex items-center justify-center text-white flex-shrink-0">
              <span class="material-symbols-outlined" style="font-size:20px">school</span>
            </div>
            <div class="flex-1 min-w-0">
              <p class="font-bold text-sm text-[#183764] dark:text-white truncate">
                {{ e.course?.title ?? e.courseTitle ?? e.title ?? 'كورس' }}
              </p>
              <p class="text-xs text-[#8892a0] mt-0.5">{{ e.course?.teacherName ?? e.teacherName ?? '' }}</p>
            </div>
            <div class="text-right flex-shrink-0">
              <div class="text-xs font-bold text-[#183764] dark:text-white">{{ progress(e) }}%</div>
              <div class="w-16 h-1.5 bg-[#DDE1EA] dark:bg-slate-700 rounded-full mt-1 overflow-hidden">
                <div class="h-full bg-[#4BBBA0] rounded-full" [style.width]="progress(e) + '%'"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ═══ نتائج الامتحانات ═══ -->
      <div *ngIf="activeTab() === 'quizzes'">
        <h3 class="font-bold text-[#183764] dark:text-white mb-4">نتائج الامتحانات</h3>
        <div *ngIf="loadingQuizzes()" class="flex justify-center py-12">
          <span class="material-symbols-outlined animate-spin text-[#8892a0]">refresh</span>
        </div>
        <div *ngIf="!loadingQuizzes() && quizAttempts().length === 0" class="text-center py-12 text-[#8892a0]">
          <span class="material-symbols-outlined" style="font-size:48px;opacity:.3">quiz</span>
          <p class="mt-2 text-sm">لا توجد نتائج امتحانات بعد</p>
        </div>
        <div class="space-y-3">
          <div *ngFor="let a of quizAttempts()"
               class="flex items-center gap-4 p-4 rounded-xl border border-[#DDE1EA] dark:border-slate-800">
            <div class="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0"
                 [class]="a.passed ? 'bg-green-100 dark:bg-green-500/20' : 'bg-red-100 dark:bg-red-500/20'">
              <span class="material-symbols-outlined" style="font-size:20px"
                    [class]="a.passed ? 'text-green-600' : 'text-red-500'">
                {{ a.passed ? 'check_circle' : 'cancel' }}
              </span>
            </div>
            <div class="flex-1 min-w-0">
              <p class="font-bold text-sm text-[#183764] dark:text-white truncate">{{ a.quizTitle || a.quiz?.title || 'اختبار' }}</p>
              <p class="text-xs text-[#8892a0] mt-0.5">{{ a.submittedAt | date:'shortDate' }}</p>
            </div>
            <div class="text-right flex-shrink-0">
              <span class="text-lg font-black" [class]="a.passed ? 'text-green-600' : 'text-red-500'">
                {{ a.score ?? '—' }} / {{ a.totalMarks ?? '—' }}
              </span>
              <p class="text-xs text-[#8892a0]">{{ a.percentage != null ? (a.percentage | number:'1.0-0') + '%' : '' }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- ═══ نتائج الواجبات ═══ -->
      <div *ngIf="activeTab() === 'assignments'">
        <h3 class="font-bold text-[#183764] dark:text-white mb-4">نتائج الواجبات</h3>
        <div *ngIf="loadingAssignments()" class="flex justify-center py-12">
          <span class="material-symbols-outlined animate-spin text-[#8892a0]">refresh</span>
        </div>
        <div *ngIf="!loadingAssignments() && assignmentAttempts().length === 0" class="text-center py-12 text-[#8892a0]">
          <span class="material-symbols-outlined" style="font-size:48px;opacity:.3">assignment</span>
          <p class="mt-2 text-sm">لا توجد نتائج واجبات بعد</p>
        </div>
        <div class="space-y-3">
          <div *ngFor="let a of assignmentAttempts()"
               class="flex items-center gap-4 p-4 rounded-xl border border-[#DDE1EA] dark:border-slate-800">
            <div class="w-10 h-10 rounded-xl bg-blue-100 dark:bg-blue-500/20 flex items-center justify-center flex-shrink-0">
              <span class="material-symbols-outlined text-blue-600" style="font-size:20px">assignment_turned_in</span>
            </div>
            <div class="flex-1 min-w-0">
              <p class="font-bold text-sm text-[#183764] dark:text-white truncate">{{ a.assignmentTitle || a.assignment?.title || 'واجب' }}</p>
              <p class="text-xs text-[#8892a0] mt-0.5">{{ a.submittedAt | date:'shortDate' }}</p>
            </div>
            <div class="text-right flex-shrink-0">
              <span class="text-lg font-black text-blue-600">{{ a.score ?? '—' }}</span>
              <p class="text-xs text-[#8892a0]">{{ a.status === 'GRADED' ? 'مُصحَّح' : 'قيد المراجعة' }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- ═══ بيانات المحفظة ═══ -->
      <div *ngIf="activeTab() === 'wallet'">
        <h3 class="font-bold text-[#183764] dark:text-white mb-4">بيانات المحفظة</h3>

        <div *ngIf="!wallet()" class="text-center py-12 text-[#8892a0]">
          <span class="material-symbols-outlined" style="font-size:48px;opacity:.3">wallet</span>
          <p class="mt-2 text-sm">لا توجد بيانات محفظة</p>
        </div>

        <div *ngIf="wallet()">
          <div class="grid grid-cols-3 gap-4 mb-6">
            <div class="bg-gradient-to-br from-[#183764] to-[#1a4a8a] rounded-2xl p-5 text-white text-center">
              <p class="text-xs opacity-70 mb-1">الرصيد الحالي</p>
              <p class="text-3xl font-black">{{ wallet()?.balance | number:'1.0-0' }}</p>
              <p class="text-xs opacity-60 mt-0.5">ج.م</p>
            </div>
            <div class="bg-[#F5F6FA] dark:bg-white/5 rounded-2xl p-5 text-center">
              <p class="text-xs text-[#8892a0] mb-1">إجمالي المشحون</p>
              <p class="text-2xl font-black text-[#183764] dark:text-white">{{ wallet()?.totalDeposited | number:'1.0-0' }}</p>
              <p class="text-xs text-[#8892a0] mt-0.5">ج.م</p>
            </div>
            <div class="bg-[#F5F6FA] dark:bg-white/5 rounded-2xl p-5 text-center">
              <p class="text-xs text-[#8892a0] mb-1">إجمالي المصروف</p>
              <p class="text-2xl font-black text-[#183764] dark:text-white">{{ wallet()?.totalSpent | number:'1.0-0' }}</p>
              <p class="text-xs text-[#8892a0] mt-0.5">ج.م</p>
            </div>
          </div>

          <h4 class="font-bold text-sm text-[#183764] dark:text-white mb-3">آخر المعاملات</h4>

          <div *ngIf="loadingTx()" class="flex justify-center py-8">
            <span class="material-symbols-outlined animate-spin text-[#8892a0]">refresh</span>
          </div>

          <div *ngIf="!loadingTx()" class="space-y-2">
            <div *ngFor="let tx of walletTx()"
                 class="flex items-center gap-3 p-3 rounded-xl border border-[#DDE1EA] dark:border-slate-800">
              <div class="w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0"
                   [class]="tx.type === 'DEPOSIT' ? 'bg-green-100 dark:bg-green-500/20' : 'bg-red-100 dark:bg-red-500/20'">
                <span class="material-symbols-outlined" style="font-size:16px"
                      [class]="tx.type === 'DEPOSIT' ? 'text-green-600' : 'text-red-500'">
                  {{ tx.type === 'DEPOSIT' ? 'arrow_downward' : 'arrow_upward' }}
                </span>
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-xs font-semibold text-[#183764] dark:text-white truncate">{{ tx.description || 'معاملة' }}</p>
                <p class="text-[10px] text-[#8892a0]">{{ tx.completedAt || tx.createdAt | date:'short' }}</p>
              </div>
              <span class="font-black text-sm flex-shrink-0"
                    [class]="tx.type === 'DEPOSIT' ? 'text-green-600' : 'text-red-500'">
                {{ tx.type === 'DEPOSIT' ? '+' : '-' }}{{ tx.amount | number:'1.0-0' }} ج.م
              </span>
            </div>
            <div *ngIf="walletTx().length === 0" class="text-center py-8 text-[#8892a0] text-sm">لا توجد معاملات</div>
          </div>
        </div>
      </div>

    </div>
  </div>
</div>
  `
})
export class ProfileComponent implements OnInit {
  loading            = signal(true);
  me                 = signal<any>(null);
  enrollments        = signal<any[]>([]);
  wallet             = signal<any>(null);
  walletTx           = signal<any[]>([]);
  loadingTx          = signal(false);
  quizAttempts       = signal<any[]>([]);
  assignmentAttempts = signal<any[]>([]);
  loadingQuizzes     = signal(false);
  loadingAssignments = signal(false);

  // Edit profile
  editMode    = signal(false);
  saving      = signal(false);
  saveError   = signal('');
  saveSuccess = signal('');
  editForm: any = {};

  activeTab = signal<Tab>('profile');

  tabs = [
    { id: 'profile'     as Tab, label: 'الملف الشخصي',    icon: 'person' },
    { id: 'courses'     as Tab, label: 'كورساتي',          icon: 'school' },
    { id: 'quizzes'     as Tab, label: 'نتائج الامتحانات', icon: 'quiz' },
    { id: 'assignments' as Tab, label: 'نتائج الواجبات',   icon: 'assignment' },
    { id: 'wallet'      as Tab, label: 'بيانات المحفظة',   icon: 'wallet' },
  ];

  constructor(private api: StudentApiService, public router: Router, private route: ActivatedRoute) {}

  ngOnInit() {
    const tabParam = this.route.snapshot.queryParamMap.get('tab') as Tab | null;
    if (tabParam && ['profile','courses','quizzes','assignments','wallet'].includes(tabParam)) {
      this.activeTab.set(tabParam);
    }
    forkJoin({
      me:          this.api.getMe().pipe(catchError(() => of(null))),
      enrollments: this.api.getMyEnrollments().pipe(catchError(() => of([]))),
      wallet:      this.api.getMyWallet().pipe(catchError(() => of(null))),
    }).subscribe(({ me, enrollments, wallet }) => {
      this.me.set(me);
      this.enrollments.set(Array.isArray(enrollments) ? enrollments : []);
      this.wallet.set(wallet);
      this.loading.set(false);
    });
  }

  loadTab(tab: Tab) {
    this.activeTab.set(tab);
    this.editMode.set(false);

    const studentId = this.me()?.id;

    if (tab === 'wallet' && this.walletTx().length === 0) {
      this.loadingTx.set(true);
      this.api.getMyWalletTransactions().pipe(catchError(() => of([]))).subscribe((txs: any) => {
        const list = Array.isArray(txs) ? txs : (txs?.content ?? []);
        this.walletTx.set(list);
        this.loadingTx.set(false);
      });
    }
    if (tab === 'quizzes' && this.quizAttempts().length === 0 && studentId) {
      this.loadingQuizzes.set(true);
      this.api.getMyQuizAttempts(studentId).pipe(catchError(() => of([]))).subscribe((data: any) => {
        this.quizAttempts.set(Array.isArray(data) ? data : (data?.content ?? []));
        this.loadingQuizzes.set(false);
      });
    }
    if (tab === 'assignments' && this.assignmentAttempts().length === 0 && studentId) {
      this.loadingAssignments.set(true);
      this.api.getMyAssignmentAttempts(studentId).pipe(catchError(() => of([]))).subscribe((data: any) => {
        this.assignmentAttempts.set(Array.isArray(data) ? data : (data?.content ?? []));
        this.loadingAssignments.set(false);
      });
    }
  }

  startEdit() {
    const me = this.me();
    // Pre-fill names from fullName split if firstName not separately stored
    const parts = (me?.fullName || me?.name || '').split(' ');
    this.editForm = {
      firstName:  me?.firstName  || parts[0] || '',
      secondName: me?.secondName || parts[1] || '',
      thirdName:  me?.thirdName  || parts[2] || '',
      fourthName: me?.fourthName || parts[3] || '',
      grade:      me?.grade      || '',
      governorate: me?.governorate || '',
      area:       me?.area       || '',
      schoolName: me?.schoolName || '',
      centerName: me?.centerName || me?.center?.name || '',
      online:     me?.online     ?? null,
    };
    this.saveError.set('');
    this.saveSuccess.set('');
    this.editMode.set(true);
  }

  saveProfile() {
    this.saving.set(true);
    this.saveError.set('');
    this.saveSuccess.set('');
    this.api.updateProfile(this.editForm).subscribe({
      next: (updated: any) => {
        this.saving.set(false);
        this.saveSuccess.set('تم حفظ البيانات بنجاح ✓');
        // Refresh me data
        const newMe = updated ?? this.me();
        this.me.set({ ...this.me(), ...newMe });
        setTimeout(() => { this.editMode.set(false); this.saveSuccess.set(''); }, 1500);
      },
      error: (err: any) => {
        this.saving.set(false);
        this.saveError.set(err?.error?.message || 'فشل في حفظ البيانات');
      }
    });
  }

  qrUrl = computed(() => {
    const code = this.me()?.studentCode;
    if (!code) return '';
    return `https://api.qrserver.com/v1/create-qr-code/?size=160x160&data=${encodeURIComponent(code)}&bgcolor=ffffff&color=183764&margin=4`;
  });

  get initial(): string {
    const n = this.me()?.fullName || this.me()?.name || 'ط';
    return n[0] || 'ط';
  }

  get walletBalance(): string {
    const w = this.wallet();
    return w ? (w.balance ?? 0).toLocaleString('ar-EG') : '0';
  }

  progress(e: any): number {
    return Math.min(100, Math.round(e?.progress ?? e?.completionPercentage ?? 0));
  }
}
