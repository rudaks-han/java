package kr.co.spectra.attic.tools.convert.customize.share;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.net.URLConnection;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {
    public static String getEERVersionInPath(String siteName) {
        String eerVersion = "";
        String product = "";

        String regexp = ".*(EER|ECC)_([1-4]\\.[0-9]\\.[0-9]).*";

        Pattern infoPattern = Pattern.compile(regexp);
        Matcher infoMatcher = infoPattern.matcher(siteName);

        if (infoMatcher.find()) {
            product = infoMatcher.group(1);
            eerVersion = infoMatcher.group(2);
        }

        return product + "_" + eerVersion;
    }

    public static String getSiteNameInFilePath(File file, String inputFilePath) {
        String temp = file.getAbsolutePath().replaceAll(inputFilePath, ""); // /칸투칸_OnDemand_EER_1.7/trunk/doc/kantukan_501_커스터마이징내역서.docx
        String [] arTemp = temp.split("/");
        String siteName = arTemp[1];

        return siteName;
    }

    public static void printPrettyLog(String str) {
        System.out.println(StringUtils.repeat("#", 100));
        System.out.println(StringUtils.center(str, 100));
        System.out.println(StringUtils.repeat("#", 100));
    }

    public static void printFooterPrettyLog(String str) {
        System.out.println(StringUtils.center(str, 100));
        System.out.println(StringUtils.repeat("#", 100));
    }

    public static void printHeaderPrettyLog(String str) {
        System.out.println(StringUtils.repeat("#", 100));
        System.out.println(StringUtils.center(str, 100));
    }

    public static void printLog(String str) {
        System.out.println("# " + str);
    }

    public static String getFileNameByRegExp(String value) {
        String filename = "";

        String regexp = "[a-zA-Z0-9-_.]*\\.[a-zA-Z]*";

        Pattern infoPattern = Pattern.compile(regexp);
        Matcher infoMatcher = infoPattern.matcher(value);
        while (infoMatcher.find()){
            filename = infoMatcher.group();
        }

        return filename;
    }

    public static int getFileLines(String filename) {
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(filename));
            byte[] c = new byte[1024];
            int count = 0;
            int readChars = 0;
            boolean empty = true;
            while ((readChars = is.read(c)) != -1) {
                empty = false;
                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n') {
                        ++count;
                    }
                }
            }
            return (count == 0 && !empty) ? 1 : count;
        } catch (IOException e) {
            return -1;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {

                }
            }
        }
    }

    public static String getFileMimeType(String filename) {
        String mimeType = URLConnection.guessContentTypeFromName(filename);

        if (mimeType == null)
            return "";

        return mimeType;
    }

    public static Collection<File> listFiles(String path, String extension) {
        Util.printLog("reading directory: " + path);

        String [] extensions = extension.split(",");
        return FileUtils.listFiles(new File(path), extensions, true);
    }


    public static String getOutputFilename(File file, String extension) {
        return file.getName().substring(0, file.getName().lastIndexOf(".")) + "." + extension;
    }

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
            //printLog("[cmd] " + str);
            p = Runtime.getRuntime().exec(commands);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));

            String line = "";
            while ((line = reader.readLine())!= null)
            {
                output.append(line + "\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
            printLog(e.getMessage());
        }

        return output.toString();
    }

    public static void main(String[] args) {
        System.out.println(getFileMimeType("/Users/rudaks/temp/한경만.jpeg"));
    }
}
