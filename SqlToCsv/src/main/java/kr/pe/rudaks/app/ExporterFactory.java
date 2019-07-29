package kr.pe.rudaks.app;

import kr.pe.rudaks.app.exporter.QueryCsvExporter;
import kr.pe.rudaks.app.exporter.QueryExcelExporter;
import kr.pe.rudaks.app.exporter.QueryExporter;

public class ExporterFactory
{
	public static ExporterFactory exporterFactory;
	private static QueryExporter queryExporter = null;

	public static QueryExporter getInstance(String type) {
		if ("csv".equals(type)) {
			queryExporter = new QueryCsvExporter();
		} else if ("excel".equals(type)) {
			queryExporter = new QueryExcelExporter();
		}

		return queryExporter;
	}
}
