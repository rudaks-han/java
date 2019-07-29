package kr.pe.rudaks.app.exporter;

import kr.pe.rudaks.app.ResultData;
import kr.pe.rudaks.app.util.RecordSet;
import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class QueryExcelExporter extends QueryExporter
{
    private int MAX_CELL_WIDTH = 60000;

    private HSSFWorkbook workbook;
    private HSSFSheet sheet;
    private HSSFFont font;

    private HSSFRow row;
    private HSSFCell cell;

    private CellStyle headerStyle;
    private CellStyle bodyStyle;

    public QueryExcelExporter() {
        workbook = new HSSFWorkbook();
    }

    private void createSheet(String tabName) {

        sheet = workbook.createSheet(tabName);
        font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setFontName("맑은 고딕");

        headerStyle = getHeaderStyle(workbook);
        bodyStyle = getBodyStyle(workbook);

        headerStyle.setFont(font);
        bodyStyle.setFont(font);
    }

    public void save(List<ResultData> resultDataList) throws IOException {
        for (ResultData resultData: resultDataList) {
            if (resultData.getTabName() == null)
                createSheet(this.filename.replaceAll(".sql", ""));
            else
                createSheet(resultData.getTabName());

            RecordSet rset = resultData.getRset();

            createHeaderCell(rset);
            createDataCell(rset);

            saveAsFile();
        }
    }

    @Override
    public String getOutputFilename() {
        String filename = this.filename.replaceAll(".sql", "");
        String outputFile = outputPath + "/" + filename + ".xls";

        return outputFile;
    }

    private void saveAsFile() {
        try
        {
            //FileUtils.forceMkdir(new File(getOutputFilename()));
            FileOutputStream fos = new FileOutputStream(getOutputFilename());
            workbook.write(fos);
            fos.close();

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

    private void createHeaderCell(RecordSet rset)
    {
        row = sheet.createRow((short) 0);
        row.setHeight((short) 1024);

        for (int i=0; i<rset.getColumnCount(); i++) {
            int columnIndex = i+1;
            cell = row.createCell(i);
            cell.setCellValue(rset.getColumnName(columnIndex));
            cell.setCellStyle(headerStyle);
        }
    }

    private void createDataCell(RecordSet rset)
    {
        try {
            int columnCount = rset.getColumnCount();
            int rowCount = 1;
            while (rset.next()) {
                row = sheet.createRow((short) rowCount);

                for (int i = 0; i < columnCount; i++) {
                    cell = row.createCell(i);
                    cell.setCellValue(rset.getString(i + 1));
                    cell.setCellStyle(bodyStyle);
                }

                rowCount++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
