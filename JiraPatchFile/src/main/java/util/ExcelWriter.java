package util;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelWriter
{
    public static HSSFWorkbook workbook = new HSSFWorkbook();

    public static void write(List<HashMap<String, String>> list, String excelFilename)
    {
        ArrayList<String> columnList = new ArrayList<String>();

        if (list != null && list.size() > 0)
        {
            columnList.add("Key");
            columnList.add("Summary");
            columnList.add("Priority");
            columnList.add("Description");
            columnList.add("패치중요도");
            columnList.add("SVN Rev.No");
            columnList.add("처리내역");
            columnList.add("메뉴");
            columnList.add("패치 파일 리스트");
        }

        HSSFSheet sheet = workbook.createSheet("패치목록");
        HSSFRow row = null; // 행
        HSSFCell cell = null; // 셀

        HSSFFont font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setFontName("맑은 고딕");

        CellStyle alignLeftStyle = workbook.createCellStyle();
        alignLeftStyle.setAlignment(CellStyle.ALIGN_LEFT);
        alignLeftStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        alignLeftStyle.setFont(font);
        CellStyle alignRightStyle = workbook.createCellStyle();
        alignRightStyle.setAlignment(CellStyle.ALIGN_RIGHT);
        alignRightStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        alignRightStyle.setFont(font);
        CellStyle alignCenterStyle = workbook.createCellStyle();
        alignCenterStyle.setAlignment(CellStyle.ALIGN_CENTER);
        alignCenterStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        alignCenterStyle.setFont(font);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setAlignment(CellStyle.ALIGN_CENTER);
        headerStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        headerStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);
        headerStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);
        headerStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);
        headerStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN);
        headerStyle.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
        headerStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        headerStyle.setFont(font);

        CellStyle bodyStyle = workbook.createCellStyle();
        bodyStyle.setAlignment(CellStyle.ALIGN_LEFT);
        bodyStyle.setVerticalAlignment(CellStyle.VERTICAL_TOP);
        bodyStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);
        bodyStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);
        bodyStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);
        bodyStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN);
        bodyStyle.setFillForegroundColor(HSSFColor.WHITE.index);
        bodyStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        bodyStyle.setWrapText(true);
        bodyStyle.setFont(font);

        if (list != null && list.size() > 0)
        {
            int i = 0;
            // header
            row = sheet.createRow((short) i);
            row.setHeight((short) 1024);
            if (columnList != null && columnList.size() > 0)
            {
                for (int j = 0; j < columnList.size(); j++)
                {
                    cell = row.createCell(j);
                    cell.setCellValue(columnList.get(j));
                    cell.setCellStyle(headerStyle);
                }
            }

            i++;
            for (Map<String, String> mapObject : list)
            {
                row = sheet.createRow((short) i);
                row.setHeight((short) -1);
                i++;
                if (columnList != null && columnList.size() > 0)
                {
                    for (int j = 0; j < columnList.size(); j++)
                    {
                        cell = row.createCell(j);
                        String columnName = columnList.get(j);
                        String value = "";

                        switch (j)
                        {
                            case 0:
                                value = mapObject.get("key");
                                //sheet.setColumnWidth(j, 2800);
                                sheet.autoSizeColumn((short) j);
                                sheet.setColumnWidth(j, (sheet.getColumnWidth(j)) + 512);
                                break;
                            case 1:
                                value = mapObject.get("summary");
                                sheet.setColumnWidth(j, 2800*3);
                                break;
                            case 2:
                                value = mapObject.get("priority");
                                sheet.autoSizeColumn((short) j);
                                sheet.setColumnWidth(j, (sheet.getColumnWidth(j)) + 512);
                                break;
                            case 3:
                                value = mapObject.get("description");
                                sheet.setColumnWidth(j, 2800*4);
                                break;
                            case 4:
                                value = mapObject.get("patchImportance");
                                sheet.autoSizeColumn((short) j);
                                sheet.setColumnWidth(j, (sheet.getColumnWidth(j)) + 512);
                                break;
                            case 5:
                                value = mapObject.get("revision");
                                sheet.autoSizeColumn((short) j);
                                sheet.setColumnWidth(j, (sheet.getColumnWidth(j)) + 512);
                                break;
                            case 6:
                                value = mapObject.get("responseHistory");
                                sheet.setColumnWidth(j, 2800*5);
                                break;
                            case 7:
                                value = mapObject.get("menu");
                                sheet.setColumnWidth(j, 2800*4);
                                break;
                            case 8:
                                value = mapObject.get("patchFileList");
                                sheet.setColumnWidth(j, 2800*6);
                                break;
                        }

                        cell.setCellValue(value);
                        cell.setCellStyle(bodyStyle);
                    }
                }
            }
        }

        try
        {
            Util.debug("[save as excel] " + System.getProperty("user.dir") + "/output/" + excelFilename);
            FileOutputStream fos = new FileOutputStream(System.getProperty("user.dir") + "/output/" + excelFilename);
            workbook.write(fos);
            fos.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            Util.debug(e.getMessage());
        }

    }
}
