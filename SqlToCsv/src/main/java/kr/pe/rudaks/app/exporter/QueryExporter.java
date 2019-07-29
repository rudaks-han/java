package kr.pe.rudaks.app.exporter;

import kr.pe.rudaks.app.ResultData;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public abstract class QueryExporter {

    protected String filename;
    protected String outputPath;
    protected String encoding;

    public abstract void save(List<ResultData> rsets) throws IOException;
    public abstract String getOutputFilename();

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;

        try {
            FileUtils.forceMkdir(new File(outputPath));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}
