import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { Staff } from '../../models/models';
import { ToastrService } from 'ngx-toastr';

// ── Mirror of the sidebar — each entry = one permission key ──────────────
const PERMISSION_GROUPS = [
  {
    section: 'السنوات الدراسية',
    icon: 'school',
    items: [
      { key: 'LEVELS_VIEW',       label: 'السنوات الدراسية',    icon: 'school' },
      { key: 'CATEGORIES_VIEW',   label: 'التصنيفات',            icon: 'label' },
      { key: 'COURSE_ADD',        label: 'إضافة كورس',           icon: 'add_circle' },
      { key: 'LEVEL_STUDENTS',    label: 'طلاب السنة الدراسية',  icon: 'groups' },
      { key: 'LEVEL_OVERVIEW',    label: 'نظرة عامة على السنة',  icon: 'bar_chart' },
    ]
  },
  {
    section: 'التقييم والنشاط',
    icon: 'quiz',
    items: [
      { key: 'QUIZZES',           label: 'بنك الاختبارات',       icon: 'quiz' },
      { key: 'ASSIGNMENTS',       label: 'المهام والواجبات',     icon: 'assignment_turned_in' },
      { key: 'QUESTION_BANK',     label: 'مخزن الأسئلة',         icon: 'storage' },
    ]
  },
  {
    section: 'شؤون الطلاب',
    icon: 'groups',
    items: [
      { key: 'NEW_REQUESTS',      label: 'طلبات التسجيل',        icon: 'assignment_ind' },
      { key: 'STUDENTS_MANAGE',   label: 'إدارة الطلاب',         icon: 'groups' },
      { key: 'ATTENDANCE',        label: 'سجل الحضور',           icon: 'verified_user' },
    ]
  },
  {
    section: 'الأدوات والمالية',
    icon: 'account_balance_wallet',
    items: [
      { key: 'WALLET',            label: 'المحفظة المالية',      icon: 'account_balance_wallet' },
      { key: 'WALLET_HISTORY',    label: 'سجل المحافظ',          icon: 'history' },
      { key: 'COUPONS',           label: 'الكوبونات والخصم',     icon: 'confirmation_number' },
      { key: 'CREATE_CODES',      label: 'توليد أكواد',          icon: 'key' },
      { key: 'CODES_LIST',        label: 'كل الأكواد',           icon: 'list_alt' },
    ]
  },
  {
    section: 'السنتر والمتجر',
    icon: 'corporate_fare',
    items: [
      { key: 'CENTER_SCHEDULE',   label: 'جدول السناتر',                  icon: 'calendar_month' },
      { key: 'BOOKS_CODES',       label: 'أماكن بيع الكتب والأكواد',     icon: 'storefront' },
      { key: 'HOME_LAYOUT',       label: 'تخصيص الهوم',                  icon: 'dashboard_customize' },
      { key: 'SUPPORT_CHANNELS',  label: 'قنوات الدعم',                   icon: 'support_agent' },
      { key: 'TOPIC_TREE',        label: 'شجرة المحتوى',                  icon: 'account_tree' },
      { key: 'TOPIC_ANALYTICS',   label: 'تحليل نقاط الضعف',             icon: 'analytics' },
    ]
  },
  {
    section: 'إدارة النظام',
    icon: 'admin_panel_settings',
    items: [
      { key: 'ACTIVITY_LOGS',     label: 'سجل النشاطات',        icon: 'manage_history' },
      { key: 'STAFF_MANAGE',      label: 'فريق العمل',           icon: 'admin_panel_settings' },
      { key: 'CENTERS_MANAGE',    label: 'السناتر والفروع',      icon: 'corporate_fare' },
      { key: 'BANNERS',           label: 'البانرات الإعلانية',   icon: 'campaign' },
    ]
  },
];

