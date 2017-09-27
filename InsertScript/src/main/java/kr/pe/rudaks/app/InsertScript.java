package kr.pe.rudaks.app;

import java.sql.*;
import java.util.*;
import java.io.*;

public class InsertScript
{
	private static Connection conn;

	public static String LINE = "\r\n";

	private String url;
	private String driver;
	private String user;
	private String pass;
	private String filename;
	private String tableName;

	private String outputFilename = "";

	public void setUrl(String url)
	{
		this.url = url;
	}
	public void setDriver(String driver)
	{
		this.driver = driver;
	}
	public void setUser(String user)
	{
		this.user = user;
	}
	public void setPass(String pass)
	{
		this.pass = pass;
	}
	public void setFilename(String filename)
	{
		this.filename = filename;
	}
	public void setTableName(String tableName)
	{
		this.tableName = tableName;
	}

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

	private Connection getConnection() throws Exception
	{
		if (conn == null)
		{
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
				if (i==1)
					sbInsertSql.append(rset.getColumnName(i));
				else
					sbInsertSql.append("," + rset.getColumnName(i));
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
					sbInsertSql2.append(strResult);
				}

				sbSql.append(sbInsertSql.toString() + sbInsertSql2.toString() + ");");
				sbSql.append(LINE + LINE);
				sbInsertSql2 = null;
			}

			if (fileName == null || fileName.length() == 0)
				fileName = "script.sql";
			FileOutputStream fos = new FileOutputStream("./" + fileName);
			fos.write(sbSql.toString().getBytes());
			fos.close();

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

			println("==> OK");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		String url = properties.getProperty("url");
		String driver = properties.getProperty("driver");
		String user = properties.getProperty("user");
		String pass = properties.getProperty("pass");
		String query = properties.getProperty("query");
		String filename = properties.getProperty("filename");
		String tableName = properties.getProperty("tablename");

		List<String> queryList = new ArrayList<String>();
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

		List<String> tableNameList = new ArrayList<String>();
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

		InsertScript t = new InsertScript();
		t.setUrl(url);
		t.setDriver(driver);
		t.setUser(user);
		t.setPass(pass);
		t.setTableName(tableName);
		t.setFilename(filename);
		boolean bSuccess = false;

		try
		{
			t.getConnection();

			if (query != null && query.length() > 0)
			{
				bSuccess = t.createScript(query, tableName, filename);
			}

			if (queryList != null && queryList.size() > 0)
			{
				for (int i = 0; i < queryList.size(); i++)
				{
					if (queryList.get(i) != null && queryList.get(i).length() > 0)
					{
						filename = tableNameList.get(i) + ".sql";
						bSuccess = t.createScript(queryList.get(i), tableNameList.get(i), filename);
					}
				}

			}

			t.releaseConnection();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

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
