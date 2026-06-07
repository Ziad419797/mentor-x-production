import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-center-schedule',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
<div class="space-y-6 animate-fade-in pb-10">

  <!-- Header -->
  <div class="flex items-center justify-between">
    <div>
      <h2 class="text-white font-black text-2xl">جدول السناتر</h2>
      <p class="text-slate-500 text-sm mt-1">إدارة مواعيد وجروبات كل سنتر</p>
    </div>
    <button (click)="openForm()" class="flex items-center gap-2 px-4 py-2.5 bg-indigo-600 text-white rounded-xl font-bold text-sm hover:bg-indigo-700 transition-all">
      <span class="material-icons-round text-sm">add</span>
      إضافة موعد
    </button>
  </div>

  <!-- Add / Edit Form -->
  <div *ngIf="showForm()" class="edu-card p-6 space-y-4">
    <h3 class="text-white font-bold">{{ editing() ? 'تعديل الموعد' : 'إضافة موعد جديد' }}</h3>
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      <div>
        <label class="text-slate-400 text-xs block mb-1">اسم السنتر *</label>
        <input [(ngModel)]="form.centerName" class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-white text-sm focus:border-indigo-500 focus:outline-none" placeholder="مثال: سنتر المعادي">
      </div>
      <div>
        <label class="text-slate-400 text-xs block mb-1">اسم الجروب *</label>
        <input [(ngModel)]="form.groupName" class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-white text-sm focus:border-indigo-500 focus:outline-none" placeholder="مثال: أ1">
      </div>
      <div>
        <label class="text-slate-400 text-xs block mb-1">الصف الدراسي</label>
        <input [(ngModel)]="form.gradeLevel" class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-white text-sm focus:border-indigo-500 focus:outline-none" placeholder="مثال: الصف الثالث الثانوي">
      </div>
      <div>
        <label class="text-slate-400 text-xs block mb-1">اليوم *</label>
        <select [(ngModel)]="form.dayOfWeek" class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-white text-sm focus:border-indigo-500 focus:outline-none">
          <option value="">اختر اليوم</option>
          <option *ngFor="let d of days" [value]="d">{{ d }}</option>
        </select>
      </div>
      <div>
        <label class="text-slate-400 text-xs block mb-1">وقت البداية</label>
        <input [(ngModel)]="form.startTime" type="time" class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-white text-sm focus:border-indigo-500 focus:outline-none">
      </div>
      <div>
        <label class="text-slate-400 text-xs block mb-1">وقت النهاية</label>
        <input [(ngModel)]="form.endTime" type="time" class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-white text-sm focus:border-indigo-500 focus:outline-none">
      </div>
    </div>
    <div>
      <label class="text-slate-400 text-xs block mb-1">ملاحظات</label>
      <input [(ngModel)]="form.notes" class="w-full bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-white text-sm focus:border-indigo-500 focus:outline-none" placeholder="أي ملاحظات إضافية">
    </div>
    <div class="flex gap-3">
      <button (click)="save()" [disabled]="saving()" class="px-5 py-2.5 bg-indigo-600 text-white rounded-xl font-bold text-sm hover:bg-indigo-700 transition-all disabled:opacity-50">
        {{ saving() ? 'جاري الحفظ...' : 'حفظ' }}
      </button>
      <button (click)="closeForm()" class="px-5 py-2.5 bg-slate-800 text-slate-300 rounded-xl font-bold text-sm hover:bg-slate-700 transition-all">إلغاء</button>
    </div>
  </div>

  <!-- Loading -->
  <div *ngIf="loading()" class="flex justify-center py-16">
    <span class="material-icons-round text-slate-500 animate-spin">refresh</span>
  </div>

  <!-- Empty -->
  <div *ngIf="!loading() && schedule().length === 0 && !showForm()"
       class="edu-card flex flex-col items-center py-16 text-slate-500">
    <span class="material-icons-round text-5xl mb-3 opacity-20">calendar_month</span>
    <p class="text-sm">لا يوجد جدول بعد — أضف موعداً للبداية</p>
  </div>

  <!-- Table -->
  <div *ngIf="!loading() && schedule().length > 0" class="edu-card p-0 overflow-hidden">
    <div class="overflow-x-auto">
      <table class="w-full">
        <thead>
          <tr class="border-b border-slate-800">
            <th class="text-right px-4 py-3 text-[10px] font-bold text-slate-500 uppercase">السنتر</th>
            <th class="text-right px-4 py-3 text-[10px] font-bold text-slate-500 uppercase">الجروب</th>
            <th class="text-right px-4 py-3 text-[10px] font-bold text-slate-500 uppercase">الصف</th>
            <th class="text-right px-4 py-3 text-[10px] font-bold text-slate-500 uppercase">اليوم</th>
            <th class="text-right px-4 py-3 text-[10px] font-bold text-slate-500 uppercase">الوقت</th>
            <th class="text-right px-4 py-3 text-[10px] font-bold text-slate-500 uppercase">ملاحظات</th>
            <th class="px-4 py-3"></th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let row of schedule()" class="border-b border-slate-800/50 hover:bg-slate-800/20 transition-colors">
            <td class="px-4 py-3 text-white font-semibold text-sm">{{ row.centerName }}</td>
            <td class="px-4 py-3">
              <span class="bg-indigo-500/10 text-indigo-400 px-2.5 py-1 rounded-lg text-xs font-bold">{{ row.groupName }}</span>
            </td>
            <td class="px-4 py-3 text-slate-400 text-sm">{{ row.gradeLevel || '—' }}</td>
            <td class="px-4 py-3 text-white text-sm font-semibold">{{ row.dayOfWeek }}</td>
            <td class="px-4 py-3 text-emerald-400 text-sm font-bold">{{ row.startTime }} – {{ row.endTime }}</td>
            <td class="px-4 py-3 text-slate-400 text-xs">{{ row.notes || '—' }}</td>
            <td class="px-4 py-3">
              <div class="flex gap-2 justify-end">
                <button (click)="editRow(row)" class="w-8 h-8 rounded-lg bg-slate-800 hover:bg-indigo-500 text-slate-400 hover:text-white transition-all flex items-center justify-center">
                  <span class="material-icons-round text-sm">edit</span>
                </button>
                <button (click)="deleteRow(row.id)" class="w-8 h-8 rounded-lg bg-slate-800 hover:bg-red-500 text-slate-400 hover:text-white transition-all flex items-center justify-center">
                  <span class="material-icons-round text-sm">delete</span>
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>

