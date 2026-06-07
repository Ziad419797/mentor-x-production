import { Injectable, signal, computed } from '@angular/core';
import { ApiService } from './api.service';
import { TeacherProfile } from '../models/models';

export enum Permission {
  APPROVE_STUDENTS = 'APPROVE_STUDENTS',
  REJECT_STUDENTS = 'REJECT_STUDENTS',
  VIEW_STUDENTS = 'VIEW_STUDENTS',
  MANAGE_COURSES = 'MANAGE_COURSES',
  MANAGE_QUIZZES = 'MANAGE_QUIZZES',
  MANAGE_QUESTION_BANK = 'MANAGE_QUESTION_BANK',
  MANAGE_ATTENDANCE = 'MANAGE_ATTENDANCE',
  ISSUE_CARDS = 'ISSUE_CARDS',
  TOP_UP_WALLET = 'TOP_UP_WALLET',
  MANAGE_COUPONS = 'MANAGE_COUPONS',
  MANAGE_ENROLLMENTS = 'MANAGE_ENROLLMENTS',
  SEND_NOTIFICATIONS = 'SEND_NOTIFICATIONS',
  VIEW_ANALYTICS = 'VIEW_ANALYTICS'
}

@Injectable({ providedIn: 'root' })
export class PermissionService {
  userProfile = signal<any | null>(null);
  
  // If user has no 'permissions' array, they are assumed to be a Teacher (Full Access)
  isTeacher = computed(() => {
    const profile = this.userProfile();
    return profile && (!profile.permissions || profile.role === 'TEACHER');
  });

  constructor(private api: ApiService) {}

  setProfile(profile: any) {
    this.userProfile.set(profile);
  }

  hasPermission(permission: Permission | string): boolean {
    if (this.isTeacher()) return true;
    const profile = this.userProfile();
    return profile?.permissions?.includes(permission) || false;
  }

  hasAnyPermission(permissions: (Permission | string)[]): boolean {
    if (this.isTeacher()) return true;
    const profile = this.userProfile();
    return permissions.some(p => profile?.permissions?.includes(p));
  }
}
