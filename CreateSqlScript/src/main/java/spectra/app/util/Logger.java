package spectra.app.util;

import org.apache.commons.io.FileUtils;

import java.io.File;

public class Logger
{
    private String allString = "";

    private boolean showLogTime = true;

    public void println(String str)
    {
        if (showLogTime)
        {
            str = "[" + DateUtil.getCurrDate("yyyy-MM-hh MM:hh:ss") + "] " + str;
        }

        System.out.println(str);
        allString += str + "\n";
    }

    public void print(String str)
    {
        if (showLogTime)
        {
            str = "[" + DateUtil.getCurrDate("yyyy-MM-hh MM:hh:ss") + "] " + str;
        }

        System.out.print(str);
        allString += str;
    }

    public void append(String str)
    {
        System.out.print(str);
        allString += str;
    }

    public void appendln(String str)
    {
        System.out.println(str);
        allString += str + "\n";
    }

    public void saveAsFile(String path)
    {
        try
        {
            FileUtils.writeStringToFile(new File(path), allString, "UTF-8");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
