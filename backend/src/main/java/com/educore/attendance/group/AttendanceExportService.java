package com.educore.attendance.group;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * تصدير كشف الحضور / الغياب بصيغة Excel.
 *
 * filter = "PRESENT" → الحاضرين فقط
 * filter = "ABSENT"  → الغائبين فقط
 * filter = "ALL"     → الكل
 */
@Service
@RequiredArgsConstructor
public class AttendanceExportService {

    private final AttendanceGroupSessionRepository sessionRepo;
    private final AttendanceGroupRecordRepository  recordRepo;
    private final AttendanceGroupMemberRepository  memberRepo;

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ──────────────────────────────────────────────────────────────

    /**
     * تصدير حصة معينة.
     *
     * @param sessionId  ID الحصة
     * @param filter     PRESENT | ABSENT | LATE | EXCUSED | ALL
     * @return byte[] جاهز للإرسال كـ application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
     */
    public byte[] exportSession(Long sessionId, String filter) {
        AttendanceGroupSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("الحصة غير موجودة"));

        List<AttendanceGroupRecord> records =
                recordRepo.findBySessionIdOrderByScannedAtAsc(sessionId);

        // فلترة
        records = applyFilter(records, filter);

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("كشف الحضور");
            sheet.setRightToLeft(true);

            // ── Styles ──────────────────────────────────────────
            CellStyle titleStyle   = makeTitleStyle(wb);
            CellStyle headerStyle  = makeHeaderStyle(wb);
            CellStyle dataStyle    = makeDataStyle(wb);
            CellStyle presentStyle = makeStatusStyle(wb, new XSSFColor(new byte[]{(byte)198,(byte)239,(byte)206}, null)); // أخضر فاتح
            CellStyle absentStyle  = makeStatusStyle(wb, new XSSFColor(new byte[]{(byte)255,(byte)199,(byte)206}, null)); // أحمر فاتح
            CellStyle lateStyle    = makeStatusStyle(wb, new XSSFColor(new byte[]{(byte)255,(byte)235,(byte)156}, null)); // أصفر فاتح
            CellStyle alertStyle   = makeAlertStyle(wb);

            // ── Row 0: عنوان الجروب + الحصة ─────────────────────
            int rowNum = 0;
            Row titleRow = sheet.createRow(rowNum++);
            titleRow.setHeightInPoints(30);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(
                    session.getGroup().getTitle()
                    + " — " + session.getTitle()
                    + " | " + session.getSessionDate().format(DATE_FMT)
            );
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

