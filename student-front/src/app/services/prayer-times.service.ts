import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface PrayerAlert {
  name: string;
  nameAr: string;
  time: string;
}

const PRAYER_NAMES: Record<string, string> = {
  Fajr:    'الفجر',
  Dhuhr:   'الظهر',
  Asr:     'العصر',
  Maghrib: 'المغرب',
  Isha:    'العشاء',
};

@Injectable({ providedIn: 'root' })
export class PrayerTimesService {

  /** الإشعار النشط حالياً (null = لا يوجد) */
  activeAlert = signal<PrayerAlert | null>(null);

  private timings: Record<string, string> = {};
  private notifiedToday = new Set<string>();
  private checkInterval: any;
  private lastFetchDate = '';

  constructor(private http: HttpClient) {}

  /** يُستدعى من ShellComponent.ngOnInit */
  start() {
    this.fetchAndSchedule();
    // فحص كل دقيقة
    this.checkInterval = setInterval(() => this.checkNow(), 60_000);
  }

  stop() {
    if (this.checkInterval) clearInterval(this.checkInterval);
  }

  dismissAlert() {
    this.activeAlert.set(null);
  }

  private async fetchAndSchedule() {
    const today = new Date().toDateString();
    if (today === this.lastFetchDate) return;          // مجلبناش اليوم ده
    this.lastFetchDate = today;
    this.notifiedToday.clear();

    try {
      const data: any = await this.http
        .get('https://api.aladhan.com/v1/timingsByCity?city=Cairo&country=Egypt&method=5')
        .toPromise();
      this.timings = data?.data?.timings ?? {};
      this.checkNow();
    } catch (e) {
      console.warn('PrayerTimes: فشل تحميل المواقيت', e);
    }
  }

  private checkNow() {
    if (!Object.keys(this.timings).length) return;

    const now  = new Date();
    const h    = now.getHours().toString().padStart(2, '0');
    const m    = now.getMinutes().toString().padStart(2, '0');
    const hhmm = `${h}:${m}`;

    for (const [en, ar] of Object.entries(PRAYER_NAMES)) {
      const raw = this.timings[en];
      if (!raw) continue;

      // مقارنة أول 5 أحرف فقط (HH:mm)
      const prayerHHmm = raw.substring(0, 5);
      if (prayerHHmm === hhmm && !this.notifiedToday.has(en)) {
        this.notifiedToday.add(en);
        this.activeAlert.set({ name: en, nameAr: ar, time: prayerHHmm });

        // إخفاء تلقائي بعد 3 دقائق
        setTimeout(() => {
          if (this.activeAlert()?.name === en) this.activeAlert.set(null);
        }, 3 * 60_000);
        break;
      }
    }
  }
}
