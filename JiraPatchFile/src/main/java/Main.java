import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.utils.DateUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.*;

/**
 * Created by kmhan on 2017-08-21.
 */
public class Main
{
    private static String JIRA_URL;
    private static String JIRA_USER;
    private static String JIRA_PASSWORD;
    private static String SVN_URL;
    private static String SVN_LOG_CMD;
    private static String SVN_EXPORT_CMD;
    private static String SVN_DIFF_CMD;

    private static String jiraSearchCondition;
    private static String excelFilename;
    private static String exportDiffFile;
    private static String revisionDiffVersion;

    private String currDate = DateUtils.formatDate(new Date(), "yyyyMMddHHmmss");

    public static HSSFWorkbook workbook = new HSSFWorkbook();

    // 버그 목록
    // http://211.63.24.57:8080/rest/api/2/search?jql=project%20=%20EER%20AND%20issuetype%20=%20%EB%B2%84%EA%B7%B8%20AND%20status%20in%20(Resolved,%20Closed)%20AND%20resolution%20=%20Fixed%20AND%20fixVersion%20=%20%22EER%202.0%22%20AND%20%ED%8C%A8%EC%B9%98%EC%A4%91%EC%9A%94%EB%8F%84%20in%20(%EA%B6%8C%EA%B3%A0,%20%ED%95%84%EC%88%98)