            // ── Row 1: إحصائيات سريعة ──────────────────────────
            Row statsRow = sheet.createRow(rowNum++);
            long presentCount = records.stream().filter(r -> r.getStatus() == AttendanceStatus.PRESENT).count();
            long absentCount  = records.stream().filter(r -> r.getStatus() == AttendanceStatus.ABSENT).count();
            long lateCount    = records.stream().filter(r -> r.getStatus() == AttendanceStatus.LATE).count();
            statsRow.createCell(0).setCellValue(
                    "إجمالي: " + records.size()
                    + " | حضر: " + presentCount
                    + " | غاب: " + absentCount
                    + " | متأخر: " + lateCount
            );
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 7));

            rowNum++; // فراغ

            // ── Row 3: رأس الجدول ──────────────────────────────
            String[] headers = {
                "#", "اسم الطالب", "كود الطالب", "التليفون",
                "الحالة", "طريقة التسجيل", "وقت التسجيل", "تعليق المدرس"
            };
            Row headerRow = sheet.createRow(rowNum++);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // ── Rows: البيانات ─────────────────────────────────
            int seq = 1;
            for (AttendanceGroupRecord r : records) {
                Row row = sheet.createRow(rowNum++);

                createCell(row, 0, String.valueOf(seq++), dataStyle);
                createCell(row, 1, r.getStudent().getFullName(), dataStyle);
                createCell(row, 2, r.getStudent().getStudentCode(), dataStyle);
                createCell(row, 3, r.getStudent().getPhone(), dataStyle);

                // خانة الحالة ملوّنة
                Cell statusCell = row.createCell(4);
                statusCell.setCellValue(translateStatus(r.getStatus()));
                statusCell.setCellStyle(switch (r.getStatus()) {
                    case PRESENT -> presentStyle;
                    case ABSENT  -> absentStyle;
                    case LATE    -> lateStyle;
                    default      -> dataStyle;
                });

                createCell(row, 5, translateMethod(r.getScanMethod()), dataStyle);

                String scannedStr = r.getScannedAt() != null
                        ? r.getScannedAt().format(DATETIME_FMT) : "—";
                createCell(row, 6, scannedStr, dataStyle);

                // خانة التعليق — لو فيه alert يُضاف بعده
                String comment = "";
                if (r.getAlertType() != null) {
                    comment += "⚠ " + r.getAlertMessage();
                }
                if (r.getTeacherComment() != null && !r.getTeacherComment().isBlank()) {
                    comment += (comment.isEmpty() ? "" : "\n") + r.getTeacherComment();
                }
                Cell commentCell = row.createCell(7);
                commentCell.setCellValue(comment);
                commentCell.setCellStyle(r.getAlertType() != null ? alertStyle : dataStyle);
            }

            // ── ضبط عرض الأعمدة ──────────────────────────────
            sheet.setColumnWidth(0, 1500);   // #
            sheet.setColumnWidth(1, 8000);   // الاسم
            sheet.setColumnWidth(2, 4000);   // الكود
            sheet.setColumnWidth(3, 4500);   // التليفون
            sheet.setColumnWidth(4, 3500);   // الحالة
            sheet.setColumnWidth(5, 4500);   // الطريقة
            sheet.setColumnWidth(6, 5000);   // الوقت
            sheet.setColumnWidth(7, 10000);  // التعليق

            wb.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("فشل تصدير الملف: " + e.getMessage(), e);
        }
    }

    /**
     * تصدير كامل الجروب — ورقة لكل حصة.
     */
    public byte[] exportGroup(Long groupId) {
        List<AttendanceGroupSession> sessions =
                sessionRepo.findByGroupIdOrderBySessionDateDesc(groupId);

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = makeHeaderStyle(wb);
            CellStyle dataStyle   = makeDataStyle(wb);
            CellStyle presentStyle = makeStatusStyle(wb, new XSSFColor(new byte[]{(byte)198,(byte)239,(byte)206}, null));
            CellStyle absentStyle  = makeStatusStyle(wb, new XSSFColor(new byte[]{(byte)255,(byte)199,(byte)206}, null));
            CellStyle lateStyle    = makeStatusStyle(wb, new XSSFColor(new byte[]{(byte)255,(byte)235,(byte)156}, null));

            for (AttendanceGroupSession session : sessions) {
                String sheetName = ("ح" + (session.getSessionNumber() != null ? session.getSessionNumber() : "?")
                        + "-" + session.getSessionDate().format(DATE_FMT))
                        .replaceAll("[/\\\\?*\\[\\]:]", "-");
                // Excel sheet name max 31 chars
                if (sheetName.length() > 31) sheetName = sheetName.substring(0, 31);

                XSSFSheet sheet = wb.createSheet(sheetName);
                sheet.setRightToLeft(true);

                List<AttendanceGroupRecord> records =
                        recordRepo.findBySessionIdOrderByScannedAtAsc(session.getId());

                // Header
                Row headerRow = sheet.createRow(0);
                String[] headers = {"#", "الاسم", "الكود", "التليفون", "الحالة", "الوقت", "تعليق"};
                for (int i = 0; i < headers.length; i++) {
                    Cell c = headerRow.createCell(i);
                    c.setCellValue(headers[i]);
                    c.setCellStyle(headerStyle);
                }

                int rowNum = 1;
                int seq = 1;
                for (AttendanceGroupRecord r : records) {
                    Row row = sheet.createRow(rowNum++);
                    createCell(row, 0, String.valueOf(seq++), dataStyle);
                    createCell(row, 1, r.getStudent().getFullName(), dataStyle);
                    createCell(row, 2, r.getStudent().getStudentCode(), dataStyle);
                    createCell(row, 3, r.getStudent().getPhone(), dataStyle);

                    Cell sc = row.createCell(4);
                    sc.setCellValue(translateStatus(r.getStatus()));
                    sc.setCellStyle(switch (r.getStatus()) {
                        case PRESENT -> presentStyle;
                        case ABSENT  -> absentStyle;
                        case LATE    -> lateStyle;
                        default      -> dataStyle;
                    });

                    String scannedStr = r.getScannedAt() != null
                            ? r.getScannedAt().format(DATETIME_FMT) : "—";
                    createCell(row, 5, scannedStr, dataStyle);
                    createCell(row, 6,
                            r.getTeacherComment() != null ? r.getTeacherComment() : "", dataStyle);
                }

                // column widths
                sheet.setColumnWidth(0, 1500);
                sheet.setColumnWidth(1, 8000);
                sheet.setColumnWidth(2, 4000);
                sheet.setColumnWidth(3, 4500);
                sheet.setColumnWidth(4, 3500);
                sheet.setColumnWidth(5, 5000);
                sheet.setColumnWidth(6, 8000);
            }

            wb.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("فشل تصدير الملف: " + e.getMessage(), e);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────

    private List<AttendanceGroupRecord> applyFilter(List<AttendanceGroupRecord> records, String filter) {
        if (filter == null || filter.isBlank() || filter.equalsIgnoreCase("ALL")) {
            return records;
        }
        try {
            AttendanceStatus target = AttendanceStatus.valueOf(filter.toUpperCase());
            return records.stream()
                    .filter(r -> r.getStatus() == target)
                    .toList();
        } catch (IllegalArgumentException e) {
            return records; // فلتر غير معروف → أعد الكل
        }
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private String translateStatus(AttendanceStatus status) {
        return switch (status) {
            case PRESENT -> "حضر";
            case ABSENT  -> "غائب";
            case LATE    -> "متأخر";
            case EXCUSED -> "غياب بعذر";
        };
    }

    private String translateMethod(ScanMethod method) {
        if (method == null) return "";
        return switch (method) {
            case QR_SCAN     -> "QR Scan";
            case MANUAL_ID   -> "يدوي";
            case AUTO_ABSENT -> "غياب تلقائي";
        };
    }

    // ── Style factories ──────────────────────────────────────────

    private CellStyle makeTitleStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle makeHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)31,(byte)73,(byte)125}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(style);
        return style;
    }

    private CellStyle makeDataStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(style);
        style.setWrapText(true);
        return style;
    }

    private CellStyle makeStatusStyle(XSSFWorkbook wb, XSSFColor color) {
        XSSFCellStyle style = (XSSFCellStyle) makeDataStyle(wb);
        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle makeAlertStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = (XSSFCellStyle) makeDataStyle(wb);
        XSSFFont font = wb.createFont();
        font.setColor(new XSSFColor(new byte[]{(byte)156,(byte)0,(byte)6}, null));
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)255,(byte)235,(byte)156}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void setBorder(XSSFCellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
