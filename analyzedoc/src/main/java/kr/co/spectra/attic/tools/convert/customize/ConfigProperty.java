package kr.co.spectra.attic.tools.convert.customize;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties("config")
public class ConfigProperty {

    private String inputFilePath;
    private String inputFileExtension;
    private String outputFilePath;
    private boolean writeToExcel;
    private boolean writeToDb;
    private boolean compareFileExistWithSvn;
    private String svnPath;
    private String svnDiffSavePath;
    private String svnUsername;
    private String svnPassword;
    private List<ExcelColumn> excelColumns = new ArrayList<>();
    private List<FilenameToPackage> filenameToPackages = new ArrayList<>();
    private List<SvnSiteName> svnSiteNames = new ArrayList<>();
    private List<SvnRepositoryPath> svnRepositoryPaths = new ArrayList<>();

    @Getter
    @Setter
    public static class ExcelColumn {
        private String id;
        private String columnName;
        private boolean autoSize;
        private int addColumnWidth;
        private String mapToWordParagraph;
    }

    @Getter
    @Setter
    public static class FilenameToPackage {
        private String filename;
        private String filepath;
    }

    @Getter
    @Setter
    public static class SvnSiteName {
        private String siteName;
        private String svnName;
    }

    @Getter
    @Setter
    public static class SvnRepositoryPath {
        private String siteName;
        private String svnPath;
    }
}
