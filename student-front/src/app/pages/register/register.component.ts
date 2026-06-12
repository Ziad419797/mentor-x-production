import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

const API = environment.apiBase;

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.component.html'
})
export class RegisterComponent {

  step = signal(1); // 1=phone,2=otp,3=personal,4=academic,5=mode,6=docs,7=success
  loading = signal(false);

  phone = '';
  otpDigits = ['','','','','',''];
  otpCountdown = signal(0);
  private otpTimer: any;
  demoMode = false;

  firstName  = '';
  secondName = '';
  thirdName  = '';
  fourthName = '';
  nationalId = '';
  dateOfBirth = '';
  parentPhone = '';
  password = '';
  confirmPassword = '';
  showPass = false;
  showConfirmPass = false;
  passStrength = 0;

  grade = '';
  gradeId: number | null = null;  // levelId
  governorate = '';
  area = '';
  areaCustom = '';
  schoolName = '';
  schoolType = '';
  department = '';
  levels: {id:number; name:string}[] = [];
  governorates: string[] = [];
  areas: string[] = [];
  areasLoading = false;

  studyMode: 'ONLINE' | 'CENTER' | '' = '';
  centersLoading = false;
  wantFutureCenter = false;
  filteredCenters: any[] = []; // سناتر لها جروبات للصف الدراسي
  centerId: number | null = null;
  centerName = '';
  centers: any[] = [];

  // مواعيد الجروبات
  groups: any[] = [];
  groupsLoading = false;
  selectedGroupId: number | null = null;
  selectedGroupTitle = '';

  profileImageFile: File | null = null;
  profileImagePreview: string | null = null;
  idCardFile: File | null = null;
  idCardPreview: string | null = null;

  err: any = {};

  get today() { return new Date().toISOString().split('T')[0]; }
  get otpString() { return this.otpDigits.join(''); }
  get stepProgress() { return Math.round((this.step() / 6) * 100); }
  get stepTitle() {
    const t: any = {
      1: 'رقم الهاتف', 2: 'التحقق من الهاتف', 3: 'البيانات الشخصية',
      4: 'البيانات الدراسية', 5: 'طريقة الدراسة', 6: 'المستندات'
    };
    return t[this.step()] || '';
  }

  constructor(private http: HttpClient, private router: Router) {
    this.preloadCenters();
  }

  private async preloadCenters() {
    try {
      const data: any = await this.http.get(`${API}/api/centers`).toPromise();
      const list = data?.data || data;
      this.centers = Array.isArray(list) ? list : [];
    } catch { this.centers = []; }
  }

  // ─── Step 1: Phone ───
  async s1Next() {
    this.err = {};
    if (!/^01[0-9]{9}$/.test(this.phone.trim())) {
      this.err.phone = 'رقم الهاتف غير صحيح (يجب أن يبدأ بـ 01 ويكون 11 رقم)';
      return;
    }
    this.loading.set(true);
    try {
      const checkData: any = await this.http
        .get(`${API}/api/student/register/check-phone/${this.phone.trim()}`)
        .toPromise().catch(() => null);
      if (checkData?.exists === true) {
        this.err.phone = 'هذا الرقم مسجل بالفعل. يمكنك تسجيل الدخول';
        this.loading.set(false); return;
      }
      const startData: any = await this.http
        .post(`${API}/api/student/register/start`, { phone: this.phone.trim() })
        .toPromise().catch(() => null);
      this.demoMode = !startData;
      if (startData?.otpCode) {
        this.otpDigits = startData.otpCode.toString().padStart(6, '0').split('');
      }
      this.step.set(2);
      this.startOtpTimer();
    } catch {
      this.demoMode = true;
      this.otpDigits = ['1','2','3','4','5','6'];
      this.step.set(2);
      this.startOtpTimer();
    } finally {
      this.loading.set(false);
    }
  }

  // ─── Step 2: OTP ───
  startOtpTimer() {
    clearInterval(this.otpTimer);
    let secs = 300;
    this.otpCountdown.set(secs);
    this.otpTimer = setInterval(() => {
      secs--; this.otpCountdown.set(secs);
      if (secs <= 0) clearInterval(this.otpTimer);
    }, 1000);
  }

  get otpCountdownFormatted() {
    const s = this.otpCountdown();
    return Math.floor(s/60) + ':' + String(s%60).padStart(2,'0');
  }

  onOtpInput(i: number, event: any) {
    const val = event.target.value.replace(/\D/g, '');
    this.otpDigits[i] = val.slice(-1);
    event.target.value = this.otpDigits[i];
    if (val && i < 5) {
      const next = document.getElementById('otp-' + (i + 1)) as HTMLInputElement;
      if (next) next.focus();
    }
    if (this.otpDigits.every(d => d)) this.s2Verify();
  }