@Component({
  selector: 'app-staff',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6 animate-fade-in pb-10" dir="rtl">

      <!-- Header -->
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-white font-bold text-2xl tracking-tight">إدارة فريق العمل</h2>
          <p class="text-slate-500 text-sm mt-1">إضافة موظفين وتحديد صلاحياتهم في المنصة</p>
        </div>
        <button (click)="openStaffModal()" class="btn-primary">
          <span class="material-icons-round">person_add</span>
          عضو جديد
        </button>
      </div>

      <!-- Staff Table -->
      <div class="edu-card p-0 overflow-hidden shadow-xl">
        <table class="edu-table">
          <thead>
            <tr>
              <th>الموظف</th>
              <th>رقم الهاتف</th>
              <th>الصلاحيات</th>
              <th>الحالة</th>
              <th class="text-center">الإجراءات</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let s of staffList()">
              <td>
                <div class="flex items-center gap-3">
                  <div class="w-9 h-9 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-xs font-black text-white">
                    {{ s.fullName ? s.fullName[0] : '?' }}
                  </div>
                  <div class="flex flex-col">
                    <span class="text-slate-200 font-bold text-xs">{{ s.fullName }}</span>
                    <span class="text-[10px] text-slate-500 italic">{{ s.notes || 'لا توجد ملاحظات' }}</span>
                  </div>
                </div>
              </td>
              <td class="text-xs text-right text-slate-400" dir="ltr">{{ s.phone }}</td>
              <td>
                <div class="flex flex-wrap gap-1 max-w-[280px]">
                  <span *ngFor="let p of (s.permissions || []).slice(0, 3)"
                        class="text-[9px] bg-indigo-500/10 text-indigo-400 px-2 py-0.5 rounded border border-indigo-500/20">
                    {{ labelOf(p) }}
                  </span>
                  <span *ngIf="(s.permissions.length || 0) > 3"
                        class="text-[9px] bg-slate-800 text-slate-500 px-2 py-0.5 rounded">
                    +{{ (s.permissions.length || 0) - 3 }} أخرى
                  </span>
                  <span *ngIf="!s.permissions.length" class="text-[9px] text-slate-600 italic">لا صلاحيات</span>
                </div>
              </td>
              <td>
                <button (click)="toggleStatus(s)"
                  [class.text-emerald-400]="s.status === 'ACTIVE'"
                  [class.text-slate-600]="s.status !== 'ACTIVE'"
                  class="flex items-center gap-2 text-xs font-medium hover:opacity-80 transition-opacity">
                  <span class="w-1.5 h-1.5 rounded-full"
                        [class.bg-emerald-500]="s.status === 'ACTIVE'"
                        [class.bg-slate-700]="s.status !== 'ACTIVE'"></span>
                  {{ s.status === 'ACTIVE' ? 'نشط' : 'معطل' }}
                </button>
              </td>
              <td>
                <div class="flex items-center justify-center gap-2">
                  <button (click)="openStaffModal(s)" class="btn-icon text-indigo-400" title="تعديل">
                    <span class="material-icons-round text-sm">edit</span>
                  </button>
                  <button (click)="deleteStaff(s.id)" class="btn-icon text-red-400" title="حذف">
                    <span class="material-icons-round text-sm">delete</span>
                  </button>
                </div>
              </td>
            </tr>
            <tr *ngIf="staffList().length === 0">
              <td colspan="5" class="text-center py-20 text-slate-500 italic">لا يوجد موظفون مضافون بعد</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- ══ Modal ══ -->
      <div *ngIf="showModal()" class="modal-overlay">
        <div class="modal-box max-w-2xl flex flex-col" style="height:90vh">

          <div class="modal-header">
            <h3 class="text-white font-bold">{{ editingStaff() ? 'تعديل بيانات عضو' : 'إضافة عضو جديد' }}</h3>
            <button (click)="showModal.set(false)" class="btn-icon">
              <span class="material-icons-round">close</span>
            </button>
          </div>

          <div class="modal-body flex-1 overflow-y-auto custom-scrollbar p-6 space-y-6">

            <!-- Basic info -->
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="edu-label">الاسم الكامل *</label>
                <input type="text" [(ngModel)]="staffForm.fullName" class="edu-input" placeholder="اسم الموظف">
              </div>
              <div>
                <label class="edu-label">رقم الهاتف *</label>
                <input type="tel" [(ngModel)]="staffForm.phone" class="edu-input" dir="ltr" placeholder="01xxxxxxxxx">
              </div>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="edu-label">كلمة المرور {{ editingStaff() ? '(اتركها فارغة للإبقاء)' : '*' }}</label>
                <input type="password" [(ngModel)]="staffForm.password" class="edu-input" placeholder="••••••••">
              </div>
              <div>
                <label class="edu-label">ملاحظات</label>
                <input type="text" [(ngModel)]="staffForm.notes" class="edu-input" placeholder="مثال: سكرتيرة السنتر">
              </div>
            </div>

            <!-- Permissions -->
            <div class="border-t border-slate-800 pt-5 space-y-4">
              <div class="flex items-center justify-between">
                <h4 class="text-slate-200 font-black text-sm">الصلاحيات الممنوحة</h4>
                <div class="flex gap-2">
                  <button (click)="selectAll()" class="text-[10px] px-2 py-1 rounded-lg bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 hover:bg-indigo-500/20 transition-colors">
                    تحديد الكل
                  </button>
                  <button (click)="clearAll()" class="text-[10px] px-2 py-1 rounded-lg bg-slate-800 text-slate-500 border border-slate-700 hover:bg-slate-700 transition-colors">
                    إلغاء الكل
                  </button>
                </div>
              </div>

              <!-- Permission groups -->
              <div *ngFor="let group of permGroups" class="space-y-2">
                <!-- Group header -->
                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-2">
                    <span class="material-icons-round text-slate-500 text-sm">{{ group.icon }}</span>
                    <span class="text-[11px] text-slate-400 font-bold uppercase tracking-wider">{{ group.section }}</span>
                  </div>
                  <button (click)="toggleGroup(group)"
                    class="text-[9px] px-2 py-0.5 rounded bg-slate-800 text-slate-500 border border-slate-700 hover:text-slate-300 transition-colors">
                    {{ groupAllSelected(group) ? 'إلغاء المجموعة' : 'تحديد المجموعة' }}
                  </button>
                </div>

                <!-- Items grid -->
                <div class="grid grid-cols-2 md:grid-cols-3 gap-2">
                  <label *ngFor="let item of group.items"
                    class="flex items-center gap-2.5 p-3 rounded-xl border cursor-pointer transition-all"
                    [ngClass]="hasPermission(item.key)
                      ? 'bg-indigo-600/10 border-indigo-500/40'
                      : 'bg-slate-800/40 border-slate-700/50'">
                    <input type="checkbox" [checked]="hasPermission(item.key)" (change)="togglePermission(item.key)" class="w-3.5 h-3.5 accent-indigo-500 shrink-0">
                    <span class="material-icons-round text-sm shrink-0"
                          [class.text-indigo-400]="hasPermission(item.key)"
                          [class.text-slate-600]="!hasPermission(item.key)">{{ item.icon }}</span>
                    <span class="text-[10px] leading-tight"
                          [class.text-slate-200]="hasPermission(item.key)"
                          [class.text-slate-500]="!hasPermission(item.key)">{{ item.label }}</span>
                  </label>
                </div>
              </div>
            </div>

            <!-- Summary -->
            <div class="rounded-xl bg-slate-800/50 border border-slate-700 p-3 flex items-center justify-between">
              <span class="text-xs text-slate-400">الصلاحيات المختارة</span>
              <span class="text-sm font-black text-indigo-400">{{ staffForm.permissions.length }} / {{ totalPermCount }}</span>
            </div>

          </div>

          <div class="modal-footer">
            <button (click)="showModal.set(false)" class="btn-secondary">إلغاء</button>
            <button (click)="saveStaff()" [disabled]="!staffForm.fullName || !staffForm.phone" class="btn-primary px-10">
              حفظ العضو
            </button>
          </div>

        </div>
      </div>

    </div>
  `
})
export class StaffComponent implements OnInit {

  staffList    = signal<Staff[]>([]);
  showModal    = signal(false);
  editingStaff = signal<Staff | null>(null);

  permGroups   = PERMISSION_GROUPS;
  totalPermCount = PERMISSION_GROUPS.reduce((s, g) => s + g.items.length, 0);

  staffForm: any = { fullName: '', phone: '', password: '', notes: '', permissions: [] };

  // label lookup map
  private labelMap: Record<string, string> = {};

  constructor(private api: ApiService, private toastr: ToastrService) {
    PERMISSION_GROUPS.forEach(g => g.items.forEach(i => (this.labelMap[i.key] = i.label)));
  }

  ngOnInit() { this.loadStaff(); }

  loadStaff() {
    this.api.getStaff().subscribe({
      next: list => this.staffList.set(list),
      error: ()  => this.staffList.set([])
    });
  }

  openStaffModal(staff?: Staff) {
    this.editingStaff.set(staff ?? null);
    this.staffForm = staff
      ? { fullName: staff.fullName, phone: staff.phone, password: '', notes: staff.notes || '', permissions: [...(staff.permissions || [])] }
      : { fullName: '', phone: '', password: '', notes: '', permissions: [] };
    this.showModal.set(true);
  }

  labelOf(key: string) { return this.labelMap[key] ?? key; }

  hasPermission(key: string)  { return this.staffForm.permissions.includes(key); }

  togglePermission(key: string) {
    const i = this.staffForm.permissions.indexOf(key);
    if (i > -1) this.staffForm.permissions.splice(i, 1);
    else this.staffForm.permissions.push(key);
  }

  groupAllSelected(group: typeof PERMISSION_GROUPS[0]) {
    return group.items.every(i => this.hasPermission(i.key));
  }

  toggleGroup(group: typeof PERMISSION_GROUPS[0]) {
    if (this.groupAllSelected(group)) {
      group.items.forEach(i => {
        const idx = this.staffForm.permissions.indexOf(i.key);
        if (idx > -1) this.staffForm.permissions.splice(idx, 1);
      });
    } else {
      group.items.forEach(i => { if (!this.hasPermission(i.key)) this.staffForm.permissions.push(i.key); });
    }
  }

  selectAll() {
    this.staffForm.permissions = PERMISSION_GROUPS.flatMap(g => g.items.map(i => i.key));
  }

  clearAll() { this.staffForm.permissions = []; }

  saveStaff() {
    const editing = this.editingStaff();
    const perms: string[] = this.staffForm.permissions;

    if (editing) {
      // عند التعديل: حدّث البيانات الأساسية أولاً ثم الصلاحيات
      const updateBody: any = { fullName: this.staffForm.fullName, notes: this.staffForm.notes, active: true };
      if (this.staffForm.password) updateBody['newPassword'] = this.staffForm.password;

      this.api.updateStaff(editing.id, updateBody).subscribe({
        next: () => {
          this.api.updateStaffPermissions(editing.id, perms).subscribe({
            next: () => { this.toastr.success('تم الحفظ بنجاح'); this.showModal.set(false); this.loadStaff(); },
            error: ()  => this.toastr.error('فشل في حفظ الصلاحيات')
          });
        },
        error: () => this.toastr.error('فشل في حفظ البيانات')
      });
    } else {
      // إنشاء موظف جديد — الصلاحيات بتتبعت مع البيانات
      this.api.createStaff(this.staffForm).subscribe({
        next: () => { this.toastr.success('تم الحفظ بنجاح'); this.showModal.set(false); this.loadStaff(); },
        error: (err) => {
          console.error('createStaff body:', JSON.stringify(err?.error));
          const msg = err?.error?.message || err?.error?.error || err?.message || 'فشل في حفظ البيانات';
          this.toastr.error(msg);
        }
      });
    }
  }

  toggleStatus(s: Staff) {
    this.api.toggleStaffStatus(s.id).subscribe({ next: () => this.loadStaff() });
  }

  deleteStaff(id: number) {
    if (!confirm('هل أنت متأكد من حذف هذا الموظف؟')) return;
    this.api.deleteStaff(id).subscribe({
      next: () => { this.toastr.success('تم الحذف'); this.loadStaff(); }
    });
  }
}
