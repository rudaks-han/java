package kr.pe.rudaks.app;

import org.apache.commons.io.FileUtils;

import java.sql.*;
import java.util.*;
import java.io.*;

public class InsertScript
{
	private static Connection conn;

	public static String LINE = "\r\n";

	private String dbType;
	private String url;
	private String driver;
	private String user;
	private String pass;
	private String filename;
	private String tableName;
	private String query;
	List<String> queryList;
	List<String> tableNameList;
	private String dateConvertNow;
	private List<String> dateColumnList = new ArrayList<String>();

	private String outputFilename = "";

	static {
		if( File.separator.equals("/") )
		{
			LINE = "\n";
		}
		else
		{
			LINE = "\r\n";
		}
	}

	private void readProperty()
	{
		Properties properties = new Properties();
		try
		{
			print("[properties] db.properties");

			FileInputStream fi = null;
			try
			{
				fi = new FileInputStream("db.properties");
				properties.load(fi);
			}
			catch (FileNotFoundException e)
			{
				InputStream stream = InsertScript.class.getClassLoader().getResourceAsStream("db.properties");
				properties.load(stream);
			}
			finally
			{
				if (fi != null)
					fi.close();
			}

			println("==> OK");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		url = properties.getProperty("url");
		driver = properties.getProperty("driver");
		user = properties.getProperty("user");
		pass = properties.getProperty("pass");
		query = properties.getProperty("query");
		filename = properties.getProperty("filename");
		tableName = properties.getProperty("tablename");

		queryList = new ArrayList<String>();
		for (int i = 0; i < 100; i++)
		{
			String temp = properties.getProperty("query." + i);
			if (temp != null && temp.length() > 0)
			{
				queryList.add(temp);
			}
			else
			{
				break;
			}
		}

		tableNameList = new ArrayList<String>();
		for (int i = 0; i < 100; i++)
		{
			String temp = properties.getProperty("tablename." + i);
			if (temp != null && temp.length() > 0)
			{
				tableNameList.add(temp);
			}
			else
			{
				break;
			}
		}

		dateConvertNow = properties.getProperty("date.convert.now");
		String dateColumn = properties.getProperty("date.column");
		if (dateColumn != null && dateColumn.length() > 0)
		{
			String [] arDateColumn = dateColumn.split(",");
			if (arDateColumn != null && arDateColumn.length > 0)
			{
				for (String temp : arDateColumn)
				{
					dateColumnList.add(temp.toUpperCase().trim());
				}
			}
		}

	}

	private boolean executeScript()
	{
		boolean bSuccess = false;
		try
		{
			getConnection();

			if (query != null && query.length() > 0)
			{
				bSuccess = createScript(query, tableName, filename);
			}

			if (queryList != null && queryList.size() > 0)
			{
				for (int i = 0; i < queryList.size(); i++)
				{
					if (queryList.get(i) != null && queryList.get(i).length() > 0)
					{
						filename = tableNameList.get(i) + ".sql";
						bSuccess = createScript(queryList.get(i), tableNameList.get(i), filename);
					}
				}

			}

			releaseConnection();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return bSuccess;
	}

	private Connection getConnection() throws Exception
	{
		if (conn == null)
		{
			if (driver.indexOf("oracle") > -1)
				dbType = "oracle";
			else if (driver.indexOf("postgresql") > -1)
				dbType = "postgresql";
			else if (driver.indexOf("mysql") > -1)
				dbType = "mysql";

			println("[db type]" + dbType);
			print("[jdbc driver]" + driver);
			Class.forName(driver);
			println("==> OK");
			print("[Connection url]" + url);
			print(", user : " + user);
			print(", pass : " + pass);
			conn = DriverManager.getConnection(url, user, pass);
			println("==> OK");
		}
		return conn;
	}

	private void releaseConnection() throws Exception
	{
		if (conn  != null)
		{
			conn.close();
		}
	}

	private boolean createScript(String query, String tableName, String fileName)
	{
		boolean bSuccess = false;

		Statement stmt = null;
		ResultSet rs = null;
		RecordSet rset = null;
		StringBuffer sbSql = new StringBuffer();
		try
		{
			stmt = conn.createStatement();
			if (query != null && query.length() > 0 && !query.toUpperCase().startsWith("SELECT "))
				query = "SELECT * FROM " + query;

			println("\n[execute sql] " + query);

			rs = stmt.executeQuery(query);
			rset = new RecordSet(rs);
			print("[Fetching resultset]");
			println("total record : " + rset.getRowCount());

			StringBuffer sbInsertSql = new StringBuffer();
			StringBuffer sbInsertSql2 = null;
			sbInsertSql.append("INSERT INTO " + tableName + "(");

			for (int i=1; i<=rset.getColumnCount(); i++)
			{
				String columnName = rset.getColumnName(i);
				if (i==1)
					sbInsertSql.append(columnName);
				else
					sbInsertSql.append("," + columnName);
			}
			sbInsertSql.append(") ");
			sbInsertSql.append("VALUES(");
			int iLoop = 0;

			String strResult = null;
			while (rset.next())
			{
				sbInsertSql2 = new StringBuffer();
				for (int i=1; i<=rset.getColumnCount(); i++)
				{
					if (i>1)
						sbInsertSql2.append(", ");

					if ("Y".equals(dateConvertNow) && dateColumnList.size() > 0 && dateColumnList.contains(rset.getColumnName(i)))
					{
						strResult = getDbCurrDate();
					}
					else
					{
						if (rset.getColumnType(i) == Types.INTEGER || rset.getColumnType(i) == Types.NUMERIC)
						{
							strResult = rset.getInt(i) + "";
						}
						else if (rset.getColumnType(i) == Types.TIMESTAMP)
						{
							strResult = "'" + rset.getTimestamp(i) + "'";
						}
						else if (rset.getColumnType(i) == Types.DATE)
						{
							if (driver.indexOf("oracle") > -1)
							{
								strResult = "TO_DATE('" + rset.getString(i).substring(0, 19) + "', 'yy-mm-dd hh24:mi:ss')";
							}
							else
							{
								strResult = "'" + rset.getString(i) + "'";
							}
						}
						else
						{
							if (rset.getString(i) == null)
								strResult = "null";
							else
							{
								strResult = escapeSql(rset.getString(i));
								strResult = "'" + strResult + "'";
							}
						}
					}
					sbInsertSql2.append(strResult);
				}

				sbSql.append(sbInsertSql.toString() + sbInsertSql2.toString() + ");");
				sbSql.append(LINE + LINE);
				sbInsertSql2 = null;
			}

			if (fileName == null || fileName.length() == 0)
				fileName = "script.sql";

			FileUtils.writeStringToFile(new File("./" + fileName), sbSql.toString(), "UTF-8");

			if (outputFilename != null && outputFilename.length() > 0)
				outputFilename += ",";

			outputFilename += fileName;

			bSuccess = true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (stmt != null) {	try { stmt.close(); } catch (Exception e) {} }
		}
		return bSuccess;
	}

	private String getDbCurrDate()
	{
		String result = "";
		if ("oracle".equals(dbType))
		{
			result = "TO_CHAR(sysdate, 'YYYYMMDDHH24MISS')";
		}
		else if ("mysql".equals(dbType))
		{
			result = "date_format(now(), '%Y%m%d%H%i%s')";
		}
		else if ("postgresql".equals(dbType))
		{
			result = "TO_CHAR(now(), 'YYYYMMDDHH24MISS')";
		}

		return result;
	}

	public static void println(String str)
	{
		System.out.println(str);
	}

	public static void print(String str)
	{
		System.out.print(str);
	}

	public static void main(String[] args)
	{
		InsertScript t = new InsertScript();

		t.readProperty();

		t.executeScript();

		boolean bSuccess = false;



		if (bSuccess)
		{
			print("\n[Successfully insert script] " + t.outputFilename);
		}
	}


	public String escapeSql(String str)
	{
		if (str != null)
			return str.replaceAll("'", "''");
		return str;

	}

	public static void execCommand(String filename)
	{
		String command = "";
		if ("\\".equals(java.io.File.separator))
		{
			command = "notepad " + filename;
		}
		else
		{
			command = "vi " + filename;
		}

		try
		{
    		/*Process proc = Runtime.getRuntime().exec(command);
    		proc.waitFor();
    		
    		if (proc.exitValue() != 0)
    		    System.out.println("Error");
    		   
    		proc.destroy ( );
*/
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
