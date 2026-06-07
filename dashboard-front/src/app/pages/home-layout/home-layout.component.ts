import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CdkDragDrop, CdkDrag, CdkDropList, moveItemInArray } from '@angular/cdk/drag-drop';
import { ApiService } from '../../services/api.service';
import { ToastrService } from 'ngx-toastr';

export interface WidgetConfig {
  id: string;
  label: string;
  icon: string;
  enabled: boolean;
  widthPct: number;   // 25 | 33 | 50 | 66 | 75 | 100
  heightPx: number;   // 0=auto, or 150..500
  rowSpan: number;    // 1 | 2 | 3
  order: number;
}

export const DEFAULT_WIDGETS: WidgetConfig[] = [
  { id: 'welcome',          label: 'كارد الترحيب',              icon: 'waving_hand',    enabled: true,  widthPct: 50,  heightPx: 0, rowSpan: 1, order: 0 },
  { id: 'teacher_card',     label: 'كارد المدرس',                icon: 'person',         enabled: true,  widthPct: 50,  heightPx: 0, rowSpan: 1, order: 1 },
  { id: 'featured_courses', label: 'الكورسات المميزة',           icon: 'star',           enabled: true,  widthPct: 50,  heightPx: 0, rowSpan: 1, order: 2 },
  { id: 'my_courses_link',  label: 'كل الكورسات + الأزرار',     icon: 'menu_book',      enabled: true,  widthPct: 50,  heightPx: 0, rowSpan: 1, order: 3 },
  { id: 'my_progress',      label: 'تقدم الطالب (كورساتك)',     icon: 'trending_up',    enabled: true,  widthPct: 50,  heightPx: 0, rowSpan: 1, order: 4 },
  { id: 'stats',            label: 'الإحصائيات (4 أرقام)',       icon: 'analytics',      enabled: true,  widthPct: 50,  heightPx: 0, rowSpan: 1, order: 5 },
  { id: 'store',            label: 'ستور الكتب',                 icon: 'store',          enabled: false, widthPct: 50,  heightPx: 0, rowSpan: 1, order: 6 },
  { id: 'ai_chat',          label: 'المساعد الذكي',              icon: 'smart_toy',      enabled: false, widthPct: 50,  heightPx: 0, rowSpan: 1, order: 7 },
  { id: 'support',          label: 'عندك مشكلة؟ تواصل مع الدعم',icon: 'support_agent',  enabled: false, widthPct: 100, heightPx: 0, rowSpan: 1, order: 8 },
];

@Component({
  selector: 'app-home-layout',
  standalone: true,
  imports: [CommonModule, FormsModule, CdkDropList, CdkDrag],
  templateUrl: './home-layout.component.html',
  styles: [`
    .cdk-drag-preview  { opacity:.9; box-shadow:0 8px 32px rgba(0,0,0,.2); border-radius:16px; }
    .cdk-drag-placeholder { opacity:.25; border:2px dashed #4BBBA0 !important; background:#e6f7f4 !important; }
    .cdk-drag-animating { transition:transform 250ms cubic-bezier(0,0,.2,1); }
    .cdk-drop-list-dragging .cdk-drag:not(.cdk-drag-placeholder) { transition:transform 250ms cubic-bezier(0,0,.2,1); }
  `]
})
export class HomeLayoutComponent implements OnInit {
  loading = signal(true);
  saving  = signal(false);
  widgets = signal<WidgetConfig[]>([]);

  constructor(private api: ApiService, private toastr: ToastrService) {}

  ngOnInit() {
    this.api.getHomeLayout().subscribe(raw => {
      if (raw) {
        try {
          const saved: WidgetConfig[] = JSON.parse(raw);
          const merged = DEFAULT_WIDGETS.map(def => {
            const found = saved.find((s: any) => s.id === def.id);
            return found ? { ...def, ...found } : def;
          });
          this.widgets.set(merged.sort((a, b) => a.order - b.order));
        } catch { this.widgets.set([...DEFAULT_WIDGETS]); }
      } else {
        this.widgets.set([...DEFAULT_WIDGETS]);
      }
      this.loading.set(false);
    });
  }

  drop(event: CdkDragDrop<WidgetConfig[]>) {
    const arr = [...this.widgets()];
    moveItemInArray(arr, event.previousIndex, event.currentIndex);
    arr.forEach((w, i) => w.order = i);
    this.widgets.set(arr);
  }

  toggle(id: string) {
    this.widgets.set(this.widgets().map(w => w.id === id ? { ...w, enabled: !w.enabled } : w));
  }

  setWidth(id: string, val: number) {
    const v = Math.min(100, Math.max(10, val || 50));
    this.widgets.set(this.widgets().map(w => w.id === id ? { ...w, widthPct: v } : w));
  }

  setHeight(id: string, val: number) {
    const v = Math.max(0, val || 0);
    this.widgets.set(this.widgets().map(w => w.id === id ? { ...w, heightPx: v } : w));
  }

  growRow(id: string)   { this.widgets.set(this.widgets().map(w => w.id === id ? { ...w, rowSpan: Math.min((w.rowSpan||1) + 1, 3) } : w)); }
  shrinkRow(id: string) { this.widgets.set(this.widgets().map(w => w.id === id ? { ...w, rowSpan: Math.max((w.rowSpan||1) - 1, 1) } : w)); }
  canGrowRow(w: WidgetConfig)   { return (w.rowSpan||1) < 3; }
  canShrinkRow(w: WidgetConfig) { return (w.rowSpan||1) > 1; }

  wrapperStyle(w: WidgetConfig): string {
    const pct = w.widthPct ?? 50;
    const colMap: Record<number,number> = {25:3, 33:4, 50:6, 66:8, 75:9, 100:12};
    const closest = Object.keys(colMap).map(Number).reduce((a,b) => Math.abs(b-pct) < Math.abs(a-pct) ? b : a);
    const colSpan = colMap[closest] ?? 6;
    const rowSpan = w.rowSpan ?? 1;
    return `grid-column:span ${colSpan};grid-row:span ${rowSpan}`;
  }
  previewStyle(w: WidgetConfig): string {
    if (w.heightPx) return `height:${w.heightPx}px;overflow:hidden`;
    const autoH: Record<number,string> = { 1: 'min-height:120px', 2: 'min-height:260px', 3: 'min-height:400px' };
    return autoH[w.rowSpan||1] ?? 'min-height:120px';
  }
  // kept for student home
  widgetStyle(w: WidgetConfig): string {
    const h = w.heightPx ? `height:${w.heightPx}px;overflow:hidden` : '';
    return [`width:${w.widthPct}%`, h].filter(Boolean).join(';');
  }

  save() {
    this.saving.set(true);
    const config = JSON.stringify(this.widgets());
    this.api.saveHomeLayout(config).subscribe({
      next: () => { this.saving.set(false); this.toastr.success('تم حفظ التخصيص بنجاح'); },
      error: () => { this.saving.set(false); this.toastr.error('حدث خطأ أثناء الحفظ'); }
    });
  }
}
