package kr.pe.rudaks.app;

import java.util.Vector;
import java.io.Reader;
import java.io.Serializable;
import java.sql.*;

public class RecordSet implements Serializable
{
	/**
	 * 데이터베이스로부터 가져온 컬럼의 이름을 담는 변수
	 */
	private Vector cols;
	/**
	 * 데이터베이스로부터 가져온 컬럼의 따른 값을 담는 변수
	 */
	private Vector vals;
	/**
	 * 	데이터베이스로부터 가져온 컬럼 Type : 예) java.sql.CHAR
	 */
	private Vector types;
	/**
	 * 컬럼의 수
	 */
	private int cols_count = 0;
	/**
	 * 실행된 SQL문을 통해 실제 가져온 Total Row Count
	 */
	private int realRowCount = -1;
	/**
	 * 실행된 SQL문을 통해 RecordSet에 담은 Total Row Count
	 */
	private int totalRowCount = -1;
	/**
	 * java.sql.ResultSet.next() 메소드 처럼 next()메소드를 구현하기 위한<br>
	 * 플래그로서 레코드 처음인가를 나타낸다.
	 */
	private boolean first = true;
	/**
	 * java.sql.ResultSet.next() 메소드 처럼 next()메소드를 구현하기 위한<br>
	 * 인덱스로서 각 레코드에 대한 인텍스 위치를 나타낸다.
	 */
	private int searchIndex = 0;
	/**
	 * RecordSet 생성자 - 주어진 ResultSet의 값을 이용해 RecordSet 초기화한다.
	 * @param rset ResultSet
	 */
	public RecordSet(ResultSet rset) throws Exception
	{
		bulidSet(rset);
	}
	/**
	 * RecordSet 생성자 - 주어진 ResultSet의 값을 이용해 RecordSet 초기화한다.<br>
	 * 웹에서 Page Navigation을 위해 주어진 시작페이지와 한 페이지에 라인 수를 <br>
	 * 매개변수로 하여 해당 페이지와 라인 수 만을 RecordSet에 담아 Build 한다.<br>
	 * @param rset ResultSet
	 * @param startPage 시작페이지
	 * @param line 라인수
	 */
	public RecordSet(ResultSet rset, int startPage, int line) throws Exception
	{
		bulidSet(rset, startPage, line);
	}
	/**
	 * java.sql.ResultSet을 이용해 RecordSet 클래스를 Build한다.
	 * @param rset ResultSet
	 */
	private void bulidSet(ResultSet rset) throws Exception
	{
		if(rset == null)
			throw new Exception("RecordSet.bulidsSet - ResultSet is null !!");

		try
		{
			cols = new Vector();
			vals = new Vector();
			types = new Vector();
			int count = 0;

			ResultSetMetaData rsmd = rset.getMetaData();

			count = rsmd.getColumnCount();

			for(int i = 1; i <= count; i++)
			{
				cols.addElement(rsmd.getColumnName(i));
				types.addElement(rsmd.getColumnType(i)+"");
			}

			while(rset.next())
			{
				for(int i = 1; i <= count; i++)
				{

					Object obj = rset.getObject(i);

					if(obj instanceof java.math.BigDecimal)
					{
						obj = new Double( ((java.math.BigDecimal)obj).doubleValue() );
					}
					else if(obj instanceof java.sql.Clob)
					{
						obj = getStringByClob(obj);
					}
					vals.addElement(obj);
				}
			}

			cols_count = cols.size();
			if(cols_count != 0)
			{
				totalRowCount = vals.size() / cols_count;
				realRowCount = totalRowCount;
			}

			//logger.debug("RecordSet.buildSet - Column Count : " + cols_count + ", Total Row Count : "  +totalRowCount);
		}
		catch(SQLException e)
		{
			throw new Exception("RecordSet.bulidsSet - RecordSet Build Error !! ", e);
		}
		finally
		{
			try
			{
				rset.close();
			}
			catch(SQLException e) {}
		}
	}
	/**
	 * java.sql.ResultSet을 이용해 RecordSet 클래스를 Build한다.<br>
	 * 그리고 웹에서 Page Navigation을 위해 주어진 시작페이지와 한 페이지에 라인 수를 <br>
	 * 매개변수로 하여 해당 페이지와 라인 수 만을 RecordSet에 담아 Build 한다.<br>
	 * @param rset ResultSet
	 * @param startPage 시작페이지
	 * @param line 라인수
	 */
	private void bulidSet(ResultSet rset, int startPage, int line) throws Exception
	{
		if(rset == null)
			throw new Exception("RecordSet.bulidsSet - ResultSet is null !!");

		int startNo = (startPage - 1) * line;

		try
		{
			cols = new Vector();
			vals = new Vector();
			types = new Vector();
			int count = 0;

			ResultSetMetaData rsmd = rset.getMetaData();
			count = rsmd.getColumnCount();

			for(int i = 1; i <= count; i++)
			{
				cols.addElement(rsmd.getColumnName(i));
				types.addElement(rsmd.getColumnType(i)+"");
			}

			int rowCount = 0;
			while(rset.next())
			{
				rowCount++;
				if(rowCount > startNo && rowCount <= startNo+line)
				{
					for(int i = 1; i <= count; i++)
					{
						Object obj = rset.getObject(i);
						if(obj instanceof java.math.BigDecimal)
						{
							obj = new Double( ((java.math.BigDecimal)obj).doubleValue() );
						}
						vals.addElement(obj);
					}
				}
			}

			this.realRowCount = rowCount;
			cols_count = cols.size();
			if(cols_count != 0)
			{
				totalRowCount = vals.size() / cols_count;
			}

			//logger.debug("RecordSet.buildSet - Column Count : " + cols_count + ", Total Row Count : "  +totalRowCount + ", Real Row Count : "  + realRowCount  );
		}
		catch(SQLException e)
		{
			throw new Exception("RecordSet.bulidsSet - RecordSet Build Error !! ", e);
		}
		finally
		{
			try
			{
				rset.close();
			}
			catch(SQLException e) {}
		}
	}
	/**
	 * 주어진 컬럼 인덱스에 해당하는 Object(클래스) 객체를 리턴한다.<br>
	 * @param index 컬럼 인덱스
	 * @return 데이터베이스로 부터 얻어낸 가공되지 않은 원형의 Object
	 * @exception SearchException
	 */
	public Object getObject(int index) throws Exception
	{
		Object obj = null;
		if( vals.size() >= 0 && index <= cols_count)
		{
			obj = vals.elementAt((searchIndex -1) * cols_count + index -1);
		}
		else
		{
			throw new Exception("RecordSet.getObject - Couldn't be retrieved from given index. Check inputed index number " + index + ".");
		}
		return obj;
	}
	/**
	 * 주어진 컬럼 인덱스에 해당하는 Object(클래스) 객체를 리턴한다.<br>
	 * 만약 해당 객체가 Null이면 주어진 default Value Object를 리턴한다.<br>
	 * @param index 컬럼 인덱스
	 * @param defaultObj Default Object
	 * @return 데이터베이스로 부터 얻어낸 가공되지 않은 원형의 Object
	 * @exception Exception
	 */
	public Object getObject(int index, Object defaultObj) throws Exception
	{
		Object obj = getObject(index);
		return obj==null? defaultObj:obj;
	}
	/**
	 * 주어진 컬럼 이름에 해당하는 Object(클래스) 객체를 리턴한다.<br>
	 * @param columnName 컬럼 이름
	 * @return 데이터베이스로 부터 얻어낸 가공되지 않은 원형의 Object
	 * @exception Exception
	 */
	public Object getObject(String columnName) throws Exception
	{
		return getObject(getColumnIndex(columnName));
	}
	/**
	 * 주어진 컬럼 인덱스에 해당하는 Object(클래스) 객체를 리턴한다.<br>
	 * 만약 해당 객체가 Null이면 주어진 default Value Object를 리턴한다.<br>
	 * @param columnName 컬럼 이름
	 * @param defaultObj Default Object
	 * @return 데이터베이스로 부터 얻어낸 가공되지 않은 원형의 Object
	 * @exception Exception
	 */
	public Object getObject(String columnName, Object defaultObj) throws Exception
	{
		return getObject(getColumnIndex(columnName), defaultObj);
	}
	/**
	 * 주어진 인덱스에 해당하는 Object에 대해 String 값으로 변환하여 리턴한다.
	 * @param index 컬럼 인덱스
	 * @return String 값
	 * @exception Exception
	 */
	public String getString(int index) throws Exception
	{
		try
		{
			Object obj = getObject(index);
			if(obj==null)
				return null;
			String ret = obj.toString();
			return ret;
		}
		catch(Exception e)
		{
			throw e;
		}
	}
	/**
	 * 주어진 인덱스에 해당하는 Object에 대해 String 값으로 변환하여 리턴한다.
	 * 만약 해당 객체가 Null이면 주어진 default Value를 리턴한다.<br>
	 * @param index 컬럼 인덱스
	 * @param defaultVal Default Value
	 * @return String 값
	 * @exception Exception
	 */
	public String getString(int index, String defaultVal) throws Exception
	{
		try
		{
			return getObject(index, defaultVal).toString();
		}
		catch(Exception e)
		{
			throw e;
		}
	}
	/**
	 * 주어진 컬럼이름에 해당하는 Object에 대해 String 값으로 변환하여 리턴한다.
	 * @param columnName 컬럼 이름
	 * @return String 값
	 * @exception Exception
	 */
	public String getString(String columnName) throws Exception
	{
		return getString(getColumnIndex(columnName));
	}
	/**
	 * 주어진 컬럼이름에 해당하는 Object에 대해 String 값으로 변환하여 리턴한다.<br>
	 * 만약 해당 객체가 Null이면 주어진 default Value를 리턴한다.<br>
	 * @param columnName 컬럼 이름
	 * @param defaultVal Default Value
	 * @return String 값
	 * @exception Exception
	 */
	public String getString(String columnName, String defaultVal) throws Exception
	{
		return  getString(getColumnIndex(columnName), defaultVal);
	}
	/**
	 * 주어진 인덱스에 해당하는 Object에 대해 기본형 자료형인 double형으로 변환하여 리턴한다.
	 * @param index 컬럼 인덱스
	 * @return double 값 - 만약 해당 Object가 Null값이면 -1.0D를 리턴한다.
	 * @exception Exception
	 */
	public double getDouble(int index) throws Exception
	{
		String strValue = getString(index);
		if(strValue==null) strValue = "-1.0";
		return Double.parseDouble(strValue);
	}
	/**
	 * 주어진 컬럼이름에 해당하는 Object에 대해 기본형 자료형인 double형으로 변환하여 리턴한다.
	 * @param columnName 컬럼 이름
	 * @return double 값 - 만약 해당 Object가 Null값이면 -1.0D를 리턴한다.
	 * @exception Exception
	 */
	public double getDouble(String columnName) throws Exception
	{
		return getDouble(getColumnIndex(columnName));
	}
	/**
	 * 주어진 인덱스에 해당하는 Object에 대해 기본형 자료형인 float형으로 변환하여 리턴한다.
	 * @param index 컬럼 인덱스
	 * @return float 값 - 만약 해당 Object가 Null값이면 -1.0f를 리턴한다.
	 * @exception Exception
	 */
	public float getFloat(int index) throws Exception
	{
		String strValue = getString(index);
		if(strValue==null) strValue = "-1.0";
		return Float.parseFloat(strValue);
	}
	/**
	 * 주어진 컬럼이름에 해당하는 Object에 대해 기본형 자료형인 float형으로 변환하여 리턴한다.
	 * @param columnName 컬럼 이름
	 * @return float 값 - 만약 해당 Object가 Null값이면 -1.0f를 리턴한다.
	 * @exception Exception
	 */
	public float getFloat(String columnName) throws Exception
	{
		return getFloat(getColumnIndex(columnName));
	}
	/**
	 * 주어진 인덱스에 해당하는 Object에 대해 기본형 자료형인 int형으로 변환하여 리턴한다.
	 * @param index 컬럼 인덱스
	 * @return int 값 - 만약 해당 Object가 Null값이면 -1을 리턴한다.
	 * @exception Exception
	 */
	public int getInt(int index) throws Exception
	{
		String strValue = getString(index);
		if(strValue==null) strValue = "-1";
		Double dValue = new Double(strValue);
		return dValue.intValue();
	}
	/**
	 * 주어진 컬럼이름에 해당하는 Object에 대해 기본형 자료형인 int형으로 변환하여 리턴한다.
	 * @param columnName 컬럼 이름
	 * @return int 값 - 만약 해당 Object가 Null값이면 -1을 리턴한다.
	 * @exception Exception
	 */
	public int getInt(String columnName) throws Exception
	{
		return getInt(getColumnIndex(columnName));
	}
	/**
	 * 주어진 인덱스에 해당하는 Object에 대해 기본형 자료형인 long형으로 변환하여 리턴한다.
	 * @param index 컬럼 인덱스
	 * @return long 값 - 만약 해당 Object가 Null값이면 -1을 리턴한다.
	 * @exception Exception
	 */
	public long getLong(int index) throws Exception
	{
		String strValue = getString(index);
		if(strValue==null) strValue = "-1";
		Double dValue = new Double(strValue);
		return dValue.longValue();
	}
	/**
	 * 주어진 컬럼이름에 해당하는 Object에 대해 기본형 자료형인 long형으로 변환하여 리턴한다.
	 * @param columnName 컬럼 이름
	 * @return long 값 - 만약 해당 Object가 Null값이면 -1을 리턴한다.
	 * @exception Exception
	 */
	public long getLong(String columnName) throws Exception
	{
		return getLong(getColumnIndex(columnName));
	}
	/**
	 * 주어진 인덱스에 해당하는 Object에 대해 java.sql.Date 객체로 변환하여 리턴한다.
	 * @param index 컬럼 인덱스
	 * @return java.sql.Date
	 * @exception Exception
	 */
	public Date getDate(int index) throws Exception
	{
		String strValue = getString(index);
		if(strValue==null) return null;

		return (Date) getObject(index);
	}
	/**
	 * 주어진 인덱스에 해당하는 Object에 대해 java.sql.Date 객체로 변환하여 리턴한다.
	 * 만약 해당 객체가 Null이면 주어진 default Value를 리턴한다.<br>
	 * 단, Default Value이 Format은 반드시 yyyy-MM-dd 형식이어야 한다.
	 * @param index 컬럼 인덱스
	 * @param defaultDt Default Value(단, String value format : yyyy-MM-dd)
	 * @return java.sql.Date
	 * @exception Exception
	 */
	public Date getDate(int index, String defaultDt) throws Exception
	{
		String strValue = getString(index);
		if(strValue==null) return Date.valueOf(defaultDt);

		return (Date) getObject(index);
	}
	/**
	 * 주어진 컬럼이름에 해당하는 Object에 대해 java.sql.Date 객체로 변환하여 리턴한다.
	 * @param columnName 컬럼 이름
	 * @return java.sql.Date
	 * @exception Exception
	 */
	public Date getDate(String columnName) throws Exception
	{
		return getDate(getColumnIndex(columnName));
	}
	/**
	 * 주어진 컬럼이름에 해당하는 Object에 대해 java.sql.Date 객체로 변환하여 리턴한다.
	 * 만약 해당 객체가 Null이면 주어진 default Value를 리턴한다.<br>
	 * 단, Default Value이 Format은 반드시 yyyy-MM-dd 형식이어야 한다.
	 * @param columnName 컬럼 이름
	 * @param defaultDt Default Value(단, String value format : yyyy-MM-dd)
	 * @return java.sql.Date
	 * @exception Exception
	 */
	public Date getDate(String columnName, String defaultDt) throws Exception
	{
		return getDate(getColumnIndex(columnName), defaultDt);
	}
	/**
	 * 주어진 인덱스에 해당하는 Object에 대해 java.sql.Time 객체로 변환하여 리턴한다.
	 * @param index 컬럼 인덱스
	 * @return java.sql.Time
	 * @exception Exception
	 */
	public Time getTime(int index) throws Exception
	{
		String strValue = getString(index);
		if(strValue==null) return null;

		return (Time) getObject(index);
	}
	/**
	 * 주어진 인덱스에 해당하는 Object에 대해 java.sql.Time 객체로 변환하여 리턴한다.
	 * 만약 해당 객체가 Null이면 주어진 default Value를 리턴한다.<br>
	 * 단, Default Value이 Format은 반드시 HH:mm:ss형식이어야 한다.
	 * @param index 컬럼 인덱스
	 * @param defaultTime Default Value(단, String value format : HH:mm:ss)
	 * @return java.sql.Time
	 * @exception Exception
	 */
	public Time getTime(int index, String defaultTime) throws Exception
	{
		String strValue = getString(index);
		if(strValue==null) return Time.valueOf(defaultTime);

		return (Time) getObject(index);
	}
	/**
	 * 주어진 컬럼이름에 해당하는 Object에 대해 java.sql.Time 객체로 변환하여 리턴한다.
	 * @param columnName 컬럼 이름
	 * @return java.sql.Time
	 * @exception Exception
	 */
	public Time getTime(String columnName) throws Exception
	{
		return getTime(getColumnIndex(columnName));
	}
	/**
	 * 주어진 컬럼이름에 해당하는 Object에 대해 java.sql.Time 객체로 변환하여 리턴한다.
	 * 만약 해당 객체가 Null이면 주어진 default Value를 리턴한다.<br>
	 * 단, Default Value이 Format은 반드시 HH:mm:ss 형식이어야 한다.
	 * @param columnName 컬럼 이름
	 * @param defaultTime Default Value(단, String value format : HH:mm:ss)
	 * @return java.sql.Time
	 * @exception Exception
	 */
	public Time getTime(String columnName, String defaultTime) throws Exception
	{
		return getTime(getColumnIndex(columnName), defaultTime);
	}
	/**
	 * 주어진 인덱스에 해당하는 Object에 대해 java.sql.Timestamp 객체로 변환하여 리턴한다.
	 * @param index 컬럼 인덱스
	 * @return java.sql.Timestamp
	 * @exception Exception
	 */
	public Timestamp getTimestamp(int index) throws Exception
	{
		String strValue = getString(index);
		if(strValue==null) return null;

		return (Timestamp) getObject(index);
	}
	/**
	 * 주어진 인덱스에 해당하는 Object에 대해 java.sql.Timestamp 객체로 변환하여 리턴한다.
	 * 만약 해당 객체가 Null이면 주어진 default Value를 리턴한다.<br>
	 * 단, Default Value이 Format은 반드시 yyyy-MM-dd HH:mm:ss형식이어야 한다.
	 * @param index 컬럼 인덱스
	 * @param defaultTs Default Value(단, String value format : yyyy-MM-dd HH:mm:ss)
	 * @return java.sql.Timestamp
	 * @exception Exception
	 */
	public Timestamp getTimestamp(int index, String defaultTs) throws Exception
	{
		String strValue = getString(index);
		if(strValue==null) return Timestamp.valueOf(defaultTs);

		return (Timestamp) getObject(index);
	}
	/**
	 * 주어진 컬럼이름에 해당하는 Object에 대해 java.sql.Timestamp 객체로 변환하여 리턴한다.
	 * @param columnName 컬럼 이름
	 * @return java.sql.Timestamp
	 * @exception Exception
	 */
	public Timestamp getTimestamp(String columnName) throws Exception
	{
		return getTimestamp(getColumnIndex(columnName));
	}
	/**
	 * 주어진 컬럼이름에 해당하는 Object에 대해 java.sql.Timestamp 객체로 변환하여 리턴한다.
	 * 만약 해당 객체가 Null이면 주어진 default Value를 리턴한다.<br>
	 * 단, Default Value이 Format은 반드시 yyyy-MM-dd HH:mm:ss 형식이어야 한다.
	 * @param columnName 컬럼 이름
	 * @param defaultTs Default Value(단, String value format : yyyy-MM-dd HH:mm:ss)
	 * @return java.sql.Timestamp
	 * @exception Exception
	 */
	public Timestamp getTimestamp(String columnName, String defaultTs) throws Exception
	{
		return getTimestamp(getColumnIndex(columnName), defaultTs);
	}
	/**
	 * 검색된 컬럼 네임에서 주어진 인덱스에 해당하는 컬럼네임을 리턴한다.
	 * @param 컬럼 네임에 대한 인덱스로서 0부터 시작한다.
	 * @return 컬럼 네임
	 */
	public String getColumnName(int index)
	{
		return (String)cols.elementAt(index -1);
	}
	/**
	 * 검색된 컬럼 네임은 갯수를 리턴한다.
	 * @return 검색된 컬럼 네임은 갯수
	 */
	public int getColumnCount()
	{
		return cols_count;
	}
	/**
	 * 검색된 컬럼 네임을 담은 Vector를 리턴한다.
	 * @return 모든 컬럼 네임을 담은 Vector
	 */
	public Vector getColumnNames()
	{
		return cols;
	}
	/**
	 * 실행된 SQL문을 통해 RecordSet에 담은 Total Row Count를 리턴한다.
	 * @return 실행된 SQL문을 통해 RecordSet에 담은 Total Row Count
	 */
	public int getRowCount()
	{
		return totalRowCount;
	}
	/**
	 * 실행된 SQL문을 통해 실제 가져온 Total Row Count를 리턴한다.
	 * @return 실행된 SQL문을 통해 실제 가져온 Total Row Count
	 */
	public int getRealRowCount()
	{
		return realRowCount;
	}
	/**
	 * java.sql.ResultSet.next() 메소드와 동일한 기능으로서<br>
	 * 검색할 다음 레코드로 인덱스를 이동시킨다.<br>
	 * 만약 다음 레코드가 존재하지 않을 경우에는 false를 리턴하고<br>
	 * 존재할 경우에는 레코드 인덱스를 이동시키고 true를 리턴한다.<br>
	 * @return 다음 레코드의 존재 여부
	 */
	public boolean next()
	{
		if(first && vals.size() > 0 )
		{
			first = false;
			searchIndex = 1;
			return true;
		}
		else
		{
			searchIndex++;
			if(searchIndex > totalRowCount)
			{
				return false;
			}
			else
			{
				return true;
			}
		}
	}
	/**
	 * 검색할 레코드의 인텍스를 초기화하여 처음부터 다시 검색할 수 있도록 한다.
	 */
	public void first()
	{
		searchIndex = 0;
		this.first  = true;
	}
	/**
	 * 현재의 인덱스가 처음인가 여부를 리턴한다.
	 */
	public boolean isFirst()
	{
		return searchIndex == 0;
	}
	/**
	 * 현재의 인덱스가 처음인가 여부를 리턴한다.
	 */
	public boolean isLast()
	{
		return searchIndex > totalRowCount;
	}

