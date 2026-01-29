package com.tool.sonarq.service.impl;

import com.tool.sonarq.dto.IssueDto;
import com.tool.sonarq.dto.IssueExportData;
import com.tool.sonarq.dto.model.Impact;
import com.tool.sonarq.exception.BizException;
import com.tool.sonarq.service.ExcelService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.time.LocalDateTime.now;
import static java.time.OffsetDateTime.parse;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static reactor.core.publisher.Mono.fromCallable;

@Slf4j
@Service
public class ExcelServiceImpl implements ExcelService {

    private static final Integer TEMPLATE_SHEET_DATA_START_INDEX = 23;
    private static final Integer TEMPLATE_SHEET_DATA_END_INDEX = 50;
    private static final String TEMPLATE_SHEET_NAME = "template";
    private static final String DASHBOARD_SHEET_NAME = "Dashboard";
    private static final DateTimeFormatter SONAR_INPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final DateTimeFormatter OUTPUT_FORMAT = DateTimeFormatter.ofPattern("d/M/yyyy  H:mm:ss");

    public Mono<byte[]> generateReport(Map<String, IssueExportData> dataMap, Integer rowStartIndex) {
        return fromCallable(() -> createExcel(dataMap, rowStartIndex))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(IOException.class,
                        ex -> new BizException(ex.getMessage())
                );
    }

    private byte[] createExcel(Map<String, IssueExportData> dataMap, Integer rowStartIndex) throws IOException {
        try (InputStream is =
                     of(getClass())
                             .map(Class::getClassLoader)
                             .map(classLoader ->
                                     classLoader.getResourceAsStream("templates/sonarqube_export_template.xlsx"))
                             .orElseThrow(() -> new RuntimeException("No templates found!"));

             XSSFWorkbook workbook = new XSSFWorkbook(is);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            logTotalIssues(dataMap);
            Sheet template = workbook.getSheet(TEMPLATE_SHEET_NAME);
            Integer rowIndex = ofNullable(rowStartIndex).orElse(TEMPLATE_SHEET_DATA_START_INDEX);
            for (var entry : dataMap.entrySet()) {
                Sheet sheet = workbook.cloneSheet(workbook.getSheetIndex(template));
                workbook.setSheetName(workbook.getSheetIndex(sheet), entry.getKey());
                clearDataRows(sheet, rowIndex, TEMPLATE_SHEET_DATA_END_INDEX);
                List<IssueDto> issues = of(entry.getValue())
                        .map(IssueExportData::getIssues)
                        .orElseGet(List::of);

                fillHeader(sheet, entry.getKey(), entry.getValue(), now().format(OUTPUT_FORMAT), rowIndex);
                fillData(sheet, issues, rowIndex);
            }
            Sheet dashboard = workbook.getSheet(DASHBOARD_SHEET_NAME);
            fillDashboard(dashboard, dataMap);
            int templateIndex = workbook.getSheetIndex(TEMPLATE_SHEET_NAME);
            if(templateIndex >= 0) workbook.removeSheetAt(templateIndex);
            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
            workbook.write(out);

            return out.toByteArray();
        }
    }

