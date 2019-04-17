import excel.model.Column;
import executor.SystemCmdExecutor;
import executor.factory.SystemCmdFactory;
import org.apache.commons.io.FileUtils;
import service.JiraService;
import service.SvnService;
import excel.ExcelWriter;
import util.Util;

import java.io.*;
import java.util.*;

/**
 * Created by kmhan on 2017-08-21.
 */
public class Main
{
    private String jiraUrl;
    private String jiraUser;
    private String jiraPassword;
    private String svnUrl;

    private static String jiraSearchCondition;
    private static String excelFilename;
    private static String exportDiffFile;
    private static String revisionDiffVersion;

    JiraService jiraService;
    SvnService svnService;

    private String tempDir;
    private String outputDir;

    public Main()
    {
        loadProperty();

        tempDir = System.getProperty("user.dir") + "/temp";
        outputDir = System.getProperty("user.dir") + "/output";

        jiraService = new JiraService(jiraUrl, jiraUser, jiraPassword);
        svnService = new SvnService(svnUrl, tempDir, outputDir);
    }

    public static void main(String [] args) throws IOException
    {
        Main obj = new Main();
        obj.execute();
    }

    private void loadProperty()
    {
        try
        {
            Properties prop = new Properties();

            FileInputStream fi = null;
            try
            {
                fi = new FileInputStream("patch.properties");
                prop.load(fi);
            }
            catch (FileNotFoundException e)
            {
                InputStream stream = Main.class.getClassLoader().getResourceAsStream("patch.properties");
                prop.load(stream);
            }
            finally
            {
                if (fi != null)
                    fi.close();
            }

            jiraSearchCondition = Util.readString(prop.getProperty("jira.search.condition"));
            if (jiraSearchCondition == null)
            {
                Util.debug("[error] jira.search.condition 값이 없습니다. 검색조건을 다시 설정하세요.");
                return;
            }

            excelFilename = Util.readString(prop.getProperty("excel.filename"));
            exportDiffFile = prop.getProperty("export.diff.file");
            revisionDiffVersion = prop.getProperty("revision.diff.version");

            jiraUrl = prop.getProperty("jira.url");
            jiraUser = prop.getProperty("jira.user");
            jiraPassword = prop.getProperty("jira.password");
            svnUrl = prop.getProperty("svn.url");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void execute() throws IOException
    {
        // jira의 패치목록 가져오기
        List<HashMap<String, String>> jiraPatchList = jiraService.getPatchList(jiraSearchCondition);

        if (jiraPatchList == null)
        {
            Util.debug("jiraPathList is null.");
        }

        initWorkingDirectory();

        svnService.executeSvnLogAndParse(jiraPatchList, exportDiffFile, revisionDiffVersion);

        List<Column> columnList = getColumnList();
        ExcelWriter excelWriter = new ExcelWriter("패치목록", columnList, jiraPatchList);
        excelWriter.write(excelFilename);

        Util.debug("\n[ok] " + " Saved to output folder [" + tempDir + "/" + excelFilename + "]");
    }

    public List<Column> getColumnList()
    {
        List<Column> columnList = new ArrayList<Column>();

        columnList.add(new Column("key", "Key", true, 0));
        columnList.add(new Column("summary", "Summary", false, 2800*3));
        columnList.add(new Column("priority", "Priority", false, 512));
        columnList.add(new Column("patchImportance", "패치중요도", false, 512));
        columnList.add(new Column("menu", "메뉴", false, 2800*4));
        columnList.add(new Column("revision", "SVN Rev.No", false, 512));
        columnList.add(new Column("responseHistory", "처리내역", false, 2800*5));
        columnList.add(new Column("description", "Description", false, 2800*5));
        columnList.add(new Column("patchFileList", "패치 파일 리스트", false, 2800*6));

        return columnList;
    }

    private void initWorkingDirectory() throws IOException
    {
        // temp, output 디렉토리 삭제
        FileUtils.deleteDirectory(new File(tempDir));
        FileUtils.deleteDirectory(new File(outputDir));

        // temp, output 디렉토리 만들기
        File tempDirectory = new File(tempDir);
        File outputDirectory = new File(outputDir);

        if (!tempDirectory.exists())
            FileUtils.forceMkdir(new File(tempDir));
        if (!outputDirectory.exists())
            FileUtils.forceMkdir(new File(outputDir));
    }
}