  onOtpKeydown(i: number, event: KeyboardEvent) {
    if (event.key === 'Backspace' && !this.otpDigits[i] && i > 0) {
      const prev = document.getElementById('otp-' + (i - 1)) as HTMLInputElement;
      if (prev) prev.focus();
    }
  }

  async resendOtp() {
    if (this.demoMode) { this.otpDigits = ['1','2','3','4','5','6']; this.startOtpTimer(); return; }
    try {
      const data: any = await this.http
        .post(`${API}/api/student/register/resend-otp`, { phone: this.phone }).toPromise();
      if (data?.otpCode) this.otpDigits = data.otpCode.toString().padStart(6,'0').split('');
      this.startOtpTimer();
    } catch { this.otpDigits = ['1','2','3','4','5','6']; this.startOtpTimer(); }
  }

  s2Verify() {
    this.err = {};
    if (this.otpString.length < 6) { this.err.otp = 'أدخل الرمز كاملاً'; return; }
    if (this.demoMode && this.otpString !== '123456') { this.err.otp = 'الرمز التجريبي هو 123456'; return; }
    this.step.set(3);
  }

  // ─── Step 3: Personal ───
  checkPassStrength() {
    const v = this.password;
    this.passStrength = [v.length>=6, /[A-Z]/.test(v)||/[a-z]/.test(v), /[0-9]/.test(v), /[^A-Za-z0-9]/.test(v)].filter(Boolean).length;
  }

  s3Next() {
    this.err = {};
    if (!this.firstName.trim())                         this.err.firstName = 'الاسم الأول مطلوب';
    if (!this.secondName.trim())                        this.err.secondName = 'الاسم الثاني مطلوب';
    if (!this.thirdName.trim())                         this.err.thirdName = 'الاسم الثالث مطلوب';
    if (!this.fourthName.trim())                        this.err.fourthName = 'الاسم الرابع مطلوب';
    if (!/^[0-9]{14}$/.test(this.nationalId.trim()))   this.err.nationalId = 'الرقم القومي يجب أن يكون 14 رقماً';
    if (!this.dateOfBirth)                              this.err.dateOfBirth = 'تاريخ الميلاد مطلوب';
    if (!/^01[0-9]{9}$/.test(this.parentPhone.trim())) this.err.parentPhone = 'رقم ولي الأمر غير صحيح';
    if (this.password.length < 6)                       this.err.password = 'كلمة المرور 6 أحرف على الأقل';
    if (this.password !== this.confirmPassword)         this.err.confirmPassword = 'كلمتا المرور غير متطابقتين';
    if (!Object.keys(this.err).length) { this.step.set(4); this.loadLevels(); this.loadGovernorates(); }
  }

  // ─── Step 4: Academic ───
  async loadLevels() {
    if (this.levels.length) return;
    try {
      const data: any = await this.http.get(`${API}/api/levels`).toPromise();
      this.levels = Array.isArray(data) ? data : (data?.data || []);
    } catch { this.levels = []; }
  }

  async loadGovernorates() {
    if (this.governorates.length) return;
    try {
      const data: any = await this.http.get(`${API}/api/student/register/governorates`).toPromise();
      this.governorates = Array.isArray(data) ? data : [];
    } catch {
      this.governorates = ['القاهرة','الجيزة','الإسكندرية','الدقهلية','الشرقية','المنوفية','القليوبية','أسوان','الأقصر','سوهاج'];
    }
  }

  onGradeChange() {
    const found = this.levels.find(l => l.name === this.grade);
    this.gradeId = found ? found.id : null;
    this.groups = [];
    this.selectedGroupId = null;
    this.selectedGroupTitle = '';
    if (this.gradeId) this.loadFilteredCenters();
    if (this.centerId && this.gradeId) this.loadGroups();
  }

  async onGovChange() {
    this.area = ''; this.areaCustom = ''; this.areas = [];
    if (!this.governorate) return;
    this.areasLoading = true;
    try {
      const data: any = await this.http
        .get(`${API}/api/student/register/areas/${encodeURIComponent(this.governorate)}`).toPromise();
      this.areas = Array.isArray(data) ? [...data, '__other__'] : ['__other__'];
    } catch {
      this.areas = ['المدينة','الشرق','الغرب','الشمال','الجنوب','المركز','__other__'];
    } finally { this.areasLoading = false; }
  }

  s4Next() {
    this.err = {};
    if (!this.grade)                    this.err.grade = 'الصف الدراسي مطلوب';
    if (!this.governorate)              this.err.governorate = 'المحافظة مطلوبة';
    if (!this.area)                     this.err.area = 'المنطقة مطلوبة';
    if (this.area === '__other__' && !this.areaCustom.trim()) this.err.areaCustom = 'اكتب اسم المنطقة';
    if (!this.schoolName.trim())        this.err.schoolName = 'اسم المدرسة مطلوب';
    if (!this.schoolType)               this.err.schoolType = 'نوع المدرسة مطلوب';
    if (!this.department.trim())        this.err.department = 'الإدارة التعليمية مطلوبة';
    if (!Object.keys(this.err).length) this.step.set(5);
  }

