package com.tool.sonarq.service.impl;

import com.tool.sonarq.dto.IssueDto;
import com.tool.sonarq.dto.IssueExportData;
import com.tool.sonarq.dto.model.Impact;
import com.tool.sonarq.exception.BizException;
import com.tool.sonarq.service.ExcelService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
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

    private static final DateTimeFormatter SONAR_INPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final DateTimeFormatter OUTPUT_FORMAT = DateTimeFormatter.ofPattern("d/M/yyyy  H:mm:ss");

    public Mono<byte[]> generateReport(Map<String, IssueExportData> dataMap, Integer rowStartIndex) {
        return fromCallable(() -> createExcelSheet(dataMap, rowStartIndex))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(IOException.class,
                        ex -> new BizException(ex.getMessage())
                );
    }

    private byte[] createExcelSheet(Map<String, IssueExportData> dataMap, Integer rowStartIndex) throws IOException {
        try (InputStream is =
                     of(getClass())
                             .map(Class::getClassLoader)
                             .map(classLoader ->
                                     classLoader.getResourceAsStream("templates/sonarqube_export_template.xlsx"))
                             .orElseThrow(() -> new RuntimeException("No templates found!"));

             XSSFWorkbook workbook = new XSSFWorkbook(is);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet template = workbook.getSheet("template");
            for (var entry : dataMap.entrySet()) {
                Sheet sheet = workbook.cloneSheet(workbook.getSheetIndex(template));
                workbook.setSheetName(workbook.getSheetIndex(sheet), entry.getKey());
                List<IssueDto> issues = of(entry.getValue())
                        .map(IssueExportData::getIssues)
                        .orElseGet(List::of);

                fillHeader(sheet, entry.getKey(), entry.getValue(), now().format(OUTPUT_FORMAT));
                fillData(sheet, issues, rowStartIndex);
            }
            int templateIndex = workbook.getSheetIndex("template");
            if(templateIndex >= 0) workbook.removeSheetAt(templateIndex);
//            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
            workbook.write(out);

            return out.toByteArray();
        }
    }

    private void fillHeader(Sheet sheet, String repository,
                            IssueExportData issueExportData, String analyzedDate) {
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

        Row bugs = sheet.getRow(8);
        Cell cell = bugs.getCell(1);
        cell.setBlank();
        String bugsDynamicFormula = format("COUNTIF(B%d:B%d,\"BUG\")", "startRowIndex", "startRowIndex + issues.size()");
        cell.setCellFormula(bugsDynamicFormula);

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
        Integer rowIndex = ofNullable(rowStartIndex).orElse(23);

        for (IssueDto issue : issues) {
            Row row = sheet.createRow(rowIndex++);

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

    private String toDateFormat(String date) {
        OffsetDateTime odt = parse(date, SONAR_INPUT_FORMAT);
        ZonedDateTime vnTime = odt.atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh"));

        return vnTime.format(OUTPUT_FORMAT);
    }
}
