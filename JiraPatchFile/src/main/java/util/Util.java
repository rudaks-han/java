package util;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.utils.DateUtils;

import java.io.*;
import java.util.Date;

public class Util
{
    public static String executeCommand(String [] commands)
    {
        StringBuffer output = new StringBuffer();

        Process p;
        try
        {
            String str = "";
            for (String command : commands)
            {
                if (str.length() > 0)
                    str += " ";
                str += command;
            }
            debug("[cmd] " + str);
            p = Runtime.getRuntime().exec(commands);
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


    public static void debug(String str)
    {
        System.out.println(str);

        try
        {
            FileUtils.writeStringToFile(new File(System.getProperty("user.dir") + "/logs/data_" + currDate() + ".log"), str, true);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static String currDate()
    {
        return DateUtils.formatDate(new Date(), "yyyyMMddHHmmss");
    }

    public static String readString(String str)
    {
        return readString(str, "8859_1", "UTF-8");
    }

    public static String readString(String str, String sourceEncoding, String targetEncoding)
    {
        String result = null;

        try
        {
            if (str != null && str.length() > 0)
            {
                result = new String(str.getBytes(sourceEncoding), targetEncoding);
            }
            else
            {
                Util.debug("[error] " + str + " is null.");
            }
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }

        return result;
    }
}
