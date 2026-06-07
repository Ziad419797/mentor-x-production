import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { Student } from '../../models/models';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-student-cards',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6 animate-fade-in pb-10">
      
      <!-- Header -->
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-white font-bold text-2xl">الكارنيهات (IDs)</h2>
          <p class="text-slate-500 text-sm mt-1">توليد وطباعة كارنيهات الطلاب بـ QR Code</p>
        </div>
        <div class="flex gap-3">
           <button (click)="printBatch()" [disabled]="selectedStudents.size === 0" class="btn-primary bg-emerald-600 hover:bg-emerald-500">
             <span class="material-icons-round">print</span>
             طباعة المحدد ({{ selectedStudents.size }})
           </button>
        </div>
      </div>

      <!-- Search Bar -->
      <div class="edu-card bg-slate-900/40 p-4 flex items-center gap-4">
        <div class="relative flex-1">
           <span class="material-icons-round absolute right-3 top-1/2 -translate-y-1/2 text-slate-500">search</span>
           <input type="text" [(ngModel)]="searchQuery" (input)="onSearch()" class="edu-input pr-10" placeholder="ابحث باسم الطالب أو الكود...">
        </div>
        <select class="edu-select w-48">
           <option>كل المجموعات</option>
           <option>أونلاين</option>
           <option>سنتر السويس</option>
        </select>
      </div>

      <!-- Students Table -->
      <div class="edu-card p-0 overflow-hidden shadow-xl border-slate-800">
        <table class="edu-table">
          <thead>
            <tr>
              <th class="w-12">
                <input type="checkbox" (change)="toggleAll($event)" class="w-4 h-4 accent-indigo-500">
              </th>
              <th>الطالب</th>
              <th>الكود</th>
              <th>المحافظة</th>
              <th>الحالة</th>
              <th class="text-center">معاينة</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let s of students()">
              <td>
                <input type="checkbox" [checked]="selectedStudents.has(s.id)" (change)="toggleStudent(s.id)" class="w-4 h-4 accent-indigo-500">
              </td>
              <td>
                <div class="flex items-center gap-3">
                   <div class="w-8 h-8 rounded-full bg-slate-800 flex items-center justify-center text-xs font-bold text-slate-400">
                     {{ s.fullName ? s.fullName[0] : "?" }}
                   </div>
                   <span class="text-slate-200 font-bold text-xs">{{ s.fullName }}</span>
                </div>
              </td>
              <td><code class="text-indigo-400 font-mono text-xs">{{ s.studentCode }}</code></td>
              <td class="text-xs text-slate-500">{{ s.governorate }}</td>
              <td>
                 <span [class]="s.hasCard ? 'text-emerald-400' : 'text-amber-400'" class="flex items-center gap-1 text-[10px] font-bold">
                   <span class="material-icons-round text-xs">{{ s.hasCard ? 'check_circle' : 'pending' }}</span>
                   {{ s.hasCard ? 'تم الاستخراج' : 'لم يستخرج بعد' }}
                 </span>
              </td>
              <td>
                <div class="flex justify-center">
                   <button (click)="previewCard(s)" class="btn-icon h-8 w-8 text-indigo-400">
                     <span class="material-icons-round text-sm">visibility</span>
                   </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Card Preview Overlay -->
      <div *ngIf="previewingStudent()" class="modal-overlay">
         <div (click)="previewingStudent.set(null)" class="absolute inset-0"></div>
         <div class="relative z-10 animate-scale-in">
            <!-- ID Card Mockup -->
            <div id="id-card" class="w-[350px] h-[500px] bg-white rounded-[24px] shadow-2xl overflow-hidden relative border-[12px] border-slate-900">
               <!-- Header Gradient -->
               <div class="h-32 bg-gradient-to-br from-indigo-600 to-purple-700 p-6 flex flex-col items-center">
                  <div class="w-12 h-12 rounded-xl bg-white/20 flex items-center justify-center text-white mb-2">
                    <span class="material-icons-round text-2xl">school</span>
                  </div>
                  <h3 class="text-white font-black tracking-widest text-lg">EduCore</h3>
                  <span class="text-white/60 text-[8px] font-bold uppercase tracking-tighter">Student ID Card</span>
               </div>
               
               <!-- Profile Section -->
               <div class="absolute top-24 left-0 right-0 flex justify-center">
                  <div class="w-32 h-32 rounded-3xl bg-slate-100 border-4 border-white shadow-xl overflow-hidden">
                     <img *ngIf="previewingStudent()?.profileImageUrl" [src]="previewingStudent()?.profileImageUrl" class="w-full h-full object-cover">
                     <div *ngIf="!previewingStudent()?.profileImageUrl" class="w-full h-full flex items-center justify-center text-slate-300">
                        <span class="material-icons-round text-5xl">person</span>
                     </div>
                  </div>
               </div>

               <!-- Details -->
               <div class="mt-28 p-8 text-center space-y-4">
                  <div>
                    <h4 class="text-slate-900 font-black text-xl mb-1">{{ previewingStudent()?.fullName }}</h4>
                    <p class="text-indigo-600 font-bold text-xs ltr">{{ previewingStudent()?.phone }}</p>
                  </div>
                  
                  <div class="grid grid-cols-2 gap-4 py-4 border-y border-slate-100">
                     <div class="text-center">
                        <p class="text-[8px] text-slate-400 uppercase font-bold mb-1">Grade</p>
                        <p class="text-slate-800 font-bold text-[10px]">{{ previewingStudent()?.grade || 'N/A' }}</p>
                     </div>
                     <div class="text-center">
                        <p class="text-[8px] text-slate-400 uppercase font-bold mb-1">Study Type</p>
                        <p class="text-slate-800 font-bold text-[10px]">{{ previewingStudent()?.studyType }}</p>
                     </div>
                  </div>

                  <!-- QR Code (Mockup) -->
                  <div class="flex flex-col items-center pt-4">
                     <div class="w-24 h-24 p-2 border-2 border-slate-100 rounded-2xl">
                        <img src="https://api.qrserver.com/v1/create-qr-code/?size=150x150&data={{ previewingStudent()?.studentCode }}" class="w-full h-full">
                     </div>
                     <code class="mt-2 text-slate-400 font-mono text-[10px] tracking-[4px] uppercase font-bold">{{ previewingStudent()?.studentCode }}</code>
                  </div>
               </div>
               
               <!-- Footer -->
               <div class="absolute bottom-0 left-0 right-0 bg-slate-50 py-3 text-center border-t border-slate-100">
                  <p class="text-[8px] text-slate-400 font-bold uppercase tracking-widest">Authorized by EduCore Team</p>
               </div>
            </div>

            <div class="flex gap-4 mt-6">
               <button (click)="previewingStudent.set(null)" class="btn-secondary flex-1 justify-center">إغلاق</button>
               <button (click)="printSingle()" class="btn-primary flex-1 justify-center bg-indigo-600">طباعة الكارنيه</button>
            </div>
         </div>
      </div>

    </div>
  `
})
export class StudentCardsComponent implements OnInit {
  students = signal<Student[]>([]);
  searchQuery = '';
  selectedStudents = new Set<number>();
  previewingStudent = signal<Student | null>(null);

  constructor(private api: ApiService, private toastr: ToastrService) {}

  ngOnInit(): void {
    this.loadStudents();
  }

  loadStudents() {
    this.api.getActiveStudents(0, 50, this.searchQuery).subscribe({
      next: (res) => this.students.set(res.content)
    });
  }

  onSearch() {
    this.loadStudents();
  }

  toggleStudent(id: number) {
    if (this.selectedStudents.has(id)) {
      this.selectedStudents.delete(id);
    } else {
      this.selectedStudents.add(id);
    }
  }

  toggleAll(event: any) {
    if (event.target.checked) {
      this.students().forEach(s => this.selectedStudents.add(s.id));
    } else {
      this.selectedStudents.clear();
    }
  }

  previewCard(student: Student) {
    this.previewingStudent.set(student);
  }

  printSingle() {
    window.print();
  }

  printBatch() {
    this.toastr.info('جاري تجهيز الأعداد للطباعة...');
    // In real app, open a new window with just the cards and trigger print
    window.print();
  }
}