    public static void main(String [] args) throws URISyntaxException, IOException
    {
        Main obj = new Main();

        obj.loadProperty();

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

            jiraSearchCondition = prop.getProperty("jira.search.condition");
            if (jiraSearchCondition != null && jiraSearchCondition.length() > 0)
            {
                jiraSearchCondition = new String(jiraSearchCondition.getBytes("8859_1"), "UTF-8");
            }
            else
            {
                debug("[error] jira.search.condition 값이 없습니다. 검색조건을 다시 설정하세요.");
                return;
            }

            excelFilename = prop.getProperty("excel.filename");
            if (excelFilename != null && excelFilename.length() > 0)
            {
                excelFilename = new String(excelFilename.getBytes("8859_1"), "UTF-8");
            }

            exportDiffFile = prop.getProperty("export.diff.file");
            revisionDiffVersion = prop.getProperty("revision.diff.version");

            JIRA_URL = prop.getProperty("jira.url");
            JIRA_USER = prop.getProperty("jira.user");
            JIRA_PASSWORD = prop.getProperty("jira.password");
            SVN_URL = prop.getProperty("svn.url");
            SVN_LOG_CMD = prop.getProperty("svn.log.cmd");
            SVN_EXPORT_CMD = prop.getProperty("svn.export.cmd");
            SVN_DIFF_CMD = prop.getProperty("svn.diff.cmd");
        }
        catch (Exception e)
        {
            e.printStackTrace();

            debug(e.getMessage());
        }
    }

    private void execute() throws UnsupportedEncodingException, IOException
    {
        // jira의 패치목록 가져오기
        List<HashMap<String, String>> jiraPatchList = getPatchListInJira();

        if (jiraPatchList != null)
        {
            // temp, output 디렉토리 삭제
            FileUtils.deleteDirectory(new File("./temp"));
            FileUtils.deleteDirectory(new File("./output"));

            // temp, output 디렉토리 만들기
            FileUtils.forceMkdir(new File("./temp"));
            FileUtils.forceMkdir(new File("./output"));

            for (HashMap map : jiraPatchList)
            {
                String key = (String) map.get("key"); // EER-1234
                String revision = (String) map.get("revision"); // Jira의 SVN Rev.No
                String priority = (String) map.get("priority");
                String patchImportance = (String) map.get("patchImportance");
                String menu = (String) map.get("menu");
                String responseHistory = (String) map.get("responseHistory");
                String description = (String) map.get("description");

                if (revision != null && revision.length() > 0)
                {
                    String [] arRevision = revision.split(",");
                    if (arRevision != null && arRevision.length > 0)
                    {
                        for (String _revision : arRevision)
                        {
                            _revision = _revision.trim();
                            String svnLog = executeSvnLog(_revision);

                            //debug("LOG : " + svnLog);

                            parseSvnLog(map, _revision, svnLog);
                        }
                    }
                }
            }

            // excel로 목록 만들기
            createExcel(jiraPatchList);

            debug("\n[ok] " + " Saved to output folder [" + System.getProperty("user.dir") + "/output]");
        }
    }

    private HashMap<String, Object> searchJiraByJql(String param)
    {
        HashMap resultMap = null;

        try
        {
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.setMessageConverters(getMessageConverters());
            restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(JIRA_USER, JIRA_PASSWORD));


            URI uri = new URI(JIRA_URL + "/rest/api/2/search?jql=" + param);
            debug("[condition] " + uri.getQuery());

            resultMap = restTemplate.getForObject(uri, HashMap.class);
        }
        catch (URISyntaxException e)
        {
            e.printStackTrace();
            debug(e.getMessage());
        }

        return resultMap;
    }

    private List parseJiraSearchMap(HashMap map)
    {
        List<HashMap<String, String>> resultList = new ArrayList<HashMap<String, String>>();

        if (map != null)
        {
            ArrayList<HashMap> issues = (ArrayList<HashMap>) map.get("issues");
            debug("[issue count] " + issues.size() + "");

            for (HashMap issueMap : issues)
            {
                HashMap<String, String> resultMap = new HashMap<String, String>();

                String key = (String) issueMap.get("key");
                HashMap<String, Object> fields = (HashMap<String, Object>) issueMap.get("fields");
                String summary = (String) fields.get("summary");
                String priority = "";
                if (fields.get("priority")!= null)
                    priority = (String) ((HashMap<String, Object>) fields.get("priority")).get("name");

                String patchImportance = "";
                if (fields.get("customfield_11100") != null)
                    patchImportance = (String) ((HashMap<String, Object>) fields.get("customfield_11100")).get("value");

                String menu = "";
                if (fields.get("customfield_10202") != null)
                    menu = (String) ((HashMap<String, Object>) fields.get("customfield_10202")).get("value");

                String revision = (String) fields.get("customfield_10203"); // revision
                String responseHistory = (String) fields.get("customfield_10021"); // 처리내역
                String description = (String) fields.get("description"); // description

                /*
                System.err.println("==============================================================");
                System.err.println("key : " + key);
                System.err.println("summary : " + summary);
                System.err.println("priority : " + priority);
                System.err.println("patchImportance : " + patchImportance);
                System.err.println("menu : " + menu);
                System.err.println("revision : " + revision);
                System.err.println("responseHistory : " + responseHistory);
                //System.err.println("description : " + description);
                System.err.println("==============================================================");
                */

                resultMap.put("key", key);
                resultMap.put("summary", summary);
                resultMap.put("priority", priority);
                resultMap.put("patchImportance", patchImportance);
                resultMap.put("menu", menu);
                resultMap.put("revision", revision);
                resultMap.put("responseHistory", responseHistory);
                resultMap.put("description", description);

                resultList.add(resultMap);
            }
        }

        return resultList;
    }

    private List<HashMap<String, String>> getPatchListInJira() throws UnsupportedEncodingException
    {
        List resultList = null;
        // http://211.63.24.57:8080/rest/api/2/search?jql=project = EER AND issuetype = 버그 AND status in (Resolved, Closed) AND resolution = Fixed AND fixVersion = "EER 2.0" AND 패치중요도 in (권고, 필수)
        // http://211.63.24.57:8080/rest/api/2/issue/EER-4222

        //String param = "project = EER AND issuetype = 버그 AND status in (Resolved, Closed) AND resolution = Fixed AND fixVersion = \"EER 2.0\" AND 패치중요도 in (권고, 필수)";
        String param = URLEncoder.encode(jiraSearchCondition, "UTF-8");

        HashMap<String, Object> resultMap = searchJiraByJql(param);

        if (resultMap != null)
        {
            resultList = parseJiraSearchMap(resultMap);
        }

        return resultList;
    }

    private void parseSvnLog(HashMap<String, String> map, String revision, String svnLog)
    {
        try
        {
            String jiraId = map.get("key");
            String[] arData = StringUtils.tokenizeToStringArray(svnLog, "\n");
            if (arData != null && arData.length > 0)
            {
                String patchFileList = "";
                for (String data : arData)
                {
                    if (data.startsWith("M ") || data.startsWith("A "))
                    {
                        String svnLogType = ""; // M: Merge, A: Append
                        if (data.startsWith("M "))
                        {
                            svnLogType = "M";
                        }
                        else if (data.startsWith("A "))
                        {
                            svnLogType = "A";
                        }
                        else
                        {
                            System.err.println("================== else ======================");
                        }

                        String filePath = data.substring(2);
                        String fileName = data.substring(data.lastIndexOf("/")+1);

                        if (patchFileList.length() > 0)
                            patchFileList += "\n";

                        patchFileList += filePath;

                        exportSvnFile(revision, SVN_URL + filePath);

                        try
                        {
                            // src copy
                            String src = "./temp/" + fileName;
                            String dest = "./output/" + jiraId + "/" + filePath;

                            debug("[cmd] src copy " + src + " to " + dest + "\n");
                            FileUtils.copyFile(new File(src), new File(dest));
                            FileUtils.forceDelete(new File(src));

                            if ("Y".equals(exportDiffFile) && revisionDiffVersion.length() > 0 && "M".equals(svnLogType))
                            {
                                // diff file copy
                                String diffFileName = fileName + ".rev." + revisionDiffVersion + "-" + revision + ".diff";
                                diffSvnFile(revisionDiffVersion, SVN_URL + filePath, diffFileName);

                                src = "./temp/" + diffFileName;
                                dest = "./output/" + jiraId + "/" + filePath.substring(0, filePath.lastIndexOf("/"))
                                                + "/" + diffFileName;

                                debug("[cmd] diff copy " + src + " to " + dest + "\n");
                                FileUtils.copyFile(new File(src), new File(dest));
                                FileUtils.forceDelete(new File(src));

                                // svn log history (시작 revision부터 변경 revision까지 변경사항)
                                String diffHistoryFileName = fileName + ".rev." + revisionDiffVersion + "-" + revision + ".diff-history.log";
                                executeSvnLogFile(revision, SVN_URL + filePath, diffHistoryFileName);

                                src = "./temp/" + diffHistoryFileName;
                                dest = "./output/" + jiraId + "/" + filePath.substring(0, filePath.lastIndexOf("/"))
                                                + "/" + diffHistoryFileName;

                                debug("[cmd] diff history copy " + src + " to " + dest + "\n");
                                FileUtils.copyFile(new File(src), new File(dest));
                                FileUtils.forceDelete(new File(src));
                            }
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                            debug(e.getMessage());
                        }
                    }
                }

                map.put("patchFileList", patchFileList);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            debug(e.getMessage());
        }
        //exportSvnFile(revision, "");
    }

    private String executeSvnLog(String revision)
    {
        String command = SVN_LOG_CMD + " " + revision + " " + SVN_URL;
        return executeCommand(command);
    }

    private String executeSvnLogFile(String revision, String fileUrl, String diffHistoryFileName)
    {
        String command = "cmd /c cd " + System.getProperty("user.dir") + "/temp && " + SVN_LOG_CMD + " " + revisionDiffVersion + ":" + revision + " " + fileUrl + " > " + diffHistoryFileName;
        return executeCommand(command);
    }

    private String exportSvnFile(String revision, String fileUrl)
    {
        String command = SVN_EXPORT_CMD + " -r " + revision + " " + fileUrl + " ./temp";
        return executeCommand(command);
    }

    private String diffSvnFile(String diffVersion, String fileUrl, String diffFileName)
    {
        //String command = SVN_DIFF_CMD + " -r " + diffVersion + " " + fileUrl;
        String command = "cmd /c cd " + System.getProperty("user.dir") + "/temp && " + SVN_DIFF_CMD + " -r " + diffVersion + " " + fileUrl + " > " + diffFileName;
        return executeCommand(command);
    }

    private List<HttpMessageConverter<?>> getMessageConverters() {
        List<HttpMessageConverter<?>> converters =
                        new ArrayList<HttpMessageConverter<?>>();
        converters.add(new MappingJackson2HttpMessageConverter());
        return converters;
    }

    public void byCommonsExec(String[] command) throws IOException,InterruptedException {
        DefaultExecutor executor = new DefaultExecutor();
        CommandLine cmdLine = CommandLine.parse(command[0]);
        for (int i=1, n=command.length ; i<n ; i++ ) {
            cmdLine.addArgument(command[i]);
        }
        executor.execute(cmdLine);
    }

    private String executeCommand(String command) {
        StringBuffer output = new StringBuffer();

        Process p;
        try
        {
            debug("[cmd] " + command);
            p = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), "MS949"));

            String line = "";
            while ((line = reader.readLine())!= null)
            {
                output.append(line + "\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
            debug(e.getMessage());
        }

        return output.toString();
    }

    public void createExcel(List<HashMap<String, String>> list)
    {
        ArrayList<String> columnList = new ArrayList<String>();

        if (list != null && list.size() > 0)
        {
            columnList.add("Key");
            columnList.add("Summary");
            columnList.add("Priority");
            columnList.add("Description");
            columnList.add("패치중요도");
            columnList.add("SVN Rev.No");
            columnList.add("처리내역");
            columnList.add("메뉴");
            columnList.add("패치 파일 리스트");
        }

        HSSFSheet sheet = workbook.createSheet("패치목록");
        HSSFRow row = null; // 행
        HSSFCell cell = null; // 셀

        HSSFFont font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setFontName("맑은 고딕");

        CellStyle alignLeftStyle = workbook.createCellStyle();
        alignLeftStyle.setAlignment(CellStyle.ALIGN_LEFT);
        alignLeftStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        alignLeftStyle.setFont(font);
        CellStyle alignRightStyle = workbook.createCellStyle();
        alignRightStyle.setAlignment(CellStyle.ALIGN_RIGHT);
        alignRightStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        alignRightStyle.setFont(font);
        CellStyle alignCenterStyle = workbook.createCellStyle();
        alignCenterStyle.setAlignment(CellStyle.ALIGN_CENTER);
        alignCenterStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        alignCenterStyle.setFont(font);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setAlignment(CellStyle.ALIGN_CENTER);
        headerStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        headerStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);
        headerStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);
        headerStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);
        headerStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN);
        headerStyle.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
        headerStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        headerStyle.setFont(font);

        CellStyle bodyStyle = workbook.createCellStyle();
        bodyStyle.setAlignment(CellStyle.ALIGN_LEFT);
        bodyStyle.setVerticalAlignment(CellStyle.VERTICAL_TOP);
        bodyStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);
        bodyStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);
        bodyStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);
        bodyStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN);
        bodyStyle.setFillForegroundColor(HSSFColor.WHITE.index);
        bodyStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        bodyStyle.setWrapText(true);
        bodyStyle.setFont(font);

        if (list != null && list.size() > 0)
        {
            int i = 0;
            // header
            row = sheet.createRow((short) i);
            row.setHeight((short) 1024);
            if (columnList != null && columnList.size() > 0)
            {
                for (int j = 0; j < columnList.size(); j++)
                {
                    cell = row.createCell(j);
                    cell.setCellValue(columnList.get(j));
                    cell.setCellStyle(headerStyle);
                }
            }

            i++;
            for (Map<String, String> mapObject : list)
            {
                row = sheet.createRow((short) i);
                row.setHeight((short) -1);
                i++;
                if (columnList != null && columnList.size() > 0)
                {
                    for (int j = 0; j < columnList.size(); j++)
                    {
                        cell = row.createCell(j);
                        String columnName = columnList.get(j);
                        String value = "";

                        switch (j)
                        {
                            case 0:
                                value = mapObject.get("key");
                                //sheet.setColumnWidth(j, 2800);
                                sheet.autoSizeColumn((short) j);
                                sheet.setColumnWidth(j, (sheet.getColumnWidth(j)) + 512);
                                break;
                            case 1:
                                value = mapObject.get("summary");
                                sheet.setColumnWidth(j, 2800*3);
                                break;
                            case 2:
                                value = mapObject.get("priority");
                                sheet.autoSizeColumn((short) j);
                                sheet.setColumnWidth(j, (sheet.getColumnWidth(j)) + 512);
                                break;
                            case 3:
                                value = mapObject.get("description");
                                sheet.setColumnWidth(j, 2800*4);
                                break;
                            case 4:
                                value = mapObject.get("patchImportance");
                                sheet.autoSizeColumn((short) j);
                                sheet.setColumnWidth(j, (sheet.getColumnWidth(j)) + 512);
                                break;
                            case 5:
                                value = mapObject.get("revision");
                                sheet.autoSizeColumn((short) j);
                                sheet.setColumnWidth(j, (sheet.getColumnWidth(j)) + 512);
                                break;
                            case 6:
                                value = mapObject.get("responseHistory");
                                sheet.setColumnWidth(j, 2800*5);
                                break;
                            case 7:
                                value = mapObject.get("menu");
                                sheet.setColumnWidth(j, 2800*4);
                                break;
                            case 8:
                                value = mapObject.get("patchFileList");
                                sheet.setColumnWidth(j, 2800*6);
                                break;
                        }

                        cell.setCellValue(value);
                        cell.setCellStyle(bodyStyle);
                    }
                }
            }
        }

        try
        {
            debug("[save as excel] " + "./output/" + excelFilename);
            FileOutputStream fos = new FileOutputStream("./output/" + excelFilename);
            workbook.write(fos);
            fos.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            debug(e.getMessage());
        }

    }

    private void debug(String str)
    {
        System.err.println(str);

        try
        {
            FileUtils.writeStringToFile(new File(System.getProperty("user.dir") + "/logs/data_" + currDate + ".log"), str, true);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
