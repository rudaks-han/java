package service;

import executor.SystemCmdExecutor;
import executor.factory.SystemCmdFactory;
import org.apache.commons.io.FileUtils;
import org.springframework.util.StringUtils;
import util.Util;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class SvnService
{
    private String svnUrl;
    private String svnLogCmd = "svn log -v -r";
    private String svnExportCmd = "svn export";
    private String svnDiffCmd = "svn diff";

    private String tempDir;
    private String outputDir;

    private SystemCmdExecutor systemCmdExecutor;

    public SvnService(String svnUrl, String tempDir, String outputDir)
    {
        this.systemCmdExecutor = SystemCmdFactory.getInstance();
        this.svnUrl = svnUrl;
        this.tempDir = tempDir;
        this.outputDir = outputDir;
    }

    public void executeSvnLogAndParse(List<HashMap<String, String>> jiraPatchList, String exportDiffFile, String baseRevision)
    {
        for (HashMap map : jiraPatchList)
        {
            String revision = (String) map.get("revision"); // Jira의 SVN Rev.No

            String key = (String) map.get("key"); // EER-1234
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

                        parseSvnLog(map, _revision, svnLog, exportDiffFile, baseRevision);
                    }
                }
            }
        }
    }

    private String executeSvnLog(String revision)
    {
        String command = svnLogCmd + " " + revision + " " + svnUrl;
        return systemCmdExecutor.executeCommand(command);
    }

    private void parseSvnLog(HashMap<String, String> map, String revision, String svnLog, String exportDiffFile, String baseRevision)
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
                        String svnLogType = getSvnLogType(data); // M: Merge, A: Append

                        String filePath = getFilePathFromSvnLog(data);
                        String fileName = getFileNameFromSvnLog(data);

                        if (patchFileList.length() > 0)
                            patchFileList += "\n";

                        patchFileList += filePath;

                        exportSvnFile(revision, svnUrl + filePath);

                        // src copy
                        String src = tempDir + "/" + fileName;
                        String dest = outputDir + "/" + jiraId + "/" + filePath;

                        Util.debug("[copy] src copy " + src + " to " + dest + "\n");
                        FileUtils.copyFile(new File(src), new File(dest));
                        FileUtils.forceDelete(new File(src));

                        if ("Y".equals(exportDiffFile) && baseRevision.length() > 0 && "M".equals(svnLogType))
                        {
                            // base revision과 비교
                            executeSvnDiff(fileName, filePath, jiraId, baseRevision, revision);

                            executeSvnLog(fileName, filePath, jiraId, baseRevision, revision);

                            // 이전버전과 비교
                            baseRevision = (Integer.parseInt(revision) - 1) + "";
                            executeSvnDiff(fileName, filePath, jiraId, baseRevision, revision);

                            executeSvnLog(fileName, filePath, jiraId, baseRevision, revision);
                        }

                    }
                }

                map.put("patchFileList", patchFileList);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Util.debug(e.getMessage());
        }
        //exportSvnFile(revision, "");
    }

    private String exportSvnFile(String revision, String fileUrl)
    {
        String filename = fileUrl.substring(fileUrl.lastIndexOf("/")+1);
        String command = "cd " + tempDir + " && " + svnExportCmd + " -r " + revision + " " + fileUrl + " " + " " + filename;
        return systemCmdExecutor.executeCommand(command);
    }
    
    private void executeSvnDiff(String fileName, String filePath, String jiraId, String baseRevision, String revision)
    {
        try
        {
            // diff file copy
            String diffFileName = fileName + ".rev." + baseRevision + "-" + revision + ".diff";
            systemCmdExecutor.diffSvnFile(baseRevision, svnUrl + filePath, diffFileName, svnDiffCmd);

            String src = tempDir + "/" + diffFileName;
            String dest = outputDir + "/" + jiraId + "/" + filePath.substring(0, filePath.lastIndexOf("/"))
                    + "/" + diffFileName;

            Util.debug("[copy] diff copy " + src + " to " + dest + "\n");
            FileUtils.copyFile(new File(src), new File(dest));
            FileUtils.forceDelete(new File(src));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Util.debug(e.getMessage());
        }
    }

    private void executeSvnLog(String fileName, String filePath, String jiraId, String baseRevision, String revision)
    {
        try
        {
            // svn log history (시작 revision부터 변경 revision까지 변경사항)
            String diffHistoryFileName = fileName + ".rev." + baseRevision + "-" + revision + ".diff-history.log";
            systemCmdExecutor.executeSvnLogFile(revision, svnUrl + filePath, diffHistoryFileName, svnLogCmd, baseRevision);

            String src = tempDir + "/" + diffHistoryFileName;
            String dest = outputDir + "/" + jiraId + "/" + filePath.substring(0, filePath.lastIndexOf("/"))
                    + "/" + diffHistoryFileName;

            Util.debug("[copy] diff history copy " + src + " to " + dest + "\n");
            FileUtils.copyFile(new File(src), new File(dest));
            FileUtils.forceDelete(new File(src));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Util.debug(e.getMessage());
        }
    }

    private String getSvnLogType(String data)
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

        return svnLogType;
    }

    private String getFilePathFromSvnLog(String data)
    {
        return data.substring(2);
    }

    private String getFileNameFromSvnLog(String data)
    {
        return data.substring(data.lastIndexOf("/")+1);
    }
}