  // ─── Step 5: Mode ───
  async selectMode(mode: 'ONLINE' | 'CENTER') {
    this.studyMode = mode;
    this.err = {};
    this.centerId = null; this.centerName = ''; this.groups = []; this.selectedGroupId = null;
    this.wantFutureCenter = false;
    if (mode === 'CENTER') await this.loadFilteredCenters();
  }

  private async loadFilteredCenters() {
    if (!this.gradeId) {
      // No grade selected yet — use all centers as fallback
      this.filteredCenters = this.centers;
      return;
    }
    this.centersLoading = true;
    try {
      const data: any = await this.http
        .get(`${API}/api/student/register/centers-with-groups?levelId=${this.gradeId}`)
        .toPromise();
      this.filteredCenters = Array.isArray(data) ? data : [];
      // If no filtered centers, fallback to all centers
      if (!this.filteredCenters.length) this.filteredCenters = this.centers;
    } catch { this.filteredCenters = this.centers; }
    finally { this.centersLoading = false; }
  }

  onCenterChange() {
    const found = this.centers.find(c => c.name === this.centerName);
    this.centerId = found ? found.id : null;
    this.groups = []; this.selectedGroupId = null; this.selectedGroupTitle = '';
    if (this.centerId && this.gradeId) this.loadGroups();
  }

  async loadGroups() {
    if (!this.centerId || !this.gradeId) return;
    this.groupsLoading = true;
    try {
      const data: any = await this.http
        .get(`${API}/api/student/register/groups?centerId=${this.centerId}&levelId=${this.gradeId}`)
        .toPromise();
      this.groups = Array.isArray(data) ? data : (data?.data || []);
    } catch { this.groups = []; }
    finally { this.groupsLoading = false; }
  }

  onGroupChange() {
    const found = this.groups.find(g => g.id === Number(this.selectedGroupId));
    this.selectedGroupTitle = found?.title || '';
  }

  s5Next() {
    this.err = {};
    if (!this.studyMode)                                  { this.err.mode = 'اختر طريقة الدراسة'; return; }
    if (this.studyMode === 'CENTER') {
      if (!this.centerName)                               { this.err.center = 'اختر السنتر'; return; }
      if (this.groups.length > 0 && !this.selectedGroupId) { this.err.group = 'اختر الميعاد'; return; }
    }
    // ONLINE with future center: group optional
    this.step.set(6);
  }

  // ─── Step 6: Documents ───
  onPhotoSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.profileImageFile = file;
    const reader = new FileReader();
    reader.onload = e => this.profileImagePreview = e.target?.result as string;
    reader.readAsDataURL(file);
  }

  onIdCardSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.idCardFile = file;
    const reader = new FileReader();
    reader.onload = e => this.idCardPreview = e.target?.result as string;
    reader.readAsDataURL(file);
  }

  async submit() {
    this.err = {};
    if (!this.profileImageFile) { this.err.submit = 'الصورة الشخصية مطلوبة'; return; }
    if (!this.idCardFile)       { this.err.submit = 'صورة بطاقة الهوية مطلوبة'; return; }
    this.loading.set(true);
    try {
      const jsonData: any = {
        phone:               this.phone.trim(),
        otp:                 parseInt(this.otpString, 10),
        nationalId:          this.nationalId.trim(),
        dateOfBirth:         this.dateOfBirth,
        firstName:           this.firstName.trim(),
        secondName:          this.secondName.trim(),
        thirdName:           this.thirdName.trim(),
        fourthName:          this.fourthName.trim(),
        parentPhone:         this.parentPhone.trim(),
        password:            this.password,
        grade:               this.grade,
        governorate:         this.governorate,
        area:                this.area === '__other__' ? this.areaCustom.trim() : this.area,
        schoolName:          this.schoolName.trim(),
        schoolType:          this.schoolType,
        educationDepartment: this.department.trim(),
        online:              this.studyMode === 'ONLINE',
        centerName:          this.studyMode === 'CENTER' ? this.centerName : null,
        attendanceGroupId:   this.selectedGroupId || null,
      };

      const fd = new FormData();
      fd.append('data', new Blob([JSON.stringify(jsonData)], { type: 'application/json' }), 'data.json');
      if (this.profileImageFile) fd.append('profileImage',     this.profileImageFile);
      if (this.idCardFile)       fd.append('identityDocument', this.idCardFile);

      await this.http.post(`${API}/api/student/register/complete`, fd).toPromise();
      this.step.set(7);
    } catch (e: any) {
      const msg = e?.error?.message || e?.error?.error || 'حدث خطأ أثناء التسجيل';
      if (msg?.includes('OTP') || msg?.includes('expired')) { this.err.otp = msg; this.step.set(2); }
      else { this.err.submit = msg; }
    } finally { this.loading.set(false); }
  }

  goLogin() { this.router.navigate(['/login']); }
}

