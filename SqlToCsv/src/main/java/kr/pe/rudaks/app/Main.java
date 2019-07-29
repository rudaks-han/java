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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	            print("\n[read sql] " + key);
	            
	            stmt = conn.createStatement();
	            try
	            {
					sql = mapToSqlVariable(replaceStringMap, sql);

					String [] sqls = sql.split(";"); // ; 로 구분되어 있는 sql은 탭으로 구성한다.

					List<ResultData> list = new ArrayList<ResultData>();

					Boolean [] validSqlFlags = validateSqlFlag(sqls);

					int validSqlCount = validSqlCount(validSqlFlags);
					println(" ==> " + validSqlCount + " sql(s) found");

					for (int i=0; i<sqls.length; i++) {

						if (!validSqlFlags[i]) // SQL이 유효하지 않다면
							continue;

						String tabName = "";
						if (validSqlCount == 1) {
							tabName = null;
						} else {
							tabName = getTabName(sqls[i]);;
							if ("".equals(tabName)) {
								tabName = "TAB " + (i + 1);
							}
						}

						print("[execute sql] ");
						rs = stmt.executeQuery(sqls[i]);
                        println(" ==> done. ");
						RecordSet rset = new RecordSet(rs);
						rs.close();

						list.add(new ResultData(tabName, rset));
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

	private Boolean[] validateSqlFlag(String [] sqls) {
		Boolean [] flag = new Boolean[sqls.length];
		for (int i=0; i<sqls.length; i++) {
			String sql = sqls[i];
			sql = sql.replace(" ", "");
			sql = sql.replace("\t", "");
			sql = sql.replace("\n", "");

			if (sql.length() > 0)
				flag[i] = true;
			else
				flag[i] = false;
		}

		return flag;
	}

	private int validSqlCount(Boolean [] sqls) {
		int count = 0;
		for (int i=0; i<sqls.length; i++) {
			if (sqls[i])
				count++;
		}

		return count;
	}

	private String getTabName(String sql) {
		String tabName = "";

		String regexp = "-- \\[(.*)\\].*";

		Pattern infoPattern = Pattern.compile(regexp);
		Matcher infoMatcher = infoPattern.matcher(sql);
		while (infoMatcher.find()){
			tabName = infoMatcher.group(1);
		}

		return tabName;
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
		HashMap<String, String> map = new HashMap<String, String>();

		if (query != null) {
			String[] params = query.split("&");

			for (String param : params) {
				String name = param.split("=")[0];
				String value = param.split("=")[1];
				map.put(name, value);
			}
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
