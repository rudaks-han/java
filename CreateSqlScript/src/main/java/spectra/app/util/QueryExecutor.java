package spectra.app.util;

import org.apache.commons.lang3.StringUtils;
import spectra.app.bean.Parameter;
import spectra.app.bean.Query;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QueryExecutor
{
    private String dbType;
    private String url;
    private String driver;
    private String user;
    private String pass;
    private String dateConvertToNow;
    private String targetDbType;
    private String sqlDelimiter;
    private List<String> dateColumnList;
    private String tableAlias = "A";
    private Logger logger;

    private static Connection conn;

    public static String LINE = "\r\n";

    public QueryExecutor()
    {
        Logger logger = new Logger();
    }

    public QueryExecutor(Logger logger)
    {
        this.logger = logger;
    }

    public void loadDbProperty(Map<String, String> dbMap, Map<String, String> settingsMap)
    {
        url = dbMap.get("url");
        driver = dbMap.get("driver");
        user = dbMap.get("user");
        pass = dbMap.get("pass");

        dateConvertToNow = settingsMap.get("dateConvertToNow");
        targetDbType = settingsMap.get("targetDbType");
        sqlDelimiter = settingsMap.get("sqlDelimiter");

        String dateColumn = (String) settingsMap.get("dateColumn");
        if (dateColumn != null && dateColumn.length() > 0)
        {
            String [] arDateColumn = dateColumn.split(",");
            if (arDateColumn != null && arDateColumn.length > 0)
            {
                dateColumnList = new ArrayList();
                for (String temp : arDateColumn)
                {
                    dateColumnList.add(temp.toUpperCase().trim());
                }
            }
        }
    }

    public String createMergeScript(Connection conn, Query query, List<Parameter> parameter)
    {
        Statement stmt = null;
        String result = "";

        try {
            String tableName = query.getTable();
            String sql = query.getSql();

            List<String> primaryKeyList = getPrimaryKeys(conn, dbType, tableName);

            if (primaryKeyList == null)
            {
                logger.println("[error] no primary key : " + sql);
                return null;
            }

            stmt = conn.createStatement();

            if (sql == null || sql.length() == 0)
                sql = "SELECT * FROM " + tableName;
            else if (sql != null && sql.length() > 0 && !sql.toUpperCase().startsWith("SELECT "))
            {
                sql = "SELECT * FROM " + sql;
            }

            if (parameter != null && parameter.size() > 0)
            {
                for (Parameter param : parameter)
                {
                    sql = StringUtils.replace(sql, "$" + param.getName(), param.getValue());
                }
            }

            result += "\n-- [execute sql for merge] " + sql + "\n";

            logger.println("[execute sql for merge] " + sql);

            ResultSet rs = stmt.executeQuery(sql);
            RecordSet rset = new RecordSet(rs);
            logger.print("[Fetching resultset] ");
            logger.appendln("total record : " + rset.getRowCount());

            List<String> setColumnList = new ArrayList<String>();

            for (int i = 1; i <= rset.getColumnCount(); i++)
            {
                String columnName = rset.getColumnName(i);
                if (primaryKeyList.contains(columnName)) // pk라면 건너뛴다.
                    continue;

                setColumnList.add(columnName);
            }

            String resultSql = null;
            while (rset.next())
            {
                if ("oracle".equals(targetDbType))
                {
                    resultSql = createMergeSql(tableName, rset, setColumnList, primaryKeyList);
                    if (resultSql != null && resultSql.length() > 0)
                    {
                        result += resultSql + sqlDelimiter + LINE;
                    }

                }
                else if ("postgresql".equals(targetDbType))
                {
                    resultSql = createUpsertSql(tableName, rset, setColumnList, primaryKeyList);
                    if (resultSql != null && resultSql.length() > 0)
                    {
                        result += resultSql + sqlDelimiter + LINE;
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (stmt != null) {	try { stmt.close(); } catch (Exception e) {} }
        }

        return result;
    }

    public String createInsertScript(Connection conn, Query query, List<Parameter> parameter)
    {
        Statement stmt = null;
        String result = "";
        try
        {
            String tableName = query.getTable();
            String sql = query.getSql();

            stmt = conn.createStatement();

            if (sql == null || sql.length() == 0)
                sql = "SELECT * FROM " + tableName;
            else if (sql != null && sql.length() > 0 && !sql.toUpperCase().startsWith("SELECT "))
                sql = "SELECT * FROM " + sql;

            if (parameter != null && parameter.size() > 0)
            {
                for (Parameter param : parameter)
                {
                    sql = StringUtils.replace(sql, "$" + param.getName(), param.getValue());
                }
            }

            result += "\n-- [execute sql for insert] " + sql + "\n";
            logger.println("[execute sql for insert] " + sql);

            ResultSet rs = stmt.executeQuery(sql);
            RecordSet rset = new RecordSet(rs);
            logger.print("[Fetching resultset] ");
            logger.appendln("total record : " + rset.getRowCount());

            while (rset.next())
            {
                result += createInsertSql(tableName, rset, false);
                result += sqlDelimiter + LINE;
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (stmt != null) {	try { stmt.close(); } catch (Exception e) {} }
        }
        return result;
    }

    public String createUpdateScript(Connection conn, Query query, List<Parameter> parameter)
    {
        Statement stmt = null;
        String result = "";

        try {
            String tableName = query.getTable();
            String sql = query.getSql();

            List<String> primaryKeyList = getPrimaryKeys(conn, dbType, tableName);

            if (primaryKeyList == null) {
                logger.println("[error] no primary key : " + query);
                return null;
            }

            stmt = conn.createStatement();

            if (sql == null || sql.length() == 0)
                sql = "SELECT * FROM " + tableName;
            else if (sql != null && sql.length() > 0 && !sql.toUpperCase().startsWith("SELECT "))
                sql = "SELECT * FROM " + sql;

            if (parameter != null && parameter.size() > 0)
            {
                for (Parameter param : parameter)
                {
                    sql = StringUtils.replace(sql, "$" + param.getName(), param.getValue());
                }
            }

            result += "\n-- [execute sql for update] " + sql + "\n";
            logger.println("[execute sql for update] " + sql);

            ResultSet rs = stmt.executeQuery(sql);
            RecordSet rset = new RecordSet(rs);
            logger.print("[Fetching resultset] ");
            logger.appendln("total record : " + rset.getRowCount());

            //System.err.logger.println("rset.getColumnCount() : " + rset.getColumnCount());
            List<String> setColumnList = new ArrayList<String>();

            for (int i = 1; i <= rset.getColumnCount(); i++) {
                String columnName = rset.getColumnName(i);
                if (primaryKeyList.contains(columnName)) // pk라면 건너뛴다.
                    continue;

                setColumnList.add(columnName);
            }

            while (rset.next())
            {
                result += createUpdateSql(tableName, rset, setColumnList, primaryKeyList);
                result += sqlDelimiter + LINE;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (stmt != null) {	try { stmt.close(); } catch (Exception e) {} }
        }
        return result;
    }

    private List getPrimaryKeys(Connection conn, String dbType, String tableName)
    {
        Statement stmt = null;
        ResultSet rs = null;
        RecordSet rset = null;
        List<String> pkList = new ArrayList<String>();

        try {
            stmt = conn.createStatement();
            String query = "";

            if ("oracle".equals(dbType))
            {
                query += "SELECT cols.column_name\n" +
                        "FROM all_constraints cons, all_cons_columns cols\n" +
                        "WHERE cols.table_name = '" + tableName.toUpperCase() + "'\n" +
                        "AND cons.constraint_type = 'P'\n" +
                        "AND cons.constraint_name = cols.constraint_name\n" +
                        "AND cons.owner = cols.owner\n" +
                        "ORDER BY cols.table_name, cols.position";
            }
            else if ("postgresql".equals(dbType))
            {
                query += "SELECT a.attname, format_type(a.atttypid, a.atttypmod) AS data_type\n" +
                        "FROM   pg_index i\n" +
                        "JOIN   pg_attribute a ON a.attrelid = i.indrelid\n" +
                        "AND a.attnum = ANY(i.indkey)\n" +
                        "WHERE  i.indrelid = '" + tableName + "'::regclass\n" +
                        "AND    i.indisprimary";
            }

            //logger.println("\n[execute sql for merge] " + query);

            rs = stmt.executeQuery(query);
            rset = new RecordSet(rs);
            logger.println("[Fetching primary key] " + tableName);
            logger.println("total record : " + rset.getRowCount());

            //System.err.logger.println("rset.getColumnCount() : " + rset.getColumnCount());

            while (rset.next())
            {
                pkList.add(rset.getString(1));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (stmt != null) {	try { stmt.close(); } catch (Exception e) {} }
        }
        return pkList;
    }

    private String getValueByColumnType(RecordSet rset, String columnName, int columnType)
    {
        String result = "";
        try
        {
            if ("Y".equals(dateConvertToNow) && dateColumnList.size() > 0 && dateColumnList.contains(columnName.toUpperCase()))
            {
                result = getDbCurrDate();
            }
            else
            {
                if (columnType == Types.INTEGER || columnType == Types.NUMERIC)
                {
                    result = rset.getInt(columnName) + "";
                }
                else if (columnType == Types.TIMESTAMP)
                {
                    result = "'" + rset.getTimestamp(columnName) + "'";
                }
                else if (columnType == Types.DATE)
                {
                    if (driver.indexOf("oracle") > -1)
                    {
                        result = "TO_DATE('" + rset.getString(columnName).substring(0, 19) + "', 'yy-mm-dd hh24:mi:ss')";
                    }
                    else {
                        result = "'" + rset.getString(columnName) + "'";
                    }
                } else {
                    if (rset.getString(columnName) == null) {
                        result = "null";
                    } else {
                        result = "'" + escapeSql(rset.getString(columnName)) + "'";
                    }
                }
            }
            return result;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return result;
    }

    private String getInsertColumnList(RecordSet rset)
    {
        String result = "";
        for (int i = 1; i <= rset.getColumnCount(); i++)
        {
            String columnName = rset.getColumnName(i);
            if (i == 1)
                result += columnName;
            else
                result += "," + columnName;
        }

        return result;
    }

    private String getInsertValueList(RecordSet rset)
    {
        String result = "";
        try
        {
            for (int i = 1; i <= rset.getColumnCount(); i++)
            {
                if (i > 1)
                    result += ", ";

                String columnName = rset.getColumnName(i);
                int columnType = rset.getColumnType(columnName);

                result += getValueByColumnType(rset, columnName, columnType);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return result;
    }

    private String getUpdateSetList(RecordSet rset, List<String> columnList)
    {
        String result = "";
        try {
            int k = 0;
            for (String columnName : columnList)
            {
                if (k > 0)
                    result += ", ";
                int columnType = rset.getColumnType(columnName);
                result += columnName + " = " + getValueByColumnType(rset, columnName, columnType) + " ";
                k++;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return result;
    }

    private String getWhereByPk(RecordSet rset, List<String> primaryKeyList, boolean withTableAlias)
    {
        String result = "";
        try {
            int pkLoop = 0;
            for (String pk : primaryKeyList)
            {
                if (pkLoop > 0)
                    result += " AND ";

                if (withTableAlias)
                    result += tableAlias + ".";
                result +=  pk + " = ";

                int columnType = rset.getColumnType(pk);

                if (columnType == Types.INTEGER || columnType == Types.NUMERIC)
                {
                    result += rset.getInt(pk) + "";
                }
                else if (columnType == Types.TIMESTAMP)
                {
                    result += "'" + rset.getTimestamp(pk) + "'";
                }
                else if (columnType == Types.DATE)
                {
                    if (driver.indexOf("oracle") > -1)
                    {
                        result += "TO_DATE('" + rset.getString(pk).substring(0, 19) + "', 'yy-mm-dd hh24:mi:ss')";
                    }
                    else
                        {
                        result += "'" + rset.getString(pk) + "'";
                    }
                }
                else
                {
                    if (rset.getString(pk) == null)
                        result += "null";
                    else
                        {
                        result += "'" + escapeSql(rset.getString(pk)) + "'";
                    }
                }

                pkLoop++;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return result;
    }

    private String createInsertSql(String tableName, RecordSet rset, boolean withTableAlias)
    {
        StringBuffer sb = new StringBuffer();
        try
        {
            sb.append("INSERT INTO " + tableName);
            if (withTableAlias)
                sb.append(" AS " + tableAlias);
            sb.append(" (");

            sb.append(getInsertColumnList(rset));

            sb.append(") ");
            sb.append("VALUES(");

            sb.append(getInsertValueList(rset));

            sb.append(")");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return sb.toString();
    }

    private String createUpdateSql(String tableName, RecordSet rset, List<String> setColumnList, List<String> primaryKeyList)
    {
        StringBuffer sb = new StringBuffer();
        try {
            String whereSql = getWhereByPk(rset, primaryKeyList, false);
            if (whereSql.length() > 0)
            {

                sb.append("UPDATE " + tableName + " ");
                sb.append("SET ");

                sb.append(getUpdateSetList(rset, setColumnList));

                // where
                sb.append("WHERE ");

                sb.append(getWhereByPk(rset, primaryKeyList, false));
            }
            else
            {
                logger.println("[no primary key] " + tableName);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return sb.toString();
    }

    private String createMergeSql(String tableName, RecordSet rset, List<String> setColumnList, List<String> primaryKeyList)
    {
        StringBuffer sb = new StringBuffer();
        try
        {
            String whereSql = getWhereByPk(rset, primaryKeyList, false);

            if (whereSql.length() == 0) // PK가 없을때
            {
                logger.println("[no primary key] " + tableName);
                return "";
            }

            sb.append("MERGE INTO " + tableName + " ");
            sb.append("USING dual ");
            sb.append("ON (");
            for (int i=0; i<primaryKeyList.size(); i++)
            {
                if (i>0)
                    sb.append("AND ");
                String columnName = primaryKeyList.get(i);
                int columnType = rset.getColumnType(columnName);

                String value = getValueByColumnType(rset, columnName, columnType);

                sb.append(columnName + " = " + value + " ");
            }
            sb.append(") ");

            // 모든 컬럼이 PK일 경우는 update를 사용할 수 없음
            if (setColumnList.size() > 0)
            {
                sb.append("WHEN MATCHED THEN ");
                sb.append("UPDATE SET ");
                sb.append(getUpdateSetList(rset, setColumnList));
            }

            sb.append("WHEN NOT MATCHED THEN ");
            sb.append("INSERT (");

            sb.append(getInsertColumnList(rset));

            sb.append(") ");
            sb.append("VALUES(");

            for (int i = 1; i <= rset.getColumnCount(); i++)
            {
                if (i > 1)
                    sb.append(", ");

                String columnName = rset.getColumnName(i);
                int columnType = rset.getColumnType(columnName);

                sb.append(getValueByColumnType(rset, columnName, columnType));
            }

            sb.append(")");

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return sb.toString();
    }

    private String createUpsertSql(String tableName, RecordSet rset, List<String> setColumnList, List<String> primaryKeyList)
    {
        StringBuffer sb = new StringBuffer();
        try {

            String whereSql = getWhereByPk(rset, primaryKeyList, true);

            if (whereSql.length() == 0) // PK가 없을때
            {
                logger.println("[no primary key] " + tableName);
                return "";
            }

            sb.append(createInsertSql(tableName, rset, true) + " ");

            sb.append("ON CONFLICT (");
            for (int i=0; i<primaryKeyList.size(); i++)
            {
                if (i>0)
                    sb.append(", ");
                String columnName = primaryKeyList.get(i);
                sb.append(columnName);
            }
            sb.append(") ");

            if (setColumnList.size() > 0)
            {
                sb.append("DO UPDATE SET ");
                sb.append(getUpdateSetList(rset, setColumnList));
                sb.append("WHERE ");
                sb.append(getWhereByPk(rset, primaryKeyList, true));
            }
            else if (setColumnList.size() == 0) // 모든 컬럼이 PK일때
            {
                sb.append("DO NOTHING ");
                logger.println("[all primary key] " + tableName);
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return sb.toString();
    }

    private String getDbCurrDate()
    {
        String result = "";
        if ("oracle".equals(targetDbType))
        {
            result = "TO_CHAR(sysdate, 'YYYYMMDDHH24MISS')";
        }
        else if ("mysql".equals(targetDbType))
        {
            result = "date_format(now(), '%Y%m%d%H%i%s')";
        }
        else if ("postgresql".equals(targetDbType))
        {
            result = "TO_CHAR(now(), 'YYYYMMDDHH24MISS')";
        }

        return result;
    }

    public String escapeSql(String str)
    {
        if (str != null)
            return str.replaceAll("'", "''");
        return str;

    }

    public Connection getConnection() throws Exception
    {
        if (conn == null)
        {
            if (driver.indexOf("oracle") > -1)
                dbType = "oracle";
            else if (driver.indexOf("postgresql") > -1)
                dbType = "postgresql";
            else if (driver.indexOf("mysql") > -1)
                dbType = "mysql";

            logger.println("[db type] " + dbType);
            logger.println("[jdbc driver] " + driver);
            Class.forName(driver);
            logger.println("==> OK");
            logger.print("[Connection url] " + url);
            logger.println(", user : " + user);
            logger.println(", pass : " + pass);
            conn = DriverManager.getConnection(url, user, pass);
            logger.println("==> OK");

            ResultSet rs = null;
            DatabaseMetaData meta = conn.getMetaData();
            // The Oracle database stores its table names as Upper-Case,
            // if you pass a table name in lowercase characters, it will not work.
            // MySQL database does not care if table name is uppercase/lowercase.
            //
            rs = meta.getPrimaryKeys(null, null, "t_ticket");
            while( rs.next( ) )
            {
                String columnName = rs.getString("COLUMN_NAME");
                //System.out.println("getPrimaryKeys(): columnName=" + columnName);
            }

        }
        return conn;
    }

    public void releaseConnection() throws Exception
    {
        if (conn  != null)
        {
            conn.close();
        }
    }

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public String getDateConvertToNow() {
        return dateConvertToNow;
    }

    public void setDateConvertToNow(String dateConvertToNow) {
        this.dateConvertToNow = dateConvertToNow;
    }

    public List<String> getDateColumnList() {
        return dateColumnList;
    }

    public void setDateColumnList(List<String> dateColumnList) {
        this.dateColumnList = dateColumnList;
    }
}