</div>
  `
})
export class CenterScheduleComponent implements OnInit {
  loading  = signal(true);
  saving   = signal(false);
  showForm = signal(false);
  editing  = signal<any>(null);
  schedule = signal<any[]>([]);

  days = ['السبت','الأحد','الاثنين','الثلاثاء','الأربعاء','الخميس','الجمعة'];

  form = { centerName:'', groupName:'', gradeLevel:'', dayOfWeek:'', startTime:'', endTime:'', notes:'' };

  constructor(private api: ApiService, private toastr: ToastrService) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.api.getCenterSchedule().subscribe({
      next: (data) => { this.schedule.set(data); this.loading.set(false); },
      error: () => { this.loading.set(false); }
    });
  }

  openForm() { this.resetForm(); this.editing.set(null); this.showForm.set(true); }
  closeForm() { this.showForm.set(false); this.editing.set(null); }

  editRow(row: any) {
    this.editing.set(row);
    this.form = { centerName: row.centerName, groupName: row.groupName, gradeLevel: row.gradeLevel || '',
                  dayOfWeek: row.dayOfWeek, startTime: row.startTime || '', endTime: row.endTime || '', notes: row.notes || '' };
    this.showForm.set(true);
  }

  save() {
    if (!this.form.centerName || !this.form.groupName || !this.form.dayOfWeek) {
      this.toastr.error('برجاء ملء الحقول المطلوبة'); return;
    }
    this.saving.set(true);
    const req = this.editing()
      ? this.api.updateCenterScheduleEntry(this.editing().id, this.form)
      : this.api.addCenterScheduleEntry(this.form);

    req.subscribe({
      next: () => {
        this.toastr.success(this.editing() ? 'تم التعديل' : 'تمت الإضافة');
        this.saving.set(false);
        this.closeForm();
        this.load();
      },
      error: () => { this.toastr.error('حدث خطأ'); this.saving.set(false); }
    });
  }

  deleteRow(id: number) {
    if (!confirm('هل تريد حذف هذا الموعد؟')) return;
    this.api.deleteCenterScheduleEntry(id).subscribe({
      next: () => { this.toastr.success('تم الحذف'); this.load(); },
      error: () => this.toastr.error('فشل الحذف')
    });
  }

  private resetForm() {
    this.form = { centerName:'', groupName:'', gradeLevel:'', dayOfWeek:'', startTime:'', endTime:'', notes:'' };
  }
}
