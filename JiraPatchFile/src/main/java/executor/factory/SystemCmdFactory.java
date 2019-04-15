package executor.factory;

import executor.LinuxCmdExecutorExecutor;
import executor.SystemCmdExecutor;
import executor.WindowsCmdExecutor;
import util.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class SystemCmdFactory
{
    public static SystemCmdExecutor systemCmdExecutor;

    public static SystemCmdExecutor getInstance()
    {
        if (systemCmdExecutor == null)
        {
            if ("/".equals(File.separator))
            {
                systemCmdExecutor = new LinuxCmdExecutorExecutor();
            }
            else
            {
                systemCmdExecutor = new WindowsCmdExecutor();
            }
        }
        return systemCmdExecutor;
    }
}
