package spectra.app;

import com.google.common.io.ByteStreams;
import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;
import spectra.app.bean.Configuration;
import spectra.app.bean.Parameter;
import spectra.app.bean.Query;
import spectra.app.util.DateUtil;
import spectra.app.util.Logger;
import spectra.app.util.QueryExecutor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Main
{
	public static String LINE = "\r\n";
	private Map<String, String> dbMap;

	private List<Parameter> parameters;
	private Map<String, String> settingsMap;
	private List<String> dateColumnList = new ArrayList<String>();

	private List<Query> mergeQueryList = new ArrayList<Query>();
	private List<Query> insertQueryList = new ArrayList<Query>();
	private List<Query> updateQueryList = new ArrayList<Query>();

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

	public static void main(String[] args) throws IOException {
		Main main = new Main();

		Configuration config = main.loadConfig();

		main.loadDbProperty(config);
		main.loadSettings(config);
		main.loadParameters(config);
		main.loadQuery(config);

		String currDate = DateUtil.getCurrDate("yyyyMMddHHss");
		main.executeScript(currDate);
	}


	private String executeScript(String currDate)
	{
		String result = "";

		Connection conn = null;
		QueryExecutor queryExecutor = null;
		try
		{
			Logger logger = new Logger();

			queryExecutor = new QueryExecutor(logger);
			queryExecutor.loadDbProperty(dbMap, settingsMap);
			conn = queryExecutor.getConnection();

			if (mergeQueryList != null && mergeQueryList.size() > 0)
			{
				logger.println("mergeQueryList count : "+ mergeQueryList.size());
				for (Query query: mergeQueryList)
				{
					String queryResult = queryExecutor.createMergeScript(conn, query, parameters);
					if ("N".equals(settingsMap.get("saveFileToOne")))
					{
                        if (queryResult != null && queryResult.length() > 0)
                        {
                        	String savePath = "./output/[merge]" + query.getTable() + ".sql";
							logger.println("save to "+ savePath);
							FileUtils.writeStringToFile(new File(savePath), queryResult, "UTF-8");
						}
					}

					result += queryResult;
				}
			}

			if (insertQueryList != null && insertQueryList.size() > 0)
			{
				logger.println("insertQueryList count : "+ insertQueryList.size());
				for (Query query: insertQueryList)
				{
					String queryResult = queryExecutor.createInsertScript(conn, query, parameters);
					if ("N".equals(settingsMap.get("saveFileToOne")))
					{
					    if (queryResult != null && queryResult.length() > 0)
					    {
							String savePath = "./output/[insert]" + query.getTable() + ".sql";
							logger.println("save to "+ savePath);
							FileUtils.writeStringToFile(new File(savePath), queryResult, "UTF-8");
						}
					}

					result += queryResult;
				}
			}

			if (updateQueryList != null && updateQueryList.size() > 0)
			{
				logger.println("updateQueryList count : "+ updateQueryList.size());
				for (Query query: updateQueryList)
				{
					String queryResult = queryExecutor.createUpdateScript(conn, query, parameters);
					if ("N".equals(settingsMap.get("saveFileToOne")))
					{
                        if (queryResult != null && queryResult.length() > 0)
                        {
							String savePath = "./output/[update]" + query.getTable() + ".sql";
							logger.println("save to "+ savePath);
							FileUtils.writeStringToFile(new File(savePath), queryResult, "UTF-8");
						}
					}

					result += queryResult;
				}
			}

			if ("Y".equals(settingsMap.get("saveFileToOne")))
			{
                if (result != null && result.length() > 0)
				{
					String outputFilename = "./output/script_" + currDate + ".sql";
					FileUtils.writeStringToFile(new File(outputFilename), result, "UTF-8");

					logger.println("\n[Saved file] " + outputFilename);
				}
			}

			queryExecutor.releaseConnection();

			logger.saveAsFile("./output/log_" + currDate + ".log");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally {
			if (conn != null)
			{
				try {
					queryExecutor.releaseConnection();
				}
				catch (Exception e) {}
			}
		}

		return result;
	}



	private void loadDbProperty(Configuration configuration)
	{
		this.dbMap = configuration.getDatabase();

	}

	private void loadParameters(Configuration configuration)
	{
		this.parameters = configuration.getParameters();
	}

	private void loadSettings(Configuration configuration)
	{
		this.settingsMap = configuration.getSettings();
	}

	private void loadQuery(Configuration configuration)
	{
		List<Query> queryList = configuration.getQuery();
		for (Query query : queryList)
		{
			if (query.getType() == null || "merge".equals(query.getType()))
			{
				List<String> tableList = Arrays.asList(query.getTable().split("\\s*,\\s*"));
				List<String> sqlList = null;
				if (query.getSql() != null)
					sqlList = Arrays.asList(query.getSql().split("\\s*,\\s*"));
				else if (query.getWhere() != null)
				{
					String [] arWhereQuery = query.getWhere().split("\\s*,\\s*");
					sqlList = new ArrayList<String>();
					for (int i=0; i<arWhereQuery.length; i++)
					{
						sqlList.add("SELECT * FROM " + tableList.get(i) + " WHERE " + arWhereQuery[i]);
					}
				}


				for(int i=0; i<tableList.size(); i++)
				{
					Query newQuery = new Query();
					newQuery.setTable(tableList.get(i));
					if (sqlList != null)
						newQuery.setSql(sqlList.get(i));
					newQuery.setType("merge");
					mergeQueryList.add(newQuery);
				}
			}
			else if ("insert".equals(query.getType()))
			{
				insertQueryList.add(query);
			}
			else if ("update".equals(query.getType()))
			{
				updateQueryList.add(query);
			}
		}
	}

	public Configuration loadConfig() throws IOException
	{
		URL url = getConfigURL();
		InputStream is = url.openConnection().getInputStream();
		return new Yaml().loadAs(
				new ByteArrayInputStream(ByteStreams.toByteArray(is))
				, Configuration.class);
	}

	private URL getConfigURL() throws IOException
	{
		URL url = null;

		File f = new File("./db.yml");
		if (f.exists())
		{
			url = f.toURI().toURL();
		}
		else
		{
			url = getClass().getClassLoader().getResource("./db.yml");
			if (url == null)
			{
				System.err.println("./db.yml파일이 없습니다.");
			}
			else
			{
				url.openStream().close();
			}
		}

		return url;
	}

}
