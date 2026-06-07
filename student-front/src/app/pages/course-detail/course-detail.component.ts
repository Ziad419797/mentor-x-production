import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { StudentApiService } from '../../services/student-api.service';

@Component({
  selector: 'app-course-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './course-detail.component.html'
})
export class CourseDetailComponent implements OnInit {
  loading        = signal(true);
  course         = signal<any | null>(null);
  sessions       = signal<any[]>([]);
  studentOnline  = signal<boolean | null>(null);

  private gradients = [
    'linear-gradient(135deg,#183764,#0f4080)',
    'linear-gradient(135deg,#006b58,#004d3e)',
    'linear-gradient(135deg,#7c3d00,#562d00)',
    'linear-gradient(135deg,#4a1d96,#2e1065)',
    'linear-gradient(135deg,#9d174d,#831843)',
    'linear-gradient(135deg,#1e3a5f,#0f2040)',
  ];

  constructor(
    private api: StudentApiService,
    private route: ActivatedRoute,
    public  router: Router
  ) {}

  ngOnInit() {
    const courseId = Number(this.route.snapshot.paramMap.get('id'));

    this.api.getMe().subscribe({
      next: (me: any) => { this.studentOnline.set(me?.online ?? null); },
      error: () => {}
    });

    this.api.getCourseById(courseId).subscribe({
      next: c => {
        this.course.set(c);
        this.api.getSessionsByCourse(courseId).subscribe({
          next: (ss: any) => {
            const all: any[] = Array.isArray(ss) ? ss : (ss?.content ?? []);
            const online = this.studentOnline();
            const filtered = all.filter((s: any) => {
              const t = s.teachingType;
              if (!t || t === 'BOTH') return true;
              if (online === true)  return t === 'ONLINE';
              if (online === false) return t === 'CENTER';
              return true;
            });
            this.sessions.set(filtered.length ? filtered : all);
            this.loading.set(false);
          },
          error: () => this.loading.set(false)
        });
      },
      error: () => this.loading.set(false)
    });
  }

  goToSession(s: any) {
    this.router.navigate(['/session', s.id]);
  }

  gradient(i: number) { return this.gradients[i % this.gradients.length]; }

  teachingTypeLabel(t: string): string {
    if (t === 'ONLINE') return 'أونلاين';
    if (t === 'CENTER') return 'سنتر';
    return 'مشترك';
  }
}
