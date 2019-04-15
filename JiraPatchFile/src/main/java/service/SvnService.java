package service;

import executor.SystemCmdExecutor;
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

    private SystemCmdExecutor systemCmdExecutor;

    public SvnService(String svnUrl, SystemCmdExecutor systemCmdExecutor)
    {
        this.systemCmdExecutor = systemCmdExecutor;
        this.svnUrl = svnUrl;
    }

    public void executeSvnLogAndParse(List<HashMap<String, String>> jiraPatchList, String exportDiffFile, String revisionDiffVersion)
    {
        for (HashMap map : jiraPatchList)
        {
            String revision = (String) map.get("revision"); // Jira의 SVN Rev.No

                /*String key = (String) map.get("key"); // EER-1234
                String priority = (String) map.get("priority");
                String patchImportance = (String) map.get("patchImportance");
                String menu = (String) map.get("menu");
                String responseHistory = (String) map.get("responseHistory");
                String description = (String) map.get("description");*/

            if (revision != null && revision.length() > 0)
            {
                String [] arRevision = revision.split(",");
                if (arRevision != null && arRevision.length > 0)
                {
                    for (String _revision : arRevision)
                    {
                        _revision = _revision.trim();
                        String svnLog = executeSvnLog(_revision);

                        parseSvnLog(map, _revision, svnLog, exportDiffFile, revisionDiffVersion);
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

    private void parseSvnLog(HashMap<String, String> map, String revision, String svnLog, String exportDiffFile, String revisionDiffVersion)
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

                        exportSvnFile(revision, svnUrl + filePath);

                        try
                        {
                            // src copy
                            String src = System.getProperty("user.dir") + "/temp/" + fileName;
                            String dest = System.getProperty("user.dir") + "/output/" + jiraId + "/" + filePath;


                            Util.debug("[executor] src copy " + src + " to " + dest + "\n");
                            FileUtils.copyFile(new File(src), new File(dest));
                            FileUtils.forceDelete(new File(src));

                            if ("Y".equals(exportDiffFile) && revisionDiffVersion.length() > 0 && "M".equals(svnLogType))
                            {
                                // diff file copy
                                String diffFileName = fileName + ".rev." + revisionDiffVersion + "-" + revision + ".diff";
                                systemCmdExecutor.diffSvnFile(revisionDiffVersion, svnUrl + filePath, diffFileName, svnDiffCmd);

                                src = System.getProperty("user.dir") + "/temp/" + diffFileName;
                                dest = System.getProperty("user.dir") + "/output/" + jiraId + "/" + filePath.substring(0, filePath.lastIndexOf("/"))
                                        + "/" + diffFileName;

                                Util.debug("[executor] diff copy " + src + " to " + dest + "\n");
                                FileUtils.copyFile(new File(src), new File(dest));
                                FileUtils.forceDelete(new File(src));

                                // svn log history (시작 revision부터 변경 revision까지 변경사항)
                                String diffHistoryFileName = fileName + ".rev." + revisionDiffVersion + "-" + revision + ".diff-history.log";
                                systemCmdExecutor.executeSvnLogFile(revision, svnUrl + filePath, diffHistoryFileName, svnLogCmd, revisionDiffVersion);

                                src = System.getProperty("user.dir") + "/temp/" + diffHistoryFileName;
                                dest = System.getProperty("user.dir") + "/output/" + jiraId + "/" + filePath.substring(0, filePath.lastIndexOf("/"))
                                        + "/" + diffHistoryFileName;

                                Util.debug("[executor] diff history copy " + src + " to " + dest + "\n");
                                FileUtils.copyFile(new File(src), new File(dest));
                                FileUtils.forceDelete(new File(src));
                            }
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                            Util.debug(e.getMessage());
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
        String command = "cd " + System.getProperty("user.dir") + "/temp && " + svnExportCmd + " -r " + revision + " " + fileUrl + " " + " " + filename;
        return systemCmdExecutor.executeCommand(command);
    }
}
