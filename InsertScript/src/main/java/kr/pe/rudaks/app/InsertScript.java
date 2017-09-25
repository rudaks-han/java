package kr.pe.rudaks.app;

import java.sql.*;
import java.util.*;
import java.io.*;

public class InsertScript
{
	public static String LINE = "\r\n";

	private String url;
	private String driver;
	private String user;
	private String pass;
	private String filename;
	private String tableName;

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

	private boolean createScript(String query, String tableName, String fileName)
	{
		boolean bSuccess = false;
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		RecordSet rset = null;
		StringBuffer sbSql = new StringBuffer();
		try
		{
			print("[jdbc driver]" + driver);
			Class.forName(driver);
			println("==> OK");
			print("[Connection url]" + url);
			print(", user : " + user);
			print(", pass : " + pass);
			conn  = DriverManager.getConnection(url,user,pass);
			println("==> OK");

			stmt = conn.createStatement();
			if (query != null && !query.toUpperCase().startsWith("SELECT "))
				query = "SELECT * FROM " + query;

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

			if (filename == null && filename.length() == 0)
				filename = "script.sql";
			FileOutputStream fos = new FileOutputStream("./" + filename);
			fos.write(sbSql.toString().getBytes());
			fos.close();
			bSuccess = true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (stmt != null) {	try { stmt.close(); } catch (Exception e) {} }
			if (conn != null) { try { conn.close(); } catch (Exception e) {} }
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
			InputStream stream = InsertScript.class.getClassLoader().getResourceAsStream("db.properties");
			properties.load(stream);
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

		InsertScript t = new InsertScript();
		t.setUrl(url);
		t.setDriver(driver);
		t.setUser(user);
		t.setPass(pass);
		t.setTableName(tableName);
		t.setFilename(filename);
		boolean bSuccess = t.createScript(query, tableName, filename);
		if (bSuccess)
		{
			print("[Successfully insert script] " + filename);

			execCommand(filename);
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
