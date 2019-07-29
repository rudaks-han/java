package kr.pe.rudaks.app;

import kr.pe.rudaks.app.exporter.QueryExporter;
import kr.pe.rudaks.app.util.DateUtil;
import kr.pe.rudaks.app.util.RecordSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class Main 
{
	private static final long CHUNKED_UPLOAD_CHUNK_SIZE = 8L << 20; // 8MiB
    private static final int CHUNKED_UPLOAD_MAX_ATTEMPTS = 5;
    
	private String driver;
	private String url;
	private String user;
	private String password;
	private String startdate;
	private String enddate;
	private String exportType;
	private String sqlFilesEncoding;
	private String sqlFilesDir;
	private String outputEncoding;
	private String outputDir;
	private String outputDaily;

	private String replaceString;
	
	public static void main(String[] args) throws Exception 
	{
		Main main = new Main();
		
    	print("[properties] db.properties");
    	main.loadProperty("db.properties");
    	println("==> OK");
		
		main.execute();
	}

	private void execute() throws IOException
	{		
		HashMap<String, String> fileListMap = getFileList();
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		try
		{
			print("[jdbc driver]" + driver);
			Class.forName(driver);
			println("==> OK");
			print("[Connection url]" + url);
			print(", user : " + user);
			print(", pass : " + password);
			
			conn = DriverManager.getConnection(url, user, password);
			println("==> OK");
            
			HashMap replaceStringMap = getQueryMap(replaceString);
			
			String currDate = DateUtil.getCurrDate("yyyyMMdd");
			String yesterday = DateUtil.calDays(currDate, -1);
			
            if ("true".equals(outputDaily))
            {
            	outputDir = outputDir + "/" + yesterday;
            	FileUtils.forceMkdir(new File(outputDir));
            }
            
			for (String key : fileListMap.keySet())
			{
	            String sql = fileListMap.get(key);
	            //System.err.println(sql);
	            
	            println("[execute sql] " + key);
	            
	            stmt = conn.createStatement();
	            try
	            {
					sql = mapToSqlVariable(replaceStringMap, sql);

					String [] sqls = sql.split(";"); // ; 로 구분되어 있는 sql은 탭으로 구성한다.

					List<ResultData> list = new ArrayList<ResultData>();

					ResultData resultData = null;

					for (int i=0; i<sqls.length; i++) {

						rs = stmt.executeQuery(sqls[i]);
						RecordSet rset = new RecordSet(rs);
						rs.close();

						list.add(new ResultData("TAB " + (i+1), rset));
					}

					QueryExporter queryExporter = ExporterFactory.getInstance(exportType);
					queryExporter.setEncoding(outputEncoding);
					queryExporter.setFilename(key);
					queryExporter.setOutputPath(outputDir);
					queryExporter.save(list);

				}
	            catch (Exception e)
	            {
	            	println("[ERROR] sql");
	            	println(sql);
	            	e.printStackTrace();
	            }
	            stmt.close();
	        }
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
    	{
    		if (stmt != null) { try { stmt.close(); } catch (Exception e) {} }
    		if (conn != null) { try { conn.close(); } catch (Exception e) {} }
    	}
	}

	private String mapToSqlVariable(HashMap replaceStringMap, String sql) {
		if (replaceStringMap != null && replaceStringMap.size() > 0)
		{
			Set<String> keys = replaceStringMap.keySet();
			for (String name : keys)
			{
				String value = (String) replaceStringMap.get(name);
				sql = StringUtils.replace(sql, name, value);
			}
		}

		return sql;
	}

	private HashMap<String, String> getFileList() throws IOException
	{
		HashMap<String, String> hm = new HashMap<String, String>();

		if (sqlFilesDir.startsWith("."))
			sqlFilesDir = System.getProperty("user.dir") + "/" + sqlFilesDir;

		//sqlFilesDir = "D:\\_GIT\\java\\SqlToCsv\\sql";
		File fileList = new File(sqlFilesDir);
		
		File [] selectedFiles = fileList.listFiles(new FileFilter() {
			
			//@Override
			public boolean accept(File pathname) {
				// TODO Auto-generated method stub
				if (pathname.getName().endsWith("sql"))
					return true;
				else
					return false;
			}
		});
		
		if (selectedFiles != null)
		{
			for (File selectedFile : selectedFiles) 
			{
			    String sql = FileUtils.readFileToString(selectedFile, sqlFilesEncoding);
			    
			    //System.out.println("sql : " + sql);
			    hm.put(selectedFile.getName(), sql);
			}
		}
		else
		{
			System.err.println("no selected file in " + sqlFilesDir);
		}
		
		return hm;
	}
	

	

	
	private void loadProperty(String filePath)
	{
		try
		{
			Properties prop = new Properties();
			prop.load(new FileInputStream(filePath));
			
			driver = prop.getProperty("driver");
			url = prop.getProperty("url");
			user = prop.getProperty("user");
			password = prop.getProperty("password");			
			startdate = prop.getProperty("startdate");
			enddate = prop.getProperty("enddate");
			exportType = prop.getProperty("export.type");
			sqlFilesEncoding = prop.getProperty("sql.files.encoding");
			sqlFilesDir = prop.getProperty("sql.files.dir");
			outputEncoding = prop.getProperty("output.file.encoding");
			outputDir = prop.getProperty("output.dir");
			outputDaily = prop.getProperty("output.daily");
			replaceString = prop.getProperty("replace.string");
			
			prop = null;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static HashMap<String, String> getQueryMap(String query)
	{
	    String[] params = query.split("&");
	    HashMap<String, String> map = new HashMap<String, String>();
	    for (String param : params)
	    {
	        String name = param.split("=")[0];
	        String value = param.split("=")[1];
	        map.put(name, value);
	    }
	    return map;
	}
	
	private static void println(String str)
	{
		System.out.println(str);
	}
	
	private static void print(String str)
	{
		System.out.print(str);
	}

}
