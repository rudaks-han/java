package kr.co.spectra.attic.tools.convert.customize.share;

import kr.co.spectra.attic.tools.convert.customize.ConfigProperty;
import kr.co.spectra.attic.tools.convert.customize.share.Util;
import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelWriter
{
    private int MAX_CELL_WIDTH = 60000;

    private HSSFWorkbook workbook;
    private HSSFSheet sheet;
    private HSSFFont font;

    private List<ConfigProperty.ExcelColumn> columnList;
    private List<HashMap<String, String>> dataList;

    private HSSFRow row;
    private HSSFCell cell;

    private CellStyle headerStyle;
    private CellStyle bodyStyle;

    public ExcelWriter(String sheetName, List<ConfigProperty.ExcelColumn> columnList, List<HashMap<String, String>> dataList)
    {
        workbook = new HSSFWorkbook();
        sheet = workbook.createSheet(sheetName);
        this.columnList = columnList;
        this.dataList = dataList;
        font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setFontName("맑은 고딕");

        headerStyle = getHeaderStyle(workbook);
        bodyStyle = getBodyStyle(workbook);

        headerStyle.setFont(font);
        bodyStyle.setFont(font);
    }

    public void write(String excelFilePath)
    {
        if (dataList != null && dataList.size() > 0)
        {
            createHeaderCell();
            createDataCell();
        }

        try
        {
            FileUtils.forceMkdir(new File(excelFilePath.substring(0, excelFilePath.lastIndexOf("/"))));
            FileOutputStream fos = new FileOutputStream(excelFilePath);
            workbook.write(fos);
            fos.close();

            Util.printLog("saved file to : " + excelFilePath);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    private CellStyle getHeaderStyle(HSSFWorkbook workbook)
    {
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setAlignment(CellStyle.ALIGN_CENTER);
        headerStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        headerStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);
        headerStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);
        headerStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);
        headerStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN);
        headerStyle.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
        headerStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

        return headerStyle;
    }

    private CellStyle getBodyStyle(HSSFWorkbook workbook)
    {
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

        return bodyStyle;
    }

    private void createHeaderCell()
    {
        row = sheet.createRow((short) 0);
        row.setHeight((short) 1024);
        for (int i = 0; i < columnList.size(); i++)
        {
            cell = row.createCell(i);
            cell.setCellValue(columnList.get(i).getColumnName());
            cell.setCellStyle(headerStyle);

            String id = columnList.get(i).getId();
            boolean autoSize = columnList.get(i).isAutoSize();
            int addColumnWidth = columnList.get(i).getAddColumnWidth();

            int columnWidth = sheet.getColumnWidth(i) + addColumnWidth;
            if (columnWidth >= MAX_CELL_WIDTH)
                columnWidth = MAX_CELL_WIDTH;
            sheet.setColumnWidth(i, columnWidth);
            if (autoSize)
                sheet.autoSizeColumn((short) i);
        }
    }

    private void createDataCell()
    {
        int i = 1;
        for (Map<String, String> mapObject : dataList)
        {
            row = sheet.createRow((short) i);
            row.setHeight((short) -1);

            if (columnList != null && columnList.size() > 0)
            {
                addDataCell(mapObject);
            }

            i++;
        }
    }

    private void addDataCell(Map<String, String> map)
    {
        for (int i = 0; i < columnList.size(); i++)
        {
            cell = row.createCell(i);

            String id = columnList.get(i).getId();
            String value = map.get(id);
            cell.setCellValue(value);
            cell.setCellStyle(bodyStyle);
        }
    }

}
