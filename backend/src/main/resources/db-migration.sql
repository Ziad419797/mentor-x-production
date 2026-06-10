-- Run this SQL manually in your PostgreSQL database if Hibernate ddl-auto=update
-- didn't add the columns automatically after restart.

-- Centers table: new columns
ALTER TABLE centers ADD COLUMN IF NOT EXISTS area VARCHAR(150);
ALTER TABLE centers ADD COLUMN IF NOT EXISTS maps_link VARCHAR(500);
ALTER TABLE centers ADD COLUMN IF NOT EXISTS sells_books BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE centers ADD COLUMN IF NOT EXISTS sells_codes BOOLEAN NOT NULL DEFAULT FALSE;

-- AttendanceGroups table: new columns
ALTER TABLE attendance_groups ADD COLUMN IF NOT EXISTS max_capacity INTEGER;
ALTER TABLE attendance_groups ADD COLUMN IF NOT EXISTS day_of_week VARCHAR(20);
ALTER TABLE attendance_groups ADD COLUMN IF NOT EXISTS meeting_time VARCHAR(10);

-- Students table: fix column lengths (ddl-auto=update won't shrink/expand existing columns)
ALTER TABLE students ALTER COLUMN grade TYPE VARCHAR(100);
ALTER TABLE students ALTER COLUMN governorate TYPE VARCHAR(100);
ALTER TABLE students ALTER COLUMN area TYPE VARCHAR(150);
ALTER TABLE students ALTER COLUMN school_name TYPE VARCHAR(200);
ALTER TABLE students ALTER COLUMN education_department TYPE VARCHAR(150);
ALTER TABLE students ALTER COLUMN center_name TYPE VARCHAR(200);
ALTER TABLE students ALTER COLUMN first_name TYPE VARCHAR(100);
ALTER TABLE students ALTER COLUMN second_name TYPE VARCHAR(100);
ALTER TABLE students ALTER COLUMN third_name TYPE VARCHAR(100);
ALTER TABLE students ALTER COLUMN fourth_name TYPE VARCHAR(100);

-- Courses table: fix nullable boolean columns
ALTER TABLE courses ALTER COLUMN featured SET DEFAULT FALSE;
ALTER TABLE courses ALTER COLUMN track_attendance SET DEFAULT FALSE;
ALTER TABLE courses ALTER COLUMN pinned SET DEFAULT FALSE;
UPDATE courses SET featured = FALSE WHERE featured IS NULL;
UPDATE courses SET track_attendance = FALSE WHERE track_attendance IS NULL;
UPDATE courses SET pinned = FALSE WHERE pinned IS NULL;

-- Add school_type column (نوع المدرسة: عام أو أزهر)
ALTER TABLE students ADD COLUMN IF NOT EXISTS school_type VARCHAR(20);
-- Set default for existing rows so NOT NULL constraint doesn't fail
UPDATE students SET school_type = 'عام' WHERE school_type IS NULL;
ALTER TABLE students ALTER COLUMN school_type SET NOT NULL;
ALTER TABLE students ALTER COLUMN school_type SET DEFAULT 'عام';
