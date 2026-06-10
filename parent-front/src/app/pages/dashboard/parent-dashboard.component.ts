import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ParentApiService } from '../../services/parent-api.service';

@Component({
  selector: 'app-parent-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div dir="rtl" style="padding-bottom:32px;">

      <!-- Loading skeleton -->
      <div *ngIf="loading()" style="display:flex;flex-direction:column;gap:16px;">
        <div style="height:80px;border-radius:16px;background:#162033;"></div>
        <div style="height:100px;border-radius:16px;background:#162033;"></div>
        <div *ngFor="let i of [1,2]" style="height:180px;border-radius:16px;background:#162033;"></div>
      </div>

      <ng-container *ngIf="!loading() && summary()">

        <!-- Welcome banner — like student app welcome card -->
        <div style="background:#162033;border:1px solid #1e293b;border-radius:20px;padding:20px;margin-bottom:16px;position:relative;overflow:hidden;">
          <!-- Subtle glow -->
          <div style="position:absolute;top:-40px;left:-40px;width:160px;height:160px;background:radial-gradient(circle,rgba(75,187,160,.08) 0%,transparent 70%);pointer-events:none;"></div>
          <div style="position:relative;">
            <p style="color:#8892a0;font-size:12px;margin:0 0 4px;">أهلاً بك 👋</p>
            <h1 style="color:#fff;font-size:20px;font-weight:800;margin:0 0 12px;">{{ summary().parentName || 'ولي الأمر' }}</h1>
            <!-- Progress bar: avg completion -->
            <div style="background:#0f172a;border-radius:8px;padding:12px;">
              <div style="display:flex;justify-content:space-between;font-size:11px;margin-bottom:6px;">
                <span style="color:#8892a0;font-weight:600;">متابعة الأبناء</span>
                <span style="color:#4BBBA0;font-weight:700;">{{ summary().childrenCount }} {{ summary().childrenCount === 1 ? 'ابن' : 'أبناء' }}</span>
              </div>
              <div style="height:6px;border-radius:3px;background:#1e293b;overflow:hidden;">
                <div style="height:100%;border-radius:3px;background:linear-gradient(90deg,#4BBBA0,#183764);width:100%;transition:width 600ms;"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- Stats row — 3 chips like student stats -->
        <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:10px;margin-bottom:20px;">
          <div style="background:#162033;border:1px solid #1e293b;border-radius:14px;padding:14px;text-align:center;">
            <div style="font-size:22px;font-weight:800;color:#fff;">{{ summary().childrenCount }}</div>
            <div style="color:#8892a0;font-size:10px;margin-top:3px;font-weight:600;">الأبناء</div>
          </div>
          <div style="background:#162033;border:1px solid #1e293b;border-radius:14px;padding:14px;text-align:center;">
            <div style="font-size:22px;font-weight:800;color:#4BBBA0;">{{ totalEnrollments() }}</div>
            <div style="color:#8892a0;font-size:10px;margin-top:3px;font-weight:600;">اشتراكات</div>
          </div>
          <div style="background:#162033;border:1px solid #1e293b;border-radius:14px;padding:14px;text-align:center;">
            <div style="font-size:22px;font-weight:800;color:#F59239;">{{ totalLessons() }}</div>
            <div style="color:#8892a0;font-size:10px;margin-top:3px;font-weight:600;">دروس</div>
          </div>
        </div>

        <!-- Section label -->
        <p style="color:#8892a0;font-size:11px;font-weight:700;letter-spacing:.6px;text-transform:uppercase;margin-bottom:12px;">أبنائي</p>

        <!-- Child cards — student-app card style -->
        <div style="display:flex;flex-direction:column;gap:14px;">
          <a *ngFor="let child of summary().children; let i = index"
             [routerLink]="['/child', child.studentId]"
             style="text-decoration:none;display:block;background:#162033;border:1px solid #1e293b;border-radius:20px;overflow:hidden;transition:border-color 200ms,transform 150ms;"
             onmouseover="this.style.borderColor='#4BBBA0';this.style.transform='translateY(-2px)'"
             onmouseout="this.style.borderColor='#1e293b';this.style.transform='translateY(0)'">

            <!-- Top: avatar + name + badge -->
            <div style="padding:16px 16px 10px;display:flex;align-items:center;gap:14px;">

              <!-- Avatar — rotates between student-app gradients -->
              <div style="width:52px;height:52px;border-radius:14px;flex-shrink:0;overflow:hidden;display:flex;align-items:center;justify-content:center;color:#fff;font-weight:800;font-size:17px;position:relative;"
                   [style.background]="avatarGradient(i)">
                <img *ngIf="child.profileImageUrl" [src]="child.profileImageUrl"
                     style="width:100%;height:100%;object-fit:cover;position:absolute;inset:0;" />
                <span *ngIf="!child.profileImageUrl">{{ getInitials(child.studentName) }}</span>
              </div>

              <div style="flex:1;min-width:0;">
                <h3 style="color:#fff;font-weight:700;margin:0 0 5px;font-size:15px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">
                  {{ child.studentName }}
                </h3>
                <div style="display:flex;align-items:center;gap:6px;flex-wrap:wrap;">
                  <span style="color:#8892a0;font-size:12px;">{{ child.grade }}</span>
                  <span style="width:3px;height:3px;background:#334155;border-radius:50%;display:inline-block;"></span>
                  <!-- Online/Center badge — student-app style -->
                  <span style="font-size:11px;font-weight:700;padding:2px 8px;border-radius:20px;"
                        [style.background]="isOnline(child.studyType) ? 'rgba(75,187,160,.15)' : 'rgba(245,146,57,.12)'"
                        [style.color]="isOnline(child.studyType) ? '#4BBBA0' : '#F59239'">
                    {{ isOnline(child.studyType) ? 'أونلاين' : 'سنتر' }}
                  </span>
                  <span *ngIf="!isOnline(child.studyType) && child.centerName"
                        style="color:#F59239;font-size:11px;font-weight:600;">{{ child.centerName }}</span>
                </div>
              </div>

              <!-- Notification dot -->
              <div *ngIf="child.unreadNotifications > 0"
                   style="width:20px;height:20px;border-radius:50%;background:#ef4444;color:#fff;font-size:10px;display:flex;align-items:center;justify-content:center;font-weight:800;flex-shrink:0;">
                {{ child.unreadNotifications }}
              </div>
            </div>

            <!-- Code + Phone — styled like student info chips -->
            <div style="padding:8px 16px;display:flex;gap:14px;align-items:center;background:#0f172a;margin:0 12px;border-radius:10px;margin-bottom:10px;">
              <div style="display:flex;align-items:center;gap:5px;">
                <span class="material-icons-round" style="font-size:12px;color:#8892a0;">badge</span>
                <span style="color:#8892a0;font-size:11px;">كود:</span>
                <code style="color:#F59239;font-size:12px;font-weight:800;letter-spacing:.5px;">{{ child.studentCode || '—' }}</code>
              </div>
              <div *ngIf="child.phone" style="display:flex;align-items:center;gap:5px;">
                <span class="material-icons-round" style="font-size:12px;color:#8892a0;">phone</span>
                <span style="color:#8892a0;font-size:11px;font-family:monospace;direction:ltr;">{{ child.phone }}</span>
              </div>
            </div>

            <!-- Stats row — like student's 3-stat bar -->
            <div style="padding:10px 16px 14px;display:grid;grid-template-columns:repeat(3,1fr);gap:4px;">
              <div style="text-align:center;">
                <div style="color:#fff;font-weight:800;font-size:17px;">{{ child.activeEnrollments }}</div>
                <div style="color:#8892a0;font-size:10px;margin-top:2px;font-weight:600;">اشتراكات</div>
              </div>
              <div style="text-align:center;border-inline:1px solid #1e293b;">
                <div style="color:#4BBBA0;font-weight:800;font-size:17px;">{{ child.completedLessons }}</div>
                <div style="color:#8892a0;font-size:10px;margin-top:2px;font-weight:600;">دروس مكتملة</div>
              </div>
              <div style="text-align:center;">
                <div style="color:#F59239;font-weight:800;font-size:17px;">{{ child.totalAttendance }}</div>
                <div style="color:#8892a0;font-size:10px;margin-top:2px;font-weight:600;">حضور</div>
              </div>
            </div>

            <!-- CTA footer — like student card "الدخول للكورس" button -->
            <div style="border-top:1px solid #1e293b;padding:10px 16px;display:flex;justify-content:space-between;align-items:center;">
              <span style="color:#8892a0;font-size:11px;">عرض ملف الابن</span>
              <div style="display:flex;align-items:center;gap:4px;color:#4BBBA0;font-size:12px;font-weight:700;">
                تفاصيل
                <span class="material-icons-round" style="font-size:15px;">chevron_left</span>
              </div>
            </div>
          </a>
        </div>

        <!-- Empty state -->
        <div *ngIf="summary().children?.length === 0"
             style="text-align:center;padding:64px 0;color:#8892a0;">
          <span class="material-icons-round" style="font-size:48px;display:block;margin-bottom:12px;opacity:0.2;">people</span>
          <p style="margin:0;font-size:14px;">لا يوجد أبناء مسجّلون حتى الآن</p>
        </div>

      </ng-container>

      <!-- Error state -->
      <div *ngIf="!loading() && !summary()"
           style="text-align:center;padding:64px 0;color:#8892a0;">
        <span class="material-icons-round" style="font-size:48px;display:block;margin-bottom:12px;opacity:0.2;">cloud_off</span>
        <p style="margin:0;">تعذّر تحميل البيانات</p>
        <button (click)="load()"
                style="margin-top:14px;color:#4BBBA0;background:rgba(75,187,160,.1);border:1px solid rgba(75,187,160,.3);border-radius:10px;padding:8px 20px;cursor:pointer;font-size:13px;font-family:'Cairo',sans-serif;font-weight:600;">
          إعادة المحاولة
        </button>
      </div>

    </div>
  `
})
export class ParentDashboardComponent implements OnInit {
  summary = signal<any>(null);
  loading = signal(true);

  // Same gradients as student-app courseGradient()
  private gradients = [
    'linear-gradient(135deg,#183764,#0f4080)',
    'linear-gradient(135deg,#006b58,#004d3e)',
    'linear-gradient(135deg,#4a1d96,#2e1065)',
    'linear-gradient(135deg,#9d174d,#831843)',
    'linear-gradient(135deg,#7c3d00,#562d00)',
    'linear-gradient(135deg,#1e3a5f,#0f2040)',
  ];

  constructor(private parentApi: ParentApiService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.parentApi.getDashboardSummary().subscribe({
      next: (res: any) => { this.summary.set(res?.data ?? res); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  totalEnrollments(): number {
    return (this.summary()?.children || []).reduce((s: number, c: any) => s + (c.activeEnrollments || 0), 0);
  }

  totalLessons(): number {
    return (this.summary()?.children || []).reduce((s: number, c: any) => s + (c.completedLessons || 0), 0);
  }

  isOnline(studyType: string): boolean {
    if (!studyType) return true;
    const t = studyType.toLowerCase();
    return t.includes('online') || t === 'أونلاين';
  }

  getInitials(name: string): string {
    if (!name) return '؟';
    return name.split(' ').map((n: string) => n[0]).join('').substring(0, 2);
  }

  avatarGradient(i: number): string {
    return this.gradients[i % this.gradients.length];
  }
}
