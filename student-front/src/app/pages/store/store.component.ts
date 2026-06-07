import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-store',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
<div dir="rtl" class="max-w-2xl mx-auto pb-10 space-y-6 text-center">
  <div class="py-20">
    <div class="w-24 h-24 bg-amber-50 dark:bg-[#F59239]/10 rounded-3xl flex items-center justify-center mx-auto mb-6">
      <span class="material-symbols-outlined text-[#F59239]"
            style="font-size:48px;font-variation-settings:'FILL' 1,'wght' 400,'GRAD' 0,'opsz' 24">shopping_bag</span>
    </div>
    <h2 class="font-black text-2xl text-[#183764] dark:text-white mb-3">متجر الكتب</h2>
    <p class="text-[#8892a0] text-sm max-w-xs mx-auto">
      المتجر قيد الإنشاء. سيتم إتاحة الكتب والمواد الدراسية للشراء قريباً.
    </p>
    <a routerLink="/books-codes"
       class="inline-flex items-center gap-2 mt-8 px-6 py-3 bg-[#183764] text-white rounded-xl font-bold text-sm hover:opacity-90 transition-all">
      <span class="material-symbols-outlined" style="font-size:18px">location_on</span>
      أماكن بيع الكتب والأكواد
    </a>
  </div>
</div>
  `
})
export class StoreComponent {}
