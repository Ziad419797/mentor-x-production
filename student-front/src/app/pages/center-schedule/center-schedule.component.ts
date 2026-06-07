import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StudentApiService } from '../../services/student-api.service';

@Component({
  selector: 'app-center-schedule',
  standalone: true,
  imports: [CommonModule],
  template: `
<div dir="rtl" class="max-w-4xl mx-auto pb-10 space-y-6">

  <div>
    <h2 class="font-black text-2xl text-[#183764] dark:text-white">جدول السناتر</h2>
    <p class="text-[#8892a0] text-sm mt-1">مواعيد وجروبات كل سنتر</p>
  </div>

  <!-- Loading -->
  <div *ngIf="loading()" class="flex justify-center py-16">
    <span class="material-symbols-outlined animate-spin text-[#8892a0]">refresh</span>
  </div>

  <!-- Empty -->
  <div *ngIf="!loading() && schedule().length === 0"
       class="flex flex-col items-center py-20 text-[#8892a0]">
    <span class="material-symbols-outlined" style="font-size:56px;opacity:0.3">calendar_month</span>
    <p class="mt-3 text-sm">لا يوجد جدول متاح حالياً</p>
    <p class="text-xs mt-1">سيتم إضافة المواعيد قريباً من قِبل المدرس</p>
  </div>

  <!-- Table -->
  <div *ngIf="!loading() && schedule().length > 0"
       class="bg-white dark:bg-[#162033] rounded-2xl border border-[#DDE1EA] dark:border-slate-800 overflow-hidden">
    <div class="overflow-x-auto">
      <table class="w-full text-right">
        <thead>
          <tr class="bg-[#F5F6FA] dark:bg-white/5 border-b border-[#DDE1EA] dark:border-slate-800">
            <th class="px-4 py-3 text-[10px] font-black text-[#8892a0] uppercase tracking-wide">السنتر</th>
            <th class="px-4 py-3 text-[10px] font-black text-[#8892a0] uppercase tracking-wide">الجروب</th>
            <th class="px-4 py-3 text-[10px] font-black text-[#8892a0] uppercase tracking-wide">الصف</th>
            <th class="px-4 py-3 text-[10px] font-black text-[#8892a0] uppercase tracking-wide">اليوم</th>
            <th class="px-4 py-3 text-[10px] font-black text-[#8892a0] uppercase tracking-wide">الوقت</th>
            <th class="px-4 py-3 text-[10px] font-black text-[#8892a0] uppercase tracking-wide">ملاحظات</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let row of schedule(); let i = index"
              class="border-b border-[#DDE1EA]/50 dark:border-slate-800/50 hover:bg-[#F5F6FA] dark:hover:bg-white/5 transition-colors">
            <td class="px-4 py-3">
              <span class="font-bold text-sm text-[#183764] dark:text-white">{{ row.centerName }}</span>
            </td>
            <td class="px-4 py-3">
              <span class="px-2.5 py-1 bg-[#e8edf7] dark:bg-white/10 text-[#183764] dark:text-white rounded-lg text-xs font-bold">
                {{ row.groupName }}
              </span>
            </td>
            <td class="px-4 py-3 text-sm text-[#8892a0]">{{ row.gradeLevel }}</td>
            <td class="px-4 py-3">
              <span class="font-semibold text-sm text-[#183764] dark:text-white">{{ row.dayOfWeek }}</span>
            </td>
            <td class="px-4 py-3">
              <span class="text-sm text-[#4BBBA0] font-bold">{{ row.startTime }} – {{ row.endTime }}</span>
            </td>
            <td class="px-4 py-3 text-xs text-[#8892a0]">{{ row.notes || '—' }}</td>
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
  schedule = signal<any[]>([]);

  constructor(private api: StudentApiService) {}

  ngOnInit() {
    this.api.getCenterSchedule().subscribe({
      next: (data: any[]) => { this.schedule.set(data); this.loading.set(false); },
      error: ()           => { this.loading.set(false); }
    });
  }
}
