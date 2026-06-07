import { Injectable } from '@angular/core';
import { BehaviorSubject, distinctUntilChanged } from 'rxjs';
import { Category, Course, Session } from '../models/models';

@Injectable({ providedIn: 'root' })
export class CategoryStateService {
  /** 1. Educational Stage (Category) */
  private readonly _selectedCategory = new BehaviorSubject<Category | null>(null);
  readonly selectedCategory$ = this._selectedCategory.asObservable().pipe(distinctUntilChanged());

  /** 2. Educational Level (Course) */
  private readonly _selectedCourse = new BehaviorSubject<Course | null>(null);
  readonly selectedCourse$ = this._selectedCourse.asObservable().pipe(distinctUntilChanged());

  /** 3. Educational Unit (Session) */
  private readonly _selectedSession = new BehaviorSubject<Session | null>(null);
  readonly selectedSession$ = this._selectedSession.asObservable().pipe(distinctUntilChanged());

  /** All stages cache */
  private readonly _categories = new BehaviorSubject<Category[]>([]);
  readonly categories$ = this._categories.asObservable();

  setCategories(cats: Category[]): void {
    this._categories.next(cats);
  }

  selectCategory(cat: Category | null): void {
    this._selectedCategory.next(cat);
    // Reset lower hierarchy
    this._selectedCourse.next(null);
    this._selectedSession.next(null);
  }

  selectCourse(course: Course | null): void {
    this._selectedCourse.next(course);
    // Reset lower hierarchy
    this._selectedSession.next(null);
  }

  selectSession(session: Session | null): void {
    this._selectedSession.next(session);
  }

  get currentCategory(): Category | null { return this._selectedCategory.getValue(); }
  get currentCourse(): Course | null { return this._selectedCourse.getValue(); }
  get currentSession(): Session | null { return this._selectedSession.getValue(); }

  get currentCategoryId(): number | null { return this._selectedCategory.getValue()?.id ?? null; }
  get currentCourseId(): number | null { return this._selectedCourse.getValue()?.id ?? null; }
  get currentSessionId(): number | null { return this._selectedSession.getValue()?.id ?? null; }

  clearAll(): void {
    this._selectedCategory.next(null);
    this._selectedCourse.next(null);
    this._selectedSession.next(null);
  }
}
