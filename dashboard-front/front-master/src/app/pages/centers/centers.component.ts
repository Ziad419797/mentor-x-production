import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../services/api.service';
import { Center } from '../../models/models';
import { ToastrService } from 'ngx-toastr';

const API = 'http://localhost:8081';

@Component({
  selector: 'app-centers',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6 animate-fade-in pb-10">

      <!-- Header -->
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-white font-bold text-2xl">المراكز (Centers)</h2>
          <p class="text-slate-500 text-sm mt-1">إدارة بيانات الفروع والسناتر المعتمدة</p>
        </div>
        <button (click)="openCenterModal()" class="btn-primary">
          <span class="material-icons-round">business</span>
          مركز جديد
        </button>
      </div>

      <!-- Centers List -->
      <div class="grid grid-cols-1 gap-6">

        <div *ngFor="let c of centers()" class="edu-card border-slate-800 overflow-hidden">

          <!-- Center Header Row -->
          <div class="p-6 flex items-center gap-4">
            <div class="w-12 h-12 rounded-2xl bg-indigo-500/10 flex items-center justify-center text-indigo-400 flex-shrink-0">
              <span class="material-icons-round text-2xl">storefront</span>
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <h3 class="text-white font-bold text-lg">{{ c.name }}</h3>
                <span [class.badge-success]="c.active" [class.badge-gray]="!c.active">{{ c.active ? 'نشط' : 'معطل' }}</span>
                <span *ngIf="c.sellsBooks" class="badge-info text-[10px]">📚 يبيع كتب</span>
                <span *ngIf="c.sellsCodes" class="badge-warning text-[10px]">🔑 يبيع أكواد</span>
              </div>
              <div class="flex items-center gap-4 text-slate-500 text-xs mt-1 flex-wrap">
                <span class="flex items-center gap-1"><span class="material-icons-round text-sm">location_on</span>{{ c.governorate }}{{ c.area ? ' — ' + c.area : '' }}</span>
                <span *ngIf="c.phone" class="flex items-center gap-1 ltr"><span class="material-icons-round text-sm">phone</span>{{ c.phone }}</span>
                <span *ngIf="c.address" class="flex items-center gap-1"><span class="material-icons-round text-sm">map</span>{{ c.address }}</span>
              </div>
            </div>
            <div class="flex items-center gap-2 flex-shrink-0">
              <a *ngIf="c.mapsLink" [href]="c.mapsLink" target="_blank" class="btn-icon h-9 w-9 bg-slate-800 text-emerald-400">
                <span class="material-icons-round text-sm">directions</span>
              </a>
              <button (click)="openCenterModal(c)" class="btn-icon h-9 w-9 bg-slate-800 text-indigo-400">
                <span class="material-icons-round text-sm">edit</span>
              </button>
              <button (click)="deleteCenter(c.id)" class="btn-icon h-9 w-9 bg-slate-800 text-red-400">
                <span class="material-icons-round text-sm">delete</span>
              </button>
              <button (click)="toggleGroups(c.id)" class="btn-icon h-9 w-9 bg-slate-800 text-amber-400">
                <span class="material-icons-round text-sm">{{ expandedCenterId() === c.id ? 'expand_less' : 'groups' }}</span>
              </button>
            </div>
          </div>

          <!-- Groups Panel (expanded) -->
          <div *ngIf="expandedCenterId() === c.id" class="border-t border-slate-800 bg-slate-900/40">
            <div class="flex items-center justify-between px-6 py-4">
              <h4 class="text-slate-300 font-semibold flex items-center gap-2">
                <span class="material-icons-round text-amber-400 text-lg">schedule</span>
                مجموعات الحضور (المواعيد)
              </h4>
              <button (click)="openGroupModal(c.id)" class="btn-secondary text-xs h-8 px-4">
                <span class="material-icons-round text-sm">add</span>
                إضافة ميعاد
              </button>
            </div>
            <div *ngIf="groupsLoading()" class="px-6 pb-6 text-center text-slate-500 text-sm">
              <span class="material-icons-round animate-spin text-2xl">refresh</span>
            </div>
            <div *ngIf="!groupsLoading()" class="px-6 pb-6">
              <div *ngIf="centerGroups().length === 0" class="text-center py-8 text-slate-600 border border-dashed border-slate-700 rounded-2xl">
                <span class="material-icons-round text-4xl opacity-30">event_busy</span>
                <p class="text-sm mt-2">لا توجد مواعيد لهذا المركز بعد</p>
              </div>
              <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
                <div *ngFor="let g of centerGroups()" class="bg-slate-800/60 rounded-2xl p-4 flex items-start gap-3 border border-slate-700/50">
                  <div class="w-9 h-9 rounded-xl bg-amber-500/10 flex items-center justify-center text-amber-400 flex-shrink-0">
                    <span class="material-icons-round text-lg">schedule</span>
                  </div>
                  <div class="flex-1 min-w-0">
                    <p class="text-white font-semibold text-sm">{{ g.dayOfWeek || '' }} {{ g.meetingTime ? formatTime(g.meetingTime) : '' }}</p>
                    <p *ngIf="g.description" class="text-slate-500 text-xs mt-0.5">{{ g.description }}</p>
                    <p class="text-slate-600 text-xs mt-1">{{ getLevelName(g.levelId) }}</p>
                    <p *ngIf="g.maxCapacity" class="text-xs mt-1"
                       [ngClass]="g.membersCount >= g.maxCapacity ? 'text-red-400' : 'text-emerald-400'">
                      {{ g.membersCount }}/{{ g.maxCapacity }} طالب
                      {{ g.membersCount >= g.maxCapacity ? '(مكتمل)' : '' }}
                    </p>
                  </div>
                  <div class="flex gap-1 flex-shrink-0">
                    <button (click)="editGroup(g)" class="btn-icon h-7 w-7 bg-slate-700 text-indigo-400">
                      <span class="material-icons-round text-xs">edit</span>
                    </button>
                    <button (click)="deleteGroup(g.id)" class="btn-icon h-7 w-7 bg-slate-700 text-red-400">
                      <span class="material-icons-round text-xs">delete</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

        </div>

        <!-- Empty -->
        <div *ngIf="centers().length === 0"
             class="col-span-full py-24 text-center text-slate-600 border-2 border-dashed border-slate-800 rounded-3xl">
          <span class="material-icons-round text-6xl mb-4 opacity-10">apartment</span>
          <h3 class="text-slate-400 font-bold">لا توجد مراكز مضافة</h3>
          <button (click)="openCenterModal()" class="btn-secondary mt-4 text-xs">إضافة أول مركز</button>
        </div>

      </div>

      <!-- Center Modal -->
      <div *ngIf="showCenterModal()" class="modal-overlay">
        <div class="modal-box max-w-2xl">
          <div class="modal-header">
            <h3 class="text-white font-bold">{{ editingCenter() ? 'تعديل مركز' : 'إضافة مركز جديد' }}</h3>
            <button (click)="showCenterModal.set(false)" class="btn-icon"><span class="material-icons-round">close</span></button>
          </div>
          <div class="modal-body space-y-4 p-6">

            <!-- Name + Phone -->
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="edu-label">اسم المركز <span class="text-red-400">*</span></label>
                <input type="text" [(ngModel)]="centerForm.name" class="edu-input" placeholder="مثال: سنتر السويس التعليمي">
              </div>
              <div>
                <label class="edu-label">رقم الهاتف</label>
                <input type="tel" [(ngModel)]="centerForm.phone" class="edu-input ltr text-right" placeholder="01xxxxxxxxx">
              </div>
            </div>

            <!-- Governorate + Area -->
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="edu-label">المحافظة <span class="text-red-400">*</span></label>
                <select [(ngModel)]="centerForm.governorate" (ngModelChange)="onGovChange()" class="edu-select">
                  <option value="">{{ governorates().length ? 'اختر المحافظة' : 'جاري التحميل...' }}</option>
                  <option *ngFor="let g of governorates()" [value]="g">{{ g }}</option>
                </select>
              </div>
              <div>
                <label class="edu-label">المنطقة / الحي</label>
                <select [(ngModel)]="centerForm.area" class="edu-select" [disabled]="!areas().length">
                  <option value="">{{ areasLoading() ? 'جاري التحميل...' : 'اختر المنطقة' }}</option>
                  <option *ngFor="let a of areas()" [value]="a">{{ a }}</option>
                </select>
              </div>
            </div>

            <!-- Address -->
            <div>
              <label class="edu-label">العنوان التفصيلي</label>
              <input type="text" [(ngModel)]="centerForm.address" class="edu-input" placeholder="الشارع، الدور، العلامة المميزة">
            </div>

            <!-- Google Maps Link -->
            <div>
              <label class="edu-label">رابط خرائط جوجل</label>
              <input type="url" [(ngModel)]="centerForm.mapsLink" class="edu-input ltr" placeholder="https://maps.google.com/...">
            </div>

            <!-- Services -->
            <div class="flex items-center gap-6 pt-2">
              <label class="flex items-center gap-2 cursor-pointer">
                <input type="checkbox" [(ngModel)]="centerForm.sellsBooks" class="w-4 h-4 accent-indigo-500">
                <span class="text-slate-300 text-sm">يبيع كتب</span>
              </label>
              <label class="flex items-center gap-2 cursor-pointer">
                <input type="checkbox" [(ngModel)]="centerForm.sellsCodes" class="w-4 h-4 accent-indigo-500">
                <span class="text-slate-300 text-sm">يبيع أكواد دخول</span>
              </label>
            </div>

          </div>
          <div class="modal-footer">
            <button (click)="showCenterModal.set(false)" class="btn-secondary">إلغاء</button>
            <button (click)="saveCenter()" [disabled]="!centerForm.name || !centerForm.governorate" class="btn-primary px-10">حفظ المركز</button>
          </div>
        </div>
      </div>

      <!-- Group Modal -->
      <div *ngIf="showGroupModal()" class="modal-overlay">
        <div class="modal-box max-w-md">
          <div class="modal-header">
            <h3 class="text-white font-bold">{{ editingGroup ? 'تعديل ميعاد' : 'إضافة ميعاد جديد' }}</h3>
            <button (click)="showGroupModal.set(false)" class="btn-icon"><span class="material-icons-round">close</span></button>
          </div>
          <div class="modal-body space-y-4 p-6">

            <!-- Preview -->
            <div *ngIf="groupForm.dayOfWeek && groupForm.meetingTime"
                 class="bg-indigo-500/10 border border-indigo-500/30 rounded-xl px-4 py-3 text-center">
              <span class="text-indigo-300 font-bold text-lg">{{ groupForm.dayOfWeek }} - {{ formatTime(groupForm.meetingTime) }}</span>
            </div>

            <!-- Day + Time -->
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="edu-label">يوم الأسبوع <span class="text-red-400">*</span></label>
                <select [(ngModel)]="groupForm.dayOfWeek" class="edu-select">
                  <option value="">اختر اليوم</option>
                  <option *ngFor="let d of weekDays" [value]="d">{{ d }}</option>
                </select>
              </div>
              <div>
                <label class="edu-label">الميعاد (الوقت) <span class="text-red-400">*</span></label>
                <input type="time" [(ngModel)]="groupForm.meetingTime" class="edu-input ltr">
              </div>
            </div>

            <!-- Level — required -->
            <div>
              <label class="edu-label">الصف الدراسي <span class="text-red-400">*</span></label>
              <select [(ngModel)]="groupForm.levelId" class="edu-select">
                <option [ngValue]="null">اختر الصف</option>
                <option *ngFor="let l of levels()" [ngValue]="l.id">{{ l.name }}</option>
              </select>
              <p *ngIf="groupFormSubmitted && !groupForm.levelId" class="text-red-400 text-xs mt-1">الصف الدراسي مطلوب</p>
            </div>

            <!-- Description -->
            <div>
              <label class="edu-label">وصف إضافي (اختياري)</label>
              <input type="text" [(ngModel)]="groupForm.description" class="edu-input" placeholder="مثال: قاعة 3 — الدور الثاني">
            </div>

            <!-- Max capacity -->
            <div>
              <label class="edu-label">الحد الأقصى للطلاب (اختياري)</label>
              <input type="number" [(ngModel)]="groupForm.maxCapacity" min="1" class="edu-input" placeholder="اتركه فارغاً = بدون حد">
            </div>

          </div>
          <div class="modal-footer">
            <button (click)="showGroupModal.set(false)" class="btn-secondary">إلغاء</button>
            <button (click)="saveGroup()" [disabled]="!groupForm.dayOfWeek || !groupForm.meetingTime || !groupForm.levelId" class="btn-primary px-10">حفظ الميعاد</button>
          </div>
        </div>
      </div>

    </div>
  `
})
export class CentersComponent implements OnInit {

  centers          = signal<Center[]>([]);
  showCenterModal  = signal(false);
  editingCenter    = signal<Center | null>(null);

  governorates     = signal<string[]>([]);
  areas            = signal<string[]>([]);
  areasLoading     = signal(false);

  expandedCenterId = signal<number | null>(null);
  centerGroups     = signal<any[]>([]);
  groupsLoading    = signal(false);

  showGroupModal   = signal(false);
  editingGroup: any = null;
  groupModalCenterId: number | null = null;

  levels           = signal<any[]>([]);

  centerForm: any  = { name: '', phone: '', governorate: '', area: '', address: '', mapsLink: '', sellsBooks: false, sellsCodes: false, active: true };
  groupForm: any   = { dayOfWeek: '', meetingTime: '', description: '', levelId: null, maxCapacity: null };
  groupFormSubmitted = false;

  readonly weekDays = ['السبت', 'الأحد', 'الاثنين', 'الثلاثاء', 'الأربعاء', 'الخميس', 'الجمعة'];

  constructor(private api: ApiService, private http: HttpClient, private toastr: ToastrService) {}

  ngOnInit(): void {
    this.loadCenters();
    this.loadLevels();
    this.loadGovernorates();
  }

  loadCenters() {
    this.api.getCenters().subscribe({
      next: (res: any) => this.centers.set(res.data || res || []),
      error: () => this.toastr.error('فشل تحميل المراكز')
    });
  }

  loadLevels() {
    this.api.getLevels().subscribe({
      next: (data: any) => this.levels.set(Array.isArray(data) ? data : (data?.data || [])),
      error: () => {}
    });
  }

  loadGovernorates() {
    this.http.get<string[]>(`${API}/api/student/register/governorates`).subscribe({
      next: (data: any) => this.governorates.set(Array.isArray(data) ? data : []),
      error: () => {}
    });
  }

  onGovChange() {
    this.areas.set([]);
    this.centerForm.area = '';
    if (!this.centerForm.governorate) return;
    this.areasLoading.set(true);
    this.http.get<string[]>(`${API}/api/student/register/areas/${encodeURIComponent(this.centerForm.governorate)}`).subscribe({
      next: (data: any) => { this.areas.set(Array.isArray(data) ? data : []); this.areasLoading.set(false); },
      error: () => this.areasLoading.set(false)
    });
  }

  getLevelName(levelId: number | null): string {
    if (!levelId) return '';
    const l = this.levels().find(x => x.id === levelId);
    return l ? l.name : '';
  }

  /** يحوّل HH:mm إلى تنسيق 12 ساعة بالعربية — مثال: "10:30 ص" */
  formatTime(time: string): string {
    if (!time) return '';
    try {
      const [h, m] = time.split(':').map(Number);
      const period = h < 12 ? 'ص' : 'م';
      const hour   = h === 0 ? 12 : h > 12 ? h - 12 : h;
      return `${hour}:${String(m).padStart(2, '0')} ${period}`;
    } catch { return time; }
  }

  // ─── Centers ────────────────────────────────────────────────

  openCenterModal(c?: Center) {
    if (c) {
      this.editingCenter.set(c);
      this.centerForm = { ...c };
      if ((c as any).governorate) {
        this.areasLoading.set(true);
        this.http.get<string[]>(`${API}/api/student/register/areas/${encodeURIComponent((c as any).governorate)}`).subscribe({
          next: (data: any) => { this.areas.set(Array.isArray(data) ? data : []); this.areasLoading.set(false); },
          error: () => this.areasLoading.set(false)
        });
      }
    } else {
      this.editingCenter.set(null);
      this.areas.set([]);
      this.centerForm = { name: '', phone: '', governorate: '', area: '', address: '', mapsLink: '', sellsBooks: false, sellsCodes: false, active: true };
    }
    this.showCenterModal.set(true);
  }

  saveCenter() {
    const obs = this.editingCenter()
      ? this.api.updateCenter(this.editingCenter()!.id, this.centerForm)
      : this.api.createCenter(this.centerForm);
    obs.subscribe({
      next: () => { this.toastr.success('تم الحفظ بنجاح'); this.showCenterModal.set(false); this.loadCenters(); },
      error: () => this.toastr.error('حدث خطأ أثناء الحفظ')
    });
  }

  deleteCenter(id: number) {
    if (!confirm('هل أنت متأكد من حذف هذا المركز؟')) return;
    this.api.deleteCenter(id).subscribe({
      next: () => { this.toastr.success('تم الحذف بنجاح'); this.loadCenters(); },
      error: () => this.toastr.error('حدث خطأ أثناء الحذف')
    });
  }

  // ─── Groups ─────────────────────────────────────────────────

  toggleGroups(centerId: number) {
    if (this.expandedCenterId() === centerId) { this.expandedCenterId.set(null); return; }
    this.expandedCenterId.set(centerId);
    this.loadGroupsForCenter(centerId);
  }

  loadGroupsForCenter(centerId: number) {
    this.groupsLoading.set(true);
    this.api.getMyGroups().subscribe({
      next: (data: any) => {
        const all = Array.isArray(data) ? data : (data?.data || []);
        this.centerGroups.set(all.filter((g: any) => g.centerId === centerId || g.center?.id === centerId));
        this.groupsLoading.set(false);
      },
      error: () => { this.centerGroups.set([]); this.groupsLoading.set(false); }
    });
  }

  openGroupModal(centerId: number) {
    this.editingGroup = null;
    this.groupForm = { dayOfWeek: '', meetingTime: '', description: '', levelId: null, maxCapacity: null };
    this.groupFormSubmitted = false;
    this.groupModalCenterId = centerId;
    this.showGroupModal.set(true);
  }

  editGroup(g: any) {
    this.editingGroup = g;
    this.groupForm = {
      dayOfWeek: g.dayOfWeek || '',
      meetingTime: g.meetingTime || '',
      description: g.description || '',
      levelId: g.levelId || null,
      maxCapacity: g.maxCapacity || null
    };
    this.groupFormSubmitted = false;
    this.groupModalCenterId = g.centerId || g.center?.id;
    this.showGroupModal.set(true);
  }

  saveGroup() {
    this.groupFormSubmitted = true;
    if (!this.groupForm.dayOfWeek || !this.groupForm.meetingTime || !this.groupForm.levelId) {
      this.toastr.warning('يرجى تعبئة جميع الحقول المطلوبة');
      return;
    }
    const centerId = this.groupModalCenterId;
    const payload = { ...this.groupForm, centerId };

    const req = this.editingGroup
      ? this.api.updateGroup(this.editingGroup.id, payload)
      : this.api.createGroup(payload as any);

    req.subscribe({
      next: () => {
        this.toastr.success(this.editingGroup ? 'تم تحديث الميعاد' : 'تمت إضافة الميعاد');
        this.showGroupModal.set(false);
        if (centerId) this.loadGroupsForCenter(centerId);
      },
      error: (err: any) => this.toastr.error(err?.error?.message || 'حدث خطأ')
    });
  }

  deleteGroup(groupId: number) {
    if (!confirm('هل تريد حذف هذا الميعاد؟')) return;
    this.api.deleteGroup(groupId).subscribe({
      next: () => {
        this.toastr.success('تم حذف الميعاد');
        const cid = this.expandedCenterId();
        if (cid) this.loadGroupsForCenter(cid);
      }
    });
  }
}
