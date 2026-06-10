import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { ParentApiService } from '../../services/parent-api.service';

type Tab = 'overview' | 'attendance' | 'enrollments' | 'quizzes' | 'assignments' | 'wallet' | 'activity' | 'analytics';

@Component({
  selector: 'app-child-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div>
      <!-- Back -->
      <a routerLink="/dashboard" style="display:inline-flex;align-items:center;gap:4px;color:#94a3b8;text-decoration:none;font-size:13px;margin-bottom:20px;transition:color 150ms;" onmouseover="this.style.color='#fff'" onmouseout="this.style.color='#94a3b8'">
        <span class="material-icons-round" style="font-size:16px;">arrow_forward</span>
        العودة للرئيسية
      </a>

      <!-- ── Child header card (matches dashboard card style) ── -->
      <div *ngIf="overview()"
           style="background:#1e293b;border:1px solid #334155;border-radius:16px;overflow:hidden;margin-bottom:20px;">

        <!-- Top: avatar + name + badge -->
        <div style="padding:16px 16px 12px;display:flex;align-items:center;gap:14px;">
          <div style="width:54px;height:54px;border-radius:14px;flex-shrink:0;overflow:hidden;background:linear-gradient(135deg,#4f46e5,#7c3aed);display:flex;align-items:center;justify-content:center;color:#fff;font-weight:700;font-size:18px;position:relative;">
            <img *ngIf="overview().profileImageUrl" [src]="overview().profileImageUrl"
                 style="width:100%;height:100%;object-fit:cover;position:absolute;inset:0;" />
            <span *ngIf="!overview().profileImageUrl">{{ getInitials(overview().studentName) }}</span>
          </div>
          <div style="flex:1;min-width:0;">
            <h1 style="color:#fff;font-weight:700;margin:0 0 5px;font-size:16px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">{{ overview().studentName }}</h1>
            <div style="display:flex;align-items:center;gap:6px;flex-wrap:wrap;">
              <span style="color:#94a3b8;font-size:12px;">{{ overview().grade }}</span>
              <span style="width:3px;height:3px;background:#475569;border-radius:50%;display:inline-block;"></span>
              <span style="font-size:11px;font-weight:600;padding:2px 8px;border-radius:20px;"
                    [style.background]="isOnline(overview().studyType) ? 'rgba(14,165,233,.15)' : 'rgba(245,158,11,.15)'"
                    [style.color]="isOnline(overview().studyType) ? '#38bdf8' : '#fbbf24'">
                {{ isOnline(overview().studyType) ? 'أونلاين' : 'سنتر' }}
              </span>
              <span *ngIf="!isOnline(overview().studyType) && overview().centerName"
                    style="color:#f59e0b;font-size:11px;font-weight:500;">{{ overview().centerName }}</span>
            </div>
          </div>
        </div>

        <div style="height:1px;background:#334155;margin:0 16px;"></div>

        <!-- Code row -->
        <div style="padding:10px 16px;display:flex;gap:16px;align-items:center;">
          <div style="display:flex;align-items:center;gap:6px;">
            <span class="material-icons-round" style="font-size:13px;color:#64748b;">badge</span>
            <span style="color:#94a3b8;font-size:12px;">كود:</span>
            <code style="color:#fbbf24;font-size:12px;font-weight:700;background:rgba(245,158,11,.1);padding:2px 6px;border-radius:4px;">{{ overview().studentCode || '—' }}</code>
          </div>
          <div *ngIf="overview().phone" style="display:flex;align-items:center;gap:6px;">
            <span class="material-icons-round" style="font-size:13px;color:#64748b;">phone</span>
            <span style="color:#94a3b8;font-size:12px;font-family:monospace;direction:ltr;">{{ overview().phone }}</span>
          </div>
        </div>
      </div>
      <div *ngIf="overviewLoading()" style="height:100px;border-radius:16px;background:#1e293b;margin-bottom:20px;"></div>

      <!-- Tabs -->
      <div style="overflow-x:auto;margin-bottom:20px;border-bottom:1px solid #334155;">
        <div style="display:flex;gap:0;min-width:max-content;">
          <button *ngFor="let t of tabs" (click)="switchTab(t.key)"
                  style="padding:10px 14px;font-size:13px;font-weight:600;border:none;background:none;cursor:pointer;border-bottom:2px solid transparent;margin-bottom:-1px;transition:all 150ms;font-family:'Cairo',sans-serif;white-space:nowrap;"
                  [style.borderBottomColor]="activeTab() === t.key ? '#10b981' : 'transparent'"
                  [style.color]="activeTab() === t.key ? '#34d399' : '#94a3b8'">
            <span class="material-icons-round" style="font-size:14px;vertical-align:middle;margin-left:4px;">{{ t.icon }}</span>
            {{ t.label }}
          </button>
        </div>
      </div>

      <!-- ── Tab: Overview ────────────────────────────────── -->
      <ng-container *ngIf="activeTab() === 'overview' && overview()">
        <div style="display:grid;grid-template-columns:repeat(2,1fr);gap:12px;margin-bottom:16px;">
          <div class="edu-card" style="text-align:center;padding:16px;">
            <div style="font-size:22px;font-weight:700;color:#818cf8;">{{ overview().totalAttendance }}</div>
            <div style="color:#64748b;font-size:11px;margin-top:4px;">إجمالي الحضور</div>
          </div>
          <div class="edu-card" style="text-align:center;padding:16px;">
            <div style="font-size:22px;font-weight:700;color:#60a5fa;">{{ overview().centerAttendance }}</div>
            <div style="color:#64748b;font-size:11px;margin-top:4px;">حضور المركز</div>
          </div>
          <div class="edu-card" style="text-align:center;padding:16px;">
            <div style="font-size:22px;font-weight:700;color:#22d3ee;">{{ overview().onlineAttendance }}</div>
            <div style="color:#64748b;font-size:11px;margin-top:4px;">حضور أونلاين</div>
          </div>
          <div class="edu-card" style="text-align:center;padding:16px;">
            <div style="font-size:22px;font-weight:700;color:#34d399;">{{ activeEnrollmentsCount() }}</div>
            <div style="color:#64748b;font-size:11px;margin-top:4px;">اشتراكات نشطة</div>
          </div>
        </div>

        <div class="edu-card" style="margin-bottom:12px;">
          <h3 style="color:#fff;font-weight:600;margin:0 0 16px;font-size:14px;">تقدّم الدراسة</h3>
          <div style="display:flex;flex-direction:column;gap:10px;">
            <div style="display:flex;align-items:center;justify-content:space-between;">
              <div style="display:flex;align-items:center;gap:8px;">
                <span style="width:10px;height:10px;border-radius:50%;background:#10b981;display:inline-block;"></span>
                <span style="color:#cbd5e1;font-size:13px;">دروس مكتملة</span>
              </div>
              <span style="color:#fff;font-weight:600;">{{ overview().completedLessons }}</span>
            </div>
            <div style="display:flex;align-items:center;justify-content:space-between;">
              <div style="display:flex;align-items:center;gap:8px;">
                <span style="width:10px;height:10px;border-radius:50%;background:#f59e0b;display:inline-block;"></span>
                <span style="color:#cbd5e1;font-size:13px;">دروس قيد التقدّم</span>
              </div>
              <span style="color:#fff;font-weight:600;">{{ overview().inProgressLessons }}</span>
            </div>
            <div style="display:flex;align-items:center;justify-content:space-between;">
              <div style="display:flex;align-items:center;gap:8px;">
                <span style="width:10px;height:10px;border-radius:50%;background:#475569;display:inline-block;"></span>
                <span style="color:#cbd5e1;font-size:13px;">دروس مقفلة</span>
              </div>
              <span style="color:#fff;font-weight:600;">{{ overview().lockedLessons }}</span>
            </div>
          </div>
          <div *ngIf="totalLessons(overview()) > 0" style="margin-top:16px;">
            <div style="display:flex;justify-content:space-between;font-size:11px;color:#64748b;margin-bottom:4px;">
              <span>نسبة الإنجاز</span><span>{{ completionPct(overview()) }}%</span>
            </div>
            <div style="height:8px;border-radius:4px;background:#334155;overflow:hidden;">
              <div style="height:100%;border-radius:4px;background:#10b981;transition:width 500ms;"
                   [style.width.%]="completionPct(overview())"></div>
            </div>
          </div>
        </div>

        <div class="edu-card" *ngIf="overview().schoolName || overview().centerName">
          <h3 style="color:#fff;font-weight:600;margin:0 0 12px;font-size:14px;">معلومات إضافية</h3>
          <div style="display:flex;flex-direction:column;gap:8px;font-size:13px;">
            <div *ngIf="overview().schoolName" style="display:flex;justify-content:space-between;">
              <span style="color:#64748b;">المدرسة</span><span style="color:#fff;">{{ overview().schoolName }}</span>
            </div>
            <div *ngIf="overview().centerName" style="display:flex;justify-content:space-between;">
              <span style="color:#64748b;">المركز</span><span style="color:#fff;">{{ overview().centerName }}</span>
            </div>
            <div *ngIf="overview().governorate" style="display:flex;justify-content:space-between;">
              <span style="color:#64748b;">المحافظة</span><span style="color:#fff;">{{ overview().governorate }}</span>
            </div>
          </div>
        </div>
      </ng-container>

      <!-- ── Tab: Attendance ──────────────────────────────── -->
      <ng-container *ngIf="activeTab() === 'attendance'">
        <div *ngIf="attendanceLoading()" style="display:flex;flex-direction:column;gap:8px;">
          <div *ngFor="let i of [1,2,3,4,5]" class="edu-card animate-pulse" style="height:80px;padding:0;"></div>
        </div>
        <div *ngIf="!attendanceLoading()">
          <div *ngIf="attendancePage().content.length === 0" style="text-align:center;padding:48px 0;color:#64748b;">
            <span class="material-icons-round" style="font-size:40px;display:block;margin-bottom:8px;opacity:0.3;">event_busy</span>
            <p style="margin:0;">لا توجد سجلات حضور</p>
          </div>
          <div style="display:flex;flex-direction:column;gap:8px;">
            <div *ngFor="let rec of attendancePage().content" class="edu-card" style="padding:14px 16px;">
              <div style="display:flex;align-items:flex-start;justify-content:space-between;gap:10px;">
                <!-- Left: icon + info -->
                <div style="display:flex;align-items:flex-start;gap:10px;flex:1;min-width:0;">
                  <span class="material-icons-round" style="font-size:20px;margin-top:1px;flex-shrink:0;"
                        [style.color]="rec.status==='PRESENT'||!rec.status?'#34d399':rec.status==='ABSENT'?'#f87171':'#fbbf24'">
                    {{ (!rec.status||rec.status==='PRESENT') ? 'check_circle' : rec.status==='ABSENT' ? 'cancel' : 'schedule' }}
                  </span>
                  <div style="flex:1;min-width:0;">
                    <!-- Lesson/week title -->
                    <p style="color:#fff;font-size:13px;font-weight:600;margin:0;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">
                      {{ rec.weekTitle || 'محاضرة' }}
                    </p>
                    <!-- Group + Center (for center students) -->
                    <div *ngIf="rec.type === 'CENTER'" style="display:flex;flex-wrap:wrap;gap:8px;margin-top:4px;">
                      <span *ngIf="rec.groupName" style="display:inline-flex;align-items:center;gap:3px;color:#94a3b8;font-size:11px;">
                        <span class="material-icons-round" style="font-size:11px;">group</span>{{ rec.groupName }}
                      </span>
                      <span *ngIf="rec.centerName" style="display:inline-flex;align-items:center;gap:3px;color:#f59e0b;font-size:11px;">
                        <span class="material-icons-round" style="font-size:11px;">location_on</span>{{ rec.centerName }}
                      </span>
                    </div>
                  </div>
                </div>
                <!-- Right: badges + date -->
                <div style="display:flex;flex-direction:column;align-items:flex-end;gap:4px;flex-shrink:0;">
                  <span style="padding:2px 8px;border-radius:4px;font-size:11px;font-weight:600;"
                        [style.background]="rec.type==='CENTER'?'rgba(245,158,11,.15)':'rgba(14,165,233,.15)'"
                        [style.color]="rec.type==='CENTER'?'#fbbf24':'#38bdf8'">
                    <span class="material-icons-round" style="font-size:10px;vertical-align:middle;">{{ rec.type==='CENTER' ? 'location_on' : 'wifi' }}</span>
                    {{ rec.type==='CENTER' ? 'حضوري' : 'أونلاين' }}
                  </span>
                  <!-- Date label changes based on type -->
                  <span style="color:#64748b;font-size:11px;">
                    {{ rec.type==='CENTER' ? 'حضر:' : 'دخل:' }} {{ formatDateTime(rec.attendedAt) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div *ngIf="attendancePage().totalPages > 1" style="display:flex;justify-content:center;gap:8px;margin-top:20px;">
            <button (click)="loadAttendance(attendancePage().number-1)" [disabled]="attendancePage().number===0"
                    style="padding:6px 12px;border-radius:6px;font-size:13px;color:#94a3b8;background:none;border:1px solid #334155;cursor:pointer;font-family:'Cairo',sans-serif;"
                    [style.opacity]="attendancePage().number===0?'0.3':'1'">السابق</button>
            <span style="padding:6px 12px;color:#64748b;font-size:13px;">{{ attendancePage().number+1 }} / {{ attendancePage().totalPages }}</span>
            <button (click)="loadAttendance(attendancePage().number+1)" [disabled]="attendancePage().number+1>=attendancePage().totalPages"
                    style="padding:6px 12px;border-radius:6px;font-size:13px;color:#94a3b8;background:none;border:1px solid #334155;cursor:pointer;font-family:'Cairo',sans-serif;"
                    [style.opacity]="attendancePage().number+1>=attendancePage().totalPages?'0.3':'1'">التالي</button>
          </div>
        </div>
      </ng-container>

      <!-- ── Tab: Enrollments ─────────────────────────────── -->
      <ng-container *ngIf="activeTab() === 'enrollments'">
        <div *ngIf="enrollmentsLoading()" style="display:flex;flex-direction:column;gap:8px;">
          <div *ngFor="let i of [1,2,3]" class="edu-card animate-pulse" style="height:80px;padding:0;"></div>
        </div>
        <div *ngIf="!enrollmentsLoading()">
          <div *ngIf="activeEnrollments().length===0" style="text-align:center;padding:48px 0;color:#64748b;">
            <span class="material-icons-round" style="font-size:40px;display:block;margin-bottom:8px;opacity:0.3;">school</span>
            <p style="margin:0;">لا توجد اشتراكات نشطة</p>
          </div>
          <div style="display:flex;flex-direction:column;gap:12px;">
            <div *ngFor="let enr of activeEnrollments()" class="edu-card" style="padding:16px;">
              <div style="display:flex;align-items:flex-start;justify-content:space-between;gap:12px;">
                <div style="flex:1;min-width:0;">
                  <h4 style="color:#fff;font-weight:500;font-size:14px;margin:0;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">{{ enr.courseTitle }}</h4>
                  <p style="color:#64748b;font-size:11px;margin:3px 0 0;">{{ enrollmentTypeLabel(enr.enrollmentType) }}</p>
                </div>
                <span style="flex-shrink:0;padding:2px 8px;border-radius:4px;font-size:11px;font-weight:600;"
                      [style.background]="enr.status==='ACTIVE'?'rgba(16,185,129,.15)':'rgba(100,116,139,.15)'"
                      [style.color]="enr.status==='ACTIVE'?'#34d399':'#94a3b8'">{{ enrollmentStatusLabel(enr.status) }}</span>
              </div>
              <div style="margin-top:12px;">
                <div style="display:flex;justify-content:space-between;font-size:11px;color:#64748b;margin-bottom:4px;">
                  <span>التقدّم</span><span>{{ enr.progress | number:'1.0-0' }}%</span>
                </div>
                <div style="height:6px;border-radius:3px;background:#334155;overflow:hidden;">
                  <div style="height:100%;border-radius:3px;transition:width 500ms;"
                       [style.width.%]="enr.progress"
                       [style.background]="enr.status==='ACTIVE'?'#10b981':'#475569'"></div>
                </div>
              </div>
              <div style="display:flex;gap:16px;margin-top:8px;font-size:11px;color:#475569;">
                <span *ngIf="enr.enrolledAt">اشتراك: {{ formatDate(enr.enrolledAt) }}</span>
                <span *ngIf="enr.expiresAt">انتهاء: {{ formatDate(enr.expiresAt) }}</span>
              </div>
            </div>
          </div>
        </div>
      </ng-container>

      <!-- ── Tab: Quizzes ─────────────────────────────────── -->
      <ng-container *ngIf="activeTab() === 'quizzes'">
        <div *ngIf="quizzesLoading()" style="display:flex;flex-direction:column;gap:8px;">
          <div *ngFor="let i of [1,2,3,4]" class="edu-card animate-pulse" style="height:72px;padding:0;"></div>
        </div>
        <div *ngIf="!quizzesLoading()">
          <div *ngIf="quizzesPage().content.length===0" style="text-align:center;padding:48px 0;color:#64748b;">
            <span class="material-icons-round" style="font-size:40px;display:block;margin-bottom:8px;opacity:0.3;">quiz</span>
            <p style="margin:0;">لم يحل ابنك أي امتحان بعد</p>
          </div>
          <div style="display:flex;flex-direction:column;gap:10px;">
            <div *ngFor="let q of quizzesPage().content" class="edu-card" style="padding:14px 16px;">
              <div style="display:flex;align-items:center;justify-content:space-between;gap:12px;">
                <div style="flex:1;min-width:0;">
                  <p style="color:#fff;font-size:13px;font-weight:600;margin:0;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">{{ q.quizTitle }}</p>
                  <p style="color:#64748b;font-size:11px;margin:3px 0 0;">{{ formatDateTime(q.submittedAt) }} • محاولة {{ q.attemptNumber }}</p>
                </div>
                <div style="text-align:center;flex-shrink:0;">
                  <div style="font-size:18px;font-weight:700;" [style.color]="q.passed ? '#34d399' : '#f87171'">
                    {{ q.score ?? 0 }}<span style="font-size:11px;color:#64748b;"> / {{ q.totalMarks }}</span>
                  </div>
                  <span style="font-size:11px;padding:2px 8px;border-radius:4px;font-weight:600;"
                        [style.background]="q.passed?'rgba(16,185,129,.15)':'rgba(239,68,68,.15)'"
                        [style.color]="q.passed?'#34d399':'#f87171'">{{ q.passed ? 'ناجح' : 'راسب' }}</span>
                </div>
              </div>
              <div *ngIf="q.totalMarks > 0" style="margin-top:10px;">
                <div style="height:5px;border-radius:3px;background:#334155;overflow:hidden;">
                  <div style="height:100%;border-radius:3px;transition:width 500ms;"
                       [style.width.%]="scorePct(q.score, q.totalMarks)"
                       [style.background]="q.passed?'#10b981':'#ef4444'"></div>
                </div>
                <div style="text-align:left;font-size:11px;color:#475569;margin-top:2px;">
                  {{ scorePct(q.score, q.totalMarks) }}%
                  <span *ngIf="q.correctAnswers != null"> • {{ q.correctAnswers }} إجابة صحيحة</span>
                </div>
              </div>
            </div>
          </div>
          <div *ngIf="quizzesPage().totalPages > 1" style="display:flex;justify-content:center;gap:8px;margin-top:20px;">
            <button (click)="loadQuizzes(quizzesPage().number-1)" [disabled]="quizzesPage().number===0"
                    style="padding:6px 12px;border-radius:6px;font-size:13px;color:#94a3b8;background:none;border:1px solid #334155;cursor:pointer;font-family:'Cairo',sans-serif;"
                    [style.opacity]="quizzesPage().number===0?'0.3':'1'">السابق</button>
            <span style="padding:6px 12px;color:#64748b;font-size:13px;">{{ quizzesPage().number+1 }} / {{ quizzesPage().totalPages }}</span>
            <button (click)="loadQuizzes(quizzesPage().number+1)" [disabled]="quizzesPage().number+1>=quizzesPage().totalPages"
                    style="padding:6px 12px;border-radius:6px;font-size:13px;color:#94a3b8;background:none;border:1px solid #334155;cursor:pointer;font-family:'Cairo',sans-serif;"
                    [style.opacity]="quizzesPage().number+1>=quizzesPage().totalPages?'0.3':'1'">التالي</button>
          </div>
        </div>
      </ng-container>

      <!-- ── Tab: Assignments ─────────────────────────────── -->
      <ng-container *ngIf="activeTab() === 'assignments'">
        <div *ngIf="assignmentsLoading()" style="display:flex;flex-direction:column;gap:8px;">
          <div *ngFor="let i of [1,2,3,4]" class="edu-card animate-pulse" style="height:60px;padding:0;"></div>
        </div>
        <div *ngIf="!assignmentsLoading()">
          <div *ngIf="assignmentsPage().content.length===0" style="text-align:center;padding:48px 0;color:#64748b;">
            <span class="material-icons-round" style="font-size:40px;display:block;margin-bottom:8px;opacity:0.3;">assignment</span>
            <p style="margin:0;">لم يسلّم ابنك أي واجب بعد</p>
          </div>
          <div style="display:flex;flex-direction:column;gap:10px;">
            <div *ngFor="let a of assignmentsPage().content" class="edu-card" style="padding:14px 16px;">
              <div style="display:flex;align-items:center;justify-content:space-between;gap:12px;">
                <div style="flex:1;min-width:0;">
                  <p style="color:#fff;font-size:13px;font-weight:600;margin:0;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">{{ a.assignmentTitle }}</p>
                  <p style="color:#64748b;font-size:11px;margin:3px 0 0;">{{ formatDateTime(a.submittedAt) }}</p>
                </div>
                <div style="text-align:center;flex-shrink:0;">
                  <ng-container *ngIf="a.score != null">
                    <div style="font-size:18px;font-weight:700;color:#818cf8;">{{ a.score }}</div>
                    <div style="font-size:11px;color:#64748b;">درجة</div>
                  </ng-container>
                  <span *ngIf="a.score == null" style="font-size:11px;padding:2px 8px;border-radius:4px;background:rgba(245,158,11,.15);color:#fbbf24;font-weight:600;">في انتظار التصحيح</span>
                </div>
              </div>
            </div>
          </div>
          <div *ngIf="assignmentsPage().totalPages > 1" style="display:flex;justify-content:center;gap:8px;margin-top:20px;">
            <button (click)="loadAssignments(assignmentsPage().number-1)" [disabled]="assignmentsPage().number===0"
                    style="padding:6px 12px;border-radius:6px;font-size:13px;color:#94a3b8;background:none;border:1px solid #334155;cursor:pointer;font-family:'Cairo',sans-serif;"
                    [style.opacity]="assignmentsPage().number===0?'0.3':'1'">السابق</button>
            <span style="padding:6px 12px;color:#64748b;font-size:13px;">{{ assignmentsPage().number+1 }} / {{ assignmentsPage().totalPages }}</span>
            <button (click)="loadAssignments(assignmentsPage().number+1)" [disabled]="assignmentsPage().number+1>=assignmentsPage().totalPages"
                    style="padding:6px 12px;border-radius:6px;font-size:13px;color:#94a3b8;background:none;border:1px solid #334155;cursor:pointer;font-family:'Cairo',sans-serif;"
                    [style.opacity]="assignmentsPage().number+1>=assignmentsPage().totalPages?'0.3':'1'">التالي</button>
          </div>
        </div>
      </ng-container>

      <!-- ── Tab: Wallet ──────────────────────────────────── -->
      <ng-container *ngIf="activeTab() === 'wallet'">
        <div *ngIf="walletLoading()" class="edu-card animate-pulse" style="height:160px;margin-bottom:12px;"></div>
        <div *ngIf="!walletLoading()">
          <div *ngIf="!wallet()" style="text-align:center;padding:48px 0;color:#64748b;">
            <span class="material-icons-round" style="font-size:40px;display:block;margin-bottom:8px;opacity:0.3;">account_balance_wallet</span>
            <p style="margin:0;">لا توجد محفظة مرتبطة بهذا الحساب</p>
          </div>
          <ng-container *ngIf="wallet()">
            <div class="edu-card" style="margin-bottom:12px;padding:20px;background:linear-gradient(135deg,#0f2027,#203a43,#1a3a2a);border:1px solid rgba(16,185,129,.2);">
              <div style="color:#64748b;font-size:12px;margin-bottom:4px;">الرصيد الحالي</div>
              <div style="font-size:28px;font-weight:700;color:#34d399;">{{ walletBalance() | number:'1.2-2' }} <span style="font-size:14px;">ج.م</span></div>
              <div style="display:flex;gap:24px;margin-top:12px;">
                <div>
                  <div style="color:#64748b;font-size:11px;">إجمالي الإيداعات</div>
                  <div style="color:#60a5fa;font-size:14px;font-weight:600;">{{ wallet().totalDeposited | number:'1.2-2' }} ج.م</div>
                </div>
                <div>
                  <div style="color:#64748b;font-size:11px;">إجمالي المصروفات</div>
                  <div style="color:#f87171;font-size:14px;font-weight:600;">{{ wallet().totalSpent | number:'1.2-2' }} ج.م</div>
                </div>
              </div>
            </div>
            <div class="edu-card" *ngIf="wallet().recentTransactions?.length > 0">
              <h3 style="color:#fff;font-weight:600;margin:0 0 14px;font-size:14px;">آخر المعاملات</h3>
              <div style="display:flex;flex-direction:column;gap:8px;">
                <div *ngFor="let tx of wallet().recentTransactions" style="display:flex;align-items:center;justify-content:space-between;padding:10px 0;border-bottom:1px solid #1e293b;">
                  <div style="display:flex;align-items:center;gap:10px;">
                    <span class="material-icons-round" style="font-size:18px;"
                          [style.color]="tx.type==='DEPOSIT'?'#34d399':tx.type==='REFUND'?'#60a5fa':'#f87171'">
                      {{ tx.type==='DEPOSIT'?'add_circle':tx.type==='REFUND'?'refresh':'remove_circle' }}
                    </span>
                    <div>
                      <p style="color:#fff;font-size:12px;font-weight:500;margin:0;">{{ txTypeLabel(tx.type) }}</p>
                      <p style="color:#475569;font-size:11px;margin:2px 0 0;">{{ tx.description || '' }}</p>
                      <p style="color:#64748b;font-size:10px;margin:1px 0 0;">{{ formatDateTime(tx.createdAt) }}</p>
                    </div>
                  </div>
                  <div style="text-align:left;">
                    <div style="font-size:14px;font-weight:700;"
                         [style.color]="tx.type==='DEPOSIT'?'#34d399':tx.type==='REFUND'?'#60a5fa':'#f87171'">
                      {{ tx.type==='PURCHASE'?'-':'+' }}{{ tx.amount | number:'1.2-2' }}
                    </div>
                    <div style="color:#475569;font-size:10px;">رصيد: {{ tx.balanceAfter | number:'1.2-2' }}</div>
                  </div>
                </div>
              </div>
            </div>
          </ng-container>
        </div>
      </ng-container>


      <!-- ── Tab: Analytics ─────────────────────────────────── -->
      <ng-container *ngIf="activeTab() === 'analytics'">
        <div *ngIf="analyticsLoading()" style="display:flex;flex-direction:column;gap:8px;">
          <div *ngFor="let i of [1,2,3,4]" class="edu-card animate-pulse" style="height:80px;padding:0;"></div>
        </div>
        <div *ngIf="!analyticsLoading() && !analytics()" style="text-align:center;padding:48px 0;color:#64748b;">
          <span class="material-icons-round" style="font-size:40px;display:block;margin-bottom:8px;opacity:0.3;">bar_chart</span>
          <p style="margin:0;">لا توجد بيانات</p>
        </div>
        <ng-container *ngIf="!analyticsLoading() && analytics()">
          <!-- KPI Row -->
          <div style="display:grid;grid-template-columns:repeat(2,1fr);gap:10px;margin-bottom:16px;">
            <div class="edu-card" style="text-align:center;padding:14px;">
              <div style="font-size:26px;font-weight:700;color:#34d399;">{{ analyticsStreak() }}</div>
              <div style="color:#64748b;font-size:11px;margin-top:4px;">ستريك (أيام)</div>
            </div>
            <div class="edu-card" style="text-align:center;padding:14px;">
              <div style="font-size:26px;font-weight:700;color:#60a5fa;">{{ analyticsAvgScore() }}%</div>
              <div style="color:#64748b;font-size:11px;margin-top:4px;">متوسط الدرجات</div>
            </div>
            <div class="edu-card" style="text-align:center;padding:14px;">
              <div style="font-size:26px;font-weight:700;color:#f59e0b;">{{ analyticsQuizCount() }}</div>
              <div style="color:#64748b;font-size:11px;margin-top:4px;">كويزات أكملها</div>
            </div>
            <div class="edu-card" style="text-align:center;padding:14px;">
              <div style="font-size:26px;font-weight:700;color:#a78bfa;">{{ analyticsAchievements() }}</div>
              <div style="color:#64748b;font-size:11px;margin-top:4px;">إنجازات</div>
            </div>
          </div>

          <!-- Scores vs Average Table -->
          <div class="edu-card" style="margin-bottom:12px;">
            <div style="padding:12px 14px;font-size:13px;font-weight:600;color:#fff;border-bottom:1px solid #1e293b;">درجاته مقارنةً بالمتوسط</div>
            <div *ngIf="!(analytics().vsAvg?.length > 0)" style="padding:24px;text-align:center;color:#64748b;font-size:13px;">لا توجد بيانات</div>
            <div *ngFor="let row of analytics().vsAvg" style="padding:10px 14px;border-bottom:1px solid #0f172a;display:flex;align-items:center;gap:10px;">
              <span style="flex:1;font-size:12px;color:#94a3b8;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">{{ row.course ?? row.courseName ?? row.label }}</span>
              <span style="font-size:12px;font-weight:700;color:#34d399;min-width:36px;text-align:left;">{{ (row.myScore ?? row.myAvg ?? 0) | number:'1.0-1' }}%</span>
              <span style="font-size:11px;color:#64748b;min-width:36px;text-align:left;">↔ {{ (row.allAvg ?? 0) | number:'1.0-1' }}%</span>
            </div>
          </div>

          <!-- Progress over time -->
          <div class="edu-card" style="margin-bottom:12px;">
            <div style="padding:12px 14px;font-size:13px;font-weight:600;color:#fff;border-bottom:1px solid #1e293b;">تطور أداءه في الكويزات</div>
            <div *ngIf="!(analytics().progressOverTime?.length > 0)" style="padding:24px;text-align:center;color:#64748b;font-size:13px;">لا توجد بيانات</div>
            <div style="padding:10px 14px;display:flex;flex-direction:column;gap:6px;">
              <div *ngFor="let row of (analytics().progressOverTime ?? []).slice().reverse().slice(0,10)" style="display:flex;align-items:center;gap:8px;">
                <span style="font-size:11px;color:#64748b;min-width:80px;text-align:right;">{{ row.label ?? row.attemptDate ?? row.date }}</span>
                <div style="flex:1;height:6px;background:#1e293b;border-radius:4px;overflow:hidden;">
                  <div [style.width.%]="row.avgScore ?? row.score ?? row.percentage ?? 0"
                       style="height:100%;border-radius:4px;"
                       [style.background]="(row.avgScore ?? row.score ?? row.percentage ?? 0) >= 70 ? '#34d399' : (row.avgScore ?? row.score ?? row.percentage ?? 0) >= 50 ? '#f59e0b' : '#f87171'"></div>
                </div>
                <span style="font-size:12px;font-weight:700;min-width:32px;text-align:left;"
                      [style.color]="(row.avgScore ?? row.score ?? row.percentage ?? 0) >= 70 ? '#34d399' : (row.avgScore ?? row.score ?? row.percentage ?? 0) >= 50 ? '#f59e0b' : '#f87171'">{{ row.avgScore ?? row.score ?? row.percentage ?? 0 }}%</span>
              </div>
            </div>
          </div>

          <!-- Achievements -->
          <div class="edu-card" *ngIf="analytics().achievements?.length > 0">
            <div style="padding:12px 14px;font-size:13px;font-weight:600;color:#fff;border-bottom:1px solid #1e293b;">إنجازاته 🏆</div>
            <div style="padding:12px;display:flex;flex-wrap:wrap;gap:8px;">
              <ng-container *ngFor="let a of analytics().achievements">
                <span *ngIf="$any(a).unlocked !== false"
                      style="padding:5px 10px;border-radius:8px;background:rgba(52,211,153,.1);border:1px solid rgba(52,211,153,.25);color:#34d399;font-size:12px;font-weight:500;">
                  {{ $any(a).icon }} {{ $any(a).label ?? $any(a).name ?? '🏅 إنجاز' }}
                </span>
              </ng-container>
            </div>
          </div>
        </ng-container>
      </ng-container>

      <!-- ── Tab: Activity ────────────────────────────────── -->
      <ng-container *ngIf="activeTab() === 'activity'">
        <div *ngIf="activityLoading() && activityPage().content.length === 0" style="display:flex;flex-direction:column;gap:8px;">
          <div *ngFor="let i of [1,2,3,4,5]" class="edu-card animate-pulse" style="height:56px;padding:0;"></div>
        </div>
        <div *ngIf="!activityLoading() || activityPage().content.length > 0">
          <div *ngIf="activityPage().content.length === 0" style="text-align:center;padding:48px 0;color:#64748b;">
            <span class="material-icons-round" style="font-size:40px;display:block;margin-bottom:8px;opacity:0.3;">history</span>
            <p style="margin:0;">لا يوجد نشاط مسجل</p>
          </div>
          <div style="display:flex;flex-direction:column;gap:8px;">
            <div *ngFor="let log of activityPage().content" class="edu-card" style="padding:12px 16px;display:flex;align-items:flex-start;gap:12px;">
              <div style="width:32px;height:32px;border-radius:8px;display:flex;align-items:center;justify-content:center;flex-shrink:0;"
                   [style.background]="activityBg(log.eventType)">
                <span class="material-icons-round" style="font-size:16px;" [style.color]="activityColor(log.eventType)">
                  {{ activityIcon(log.eventType) }}
                </span>
              </div>
              <div style="flex:1;min-width:0;">
                <p style="color:#fff;font-size:13px;font-weight:500;margin:0;">{{ log.title }}</p>
                <!-- Hide device UUIDs from details -->
                <p *ngIf="cleanDetails(log.details)" style="color:#64748b;font-size:11px;margin:2px 0 0;">{{ cleanDetails(log.details) }}</p>
                <!-- Date + Time -->
                <p style="color:#475569;font-size:11px;margin:3px 0 0;">{{ formatDateTime(log.createdAt) }}</p>
              </div>
            </div>
          </div>
          <div *ngIf="activityPage().totalPages > 1" style="display:flex;justify-content:center;gap:8px;margin-top:20px;">
            <button (click)="loadActivity(activityPage().number-1)" [disabled]="activityPage().number===0"
                    style="padding:6px 12px;border-radius:6px;font-size:13px;color:#94a3b8;background:none;border:1px solid #334155;cursor:pointer;font-family:'Cairo',sans-serif;"
                    [style.opacity]="activityPage().number===0?'0.3':'1'">السابق</button>
            <span style="padding:6px 12px;color:#64748b;font-size:13px;">{{ activityPage().number+1 }} / {{ activityPage().totalPages }}</span>
            <button (click)="loadActivity(activityPage().number+1)" [disabled]="activityPage().number+1>=activityPage().totalPages"
                    style="padding:6px 12px;border-radius:6px;font-size:13px;color:#94a3b8;background:none;border:1px solid #334155;cursor:pointer;font-family:'Cairo',sans-serif;"
                    [style.opacity]="activityPage().number+1>=activityPage().totalPages?'0.3':'1'">التالي</button>
          </div>
        </div>
      </ng-container>
    </div>
  `
})
export class ChildDetailComponent implements OnInit {
  studentId = 0;
  activeTab = signal<Tab>('overview');

  overviewLoading = signal(true);
  overview = signal<any>(null);

  attendanceLoading = signal(false);
  attendancePage = signal<any>({ content: [], number: 0, totalPages: 0 });

  enrollmentsLoading = signal(false);
  enrollments = signal<any[]>([]);

  quizzesLoading = signal(false);
  quizzesPage = signal<any>({ content: [], number: 0, totalPages: 0 });

  assignmentsLoading = signal(false);
  assignmentsPage = signal<any>({ content: [], number: 0, totalPages: 0 });

  walletLoading = signal(false);
  wallet = signal<any>(null);

  activityLoading = signal(false);
  activityPage = signal<any>({ content: [], number: 0, totalPages: 0 });

  analyticsLoading = signal(false);
  analytics = signal<any>(null);

  tabs = [
    { key: 'overview'     as Tab, label: 'نظرة عامة',    icon: 'dashboard' },
    { key: 'attendance'   as Tab, label: 'الحضور',        icon: 'fact_check' },
    { key: 'enrollments'  as Tab, label: 'الاشتراكات',    icon: 'school' },
    { key: 'quizzes'      as Tab, label: 'الامتحانات',    icon: 'quiz' },
    { key: 'assignments'  as Tab, label: 'الواجبات',      icon: 'assignment' },
    { key: 'wallet'       as Tab, label: 'المحفظة',       icon: 'account_balance_wallet' },
    { key: 'activity'     as Tab, label: 'سجل النشاط',   icon: 'history' },
    { key: 'analytics'    as Tab, label: 'الإحصائيات',    icon: 'bar_chart' },
  ];

  constructor(private route: ActivatedRoute, private parentApi: ParentApiService) {}

  ngOnInit(): void {
    this.studentId = +this.route.snapshot.paramMap.get('studentId')!;
    this.loadOverview();
    this.loadAttendance(0);
    this.loadEnrollments();
    this.loadQuizzes(0);
    this.loadAssignments(0);
    this.loadWallet();
    this.loadActivity(0);
    this.loadAnalytics();
  }

  switchTab(tab: Tab): void { this.activeTab.set(tab); }

  loadOverview(): void {
    this.overviewLoading.set(true);
    this.parentApi.getChildOverview(this.studentId).subscribe({
      next: (res: any) => { this.overview.set(res?.data ?? res); this.overviewLoading.set(false); },
      error: () => this.overviewLoading.set(false)
    });
  }

  loadAttendance(page: number): void {
    this.attendanceLoading.set(true);
    this.parentApi.getChildAttendance(this.studentId, page, 15).subscribe({
      next: (res: any) => {
        const r = res?.data ?? res;
        this.attendancePage.set({ content: Array.isArray(r) ? r : (r?.content || []), number: r?.number ?? page, totalPages: r?.totalPages ?? 1 });
        this.attendanceLoading.set(false);
      },
      error: () => this.attendanceLoading.set(false)
    });
  }

  loadEnrollments(): void {
    this.enrollmentsLoading.set(true);
    this.parentApi.getChildEnrollments(this.studentId).subscribe({
      next: (res: any) => { const d = res?.data ?? res; this.enrollments.set(Array.isArray(d) ? d : (d?.content || [])); this.enrollmentsLoading.set(false); },
      error: () => this.enrollmentsLoading.set(false)
    });
  }

  loadQuizzes(page: number): void {
    this.quizzesLoading.set(true);
    this.parentApi.getChildQuizResults(this.studentId, page, 10).subscribe({
      next: (res: any) => {
        const r = res?.data ?? res;
        this.quizzesPage.set({ content: r?.content || [], number: r?.number ?? page, totalPages: r?.totalPages ?? 1 });
        this.quizzesLoading.set(false);
      },
      error: () => this.quizzesLoading.set(false)
    });
  }

  loadAssignments(page: number): void {
    this.assignmentsLoading.set(true);
    this.parentApi.getChildAssignmentResults(this.studentId, page, 10).subscribe({
      next: (res: any) => {
        const r = res?.data ?? res;
        this.assignmentsPage.set({ content: r?.content || [], number: r?.number ?? page, totalPages: r?.totalPages ?? 1 });
        this.assignmentsLoading.set(false);
      },
      error: () => this.assignmentsLoading.set(false)
    });
  }

  loadWallet(): void {
    this.walletLoading.set(true);
    this.parentApi.getChildWallet(this.studentId).subscribe({
      next: (res: any) => { this.wallet.set(res?.data ?? res); this.walletLoading.set(false); },
      error: () => { this.wallet.set(null); this.walletLoading.set(false); }
    });
  }

  loadActivity(page: number): void {
    this.activityLoading.set(true);
    this.parentApi.getChildActivity(this.studentId, page, 15).subscribe({
      next: (res: any) => {
        const r = res?.data ?? res;
        this.activityPage.set({ content: r?.content || [], number: r?.number ?? page, totalPages: r?.totalPages ?? 1 });
        this.activityLoading.set(false);
      },
      error: () => this.activityLoading.set(false)
    });
  }

  loadAnalytics(): void {
    this.analyticsLoading.set(true);
    this.parentApi.getChildAnalytics(this.studentId).subscribe({
      next: (res: any) => { this.analytics.set(res?.data ?? res); this.analyticsLoading.set(false); },
      error: () => this.analyticsLoading.set(false)
    });
  }

  analyticsStreak(): number { return this.analytics()?.streak?.currentStreak ?? 0; }
  analyticsAvgScore(): number {
    const vsAvg: any[] = this.analytics()?.vsAvg ?? [];
    if (!vsAvg.length) return 0;
    return Math.round(vsAvg.reduce((s: number, r: any) => s + (r.myScore ?? r.myAvg ?? 0), 0) / vsAvg.length);
  }
  analyticsQuizCount(): number { return (this.analytics()?.progressOverTime ?? []).length; }
  analyticsAchievements(): number { return (this.analytics()?.achievements ?? []).filter((a: any) => a.unlocked !== false).length; }

  // ── Computed helpers ──────────────────────────────────────────

  totalLessons(ov: any): number { return ov?.completedLessons ?? ov?.totalLessons ?? 0; }
  completionPct(ov: any): number {
    const total = ov?.totalLessons ?? 0;
    const done  = ov?.completedLessons ?? 0;
    return total > 0 ? Math.round((done / total) * 100) : 0;
  }

  enrollmentTypeLabel(type: string): string {
    const m: Record<string,string> = { MONTHLY: 'شهري', TERM: 'ترم', SINGLE_SESSION: 'حصة منفردة' };
    return m[type] ?? type ?? '';
  }

  enrollmentStatusLabel(status: string): string {
    const m: Record<string,string> = { ACTIVE: 'نشط', EXPIRED: 'منتهي', PENDING: 'معلق', CANCELLED: 'ملغي' };
    return m[status] ?? status ?? '';
  }

  formatDate(dt: string): string {
    if (!dt) return '';
    try { return new Date(dt).toLocaleDateString('ar-EG'); } catch { return dt; }
  }

  scorePct(score: number, total: number): number {
    if (!total || total === 0) return 0;
    return Math.round((score / total) * 100);
  }

  txTypeLabel(type: string): string {
    const m: Record<string,string> = { DEPOSIT: 'إيداع', SPEND: 'صرف', REFUND: 'استرداد', BONUS: 'مكافأة' };
    return m[type] ?? type ?? '';
  }

  activeEnrollments(): any[] {
    return (this.enrollments() || []).filter((e: any) => e.status === 'ACTIVE');
  }

  activeEnrollmentsCount(): number {
    return this.activeEnrollments().length;
  }

  walletBalance(): number {
    const w = this.wallet();
    if (!w) return 0;
    const deposited = Number(w.totalDeposited ?? 0);
    const spent     = Number(w.totalSpent     ?? 0);
    const eff = w.effectiveBalance != null ? Number(w.effectiveBalance) : null;
    if (eff !== null && Math.abs(eff - (deposited + spent)) > 0.01) return eff;
    return deposited - spent;
  }

  isOnline(studyType: string): boolean {
    if (!studyType) return true;
    const t = studyType.toLowerCase();
    return t === 'online' || t === 'أونلاين';
  }

  getInitials(name: string): string {
    if (!name) return '؟';
    const parts = name.trim().split(' ');
    return parts.length >= 2 ? parts[0][0] + parts[1][0] : parts[0][0];
  }

  attendanceType(type: string): string {
    if (!type) return '';
    return type === 'ONLINE' ? 'أونلاين' : type === 'CENTER' ? 'سنتر' : type;
  }

  formatDateTime(dt: string): string {
    if (!dt) return '';
    try {
      return new Date(dt).toLocaleString('ar-EG', { dateStyle: 'short', timeStyle: 'short' });
    } catch { return dt; }
  }

  cleanDetails(details: string): string {
    if (!details) return '';
    return details.replace(/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/gi, '').trim();
  }

  activityIcon(type: string): string {
    const m: Record<string, string> = {
      LOGIN: 'login', LOGOUT: 'logout', QUIZ_ATTEMPT: 'quiz',
      ASSIGNMENT_ATTEMPT: 'assignment', VIDEO_WATCH: 'play_circle',
      ENROLLMENT: 'school', WALLET_TOPUP: 'account_balance_wallet',
      WALLET_SPEND: 'payments', ATTENDANCE: 'fact_check'
    };
    return m[type] || 'circle';
  }

  activityColor(type: string): string {
    const m: Record<string, string> = {
      LOGIN: '#34d399', LOGOUT: '#94a3b8', QUIZ_ATTEMPT: '#60a5fa',
      ASSIGNMENT_ATTEMPT: '#a78bfa', VIDEO_WATCH: '#f59e0b',
      ENROLLMENT: '#10b981', WALLET_TOPUP: '#34d399',
      WALLET_SPEND: '#f87171', ATTENDANCE: '#22d3ee'
    };
    return m[type] || '#64748b';
  }

  activityBg(type: string): string {
    return this.activityColor(type) + '20';
  }
}
