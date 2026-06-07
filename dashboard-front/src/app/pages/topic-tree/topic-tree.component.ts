import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-topic-tree',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6 animate-fade-in pb-10">

      <!-- Header -->
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-white font-bold text-2xl">شجرة المحتوى</h2>
          <p class="text-slate-500 text-sm mt-1">إدارة المحاضرات → المواضيع → النقاط لربطها بالأسئلة</p>
        </div>
      </div>

      <!-- Filters: Level -> Session -->
      <div class="edu-card p-4 flex items-center gap-4 flex-wrap">
        <div class="flex-1 min-w-48">
          <label class="text-slate-400 text-xs font-bold block mb-1">الصف الدراسي</label>
          <select [(ngModel)]="selectedLevelId" (change)="onLevelChange()" class="edu-select w-full">
            <option [value]="0">-- اختر الصف الدراسي --</option>
            <option *ngFor="let lv of levels()" [value]="lv.id">{{ lv.name || lv.title }}</option>
          </select>
        </div>
        <div class="flex-1 min-w-48" *ngIf="sessions().length > 0 || selectedLevelId > 0">
          <label class="text-slate-400 text-xs font-bold block mb-1">المحاضرة</label>
          <select [(ngModel)]="selectedSessionId" (change)="loadTree()" class="edu-select w-full">
            <option [value]="0">-- اختر محاضرة --</option>
            <option *ngFor="let s of sessions()" [value]="s.id">{{ s.title }}</option>
          </select>
        </div>
      </div>

      <!-- Empty: no level selected -->
      <div *ngIf="selectedLevelId === 0" class="py-20 text-center text-slate-700 border-2 border-dashed border-slate-800 rounded-3xl">
        <span class="material-icons-round text-5xl block mb-3 opacity-20">school</span>
        <p class="text-sm">اختر الصف الدراسي أولاً لعرض المحاضرات</p>
      </div>

      <!-- Loading sessions -->
      <div *ngIf="selectedLevelId > 0 && loadingSessions()" class="text-center py-10">
        <span class="material-icons-round animate-spin text-indigo-400 text-3xl">refresh</span>
      </div>

      <!-- No sessions for level -->
      <div *ngIf="selectedLevelId > 0 && !loadingSessions() && sessions().length === 0"
           class="py-20 text-center text-slate-700 border-2 border-dashed border-slate-800 rounded-3xl">
        <span class="material-icons-round text-5xl block mb-3 opacity-20">live_tv</span>
        <p class="text-sm">لا توجد محاضرات مرتبطة بهذا الصف الدراسي</p>
      </div>

      <!-- Tree -->
      <div *ngIf="selectedSessionId > 0">
        <!-- Add root topic -->
        <div class="edu-card p-4 mb-4">
          <div class="flex items-center gap-3">
            <input [(ngModel)]="newTopicName" placeholder="اسم الموضوع الجديد (مستوى رئيسي)..."
                   class="edu-input flex-1" (keydown.enter)="addTopic(null)">
            <button (click)="addTopic(null)" [disabled]="!newTopicName.trim()" class="btn-primary gap-2">
              <span class="material-icons-round text-sm">add</span> إضافة موضوع
            </button>
          </div>
        </div>

        <!-- Loading -->
        <div *ngIf="loadingTree()" class="text-center py-16">
          <span class="material-icons-round animate-spin text-indigo-400 text-4xl">refresh</span>
        </div>

        <!-- Empty -->
        <div *ngIf="!loadingTree() && topics().length === 0"
             class="py-20 text-center text-slate-700 border-2 border-dashed border-slate-800 rounded-3xl">
          <span class="material-icons-round text-5xl block mb-3 opacity-20">account_tree</span>
          <p class="text-sm">لا توجد مواضيع بعد — ابدأ بإضافة موضوع رئيسي</p>
        </div>

        <!-- Topics tree -->
        <div *ngIf="!loadingTree()" class="space-y-3">
          <ng-container *ngFor="let topic of topics()">
            <ng-container *ngTemplateOutlet="topicNode; context: { $implicit: topic, level: 0 }"></ng-container>
          </ng-container>
        </div>
      </div>

    </div>

    <!-- Topic Node Template -->
    <ng-template #topicNode let-topic let-level="level">
      <div [style.marginRight]="(level * 24) + 'px'"
           class="edu-card p-3 border-slate-800 group"
           [ngClass]="level === 0 ? 'border-l-4 border-l-indigo-500' : level === 1 ? 'border-l-4 border-l-orange-400' : 'border-l-4 border-l-slate-600'">
        <div class="flex items-center gap-3">
          <!-- Icon -->
          <span class="material-icons-round text-base flex-shrink-0"
                [ngClass]="level === 0 ? 'text-indigo-400' : level === 1 ? 'text-orange-400' : 'text-slate-500'">
            {{ level === 0 ? 'folder' : level === 1 ? 'topic' : 'fiber_manual_record' }}
          </span>

          <!-- Editing mode -->
          <ng-container *ngIf="editingId() === topic.id; else viewMode">
            <input [(ngModel)]="editingName" class="edu-input flex-1 text-sm py-1"
                   (keydown.enter)="saveEdit(topic)" (keydown.escape)="editingId.set(0)">
            <button (click)="saveEdit(topic)" class="btn-primary py-1 px-3 text-xs gap-1">
              <span class="material-icons-round text-xs">check</span> حفظ
            </button>
            <button (click)="editingId.set(0)" class="btn-secondary py-1 px-2 text-xs">إلغاء</button>
          </ng-container>

          <!-- View mode -->
          <ng-template #viewMode>
            <span class="flex-1 text-slate-200 font-bold text-sm">{{ topic.name }}</span>
            <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
              <!-- Add child -->
              <button (click)="startAddChild(topic)"
                      class="btn-icon h-7 w-7 bg-slate-800 text-slate-400 hover:text-indigo-400"
                      title="إضافة نقطة فرعية">
                <span class="material-icons-round text-xs">add</span>
              </button>
              <!-- Edit -->
              <button (click)="startEdit(topic)"
                      class="btn-icon h-7 w-7 bg-slate-800 text-slate-400 hover:text-white">
                <span class="material-icons-round text-xs">edit</span>
              </button>
              <!-- Delete -->
              <button (click)="deleteTopic(topic)"
                      class="btn-icon h-7 w-7 bg-slate-800 text-slate-400 hover:text-red-400">
                <span class="material-icons-round text-xs">delete</span>
              </button>
            </div>
          </ng-template>
        </div>

        <!-- Inline add child -->
        <div *ngIf="addingChildOf() === topic.id" class="flex items-center gap-2 mt-2 pr-7">
          <input [(ngModel)]="newChildName" placeholder="اسم النقطة الفرعية..."
                 class="edu-input flex-1 text-sm py-1"
                 (keydown.enter)="addTopic(topic)" (keydown.escape)="addingChildOf.set(0)">
          <button (click)="addTopic(topic)" [disabled]="!newChildName.trim()"
                  class="btn-primary py-1 px-3 text-xs gap-1">
            <span class="material-icons-round text-xs">add</span> إضافة
          </button>
          <button (click)="addingChildOf.set(0)" class="btn-secondary py-1 px-2 text-xs">إلغاء</button>
        </div>
      </div>

      <!-- Sub-topics recursively -->
      <ng-container *ngFor="let child of topic.subTopics">
        <ng-container *ngTemplateOutlet="topicNode; context: { $implicit: child, level: level + 1 }"></ng-container>
      </ng-container>
    </ng-template>
  `
})
export class TopicTreeComponent implements OnInit {
  levels          = signal<any[]>([]);
  sessions        = signal<any[]>([]);
  topics          = signal<any[]>([]);
  loadingSessions = signal(false);
  loadingTree     = signal(false);

  selectedLevelId   = 0;
  selectedSessionId = 0;

  newTopicName  = '';
  newChildName  = '';
  editingId     = signal(0);
  editingName   = '';
  addingChildOf = signal(0);

  constructor(private api: ApiService, private toast: ToastrService) {}

  ngOnInit() {
    this.api.getLevels().subscribe({
      next: (r: any) => this.levels.set(Array.isArray(r) ? r : (r?.data ?? r?.content ?? [])),
      error: () => {}
    });
  }

  onLevelChange() {
    this.sessions.set([]); this.topics.set([]);
    this.selectedSessionId = 0;
    if (!this.selectedLevelId) return;
    this.loadingSessions.set(true);
    this.api.getSessionsByLevel(this.selectedLevelId).subscribe({
      next: (s: any) => { this.sessions.set(Array.isArray(s) ? s : (s?.data ?? [])); this.loadingSessions.set(false); },
      error: () => this.loadingSessions.set(false)
    });
  }

  loadTree() {
    this.topics.set([]);
    if (!this.selectedSessionId) return;
    this.loadingTree.set(true);
    this.api.getTopicTree(this.selectedSessionId).subscribe({
      next: t => { this.topics.set(t); this.loadingTree.set(false); },
      error: () => this.loadingTree.set(false)
    });
  }

  addTopic(parent: any | null) {
    const name = parent ? this.newChildName.trim() : this.newTopicName.trim();
    if (!name) return;
    const body: any = { name, sessionId: this.selectedSessionId };
    if (parent) body.parentTopicId = parent.id;
    this.api.createTopic(body).subscribe({
      next: () => {
        this.newTopicName = ''; this.newChildName = '';
        this.addingChildOf.set(0);
        this.loadTree();
        this.toast.success('تم إضافة الجزئية');
      },
      error: () => this.toast.error('خطأ في الإضافة')
    });
  }

  startAddChild(topic: any) { this.addingChildOf.set(topic.id); this.newChildName = ''; }
  startEdit(topic: any)     { this.editingId.set(topic.id); this.editingName = topic.name; }

  saveEdit(topic: any) {
    if (!this.editingName.trim()) return;
    this.api.updateTopic(topic.id, { name: this.editingName, sessionId: this.selectedSessionId }).subscribe({
      next: () => { this.editingId.set(0); this.loadTree(); this.toast.success('تم التعديل'); },
      error: () => this.toast.error('خطأ في التعديل')
    });
  }

  deleteTopic(topic: any) {
    if (!confirm(`حذف "${topic.name}" وكل جزئياتها الفرعية؟`)) return;
    this.api.deleteTopic(topic.id).subscribe({
      next: () => { this.loadTree(); this.toast.success('تم الحذف'); },
      error: () => this.toast.error('خطأ في الحذف')
    });
  }
}