    private void fillHeader(Sheet sheet, String repository,
                            IssueExportData issueExportData, String analyzedDate, Integer rowStartIndex) {
        Integer indexOfLastIssueRow = rowStartIndex + issueExportData.getIssues().size();
        //Set report name
        Row reportName = sheet.getRow(0);
        Cell reportNameCell = reportName.getCell(0);
        reportNameCell.setCellValue(format("Service Report - %s", repository));

        //Set service name
        Row serviceNameRow = sheet.getRow(2);
        Cell serviceNameCell = serviceNameRow.getCell(1);
        serviceNameCell.setCellValue(repository);

        //Set project key
        Row projectKeyRow = sheet.getRow(3);
        Cell projectKeyCell = projectKeyRow.getCell(1);
        projectKeyCell.setCellValue(repository);

        //Set branch
        Row branchRow = sheet.getRow(4);
        Cell branchCell = branchRow.getCell(1);
        branchCell.setCellValue(issueExportData.getBranch());

        //Set analyzed date 1
        Row analyzedDateRow1 = sheet.getRow(5);
        Cell analyzedDateCell1 = analyzedDateRow1.getCell(1);
        analyzedDateCell1.setCellValue(analyzedDate);

        //Set formula for Bugs
        Row bugsRow = sheet.getRow(8);
        Cell bugsCell = bugsRow.getCell(1);
        bugsCell.setBlank();
        String bugsDynamicFormula = format("COUNTIF(B%d:B%d,\"BUG\")", rowStartIndex, indexOfLastIssueRow);
        bugsCell.setCellFormula(bugsDynamicFormula);

        //Set formula for Vulnerabilities
        Row vulnerabilitiesRow = sheet.getRow(9);
        Cell vulnerabilitiesCell = vulnerabilitiesRow.getCell(1);
        vulnerabilitiesCell.setBlank();
        String vulnerabilitiesDynamicFormula = format("COUNTIF(B%d:B%d,\"VULNERABILITY\")", rowStartIndex, indexOfLastIssueRow);
        vulnerabilitiesCell.setCellFormula(vulnerabilitiesDynamicFormula);

        //Set formula for Code Smells
        Row codeSmellsRow = sheet.getRow(10);
        Cell codeSmellsCell = codeSmellsRow.getCell(1);
        codeSmellsCell.setBlank();
        String codeSmellsDynamicFormula = format("COUNTIF(B%d:B%d,\"CODE_SMELL\")", rowStartIndex, indexOfLastIssueRow);
        codeSmellsCell.setCellFormula(codeSmellsDynamicFormula);

        //Set data for Security Hotspots
        Row securityHotspotsRow = sheet.getRow(11);
        Cell securityHotspotsCell = securityHotspotsRow.getCell(1);
        securityHotspotsCell.setBlank();
        securityHotspotsCell.setCellValue(issueExportData.getSecurityHotspot());

        //Set formula for count Security Hotspots
        Row securityHotspotsCounterRow = sheet.getRow(11);
        Cell securityHotspotCountersCell = securityHotspotsCounterRow.getCell(3);
        securityHotspotCountersCell.setBlank();
        String securityHotspotsCounterDynamicFormula =
                format("COUNTIF(K%d:K%d,\"SAFE\")+COUNTIF(K%d:K%d,\"FIX\")", rowStartIndex, indexOfLastIssueRow,
                        rowStartIndex, indexOfLastIssueRow);
        securityHotspotCountersCell.setCellFormula(securityHotspotsCounterDynamicFormula);

        //Set data for Hotspots Reviewed percentage
        Row hotspotsReviewedPercentageRow = sheet.getRow(12);
        Cell hotspotsReviewedPercentageCell = hotspotsReviewedPercentageRow.getCell(1);
        hotspotsReviewedPercentageCell.setBlank();
        hotspotsReviewedPercentageCell.setCellValue(issueExportData.getHotspotReviewed());

        //Set data for coverage percentage
        Row coveragePercentageRow = sheet.getRow(13);
        Cell coveragePercentageCell = coveragePercentageRow.getCell(1);
        coveragePercentageCell.setBlank();
        coveragePercentageCell.setCellValue(issueExportData.getCoverage());

        //Set % duplications
        Row duplications = sheet.getRow(14);
        Cell duplicationsCell = duplications.getCell(1);
        duplicationsCell.setCellValue(issueExportData.getDuplications());

        //Set lines of code
        Row lineOfCodeRow = sheet.getRow(15);
        Cell lineOfCodeCell = lineOfCodeRow.getCell(1);
        lineOfCodeCell.setCellValue(issueExportData.getLinesOfCode());

        //Set lines to cover
        Row linesToCover = sheet.getRow(16);
        Cell linesToCoverCell = linesToCover.getCell(1);
        linesToCoverCell.setCellValue(issueExportData.getLinesToCover());

        //Set covered lines
        Row coveredLines = sheet.getRow(17);
        Cell coveredLinesCell = coveredLines.getCell(1);
        coveredLinesCell.setCellValue(issueExportData.getCoveredLines());

        //Set analyzed date 2
        Row analyzedDateRow2 = sheet.getRow(18);
        Cell analyzedDateCell2 = analyzedDateRow2.getCell(1);
        analyzedDateCell2.setCellValue(analyzedDate);
    }

    private void fillData(Sheet sheet, List<IssueDto> issues, Integer rowStartIndex) {
        for (IssueDto issue : issues) {
            Row row = sheet.createRow(rowStartIndex++);

            // Column 0: Issue Key
            row.createCell(0).setCellValue(issue.getKey());

            // Column 1: Type (BUG / CODE_SMELL / VULNERABILITY)
            row.createCell(1).setCellValue(issue.getType());

            // Column 2: Severity (BLOCKER / CRITICAL / MAJOR / MINOR / INFO)
            row.createCell(2).setCellValue(
                    issue.getImpacts()
                            .stream()
                            .findFirst()
                            .map(Impact::getSeverity)
                            .orElse(EMPTY)
            );

            // Column 3: Status (OPEN / CONFIRMED / RESOLVED ...)
            row.createCell(3).setCellValue(issue.getStatus());

            // Column 4: Rule (rule key, e.g. java:S2259)
            row.createCell(4).setCellValue(issue.getRule());

            // Column 5: Component (file path)
            row.createCell(5).setCellValue(issue.getComponent());

            // Column 6: Line number
            row.createCell(6).setCellValue(issue.getLine());

            // Column 7: Message (issue description)
            row.createCell(7).setCellValue(issue.getMessage());

            // Column 8: Effort (minutes)
            row.createCell(8).setCellValue(issue.getEffort());

            // Column 9: Tags (comma separated)
            row.createCell(9).setCellValue(
                    of(issue)
                            .map(IssueDto::getTags)
                            .map(tags -> join(",", tags))
                            .orElse(EMPTY)
            );

            // Column 10: Hotspot Review status
            row.createCell(10).setCellValue(issue.getHotspot());

            // Column 11: Created date
            row.createCell(11).setCellValue(toDateFormat(issue.getCreationDate()));

            // Column 12: Updated date
            row.createCell(12).setCellValue(toDateFormat(issue.getUpdateDate()));

            // Column 13: Author
            row.createCell(13).setCellValue(issue.getAuthor());
        }
    }

