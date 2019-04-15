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
        //String command = SVN_DIFF_CMD + " -r " + diffVersion + " " + fileUrl;
        String command = "cmd /c cd " + System.getProperty("user.dir") + "/temp && " + svnDiffCmd + " -r " + diffVersion + " " + fileUrl + " > " + diffFileName;
        return executeCommand(command);
    }

    public String executeCommand(String command)
    {
        String [] commands = {"bash", "-c", command};
        return Util.executeCommand(commands);

     /*   StringBuffer output = new StringBuffer();

        Process p;
        try
        {
            Util.debug("[executor] " + command);
            p = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), "MS949"));

            String line = "";
            while ((line = reader.readLine())!= null)
            {
                output.append(line + "\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
            Util.debug(e.getMessage());
        }

        return output.toString();*/
    }
}