	public int getColumnType(String col_name) throws Exception
	{
		return getColumnType(getColumnIndex(col_name));
	}

	public int getColumnType(int index) throws Exception
	{
		int iColumnType = -1;
		try
		{
			iColumnType = Integer.parseInt((String)types.get(index-1));
		}
		catch(Exception e)
		{
			throw new Exception("RecordSet.getColumnType -  <" + index + "> is invalid Column index !! ");
		}

		return iColumnType;
	}
	/**
	 * 컬럼이름이 들어있는 cols 변수로 부터 해당 컬럼의 정확한 index를 얻어온다.
	 * @param col_name 얻어낼 인덱스의 컬럼 이름
	 * @return 컬럼 인덱스
	 */
	private int getColumnIndex(String col_name) throws Exception
	{
		int i = 0;
		boolean hasColName = false;
		for( ; i <= cols_count; i++)
		{
			if( col_name.equalsIgnoreCase( (String)cols.elementAt(i)) )
			{
				hasColName = true;
				break;
			}
		}

		if(!hasColName)
			throw new Exception("RecordSet.getColumnIndex -  <" + col_name + "> is invalid Column Name !! ");

		return (i + 1);
	}

	public static synchronized  String getStringByClob(Object obj)
	{
		//logger.debug(LP.BEGIN);
		StringBuffer sbRet = new StringBuffer();
		try
		{
			if( obj != null )
			{
				if( obj instanceof Clob )
				{
					Clob clob = (Clob)obj;
					Reader reader = clob.getCharacterStream();
					char[] aCharBuffer = new char[1024];
					int readcnt;
					while ((readcnt = reader.read(aCharBuffer, 0, 1024)) != -1)
					{
						sbRet.append(aCharBuffer, 0, readcnt);
					}
				}
				else
				{
					//logger.warn("Param is not Clob : " + obj);
				}
			}
			else
			{
				//logger.warn("Param is null");
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			//logger.endFailwithNote("ClobManager.getString - Failed to getString");
			//logger.error(LP.EXCEPTION, e);			
		}
		//logger.debug(LP.END);

		return sbRet.toString();
	}
}