    private void fillDashboard(Sheet sheet, Map<String, IssueExportData> dataMap) {
        int BEGIN_INDEX_TO_MOVE_DOWN = 7;
        int NUMBER_OF_SAMPLE_DATA_ROW = 3;
        int NUMBER_OF_ROW_ADDED = dataMap.size() - NUMBER_OF_SAMPLE_DATA_ROW;
        int currentDataStartRowIndex = 4;
        sheet.shiftRows(BEGIN_INDEX_TO_MOVE_DOWN, sheet.getLastRowNum(), NUMBER_OF_ROW_ADDED, true, false);
        Workbook workbook = sheet.getWorkbook();
        Row templateRow = sheet.getRow(4);
        for (var entry : dataMap.entrySet()) {
            for (int i = 0; i < 16; i++) {
                Row startRow = sheet.getRow(currentDataStartRowIndex);
                if (startRow == null) {
                    startRow = sheet.createRow(currentDataStartRowIndex);
                }
                Cell cell = startRow.getCell(i);
                if (cell == null) {
                    cell = startRow.createCell(i);
                }
                Cell templateCell = templateRow.getCell(i);
                copyCellStyle(templateCell, cell, workbook);
                cell.setBlank();
                String formula = getCellFormula(entry.getKey(), currentDataStartRowIndex, i);
                cell.setCellFormula(formula);
            }
            currentDataStartRowIndex++;
        }
    }

    private void clearDataRows(Sheet sheet, int startRow, int endRow) {
        for (int r = startRow; r <= endRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            for (Cell cell : row) {
                cell.setBlank();
            }
        }
    }

    private String getCellFormula(String sheetName, int rowIndex, int cellIndex) {
        int rowNumber = rowIndex + 1;
        return switch (cellIndex) {
            case 0  -> format("'%s'!B%d", sheetName, 3);
            case 1  -> format("'%s'!B%d", sheetName, 4);
            case 2  -> format("'%s'!B%d", sheetName, 16);
            case 3  -> format("'%s'!B%d", sheetName, 17);
            case 4  -> format("'%s'!B%d", sheetName, 18);
            case 5  -> format("IF(D%d=0,0,E%d/D%d)", rowNumber, rowNumber, rowNumber);
            case 6  -> format("'%s'!B%d", sheetName, 9);
            case 7  -> format("'%s'!B%d", sheetName, 10);
            case 8  -> format("'%s'!B%d", sheetName, 11);
            case 9  -> format("'%s'!B%d", sheetName, 12);
            case 10 -> format("'%s'!B%d", sheetName, 13);
            case 11 -> format("'%s'!B%d", sheetName, 15);
            case 12 -> format("'%s'!B%d", sheetName, 19);
            case 14 -> format("ROUND(J%d*K%d,0)", rowNumber, rowNumber);
            case 15 -> format("J%d", rowNumber);
            default -> "0";
        };
    }

    private void copyCellStyle(Cell source, Cell target, Workbook workbook) {
        if (source == null || target == null) return;

        CellStyle newStyle = workbook.createCellStyle();
        newStyle.cloneStyleFrom(source.getCellStyle());

        target.setCellStyle(newStyle);
    }

    private String toDateFormat(String date) {
        OffsetDateTime odt = parse(date, SONAR_INPUT_FORMAT);
        ZonedDateTime vnTime = odt.atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh"));

        return vnTime.format(OUTPUT_FORMAT);
    }

    private void logTotalIssues(Map<String, IssueExportData> dataMap) {
        int totalIssuesCount = dataMap.values().stream()
                .map(IssueExportData::getIssues)
                .filter(Objects::nonNull)
                .mapToInt(List::size)
                .sum();

        log.info("[ExcelServiceImpl] Total sonarQ issues to be exported: {}", totalIssuesCount);
    }
}
