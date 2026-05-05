package com.social_navigator_ar.demo.controller;

import com.social_navigator_ar.demo.model.Marker;
import com.social_navigator_ar.demo.model.MarkerViewLog;
import com.social_navigator_ar.demo.repository.MarkerRepository;
import com.social_navigator_ar.demo.repository.MarkerViewLogRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final MarkerRepository markerRepository;
    private final MarkerViewLogRepository markerViewLogRepository;

    public ReportController(MarkerRepository markerRepository, MarkerViewLogRepository markerViewLogRepository) {
        this.markerRepository = markerRepository;
        this.markerViewLogRepository = markerViewLogRepository;
    }

    @GetMapping("/generate-monthly-report")
    public void generateMonthlyReport(HttpServletResponse response) throws IOException {
        List<Marker> markers = markerRepository.findAll();

        XWPFDocument document = new XWPFDocument();

        // Заголовок
        XWPFParagraph title = document.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = title.createRun();
        titleRun.setText("Ежемесячный отчёт о просмотрах меток");
        titleRun.setBold(true);
        titleRun.setFontSize(20);

        // Подзаголовок дата
        XWPFParagraph subtitle = document.createParagraph();
        subtitle.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun subtitleRun = subtitle.createRun();
        subtitleRun.setText("Период: последние 30 дней");
        subtitleRun.setFontSize(14);
        subtitleRun.setColor("888888");

        for (Marker marker : markers) {
            addMarkerSection(document, marker);
        }

        // Отправляем файл пользователю
        String filename = "monthly_report_" + LocalDate.now().toString() + ".docx";
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        document.write(response.getOutputStream());
        document.close();
    }

    private void addMarkerSection(XWPFDocument doc, Marker marker) {
        // Заголовок маркера
        XWPFParagraph heading = doc.createParagraph();
        XWPFRun run = heading.createRun();
        run.setText("Организация: " + marker.getName());
        run.setBold(true);
        run.setFontSize(16);

        // Адрес
        XWPFParagraph address = doc.createParagraph();
        address.createRun().setText("Адрес: " + (marker.getAddress() != null ? marker.getAddress() : "(не указан)"));

        // Общее количество просмотров
        int totalViews = marker.getViews();
        XWPFParagraph views = doc.createParagraph();
        views.createRun().setText("Общее количество просмотров: " + totalViews);

        // Статистика по дням
        List<Object[]> dailyStats = markerViewLogRepository.countViewsByDate(marker.getId());

        if (!dailyStats.isEmpty()) {
            XWPFTable table = doc.createTable();
            XWPFTableRow headerRow = table.getRow(0);
            headerRow.getCell(0).setText("Дата");
            headerRow.addNewTableCell().setText("Просмотры");

            for (Object[] row : dailyStats) {
                XWPFTableRow dataRow = table.createRow();
                dataRow.getCell(0).setText(row[0].toString());
                dataRow.getCell(1).setText(row[1].toString());
            }
        }

        // Разделитель
        XWPFParagraph divider = doc.createParagraph();
        divider.setSpacingAfter(10);
    }
}