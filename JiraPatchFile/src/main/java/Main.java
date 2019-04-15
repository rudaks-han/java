import executor.SystemCmdExecutor;
import executor.factory.SystemCmdFactory;
import org.apache.commons.io.FileUtils;
import service.JiraService;
import service.SvnService;
import util.ExcelWriter;
import util.Util;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

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

    private SystemCmdExecutor systemCmdExecutor;

    private String tempDir;
    private String outputDir;

    public Main()
    {
        tempDir = System.getProperty("user.dir") + "/temp";
        outputDir = System.getProperty("user.dir") + "/output";

        systemCmdExecutor = SystemCmdFactory.getInstance();

        loadProperty();

        jiraService = new JiraService(jiraUrl, jiraUser, jiraPassword);
        svnService = new SvnService(svnUrl, systemCmdExecutor, tempDir, outputDir);
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

        if (jiraPatchList != null)
        {
            initWorkingDirectory();

            svnService.executeSvnLogAndParse(jiraPatchList, exportDiffFile, revisionDiffVersion);

            // excel로 목록 만들기
            ExcelWriter.write(jiraPatchList, excelFilename);

            Util.debug("\n[ok] " + " Saved to output folder [" + tempDir + "]");
        }
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
