import { Injectable, signal, computed } from '@angular/core';

export enum Permission {
  LEVELS_VIEW      = 'LEVELS_VIEW',
  CATEGORIES_VIEW  = 'CATEGORIES_VIEW',
  COURSE_ADD       = 'COURSE_ADD',
  LEVEL_STUDENTS   = 'LEVEL_STUDENTS',
  LEVEL_OVERVIEW   = 'LEVEL_OVERVIEW',
  QUIZZES          = 'QUIZZES',
  ASSIGNMENTS      = 'ASSIGNMENTS',
  QUESTION_BANK    = 'QUESTION_BANK',
  NEW_REQUESTS     = 'NEW_REQUESTS',
  STUDENTS_MANAGE  = 'STUDENTS_MANAGE',
  ATTENDANCE       = 'ATTENDANCE',
  WALLET           = 'WALLET',
  WALLET_HISTORY   = 'WALLET_HISTORY',
  COUPONS          = 'COUPONS',
  CREATE_CODES     = 'CREATE_CODES',
  CODES_LIST       = 'CODES_LIST',
  CENTER_SCHEDULE  = 'CENTER_SCHEDULE',
  BOOKS_CODES      = 'BOOKS_CODES',
  HOME_LAYOUT      = 'HOME_LAYOUT',
  SUPPORT_CHANNELS = 'SUPPORT_CHANNELS',
  TOPIC_TREE       = 'TOPIC_TREE',
  TOPIC_ANALYTICS  = 'TOPIC_ANALYTICS',
  ACTIVITY_LOGS    = 'ACTIVITY_LOGS',
  STAFF_MANAGE     = 'STAFF_MANAGE',
  CENTERS_MANAGE   = 'CENTERS_MANAGE',
  BANNERS          = 'BANNERS',
}

@Injectable({ providedIn: 'root' })
export class PermissionService {
  userProfile = signal<any | null>(null);

  /** Staff has role='STAFF'; teachers have no role field → full access */
  isTeacher = computed(() => {
    const profile = this.userProfile();
    if (!profile) return false;
    return profile.role !== 'STAFF';
  });

  setProfile(profile: any) {
    this.userProfile.set(profile);
  }

  hasPermission(permission: Permission | string): boolean {
    if (this.isTeacher()) return true;
    const profile = this.userProfile();
    return profile?.permissions?.includes(permission) ?? false;
  }

  hasAnyPermission(permissions: (Permission | string)[]): boolean {
    if (this.isTeacher()) return true;
    const profile = this.userProfile();
    return permissions.some(p => profile?.permissions?.includes(p));
  }
}
