package spectra.app.bean;

import java.util.List;
import java.util.Map;

public class Configuration
{
	private Map<String, String> database;

	public List<Parameter> getParameters()
	{
		return parameters;
	}

	public void setParameters(List<Parameter> parameters)
	{
		this.parameters = parameters;
	}

	private List<Parameter> parameters;
	private List<Query> query;
	private Map<String, String> settings;

	public Map<String, String> getDatabase() {
		return database;
	}

	public void setDatabase(Map<String, String> database) {
		this.database = database;
	}

	public List<Query> getQuery() {
		return query;
	}

	public void setQuery(List<Query> query) {
		this.query = query;
	}

	public Map<String, String> getSettings() {
		return settings;
	}

	public void setSettings(Map<String, String> settings) {
		this.settings = settings;
	}
}
