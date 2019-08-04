package kr.co.spectra.attic.tools.convert.customize.share;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.*;

public class FileDownloader {
    public static void download(String username, String password, String fileUrl, String saveFilePath) throws IOException {

        Authenticator.setDefault(new SVNAuthenticator(username, password));

        File file = new File(saveFilePath);
        URL url = new URL(fileUrl);

        FileUtils.copyURLToFile(url, file);
    }

    public static void main(String[] args) throws Exception{
        //download("", "");
    }
}

class SVNAuthenticator extends Authenticator {
    private String username;
    private String password;

    public SVNAuthenticator(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(username, password.toCharArray());
    }
}