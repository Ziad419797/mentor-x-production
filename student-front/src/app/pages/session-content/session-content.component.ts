import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { StudentApiService } from '../../services/student-api.service';
import { forkJoin, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';

@Component({
  selector: 'app-session-content',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './session-content.component.html'
})
export class SessionContentComponent implements OnInit {
  loading      = signal(true);
  sessionId    = signal(0);
  sessionInfo  = signal<any | null>(null);
  weeks        = signal<any[]>([]);
  activeWeekId = signal<number | null>(null);
  materials    = signal<any[]>([]);
  quizzes      = signal<any[]>([]);
  assignments  = signal<any[]>([]);
  loadingWeek  = signal(false);

  // Video modal
  activeVideo  = signal<any | null>(null);
  weekError    = signal('');

  // Unified content list sorted by orderNumber
  contentItems = computed(() => {
    const mats = this.materials().map(m => ({ ...m, _type: 'material' }));
    const qzs  = this.quizzes().map(q  => ({ ...q, _type: 'quiz' }));
    const asgs = this.assignments().map(a => ({ ...a, _type: 'assignment' }));
    return [...mats, ...qzs, ...asgs].sort((a, b) => (a.orderNumber ?? 0) - (b.orderNumber ?? 0));
  });

  constructor(
    private api: StudentApiService,
    private route: ActivatedRoute,
    public router: Router
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.sessionId.set(id);

    this.api.getSessionById(id).subscribe({
      next: s => this.sessionInfo.set(s),
      error: () => {}
    });

    this.api.getWeeksBySession(id).subscribe({
      next: ws => {
        this.weeks.set(Array.isArray(ws) ? ws : []);
        this.loading.set(false);
        if (ws.length > 0) this.selectWeek(ws[0].id);
      },
      error: () => this.loading.set(false)
    });
  }

  selectWeek(weekId: number) {
    this.activeWeekId.set(weekId);
    this.loadingWeek.set(true);
    this.weekError.set('');
    forkJoin({
      materials:   this.api.getMaterialsByWeek(weekId).pipe(
        tap(r => console.log('[materials raw]', r)),
        catchError(e => { console.error('[materials error]', e); return of([]); })
      ),
      quizzes:     this.api.getQuizzesByWeek(weekId).pipe(
        tap(r => console.log('[quizzes raw]', r)),
        catchError(e => { console.error('[quizzes error]', e); return of([]); })
      ),
      assignments: this.api.getAssignmentsByWeek(weekId).pipe(
        tap(r => console.log('[assignments raw]', r)),
        catchError(e => { console.error('[assignments error]', e); return of([]); })
      ),
    }).subscribe(({ materials, quizzes, assignments }) => {
      console.log('[week content]', { materials, quizzes, assignments });
      this.materials.set(Array.isArray(materials) ? materials : []);
      this.quizzes.set(Array.isArray(quizzes) ? quizzes : []);
      this.assignments.set(Array.isArray(assignments) ? assignments : []);
      this.loadingWeek.set(false);
    });
  }

  courseId(): number | null {
    const ids = this.sessionInfo()?.courseIds;
    if (Array.isArray(ids) && ids.length) return ids[0];
    return null;
  }

  getYoutubeEmbed(m: any): string {
    const vid = m.youtubeVideoId || m.fileUrl || '';
    if (!vid) return '';
    const match = vid.match(/(?:v=|youtu.be\/)([\w-]{11})/);
    const id = match ? match[1] : vid;
    return `https://www.youtube.com/embed/${id}`;
  }

  getIcon(type: string): string {
    const t = (type || '').toUpperCase();
    if (t === 'VIDEO' || t === 'YOUTUBE') return 'smart_display';
    if (t === 'PDF') return 'picture_as_pdf';
    if (t === 'IMAGE') return 'image';
    if (t === 'DOC' || t === 'PPT') return 'description';
    return 'attach_file';
  }

  getIconColor(type: string): string {
    const t = (type || '').toUpperCase();
    if (t === 'VIDEO' || t === 'YOUTUBE') return 'text-red-500';
    if (t === 'PDF') return 'text-orange-500';
    if (t === 'IMAGE') return 'text-green-500';
    return 'text-blue-500';
  }

  downloadFile(item: any) {
    const url: string = item.fileUrl || '';
    const name = item.fileName || 'file';

    // Convert Google Drive view/share link → direct download
    const driveMatch = url.match(/\/file\/d\/([^/]+)/);
    if (driveMatch) {
      const directUrl = `https://drive.google.com/uc?export=download&id=${driveMatch[1]}`;
      const a = document.createElement('a');
      a.href = directUrl;
      a.download = name;
      a.target = '_blank';
      a.click();
      return;
    }

    // For other URLs: fetch as blob
    fetch(url)
      .then(res => res.blob())
      .then(blob => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = name;
        a.click();
        URL.revokeObjectURL(a.href);
      })
      .catch(() => window.open(url, '_blank'));
  }
}
