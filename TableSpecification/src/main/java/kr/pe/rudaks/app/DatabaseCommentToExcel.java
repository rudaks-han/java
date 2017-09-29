package kr.pe.rudaks.app;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;


public class DatabaseCommentToExcel 
{
    public static HSSFWorkbook workbook = new HSSFWorkbook();
    
    private String url;
    private String port;
    private String db;
    private String user; 
    private String password;
    private String filename;
    private String ownerName;
    
    public static void main(String args[]) 
    {
        DatabaseCommentToExcel obj = new DatabaseCommentToExcel();
         
        if (obj.loadProperty("db.properties"))
        {
            obj.execute();   
        }
    }

    private void execute()
    { 
        Connection conn = null;
        try 
        {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            
            conn = DriverManager.getConnection("jdbc:oracle:thin:@" + url + ":" + port + ":" + db, user, password);
            
            System.err.println("테이블정의서 생성");
            ArrayList columnList = executeQueryTableColumn(conn);
            
            // 테이블 명세서
            createTableColumnDefinition(columnList);
            
            System.err.println("");
            System.err.println("테이블생성양식 생성");
            ArrayList tableList = executeQueryTableDefinition(conn);
            
            // 테이블 생성양식
            createTableDefinition(tableList);
            
            System.err.println("");
            System.err.println("인덱스 생성");
            ArrayList indexList = executeQueryTableIndex(conn);

            // 테이블 인덱스
            createTableIndex(indexList);
            
            //selectTableCommentsSql

            filename = new String(filename.getBytes("8859_1"), "UTF-8");
            FileOutputStream fos = new FileOutputStream(filename);
            workbook.write(fos);
            fos.close();
            
            System.err.println("");
            System.err.println("Filename >> " + filename);
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
        finally
        {
            if (conn != null) { try {conn.close();} catch(Exception e) {}}
        }
    }
    
    public ArrayList executeQueryTableColumn(Connection conn)
    {
        PreparedStatement pstmt = null;
        PreparedStatement pstmt2 = null;
        ResultSet rs = null;
        ResultSet rsColumn = null;
        Map<String, Object> map = null;
        ArrayList<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        
        try
        {
            pstmt = conn.prepareStatement(selectTableInfoSql(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE); // 테이블 목록 가져오는 SQL
            pstmt.setString(1, ownerName);
            
            rs = pstmt.executeQuery();
            
            pstmt2 = conn.prepareStatement(selectColumnInfoSql()); // 테이블에서 컬럼정보 조회하는 SQL
            
            String tableName = null;
            int totalCount = getResultSetSize(rs);
            int loop = 0;
            ProgressBar progressBar = new ProgressBar();
            
            while (rs.next())
            {
                tableName = rs.getString("table_name");
                pstmt2.setString(1,  tableName);
                pstmt2.setString(2,  ownerName);
                pstmt2.setString(3,  tableName);
                rsColumn = pstmt2.executeQuery();
                while (rsColumn.next())
                {
                    String columnName = rsColumn.getString("column_name");
                    String comments = rsColumn.getString("comments");
                    if (comments == null)
                    {
                        comments = "";
                    }
                    String columnId = rsColumn.getString("column_id");
                    String dataType = rsColumn.getString("data_type");
                    String dataLength = rsColumn.getString("data_length");
                    String nullable = rsColumn.getString("nullable");
                    
                    String dataDefault = rsColumn.getString("data_default");
                    if (dataDefault == null)
                    {
                        dataDefault = "";
                    }
                    
                    String constraintType = rsColumn.getString("constraint_type");
                    
                    map = new HashMap<String, Object>();
                    map.put("테이블명", tableName);
                    map.put("컬럼명", columnName);
                    map.put("순서", columnId);
                    map.put("PK 여부", "P".equals(constraintType) ? "PK" : "");
                    map.put("타입", dataType);
                    map.put("길이", dataLength);
                    map.put("소수점", "0");
                    map.put("널리티 구분", "Y".equals(nullable) ? "NULL" : "NOT NULL");
                    map.put("Default", dataDefault);
                    map.put("컬럼정의", comments);
                    list.add(map);
                    
                }
             
                progressBar.update(loop, totalCount);
                loop++;
            }
        
            pstmt.close();
            pstmt2.close();
            
            rs.close();
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
        finally
        {
            if (pstmt != null) { try {pstmt.close();} catch(Exception e) {}}
            if (pstmt2 != null) { try {pstmt2.close();} catch(Exception e) {}}
        }
        return list;
    }
    
    /**
     * 테이블 생성양식
     * @param conn
     * @return
     */
    public ArrayList executeQueryTableDefinition(Connection conn)
    {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Map<String, Object> map = null;
        ArrayList<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        
        try
        {
            pstmt = conn.prepareStatement(selectTableCommentsSql(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE); // 테이블 목록 가져오는 SQL
            pstmt.setString(1, ownerName);
            
            rs = pstmt.executeQuery();
            
            int totalCount = getResultSetSize(rs);
            int loop = 0;
            ProgressBar progressBar = new ProgressBar();
            while (rs.next())
            {
                String tableName = rs.getString("table_name");
                String comments = rs.getString("comments");
                map = new HashMap<String, Object>();
                map.put("테이블명", tableName);
                map.put("최초이행 로우수", "");
                map.put("월중증가 로우수", "");
                map.put("테이블한글명", comments);
                map.put("테이블정의", "");
                list.add(map);
                
                progressBar.update(loop, totalCount);
                loop++;
            }
        
            pstmt.close();
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
        finally
        {
            if (pstmt != null) { try {pstmt.close();} catch(Exception e) {}}
        }
        return list;
    }
    
    /**
     * 인덱스
     * @param conn
     * @return
     */
    public ArrayList executeQueryTableIndex(Connection conn)
    {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Map<String, Object> map = null;
        ArrayList<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        
        try
        {
            pstmt = conn.prepareStatement(selectTableIndexSql(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE); // 테이블 목록 가져오는 SQL
            pstmt.setString(1, ownerName);
            
            rs = pstmt.executeQuery();
            
            int totalCount = getResultSetSize(rs);
            int loop = 0;
            ProgressBar progressBar = new ProgressBar();
            while (rs.next())
            {
                String tableName = rs.getString("table_name");
                String indexName = rs.getString("index_name");
                String uniqueness = rs.getString("uniqueness");
                String uniqueFlag = rs.getString("unique_flag");
                String columnName = rs.getString("column_name");
                String columnPosition = rs.getString("column_position");
                String columnLength = rs.getString("column_length");
                String descend = rs.getString("descend");
                
                map = new HashMap<String, Object>();
                map.put("신청구분", "");
                map.put("스키마명", "");
                map.put("테이블명", tableName);
                map.put("인덱스명", indexName);
                map.put("인덱스종류", uniqueness);
                map.put("유니크여부", uniqueFlag);
                map.put("컬럼명", columnName);
                map.put("인덱스컬럼순서", columnPosition);
                map.put("정렬구분", descend);
                map.put("컬럼길이", columnLength);
                list.add(map);
                
                progressBar.update(loop, totalCount);
                loop++;
            }
        
            pstmt.close();
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
        finally
        {
            if (pstmt != null) { try {pstmt.close();} catch(Exception e) {}}
        }
        return list;
    }
    
    /**
     * 테이블 명세서
     */
    public void createTableColumnDefinition(ArrayList<Map<String, Object>> list)
    {
        ArrayList<String> columnList = new ArrayList<String>();
        
        if (list != null && list.size() > 0)
        {
            columnList.add("테이블명");
            columnList.add("컬럼명");
            columnList.add("순서");
            columnList.add("PK 여부");
            columnList.add("타입");
            columnList.add("길이");
            columnList.add("소수점");
            columnList.add("널리티 구분");
            columnList.add("Default");
            columnList.add("컬럼정의");
        }
        
        
        HSSFSheet sheet = workbook.createSheet("테이블명세서");
        HSSFRow row = null; // 행
        HSSFCell cell = null; // 셀
        
        HSSFFont font = workbook.createFont();
        font.setFontHeightInPoints((short) 11);
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
        headerStyle.setFillForegroundColor(HSSFColor.LIME.index);
        headerStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        headerStyle.setFont(font);
        
        if (list != null && list.size() > 0)
        {
            int i=0;
            // header
            row = sheet.createRow((short) i);
            row.setHeight((short) 1024);
            if (columnList != null && columnList.size() > 0)
            {
                for (int j=0; j<columnList.size(); j++)
                {
                    cell = row.createCell(j);
                    cell.setCellValue(columnList.get(j));
                    cell.setCellStyle(headerStyle);
                }
            }
            
            i++;
            for (Map<String, Object> mapObject : list)
            {
                row = sheet.createRow((short) i);
                row.setHeight((short) 256);
                i++;
                if (columnList != null && columnList.size() > 0)
                {
                    for (int j=0; j<columnList.size(); j++)
                    {
                        cell = row.createCell(j);
                        String columnName = columnList.get(j);
                        cell.setCellValue(String.valueOf(mapObject.get(columnName)));
                        
                        if ("순서".equals(columnName))
                        {
                            cell.setCellStyle(alignCenterStyle);
                            cell.setCellValue(Integer.parseInt(String.valueOf(mapObject.get(columnName))));
                        } 
                        else if ("PK 여부".equals(columnName))
                        {
                            cell.setCellStyle(alignCenterStyle);
                        }
                        else if ("길이".equals(columnName))
                        {
                            cell.setCellStyle(alignRightStyle);
                            cell.setCellValue(Integer.parseInt(String.valueOf(mapObject.get(columnName))));
                        }
                        else if ("소수점".equals(columnName))
                        {
                            cell.setCellStyle(alignRightStyle);
                            cell.setCellValue(Integer.parseInt(String.valueOf(mapObject.get(columnName))));
                        }
                        else
                        {
                            cell.setCellStyle(alignLeftStyle);
                        }
                        
                    }
                }
            }
            
            for (int k=0; k<columnList.size(); k++)
            {
                sheet.autoSizeColumn((short)k);
                sheet.setColumnWidth(k, (sheet.getColumnWidth(k))+512);
            }
        }
    }
    
    /**
     * 테이블 생성양식
     */
    public void createTableDefinition(ArrayList<Map<String, Object>> list)
    {
        ArrayList<String> columnList = new ArrayList<String>();
        
        if (list != null && list.size() > 0)
        {
            columnList.add("테이블명");
            columnList.add("최초이행 로우수");
            columnList.add("월중증가 로우수");
            columnList.add("테이블한글명");
            columnList.add("테이블정의");
        }
        
        
        HSSFSheet sheet = workbook.createSheet("테이블생성양식");
        HSSFRow row = null; // 행
        HSSFCell cell = null; // 셀
        
        HSSFFont font = workbook.createFont();
        font.setFontHeightInPoints((short) 11);
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
        headerStyle.setFillForegroundColor(HSSFColor.LIME.index);
        headerStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        headerStyle.setFont(font);
        
        if (list != null && list.size() > 0)
        {
            int i=0;
            // header
            row = sheet.createRow((short) i);
            row.setHeight((short) 1024);
            if (columnList != null && columnList.size() > 0)
            {
                for (int j=0; j<columnList.size(); j++)
                {
                    cell = row.createCell(j);
                    cell.setCellValue(columnList.get(j));
                    cell.setCellStyle(headerStyle);
                }
            }
            
            i++;
            for (Map<String, Object> mapObject : list)
            {
                row = sheet.createRow((short) i);
                row.setHeight((short) 256);
                i++;
                if (columnList != null && columnList.size() > 0)
                {
                    for (int j=0; j<columnList.size(); j++)
                    {
                        cell = row.createCell(j);
                        String columnName = columnList.get(j);
                        cell.setCellValue(String.valueOf(mapObject.get(columnName)));
                        cell.setCellStyle(alignLeftStyle);
                    }
                }
            }
            
            for (int k=0; k<columnList.size(); k++)
            {
                sheet.autoSizeColumn((short)k);
                sheet.setColumnWidth(k, (sheet.getColumnWidth(k))+512);
            }
        }
    }
    
    /**
     * 인덱스
     */
    public void createTableIndex(ArrayList<Map<String, Object>> list)
    {
        ArrayList<String> columnList = new ArrayList<String>();
        
        if (list != null && list.size() > 0)
        {
            columnList.add("신청구분");
            columnList.add("스키마명");
            columnList.add("테이블명");
            columnList.add("인덱스명");
            columnList.add("인덱스종류");
            columnList.add("유니크여부");
            columnList.add("컬럼명");
            columnList.add("인덱스컬럼순서");
            columnList.add("정렬구분");
            columnList.add("컬럼길이");
        }
        
        
        HSSFSheet sheet = workbook.createSheet("인덱스");
        HSSFRow row = null; // 행
        HSSFCell cell = null; // 셀
        
        HSSFFont font = workbook.createFont();
        font.setFontHeightInPoints((short) 11);
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
        headerStyle.setFillForegroundColor(HSSFColor.LIME.index);
        headerStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        headerStyle.setFont(font);
        
        if (list != null && list.size() > 0)
        {
            int i=0;
            // header
            row = sheet.createRow((short) i);
            row.setHeight((short) 1024);
            if (columnList != null && columnList.size() > 0)
            {
                for (int j=0; j<columnList.size(); j++)
                {
                    cell = row.createCell(j);
                    cell.setCellValue(columnList.get(j));
                    cell.setCellStyle(headerStyle);
                }
            }
            
            i++;
            for (Map<String, Object> mapObject : list)
            {
                row = sheet.createRow((short) i);
                row.setHeight((short) 256);
                i++;
                if (columnList != null && columnList.size() > 0)
                {
                    for (int j=0; j<columnList.size(); j++)
                    {
                        cell = row.createCell(j);
                        String columnName = columnList.get(j);
                        cell.setCellValue(String.valueOf(mapObject.get(columnName)));
                        
                        
                        if ("인덱스컬럼순서".equals(columnName) || "컬럼길이".equals(columnName))
                        {
                            cell.setCellStyle(alignRightStyle);
                            cell.setCellValue(Integer.parseInt(String.valueOf(mapObject.get(columnName))));
                        } 
                    }
                }
            }
            
            for (int k=0; k<columnList.size(); k++)
            {
                sheet.autoSizeColumn((short)k);
                sheet.setColumnWidth(k, (sheet.getColumnWidth(k))+512);
            }
        }
    }
    
    /**
     * 테이블 정보 조회하는 SQL
     * @return
     */
    public String selectTableInfoSql()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("SELECT  table_name ");
        sb.append("FROM all_tables ");
        sb.append("WHERE owner = ? ");
        sb.append("ORDER BY table_name ");
        return sb.toString();
    }
    
    /**
     * 테이블에서 컬럼정보 조회하는 SQL
     * @return
     */
    public String selectColumnInfoSql()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("SELECT A.TABLE_NAME, A.COLUMN_ID, A.COLUMN_NAME, B.COMMENTS, A.DATA_TYPE, A.DATA_LENGTH, A.NULLABLE, A.DATA_DEFAULT, ");
        sb.append(" (SELECT 'P' ");
        sb.append("FROM USER_CONS_COLUMNS ");
        sb.append("WHERE TABLE_NAME = ? ");
        sb.append("AND COLUMN_NAME = A.column_name ");
        sb.append("AND CONSTRAINT_NAME LIKE 'PK%') AS constraint_type ");
        sb.append("FROM   ALL_TAB_COLUMNS A, ALL_COL_COMMENTS B ");
        sb.append("WHERE  A.TABLE_NAME = B.TABLE_NAME ");
        sb.append("AND  A.COLUMN_NAME = B.COLUMN_NAME ");
        sb.append("AND A.OWNER = ? ");
        sb.append("AND A.TABLE_NAME = ? ");
        //sb.append("AND ROWNUM = 1 ");
        sb.append("ORDER BY A.TABLE_NAME, A.COLUMN_ID ");
        return sb.toString();
    }
    
    /**
     * 테이블 COMMENT를 가져오는 SQL
     * @return
     */
    public String selectTableCommentsSql()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("SELECT A.TABLE_NAME, B.COMMENTS ");
        sb.append("FROM   ALL_TABLES A, ALL_TAB_COMMENTS B ");
        sb.append("WHERE  A.TABLE_NAME = B.TABLE_NAME ");
        sb.append("AND  A.OWNER = ? ");
        //sb.append("AND ROWNUM = 1 ");
        sb.append("ORDER BY A.TABLE_NAME");
        return sb.toString();
    }
    
    /**
     * 테이블 Index를 가져오는 SQL
     * @return
     */
    public String selectTableIndexSql()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("SELECT B.table_name, B.index_name, A.uniqueness, DECODE(A.uniqueness, 'UNIQUE', 'Y', 'N') AS unique_flag, B.column_name, B.column_position, B.column_length, B.descend ");
        sb.append("FROM ALL_INDEXES a, ");
        sb.append("ALL_IND_COLUMNS b ");
        sb.append("WHERE a.index_name = b.index_name ");
        sb.append("AND A.table_owner = ? ");
        //sb.append("AND ROWNUM = 1 ");
        sb.append("ORDER BY B.table_name, B.column_position ");
        
        return sb.toString();
    }
   
    public boolean loadProperty(String filePath)
    {
        try
        {
            Properties prop = new Properties();
            FileInputStream fi = null;
            try
            {
                fi = new FileInputStream(filePath);
                prop.load(fi);
            }
            catch (FileNotFoundException e)
            {
                InputStream stream = DatabaseCommentToExcel.class.getClassLoader().getResourceAsStream(filePath);
                prop.load(stream);
            }
            finally
            {
                if (fi != null)
                    fi.close();
            }

            url = prop.getProperty("url");
            port = prop.getProperty("port");
            db = prop.getProperty("db");
            user = prop.getProperty("user");
            if (user != null)
            {
                ownerName = user.toUpperCase();
            }
            password = prop.getProperty("password");
            filename = prop.getProperty("filename");
            //filepath = new String(filepath.getBytes("8859_1"), "EUC-KR");
            
            if (url == null || url.length() == 0)
            {
                System.out.println("url 정보가 없습니다.");
                return false;
            }
            
            if (port == null || port.length() == 0)
            {
                System.out.println("port 정보가 없습니다.");
                return false;
            }
            
            if (db == null || db.length() == 0)
            {
                System.out.println("db 정보가 없습니다.");
                return false;
            }
            
            if (user == null || user.length() == 0)
            {
                System.out.println("user 정보가 없습니다.");
                return false;
            }
            
            if (password == null || password.length() == 0)
            {
                System.out.println("password 정보가 없습니다.");
                return false;
            }
            
            if (filename == null || filename.length() == 0)
            {
                System.out.println("filename 정보가 없습니다.");
                return false;
            }
            prop = null;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return true;
    }
    
    public int getResultSetSize(ResultSet resultSet) 
    {
        int size = -1;

        try {
            resultSet.last(); 
            size = resultSet.getRow();
            resultSet.beforeFirst();
        } catch(SQLException e) {
            e.printStackTrace();
            return size;
        }

        return size;
    }
}
