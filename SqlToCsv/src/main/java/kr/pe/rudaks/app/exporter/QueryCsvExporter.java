package kr.pe.rudaks.app.exporter;

import au.com.bytecode.opencsv.CSVWriter;
import kr.pe.rudaks.app.ResultData;
import kr.pe.rudaks.app.util.RecordSet;
import org.apache.commons.io.output.FileWriterWithEncoding;

import java.io.IOException;
import java.util.List;

public class QueryCsvExporter extends QueryExporter {

    @Override
    public String getOutputFilename() {
        String filename = this.filename.replaceAll(".sql", "");
        String outputFile = outputPath + "/" + filename + ".csv";

        return outputFile;
    }

    public void save(List<ResultData> resultDataList) throws IOException {
        for (ResultData resultData: resultDataList) {
            CSVWriter writer = new CSVWriter(new FileWriterWithEncoding(getOutputFilename(), encoding));
            try {
                RecordSet rset = resultData.getRset();

                String[] str = new String[rset.getColumnCount()];
                for (int i = 0; i < rset.getColumnCount(); i++) {
                    int columnIndex = i + 1;
                    str[i] = rset.getColumnName(columnIndex);
                }
                writer.writeNext(str);

                while (rset.next()) {
                    for (int i = 0; i < rset.getColumnCount(); i++) {
                        int columnIndex = i + 1;
                        str[i] = rset.getString(columnIndex);
                    }

                    writer.writeNext(str);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                writer.close();
            }
        }
    }

}
