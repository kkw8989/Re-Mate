package com.example.backend.service;

import com.example.backend.domain.receipt.ReceiptStatus;
import com.example.backend.entity.MembershipStatus;
import com.example.backend.entity.Receipt;
import com.example.backend.entity.ReceiptItem;
import com.example.backend.repository.ReceiptItemRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.WorkspaceMemberRepository;
import com.example.backend.repository.WorkspaceRepository;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelExportService {

  private final ReceiptItemRepository receiptItemRepository;
  private final UserRepository userRepository;
  private final WorkspaceRepository workspaceRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;

  @Value("${app.backend.base-url:http://localhost:8080}")
  private String baseUrl;

  private static final Map<String, String> CATEGORY_LABEL =
      Map.ofEntries(
          Map.entry("FOOD", "식비"),
          Map.entry("TRANSPORT", "교통비"),
          Map.entry("OFFICE", "사무용품비"),
          Map.entry("ENTERTAINMENT", "회식/접대비"),
          Map.entry("ACCOMMODATION", "숙박비"),
          Map.entry("SHOPPING", "쇼핑"),
          Map.entry("ACTIVITY", "활동비"),
          Map.entry("EQUIPMENT", "장비/부품비"),
          Map.entry("SERVICE", "외주/용역비"),
          Map.entry("WELFARE", "복리후생비"),
          Map.entry("MEDICAL", "의료비"),
          Map.entry("ETC", "기타"),
          Map.entry("OTHER", "기타"));

  private static final Map<String, String> INAPPROPRIATE_LABEL =
      Map.of(
          "NIGHT_PAYMENT", "심야결제",
          "HOLIDAY_PAYMENT", "공휴일결제",
          "SUSPICIOUS_ENTERTAINMENT", "유흥업소의심",
          "SPLIT_PAYMENT_SUSPICIOUS", "쪼개기결제의심");

  private static final byte[] COLOR_HEADER = {(byte) 31, (byte) 78, (byte) 121};
  private static final byte[] COLOR_HEADER_LIGHT = {(byte) 189, (byte) 215, (byte) 238};
  private static final byte[] COLOR_ROW_ALT = {(byte) 242, (byte) 247, (byte) 251};
  private static final byte[] COLOR_APPROVED = {(byte) 198, (byte) 239, (byte) 206};
  private static final byte[] COLOR_REJECTED = {(byte) 255, (byte) 199, (byte) 206};
  private static final byte[] COLOR_WARNING = {(byte) 255, (byte) 235, (byte) 156};
  private static final byte[] COLOR_WARNING_STRONG = {(byte) 255, (byte) 80, (byte) 80};
  private static final byte[] COLOR_STAT_BG = {(byte) 240, (byte) 248, (byte) 255};

  public byte[] generateExcel(List<Receipt> receipts, Long workspaceId) {
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      String workspaceName =
          workspaceRepository.findById(workspaceId).map(w -> w.getName()).orElse("-");

      String adminName =
          workspaceMemberRepository
              .findAllByWorkspaceIdAndStatus(workspaceId, MembershipStatus.ACCEPTED)
              .stream()
              .filter(m -> m.getRole() != null && "ADMIN".equals(m.getRole().name()))
              .findFirst()
              .flatMap(m -> userRepository.findById(m.getUserId()))
              .map(u -> u.getName())
              .orElse("-");

      // Sheet3 먼저 생성해서 행 번호 맵 확보
      Map<Long, Integer> receiptRowMap = new HashMap<>();
      createSheet3(workbook, receipts, receiptRowMap);
      createSheet1(workbook, receipts, workspaceName, adminName, receiptRowMap);
      createSheet2(workbook, receipts, workspaceName);

      // 시트 순서 조정 (1→2→3)
      workbook.setSheetOrder("지출 목록", 0);
      workbook.setSheetOrder("요약 대시보드", 1);
      workbook.setSheetOrder("품목 상세", 2);

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      workbook.write(out);
      return out.toByteArray();
    } catch (Exception e) {
      log.error("엑셀 생성 실패", e);
      throw new RuntimeException("EXCEL_GENERATION_FAILED", e);
    }
  }

  // ===================== SHEET 1: 지출 목록 =====================
  private void createSheet1(
      XSSFWorkbook wb,
      List<Receipt> receipts,
      String workspaceName,
      String adminName,
      Map<Long, Integer> receiptRowMap) {

    XSSFSheet sheet = wb.createSheet("지출 목록");
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    CellStyle titleStyle = createTitleStyle(wb);
    CellStyle headerStyle = createHeaderStyle(wb);
    CellStyle dataStyle = createDataStyle(wb, false);
    CellStyle dataAltStyle = createDataStyle(wb, true);
    CellStyle amountStyle = createAmountStyle(wb, false);
    CellStyle amountAltStyle = createAmountStyle(wb, true);
    CellStyle approvedStyle = createStatusStyle(wb, COLOR_APPROVED, false);
    CellStyle rejectedStyle = createStatusStyle(wb, COLOR_REJECTED, false);
    CellStyle warningStyle = createStatusStyle(wb, COLOR_WARNING, false);
    CellStyle linkStyle = createLinkStyle(wb, false);
    CellStyle linkAltStyle = createLinkStyle(wb, true);

    Row titleRow = sheet.createRow(0);
    titleRow.setHeightInPoints(32);
    Cell titleCell = titleRow.createCell(0);
    titleCell.setCellValue(workspaceName + "  지출 목록");
    titleCell.setCellStyle(titleStyle);
    sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

    String[] headers = {"No.", "일자", "게시자", "지출처", "구매목적", "금액", "상태", "증빙자료", "비고"};
    Row headerRow = sheet.createRow(1);
    headerRow.setHeightInPoints(22);
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    int rowNum = 2;
    int no = 1;
    for (Receipt r : receipts) {
      boolean isAlt = (no % 2 == 0);
      Row row = sheet.createRow(rowNum++);
      row.setHeightInPoints(20);

      String ownerName = userRepository.findById(r.getUserId()).map(u -> u.getName()).orElse("-");
      String categoryKey = r.getCategory() != null ? r.getCategory() : "ETC";
      String category = CATEGORY_LABEL.getOrDefault(categoryKey, "기타");

      CellStyle statusStyle;
      String statusLabel;
      if (r.getStatus() == ReceiptStatus.APPROVED) {
        statusStyle = approvedStyle;
        statusLabel = "✔ 승인";
      } else if (r.getStatus() == ReceiptStatus.REJECTED) {
        statusStyle = rejectedStyle;
        statusLabel = "✘ 반려";
      } else {
        statusStyle = isAlt ? dataAltStyle : dataStyle;
        statusLabel = "⏳ 대기";
      }

      String note = buildNote(r);
      String truncatedNote = note.length() > 25 ? note.substring(0, 25) + "…" : note;

      CellStyle ds = isAlt ? dataAltStyle : dataStyle;
      CellStyle as = isAlt ? amountAltStyle : amountStyle;

      createCell(row, 0, String.valueOf(no++), ds);
      createCell(row, 1, r.getTradeAt() != null ? r.getTradeAt().format(fmt) : "-", ds);
      createCell(row, 2, ownerName, ds);
      createCell(row, 3, r.getStoreName() != null ? r.getStoreName() : "-", ds);
      createCell(row, 4, category, ds);

      // 금액 → Sheet3 내부 링크
      Integer sheet3Row = receiptRowMap.get(r.getId());
      if (sheet3Row != null) {
        Cell amountCell = row.createCell(5);
        amountCell.setCellValue(r.getTotalAmount());
        amountCell.setCellStyle(isAlt ? amountAltStyle : amountStyle);
        XSSFHyperlink link =
            (XSSFHyperlink) wb.getCreationHelper().createHyperlink(HyperlinkType.DOCUMENT);
        link.setAddress("'품목 상세'!A" + sheet3Row);
        amountCell.setHyperlink(link);
      } else {
        createAmountCell(row, 5, r.getTotalAmount(), as);
      }

      createCell(row, 6, statusLabel, statusStyle);

      // 증빙자료 하이퍼링크
      if (r.getFilePath() != null) {
        Cell linkCell = row.createCell(7);
        linkCell.setCellValue("🔍 보기");
        linkCell.setCellStyle(isAlt ? linkAltStyle : linkStyle);
        XSSFHyperlink imgLink =
            (XSSFHyperlink) wb.getCreationHelper().createHyperlink(HyperlinkType.URL);
        imgLink.setAddress(baseUrl + "/images/" + r.getFilePath());
        linkCell.setHyperlink(imgLink);
      } else {
        createCell(row, 7, "-", ds);
      }

      Cell noteCell = row.createCell(8);
      noteCell.setCellValue(note);
      XSSFCellStyle noteStyle = (XSSFCellStyle) (note.isEmpty() ? ds : warningStyle);
      noteCell.setCellStyle(noteStyle);
    }

    int[] colWidths = {1800, 4500, 3200, 7000, 4200, 4500, 3200, 3500, 13000};
    for (int i = 0; i < colWidths.length; i++) {
      sheet.setColumnWidth(i, colWidths[i]);
    }
  }

  // ===================== SHEET 2: 요약 대시보드 =====================
  private void createSheet2(XSSFWorkbook wb, List<Receipt> receipts, String workspaceName) {
    XSSFSheet sheet = wb.createSheet("요약 대시보드");

    CellStyle titleStyle = createTitleStyle(wb);
    CellStyle headerStyle = createHeaderStyle(wb);
    CellStyle dataStyle = createDataStyle(wb, false);
    CellStyle dataAltStyle = createDataStyle(wb, true);
    CellStyle amountStyle = createAmountStyle(wb, false);
    CellStyle statStyle = createStatStyle(wb);
    CellStyle statNumStyle = createStatNumStyle(wb);
    CellStyle approvedStyle = createStatusStyle(wb, COLOR_APPROVED, false);
    CellStyle warnRowStyle = createStatusStyle(wb, COLOR_WARNING, false);
    CellStyle warnStrongStyle = createStatusStyle(wb, COLOR_WARNING_STRONG, true);
    CellStyle emptyStyle = createDataStyle(wb, false);

    // ── 타이틀 ──
    Row titleRow = sheet.createRow(0);
    titleRow.setHeightInPoints(32);
    for (int i = 0; i <= 11; i++) {
      Cell cell = titleRow.createCell(i);
      if (i == 0) cell.setCellValue(workspaceName + "  지출 현황 요약");
      cell.setCellStyle(titleStyle);
    }
    sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 11));

    // ── 핵심 지표 (가로 배치) ──
    long totalCount = receipts.size();
    long approvedCount =
        receipts.stream().filter(r -> r.getStatus() == ReceiptStatus.APPROVED).count();
    long rejectedCount =
        receipts.stream().filter(r -> r.getStatus() == ReceiptStatus.REJECTED).count();
    long waitingCount = totalCount - approvedCount - rejectedCount;
    int totalAmount = receipts.stream().mapToInt(Receipt::getTotalAmount).sum();
    long suspiciousCount =
        receipts.stream()
            .filter(
                r -> r.getInappropriateReasons() != null && !r.getInappropriateReasons().isEmpty())
            .count();

    // 빈 행
    sheet.createRow(1).setHeightInPoints(6);

    // 지표 라벨 행
    Row statLabelRow = sheet.createRow(2);
    statLabelRow.setHeightInPoints(22);
    String[] statLabels = {"총 영수증", "승인", "반려", "대기", "총 지출금액", "의심 영수증"};
    int[] statCols = {0, 2, 4, 6, 8, 10};
    for (int i = 0; i <= 11; i++) {
      statLabelRow.createCell(i).setCellStyle(statStyle);
    }
    for (int i = 0; i < statLabels.length; i++) {
      Cell cell = statLabelRow.createCell(statCols[i]);
      cell.setCellValue(statLabels[i]);
      cell.setCellStyle(statStyle);
      statLabelRow.createCell(statCols[i] + 1).setCellStyle(statStyle);
      sheet.addMergedRegion(new CellRangeAddress(2, 2, statCols[i], statCols[i] + 1));
    }

    // 지표 숫자 행
    Row statNumRow = sheet.createRow(3);
    statNumRow.setHeightInPoints(40);
    String[] statValues = {
      totalCount + "건",
      approvedCount + "건",
      rejectedCount + "건",
      waitingCount + "건",
      String.format("%,d원", totalAmount),
      suspiciousCount + "건"
    };
    for (int i = 0; i <= 11; i++) {
      statNumRow.createCell(i).setCellStyle(statNumStyle);
    }
    for (int i = 0; i < statValues.length; i++) {
      CellStyle cs = (i == 5 && suspiciousCount > 0) ? warnStrongStyle : statNumStyle;
      Cell cell = statNumRow.createCell(statCols[i]);
      cell.setCellValue(statValues[i]);
      cell.setCellStyle(cs);
      statNumRow.createCell(statCols[i] + 1).setCellStyle(cs);
      sheet.addMergedRegion(new CellRangeAddress(3, 3, statCols[i], statCols[i] + 1));
    }

    // 빈 행
    sheet.createRow(4).setHeightInPoints(6);
    sheet.createRow(5).setHeightInPoints(6);

    // ── 인원별 / 카테고리별 (좌우 배치) ──
    int tableStartRow = 6;

    // 섹션 헤더 행
    Row sectionHeaderRow = sheet.createRow(tableStartRow);
    sectionHeaderRow.setHeightInPoints(22);
    // 인원별 섹션 헤더
    for (int i = 0; i <= 2; i++) {
      Cell cell = sectionHeaderRow.createCell(i);
      if (i == 0) cell.setCellValue("👤 인원별 지출");
      cell.setCellStyle(headerStyle);
    }
    sheet.addMergedRegion(new CellRangeAddress(tableStartRow, tableStartRow, 0, 2));
    // 사이 빈 셀
    for (int i = 3; i <= 4; i++) {
      sectionHeaderRow.createCell(i).setCellStyle(emptyStyle);
    }
    // 카테고리별 섹션 헤더
    for (int i = 5; i <= 7; i++) {
      Cell cell = sectionHeaderRow.createCell(i);
      if (i == 5) cell.setCellValue("📂 카테고리별 지출");
      cell.setCellStyle(headerStyle);
    }
    sheet.addMergedRegion(new CellRangeAddress(tableStartRow, tableStartRow, 5, 7));

    // 컬럼 헤더 행
    Row personColRow = sheet.createRow(tableStartRow + 1);
    personColRow.setHeightInPoints(18);
    createCell(personColRow, 0, "이름", headerStyle);
    createCell(personColRow, 1, "건수", headerStyle);
    createCell(personColRow, 2, "합계", headerStyle);
    createCell(personColRow, 3, "", emptyStyle);
    createCell(personColRow, 4, "", emptyStyle);
    createCell(personColRow, 5, "카테고리", headerStyle);
    createCell(personColRow, 6, "건수", headerStyle);
    createCell(personColRow, 7, "합계", headerStyle);
    createCell(personColRow, 8, "", emptyStyle);

    Map<Long, List<Receipt>> byUser =
        receipts.stream().collect(Collectors.groupingBy(Receipt::getUserId));
    Map<String, List<Receipt>> byCategory =
        receipts.stream()
            .collect(
                Collectors.groupingBy(
                    r ->
                        CATEGORY_LABEL.getOrDefault(
                            r.getCategory() != null ? r.getCategory() : "ETC", "기타")));

    int maxRows = Math.max(byUser.size(), byCategory.size());
    List<Map.Entry<Long, List<Receipt>>> userList = List.copyOf(byUser.entrySet());
    List<Map.Entry<String, List<Receipt>>> catList = List.copyOf(byCategory.entrySet());

    for (int i = 0; i < maxRows; i++) {
      Row row = sheet.createRow(tableStartRow + 2 + i);
      row.setHeightInPoints(18);
      boolean alt = (i % 2 == 0);
      CellStyle ds = alt ? dataAltStyle : dataStyle;

      if (i < userList.size()) {
        Map.Entry<Long, List<Receipt>> entry = userList.get(i);
        String name = userRepository.findById(entry.getKey()).map(u -> u.getName()).orElse("-");
        int amt = entry.getValue().stream().mapToInt(Receipt::getTotalAmount).sum();
        createCell(row, 0, name, ds);
        createCell(row, 1, entry.getValue().size() + "건", ds);
        createAmountCell(row, 2, amt, amountStyle);
      } else {
        createCell(row, 0, "", ds);
        createCell(row, 1, "", ds);
        createCell(row, 2, "", ds);
      }
      // 사이 빈 셀
      createCell(row, 3, "", emptyStyle);
      createCell(row, 4, "", emptyStyle);

      if (i < catList.size()) {
        Map.Entry<String, List<Receipt>> entry = catList.get(i);
        int amt = entry.getValue().stream().mapToInt(Receipt::getTotalAmount).sum();
        createCell(row, 5, entry.getKey(), ds);
        createCell(row, 6, entry.getValue().size() + "건", ds);
        createAmountCell(row, 7, amt, amountStyle);
      } else {
        createCell(row, 5, "", ds);
        createCell(row, 6, "", ds);
        createCell(row, 7, "", ds);
      }
      createCell(row, 8, "", emptyStyle);
    }

    // 빈 구분 행
    sheet.createRow(tableStartRow + maxRows + 2).setHeightInPoints(6);
    sheet.createRow(tableStartRow + maxRows + 3).setHeightInPoints(6);

    // ── 의심 영수증 (하단 강조) ──
    List<Receipt> suspicious =
        receipts.stream()
            .filter(
                r -> r.getInappropriateReasons() != null && !r.getInappropriateReasons().isEmpty())
            .collect(Collectors.toList());

    int suspStartRow = tableStartRow + maxRows + 4;

    // 의심 영수증 타이틀 - 전체 셀에 스타일 적용
    Row suspTitleRow = sheet.createRow(suspStartRow);
    suspTitleRow.setHeightInPoints(26);
    CellStyle suspTitleStyle = suspicious.isEmpty() ? headerStyle : warnStrongStyle;
    for (int i = 0; i <= 11; i++) {
      Cell cell = suspTitleRow.createCell(i);
      if (i == 0) cell.setCellValue("⚠️  의심 영수증  (" + suspicious.size() + "건)");
      cell.setCellStyle(suspTitleStyle);
    }
    sheet.addMergedRegion(new CellRangeAddress(suspStartRow, suspStartRow, 0, 11));

    if (!suspicious.isEmpty()) {
      // 의심 영수증 헤더 - 전체 셀 스타일 적용
      Row suspHeaderRow = sheet.createRow(suspStartRow + 1);
      suspHeaderRow.setHeightInPoints(18);
      createCell(suspHeaderRow, 0, "일자", headerStyle);
      createCell(suspHeaderRow, 1, "게시자", headerStyle);
      createCell(suspHeaderRow, 2, "지출처", headerStyle);
      createCell(suspHeaderRow, 3, "금액", headerStyle);
      for (int i = 4; i <= 11; i++) {
        Cell cell = suspHeaderRow.createCell(i);
        if (i == 4) cell.setCellValue("의심 사유");
        cell.setCellStyle(headerStyle);
      }
      sheet.addMergedRegion(new CellRangeAddress(suspStartRow + 1, suspStartRow + 1, 4, 11));

      DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      int sr = suspStartRow + 2;
      for (Receipt r : suspicious) {
        Row row = sheet.createRow(sr++);
        row.setHeightInPoints(20);
        String ownerName = userRepository.findById(r.getUserId()).map(u -> u.getName()).orElse("-");
        String tags =
            r.getInappropriateReasons().stream()
                .map(t -> INAPPROPRIATE_LABEL.getOrDefault(t, t))
                .collect(Collectors.joining("  |  "));
        createCell(row, 0, r.getTradeAt() != null ? r.getTradeAt().format(fmt) : "-", warnRowStyle);
        createCell(row, 1, ownerName, warnRowStyle);
        createCell(row, 2, r.getStoreName() != null ? r.getStoreName() : "-", warnRowStyle);
        createAmountCell(row, 3, r.getTotalAmount(), amountStyle);
        for (int i = 4; i <= 11; i++) {
          Cell cell = row.createCell(i);
          if (i == 4) cell.setCellValue(tags);
          cell.setCellStyle(warnRowStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(sr - 1, sr - 1, 4, 11));
      }
    } else {
      Row noRow = sheet.createRow(suspStartRow + 1);
      noRow.setHeightInPoints(20);
      for (int i = 0; i <= 11; i++) {
        Cell cell = noRow.createCell(i);
        if (i == 0) cell.setCellValue("✅  의심 영수증이 없습니다.");
        cell.setCellStyle(approvedStyle);
      }
      sheet.addMergedRegion(new CellRangeAddress(suspStartRow + 1, suspStartRow + 1, 0, 11));
    }

    // 컬럼 너비
    sheet.setColumnWidth(0, 4000);
    sheet.setColumnWidth(1, 3000);
    sheet.setColumnWidth(2, 5000);
    sheet.setColumnWidth(3, 5000);
    sheet.setColumnWidth(4, 1000);
    sheet.setColumnWidth(5, 4500);
    sheet.setColumnWidth(6, 3000);
    sheet.setColumnWidth(7, 5000);
    sheet.setColumnWidth(8, 1000);
    for (int i = 9; i <= 11; i++) {
      sheet.setColumnWidth(i, 3000);
    }
  }

  // ===================== SHEET 3: 품목 상세 =====================
  private void createSheet3(
      XSSFWorkbook wb, List<Receipt> receipts, Map<Long, Integer> receiptRowMap) {

    XSSFSheet sheet = wb.createSheet("품목 상세");

    CellStyle titleStyle = createTitleStyle(wb);
    CellStyle headerStyle = createHeaderStyle(wb);
    CellStyle dataStyle = createDataStyle(wb, false);
    CellStyle dataAltStyle = createDataStyle(wb, true);
    CellStyle amountStyle = createAmountStyle(wb, false);
    CellStyle amountAltStyle = createAmountStyle(wb, true);
    CellStyle groupHeaderStyle = createGroupHeaderStyle(wb);

    Row titleRow = sheet.createRow(0);
    titleRow.setHeightInPoints(32);
    Cell titleCell = titleRow.createCell(0);
    titleCell.setCellValue("품목 상세 내역");
    titleCell.setCellStyle(titleStyle);
    sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

    String[] headers = {"순번", "일자", "지출처", "품목명", "수량", "단가", "소계"};
    Row headerRow = sheet.createRow(1);
    headerRow.setHeightInPoints(22);
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    int rowNum = 2;
    int no = 1;
    boolean groupAlt = false;

    for (Receipt r : receipts) {
      List<ReceiptItem> items = receiptItemRepository.findAllByReceiptId(r.getId());
      String dateStr = r.getTradeAt() != null ? r.getTradeAt().format(fmt) : "-";
      String storeName = r.getStoreName() != null ? r.getStoreName() : "-";
      String ownerName = userRepository.findById(r.getUserId()).map(u -> u.getName()).orElse("-");

      // 영수증 그룹 헤더
      Row groupRow = sheet.createRow(rowNum);
      groupRow.setHeightInPoints(18);
      Cell groupCell = groupRow.createCell(0);
      groupCell.setCellValue(
          dateStr
              + "  |  "
              + storeName
              + "  |  "
              + ownerName
              + "  |  합계: "
              + String.format("%,d원", r.getTotalAmount()));
      groupCell.setCellStyle(groupHeaderStyle);
      sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, 6));

      // 첫 번째 행 번호 저장 (Sheet1 링크용) → 그룹 헤더 다음 행
      receiptRowMap.put(r.getId(), rowNum + 2); // 엑셀은 1-indexed
      rowNum++;

      CellStyle ds = groupAlt ? dataAltStyle : dataStyle;
      CellStyle as = groupAlt ? amountAltStyle : amountStyle;

      if (items.isEmpty()) {
        Row row = sheet.createRow(rowNum++);
        row.setHeightInPoints(18);
        createCell(row, 0, String.valueOf(no++), ds);
        createCell(row, 1, dateStr, ds);
        createCell(row, 2, storeName, ds);
        createCell(row, 3, "(품목 없음)", ds);
        createCell(row, 4, "-", ds);
        createCell(row, 5, "-", ds);
        createAmountCell(row, 6, r.getTotalAmount(), as);
      } else {
        for (ReceiptItem item : items) {
          Row row = sheet.createRow(rowNum++);
          row.setHeightInPoints(18);
          createCell(row, 0, String.valueOf(no++), ds);
          createCell(row, 1, dateStr, ds);
          createCell(row, 2, storeName, ds);
          createCell(row, 3, item.getName() != null ? item.getName() : "-", ds);
          createCell(row, 4, String.valueOf(item.getQuantity()), ds);
          createAmountCell(row, 5, item.getPrice(), as);
          createAmountCell(row, 6, item.getQuantity() * item.getPrice(), as);
        }
      }
      // 영수증 사이 빈 행
      sheet.createRow(rowNum++).setHeightInPoints(6);
      groupAlt = !groupAlt;
    }

    int[] colWidths = {2000, 4000, 7000, 9000, 2500, 4500, 4500};
    for (int i = 0; i < colWidths.length; i++) {
      sheet.setColumnWidth(i, colWidths[i]);
    }
  }

  // ===================== 스타일 =====================
  private CellStyle createTitleStyle(XSSFWorkbook wb) {
    XSSFCellStyle style = wb.createCellStyle();
    style.setFillForegroundColor(new XSSFColor(COLOR_HEADER, null));
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setAlignment(HorizontalAlignment.LEFT);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    style.setLeftBorderColor(new XSSFColor(COLOR_HEADER, null));
    XSSFFont font = wb.createFont();
    font.setBold(true);
    font.setFontHeightInPoints((short) 14);
    font.setColor(new XSSFColor(new byte[] {(byte) 255, (byte) 255, (byte) 255}, null));
    font.setFontName("맑은 고딕");
    style.setFont(font);
    return style;
  }

  private CellStyle createHeaderStyle(XSSFWorkbook wb) {
    XSSFCellStyle style = wb.createCellStyle();
    style.setFillForegroundColor(new XSSFColor(COLOR_HEADER_LIGHT, null));
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    setBorders(style, BorderStyle.THIN, new byte[] {(byte) 150, (byte) 180, (byte) 210});
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    XSSFFont font = wb.createFont();
    font.setBold(true);
    font.setFontName("맑은 고딕");
    font.setFontHeightInPoints((short) 10);
    style.setFont(font);
    return style;
  }

  private CellStyle createDataStyle(XSSFWorkbook wb, boolean alt) {
    XSSFCellStyle style = wb.createCellStyle();
    if (alt) {
      style.setFillForegroundColor(new XSSFColor(COLOR_ROW_ALT, null));
      style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }
    setBorders(style, BorderStyle.THIN, new byte[] {(byte) 210, (byte) 215, (byte) 220});
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    XSSFFont font = wb.createFont();
    font.setFontName("맑은 고딕");
    font.setFontHeightInPoints((short) 10);
    style.setFont(font);
    return style;
  }

  private CellStyle createAmountStyle(XSSFWorkbook wb, boolean alt) {
    XSSFCellStyle style = wb.createCellStyle();
    if (alt) {
      style.setFillForegroundColor(new XSSFColor(COLOR_ROW_ALT, null));
      style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }
    setBorders(style, BorderStyle.THIN, new byte[] {(byte) 210, (byte) 215, (byte) 220});
    style.setAlignment(HorizontalAlignment.RIGHT);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    DataFormat fmt = wb.createDataFormat();
    style.setDataFormat(fmt.getFormat("#,##0"));
    XSSFFont font = wb.createFont();
    font.setFontName("맑은 고딕");
    font.setFontHeightInPoints((short) 10);
    style.setFont(font);
    return style;
  }

  private CellStyle createStatusStyle(XSSFWorkbook wb, byte[] color, boolean whiteFontBold) {
    XSSFCellStyle style = wb.createCellStyle();
    style.setFillForegroundColor(new XSSFColor(color, null));
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    setBorders(style, BorderStyle.THIN, new byte[] {(byte) 200, (byte) 200, (byte) 200});
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    style.setWrapText(false);
    XSSFFont font = wb.createFont();
    font.setFontName("맑은 고딕");
    font.setFontHeightInPoints((short) 10);
    if (whiteFontBold) {
      font.setBold(true);
      font.setColor(new XSSFColor(new byte[] {(byte) 255, (byte) 255, (byte) 255}, null));
    }
    style.setFont(font);
    return style;
  }

  private CellStyle createStatStyle(XSSFWorkbook wb) {
    XSSFCellStyle style = wb.createCellStyle();
    style.setFillForegroundColor(new XSSFColor(COLOR_HEADER_LIGHT, null));
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    setBorders(style, BorderStyle.MEDIUM, COLOR_HEADER);
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    XSSFFont font = wb.createFont();
    font.setBold(true);
    font.setFontName("맑은 고딕");
    font.setFontHeightInPoints((short) 11);
    style.setFont(font);
    return style;
  }

  private CellStyle createStatNumStyle(XSSFWorkbook wb) {
    XSSFCellStyle style = wb.createCellStyle();
    style.setFillForegroundColor(new XSSFColor(COLOR_STAT_BG, null));
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    setBorders(style, BorderStyle.MEDIUM, COLOR_HEADER);
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    XSSFFont font = wb.createFont();
    font.setBold(true);
    font.setFontName("맑은 고딕");
    font.setFontHeightInPoints((short) 16);
    font.setColor(new XSSFColor(COLOR_HEADER, null));
    style.setFont(font);
    return style;
  }

  private CellStyle createGroupHeaderStyle(XSSFWorkbook wb) {
    XSSFCellStyle style = wb.createCellStyle();
    style.setFillForegroundColor(
        new XSSFColor(new byte[] {(byte) 220, (byte) 230, (byte) 242}, null));
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    setBorders(style, BorderStyle.MEDIUM, COLOR_HEADER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    XSSFFont font = wb.createFont();
    font.setBold(true);
    font.setFontName("맑은 고딕");
    font.setFontHeightInPoints((short) 10);
    font.setColor(new XSSFColor(COLOR_HEADER, null));
    style.setFont(font);
    return style;
  }

  private CellStyle createLinkStyle(XSSFWorkbook wb, boolean alt) {
    XSSFCellStyle style = wb.createCellStyle();
    if (alt) {
      style.setFillForegroundColor(new XSSFColor(COLOR_ROW_ALT, null));
      style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }
    setBorders(style, BorderStyle.THIN, new byte[] {(byte) 210, (byte) 215, (byte) 220});
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    XSSFFont font = wb.createFont();
    font.setFontName("맑은 고딕");
    font.setFontHeightInPoints((short) 10);
    font.setColor(IndexedColors.DARK_BLUE.getIndex());
    font.setUnderline(Font.U_SINGLE);
    style.setFont(font);
    return style;
  }

  private void setBorders(XSSFCellStyle style, BorderStyle borderStyle, byte[] color) {
    style.setBorderBottom(borderStyle);
    style.setBorderTop(borderStyle);
    style.setBorderLeft(borderStyle);
    style.setBorderRight(borderStyle);
    XSSFColor c = new XSSFColor(color, null);
    style.setBottomBorderColor(c);
    style.setTopBorderColor(c);
    style.setLeftBorderColor(c);
    style.setRightBorderColor(c);
  }

  private void createCell(Row row, int col, String value, CellStyle style) {
    Cell cell = row.createCell(col);
    cell.setCellValue(value != null ? value : "-");
    cell.setCellStyle(style);
  }

  private void createAmountCell(Row row, int col, int value, CellStyle style) {
    Cell cell = row.createCell(col);
    cell.setCellValue(value);
    cell.setCellStyle(style);
  }

  private String buildNote(Receipt r) {
    StringBuilder note = new StringBuilder();
    if (r.getInappropriateReasons() != null && !r.getInappropriateReasons().isEmpty()) {
      String tags =
          r.getInappropriateReasons().stream()
              .map(t -> INAPPROPRIATE_LABEL.getOrDefault(t, t))
              .collect(Collectors.joining(", "));
      note.append("⚠️ ").append(tags);
    }
    if (r.getTags() != null && r.getTags().contains("SELF_APPROVED")) {
      if (note.length() > 0) note.append(" | ");
      note.append("자기승인");
    }
    if (r.getRejectionReason() != null && !r.getRejectionReason().isBlank()) {
      if (note.length() > 0) note.append(" | ");
      note.append("반려: ").append(r.getRejectionReason());
    }
    return note.toString();
  }
}
