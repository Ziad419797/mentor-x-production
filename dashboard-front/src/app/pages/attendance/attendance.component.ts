import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { AttendanceRecord, Course, Session, Week } from '../../models/models';
import { ToastrService } from 'ngx-toastr';
import { extractPage } from '../../core/api-response.model';

@Component({
  selector: 'app-attendance',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6 animate-fade-in pb-10">
      
      <div class="grid grid-cols-1 lg:grid-cols-12 gap-8">
        
        <!-- Right Panel: Scanner & Entry -->
        <div class="lg:col-span-5 space-y-6">
           <div class="edu-card p-8 border-indigo-500/30 bg-indigo-500/5 flex flex-col items-center text-center">
              <div class="w-20 h-20 bg-indigo-500/10 rounded-full flex items-center justify-center text-indigo-400 mb-6">
                <span class="material-icons-round text-4xl">qr_code_scanner</span>
              </div>
              <h3 class="text-white font-bold text-xl mb-2">تسجيل حضور</h3>
              <p class="text-slate-500 text-xs mb-8">قم بمسح كود الطالب أو إدخال الكود يدوياً</p>
              
              <div class="w-full space-y-4 text-right">
                <div class="grid grid-cols-1 gap-4">
                   <div>
                     <label class="edu-label">المستوى التعليمي</label>
                     <select (change)="onCourseChange($event)" class="edu-select">
                        <option value="0">اختر المستوى</option>
                        <option *ngFor="let c of courses()" [value]="c.id">{{ c.title }}</option>
                     </select>
                   </div>
                   <div>
                     <label class="edu-label">الدرس التعليمي</label>
                     <select [(ngModel)]="selectedWeekId" class="edu-select" [disabled]="weeks().length === 0">
                        <option [value]="0">اختر الدرس</option>
                        <option *ngFor="let w of weeks()" [value]="w.id">{{ w.title }}</option>
                     </select>
                   </div>
                </div>

                <div class="pt-4">
                  <label class="edu-label">كود الطالب / الرقم القومي</label>
                  <div class="flex gap-2">
                    <input type="text" [(ngModel)]="studentCode" (keyup.enter)="markAttendance()" class="edu-input font-mono uppercase text-left ltr" placeholder="QR-XXXXXX">
                    <button (click)="markAttendance()" [disabled]="!studentCode || !selectedWeekId" class="btn-primary py-2 h-auto text-xs px-6">تسجيل</button>
                  </div>
                </div>
              </div>
           </div>

           <!-- Last Scan Info -->
           <div *ngIf="lastScan()" class="edu-card p-6 border-emerald-500/30 bg-emerald-500/5 animate-slide-up">
              <div class="flex items-center gap-4">
                 <div class="w-12 h-12 rounded-full bg-emerald-500 flex items-center justify-center text-white">
                    <span class="material-icons-round text-2xl">check</span>
                 </div>
                 <div>
                    <h4 class="text-emerald-400 font-bold">تم تسجيل الحضور بنجاح</h4>
                    <p class="text-slate-200 font-bold text-sm mt-1">{{ lastScan()?.studentName }}</p>
                    <span class="text-[10px] text-slate-500">{{ lastScan()?.scanTime | date:'mediumTime' }}</span>
                 </div>
              </div>
           </div>
        </div>

        <!-- Left Panel: Recent Logs -->
        <div class="lg:col-span-7 space-y-4">
           <div class="flex items-center justify-between">
              <h3 class="text-white font-bold flex items-center gap-2">
                <span class="material-icons-round text-slate-400">history</span>
                سجل الحضور الأخير
              </h3>
              <button (click)="loadLogs()" class="btn-icon h-9 w-9"><span class="material-icons-round text-sm">refresh</span></button>
           </div>

           <div class="edu-card p-0 overflow-hidden shadow-xl border-slate-800">
             <table class="edu-table">
               <thead>
                 <tr>
                   <th>الطالب</th>
                   <th>المستوى / الدرس</th>
                   <th>الوقت</th>
                   <th>الحالة</th>
                 </tr>
               </thead>
               <tbody>
                 <tr *ngFor="let log of logs(); trackBy: trackByLog">
                   <td>
                      <div class="flex flex-col">
                        <span class="text-slate-200 font-bold text-xs">{{ log?.studentName }}</span>
                        <span class="text-[10px] text-slate-500 ltr text-right">{{ log?.studentCode || '#' + log?.studentId }}</span>
                      </div>
                   </td>
                   <td>
                      <div class="flex flex-col">
                        <span class="text-[10px] text-slate-300">{{ log?.courseName }}</span>
                        <span class="text-[10px] text-slate-500">{{ log?.weekTitle }}</span>
                      </div>
                   </td>
                   <td class="text-[10px] text-slate-400">{{ log?.scanTime | date:'shortTime' }}</td>
                   <td>
                      <span class="badge-success text-[9px]">حاضر</span>
                   </td>
                 </tr>
                 <tr *ngIf="logs().length === 0">
                    <td colspan="4" class="text-center py-24 text-slate-600 italic">لا توجد سجلات لليوم</td>
                 </tr>
               </tbody>
             </table>
           </div>
        </div>

      </div>

    </div>
  `
})
export class AttendanceComponent implements OnInit {
  courses = signal<Course[]>([]);
  weeks = signal<Week[]>([]);
  logs = signal<AttendanceRecord[]>([]);
  lastScan = signal<AttendanceRecord | null>(null);

  selectedWeekId = 0;
  studentCode = '';

  constructor(private api: ApiService, private toastr: ToastrService) {}

  ngOnInit(): void {
    this.api.getCourses(0, 100).subscribe({
      next: (pg) => {
        this.courses.set(pg.content || []);
      },
      error: (err) => {
        console.error('Failed to load courses:', err);
        this.courses.set([]);
      }
    });
    this.loadLogs();
  }

  onCourseChange(event: any) {
    const id = Number(event.target.value);
    this.weeks.set([]);
    this.selectedWeekId = 0;
    if (id > 0) {
      this.api.getSessionsByCourse(id).subscribe({
        next: (sessions: Session[]) => {
          if (sessions && sessions.length > 0) {
            this.api.getWeeksBySession(sessions[0].id).subscribe({
              next: (weeks: Week[]) => {
                this.weeks.set(weeks || []);
                if (weeks && weeks.length > 0) this.selectedWeekId = weeks[0].id;
              },
              error: (err) => {
                console.error('Failed to load weeks:', err);
                this.weeks.set([]);
              }
            });
          }
        },
        error: (err) => {
          console.error('Failed to load sessions:', err);
          this.weeks.set([]);
        }
      });
    }
  }

  loadLogs() {
    this.api.getRecentAttendance(0, 20).subscribe({
      next: (pg) => {
        this.logs.set(pg.content || []);
      },
      error: (err) => {
        console.error('Failed to load logs:', err);
        this.logs.set([]);
      }
    });
  }

  markAttendance() {
    if (!this.studentCode || !this.selectedWeekId) return;

    this.api.markAttendance(this.studentCode, this.selectedWeekId).subscribe({
      next: (record: AttendanceRecord) => {
        this.toastr.success('تم تسجيل الحضور بنجاح');
        this.lastScan.set(record);
        this.studentCode = '';
        this.loadLogs();

        // Clear success message after 5s
        setTimeout(() => this.lastScan.set(null), 5000);
      },
      error: (err: any) => {
        console.error('Failed to mark attendance:', err);
        this.toastr.error(err?.error?.message || 'كود غير صحيح أو طالب غير مسجل');
      }
    });
  }

  trackByLog(index: number, log: AttendanceRecord): number {
    return log?.id || index;
  }
}
