import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { StudentApiService } from '../../services/student-api.service';

@Component({
  selector: 'app-my-courses',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './my-courses.component.html'
})
export class MyCoursesComponent implements OnInit {
  enrollments = signal<any[]>([]);
  loading     = signal(true);

  constructor(private api: StudentApiService, private router: Router) {}

  ngOnInit() {
    this.api.getMyEnrollments().subscribe({
      next: (data: any) => {
        this.enrollments.set(Array.isArray(data) ? data : []);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  courseTitle(e: any):   string { return e?.course?.title ?? e?.courseTitle ?? e?.title ?? 'كورس'; }
  courseImage(e: any):   string { return e?.course?.imageUrl || e?.course?.thumbnailUrl || e?.course?.coverImageUrl || e?.imageUrl || ''; }
  courseTeacher(e: any): string { return e?.course?.teacherName || e?.course?.teacher?.fullName || ''; }
  courseDesc(e: any):    string { return e?.course?.description || e?.description || ''; }
  progress(e: any):     number { return Math.min(100, Math.round(e?.progress ?? e?.completionPercentage ?? 0)); }
  courseId(e: any):     number { return e?.course?.id ?? e?.courseId ?? e?.id; }
  progressColor(i: number): string { return ['#183764','#4BBBA0','#F59239'][i % 3]; }

  openCourse(e: any) { this.router.navigate(['/courses', this.courseId(e)]); }
}
