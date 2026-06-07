import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StudentApiService } from '../../services/student-api.service';

@Component({
  selector: 'app-books-codes',
  standalone: true,
  imports: [CommonModule],
  template: `
<div dir="rtl" class="max-w-4xl mx-auto pb-10 space-y-6">

  <div>
    <h2 class="font-black text-2xl text-[#183764] dark:text-white">أماكن بيع الكتب والأكواد</h2>
    <p class="text-[#8892a0] text-sm mt-1">أماكن السناتر والمكتبات التي يمكنك منها شراء الكتب والأكواد</p>
  </div>

  <!-- Loading -->
  <div *ngIf="loading()" class="flex justify-center py-16">
    <span class="material-symbols-outlined animate-spin text-[#8892a0]">refresh</span>
  </div>

  <!-- Empty -->
  <div *ngIf="!loading() && locations().length === 0"
       class="flex flex-col items-center py-20 text-[#8892a0]">
    <span class="material-symbols-outlined" style="font-size:56px;opacity:0.3">location_on</span>
    <p class="mt-3 text-sm">لا توجد أماكن مضافة حالياً</p>
    <p class="text-xs mt-1">سيتم إضافة الأماكن قريباً من قِبل المدرس</p>
  </div>

  <!-- Cards grid -->
  <div *ngIf="!loading() && locations().length > 0"
       class="grid grid-cols-1 sm:grid-cols-2 gap-4">
    <div *ngFor="let loc of locations()"
         class="bg-white dark:bg-[#162033] rounded-2xl border border-[#DDE1EA] dark:border-slate-800 p-5 space-y-3">

      <!-- Name + type -->
      <div class="flex items-start justify-between gap-2">
        <div>
          <h3 class="font-bold text-base text-[#183764] dark:text-white">{{ loc.name }}</h3>
          <p class="text-xs text-[#8892a0] mt-0.5">{{ loc.address }}</p>
        </div>
        <span class="px-2.5 py-1 rounded-xl text-[10px] font-bold flex-shrink-0"
              [class]="loc.type === 'CENTER' ? 'bg-[#e8edf7] text-[#183764]' : 'bg-amber-50 text-amber-700'">
          {{ loc.type === 'CENTER' ? 'سنتر' : loc.type === 'LIBRARY' ? 'مكتبة' : 'أخرى' }}
        </span>
      </div>

      <!-- Indicators: sells what -->
      <div class="flex items-center gap-2 flex-wrap">
        <span *ngIf="loc.sellsBooks"
              class="inline-flex items-center gap-1 px-2.5 py-1 bg-[#e6f7f4] text-[#4BBBA0] rounded-lg text-xs font-bold">
          <span class="material-symbols-outlined" style="font-size:14px;font-variation-settings:'FILL' 1,'wght' 400,'GRAD' 0,'opsz' 24">menu_book</span>
          كتب
        </span>
        <span *ngIf="loc.sellsCodes"
              class="inline-flex items-center gap-1 px-2.5 py-1 bg-[#e8edf7] text-[#183764] dark:bg-white/10 dark:text-white rounded-lg text-xs font-bold">
          <span class="material-symbols-outlined" style="font-size:14px;font-variation-settings:'FILL' 1,'wght' 400,'GRAD' 0,'opsz' 24">key</span>
          أكواد
        </span>
      </div>

      <!-- Phone -->
      <p *ngIf="loc.phone" class="text-xs text-[#8892a0] flex items-center gap-1">
        <span class="material-symbols-outlined" style="font-size:14px">phone</span>
        {{ loc.phone }}
      </p>

      <!-- Notes -->
      <p *ngIf="loc.notes" class="text-xs text-[#8892a0] bg-[#F5F6FA] dark:bg-white/5 rounded-lg px-3 py-2">
        {{ loc.notes }}
      </p>
    </div>
  </div>

</div>
  `
})
export class BooksCodesComponent implements OnInit {
  loading   = signal(true);
  locations = signal<any[]>([]);

  constructor(private api: StudentApiService) {}

  ngOnInit() {
    this.api.getBooksCodesLocations().subscribe({
      next: (data: any[]) => { this.locations.set(data); this.loading.set(false); },
      error: ()           => { this.loading.set(false); }
    });
  }
}
