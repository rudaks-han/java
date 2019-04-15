package executor;

import util.Util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class WindowsCmdExecutor implements SystemCmdExecutor
{
    public String executeSvnLogFile(String revision, String fileUrl, String diffHistoryFileName, String svnLogCmd, String revisionDiffVersion)
    {
        String command = "cmd /c cd " + System.getProperty("user.dir") + "/temp && " + svnLogCmd + " " + revisionDiffVersion + ":" + revision + " " + fileUrl + " > " + diffHistoryFileName;
        return executeCommand(command);
    }

    public String diffSvnFile(String diffVersion, String fileUrl, String diffFileName, String svnDiffCmd)
    {
        String command = "cmd /c cd " + System.getProperty("user.dir") + "/temp && " + svnDiffCmd + " -r " + diffVersion + " " + fileUrl + " > " + diffFileName;
        return executeCommand(command);
    }

    public String executeCommand(String command)
    {
        String [] commands = {command};
        return Util.executeCommand(commands);
    }
}
