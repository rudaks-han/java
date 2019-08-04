package kr.co.spectra.attic.tools.convert.customize;

import kr.co.spectra.attic.tools.convert.customize.jpa.jpo.FileDiffJpo;
import kr.co.spectra.attic.tools.convert.customize.logic.FileDiffLogic;
import kr.co.spectra.attic.tools.convert.customize.share.FileDownloader;
import kr.co.spectra.attic.tools.convert.customize.share.Util;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Component
public class FileDiffComparer {

    private ConfigProperty configProperty;

    @Autowired
    private FileDiffLogic fileDiffLogic;

    public FileDiffComparer(ConfigProperty configProperty) {
        this.configProperty = configProperty;
    }

    public void setConfigProperty(ConfigProperty configProperty) {
        this.configProperty = configProperty;
    }

    public void execute(String product, String eerVersion, String siteName, String filename) {

        // 1. check EER file이 존재하는지 확인
        // 2. SVN에서 사이트 소스의 파일을 다운로드 받는다.
        // 3. 파일을 diff한다.
        // 4. DB에 정보를 저장한다.

        Util.printFooterPrettyLog(filename);

        product = product.toLowerCase();
        boolean existEERFile = checkIfExistEERFile(product, eerVersion, filename);

        // Diff 대상인 EER소스가 없다면 진행하지 않는다.
        if (!existEERFile) {
            Util.printLog("EER file not exist: " + product + "_" + eerVersion);
            return;
        }

        boolean existSvnFile = downloadSvnFile(filename, siteName);

        if (existSvnFile) {
            saveDiffFileAndWriteToDB(filename, siteName, product, eerVersion);
        }
    }

    private boolean checkIfExistEERFile(String product, String eerVersion, String filename) {
        String eerFileLocation = getEERFileLocation(product, eerVersion); // /Users/rudaks/_WORK/_SVN/SuperTalk/supertalk_tags/eer/tags/1.7.0
        String targetFilePath = getPackagePath(filename) + "/" + filename;
        String eerFilePath = eerFileLocation + targetFilePath;

        if (!new File(eerFilePath).exists()) {
            return false;
        }

        return true;
    }

    private boolean downloadSvnFile(String filename, String siteName) {
        boolean flag = false;

        String fileUrl = getSvnFileUrl(siteName, filename);
        String saveFilePath = getSvnDiffSaveFilename(siteName, filename); //configProperty.getSvnDiffSavePath() + "/" + filename + "/" + siteName + "/" + filename;

        try {
            if (!new File(saveFilePath).exists()) {
                FileDownloader.download(configProperty.getSvnUsername(), configProperty.getSvnPassword(), fileUrl, saveFilePath);
                Util.printLog("downloaded svn file: " + saveFilePath);
            }
            else {
                Util.printLog("svn file is not downloaded. The file already exists: ");
            }

            flag = true;
        } catch (IOException e) {
            Util.printLog("svn file not found: " + fileUrl);
        }

        return flag;
    }

    private void saveDiffFileAndWriteToDB(String filename, String siteName, String product, String eerVersion) {

        String saveFileRootPath = configProperty.getSvnDiffSavePath() + "/" + filename;
        String saveFilePath =  saveFileRootPath + "/" + siteName + "/" + filename;
        String eerFileLocation = getEERFileLocation(product, eerVersion); // /Users/rudaks/_WORK/_SVN/SuperTalk/supertalk_tags/eer/tags/1.7.0

        String targetFilePath = getPackagePath(filename) + "/" + filename;
        String eerFilePath = eerFileLocation + targetFilePath;

        String result = executeDiffCmd(eerFilePath, saveFilePath);

        boolean flag = writeDiffFile(result, saveFileRootPath + "/" + siteName, filename);

        if (flag) {
            int lineCount = 0;
            int addLineCount = 0;
            int removeLineCount = 0;
            String fileContent = "";

            try {
                String compareFilename = saveFileRootPath + "/" + siteName + "/" + filename;
                String diffFileName = compareFilename + ".diff";
                File diffFile = new File(diffFileName);
                List<String> lines = FileUtils.readLines(diffFile, "UTF-8");

                fileContent = FileUtils.readFileToString(diffFile, "UTF-8");
                for (String line : lines) {
                    if (line.startsWith("+")) {
                        addLineCount++;
                    } else if (line.startsWith("-")) {
                        removeLineCount++;
                    }
                    lineCount++;
                }

                String changeFilename = "diff 라인수 (추가-"+addLineCount+", 삭제-" + removeLineCount + ")";
                FileUtils.writeStringToFile(new File(saveFileRootPath + "/" + siteName + "/" + changeFilename), "No Data", "UTF-8");

                String eerFilename = getEERSaveFilename(siteName, saveFileRootPath, filename);

                writeToDiffMergeShell(saveFileRootPath, siteName, eerFilename, compareFilename);

                copyEERSource(eerFilePath, eerFilename);

            } catch (IOException e) {
                Util.printLog("diff file not found: " + filename);
            }

            FileDiffJpo fileDiffJpo = new FileDiffJpo(siteName, product, eerVersion, filename, getPackagePath(filename), lineCount, addLineCount, removeLineCount, fileContent);
            fileDiffLogic.create(fileDiffJpo);
        }
    }

    private String executeDiffCmd(String sourceFile, String targetFile) {
        String [] commands = {"diff", "-uw", sourceFile, targetFile}; // -u: +로 표시, w: 공백무시
        return Util.executeCommand(commands);
    }

    private void writeToDiffMergeShell(String saveFileRootPath, String siteName, String eerFilename, String compareFilename) {
        try {
            String content = "";
            content = "diffmerge " + eerFilename + " " + compareFilename;

            File file = new File(saveFileRootPath + "/" + siteName + "/run-diff-merge.sh");
            FileUtils.writeStringToFile(file, content, "UTF-8");
            file.setExecutable(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean writeDiffFile(String result, String filepath, String filename) {
        boolean success = false;

        try {
            if (result != null && result.length() > 0) {
                // diff파일 쓰기

                String diffFileName = filepath + "/" + filename + ".diff";

                File file = new File(diffFileName);
                FileUtils.writeStringToFile(file, result, "UTF-8");

                success = true;
            } else {
                String changeFilename = "변경사항 없음";
                FileUtils.writeStringToFile(new File(filepath + "/" + changeFilename), "No Data", "UTF-8");
                success = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return success;
    }

    private boolean copyEERSource(String source, String dest) {
        boolean existEERFile = false;
        // EER 소스 복사
        try {
            FileUtils.copyFile(new File(source), new File(dest));
            existEERFile = true;
            Util.printLog("EER file copied to : " + dest);

        } catch (Exception e) {
            Util.printLog("EER file not found: " + source);
        }

        return existEERFile;
    }

    private String getSvnDiffSaveFilename(String siteName, String filename) {
        return configProperty.getSvnDiffSavePath() + "/" + filename + "/" + siteName + "/" + filename;
    }

    private String getEERSaveFilename(String siteName, String saveFileRootPath, String filename) {
        return saveFileRootPath + "/" + siteName + "/원본_" + filename;
    }

    private String getEERFileLocation(String product, String eerVersion) {
        // EEC 1.1은 ECC의 3.1에서 소스를 찾아야 한다.

        if ("eer".equals(product)) {
            if ("1.0.0".equals(eerVersion)) {
                product = "ecc";
                eerVersion = "3.0.0";
            } else if ("1.0.1".equals(eerVersion)) {
                product = "ecc";
                eerVersion = "3.0.1";
            } else if ("1.1.0".equals(eerVersion)) {
                product = "ecc";
                eerVersion = "3.1.0";
            } else if ("1.1.1".equals(eerVersion)) {
                product = "ecc";
                eerVersion = "3.1.1";
            } else if ("1.1.1".equals(eerVersion)) {
                product = "ecc";
                eerVersion = "3.1.1";
            }
        }

        String result = configProperty.getSvnPath() + "/" + product + "/tags" + "/" + eerVersion;

        return result;
    }

    private String getPackagePath(String filename) {

        List<ConfigProperty.FilenameToPackage> filenameToPackages = configProperty.getFilenameToPackages();
        for (ConfigProperty.FilenameToPackage filenameToPackage : filenameToPackages) {
            if (filename.equals(filenameToPackage.getFilename())) {
                return filenameToPackage.getFilepath();
            }
        }

        return null;
    }

    private String getSvnSiteName(String siteName) {

        List<ConfigProperty.SvnSiteName> svnSiteNames = configProperty.getSvnSiteNames();
        for (ConfigProperty.SvnSiteName svnSiteName: svnSiteNames) {
            if (siteName.equals(svnSiteName.getSiteName())) {
                return svnSiteName.getSvnName();
            }
        }

        return siteName;
    }

    private String getSvnFileUrl(String siteName, String filename) {
        return "https://subversion.spectra.co.kr/view/*checkout*/" + getSvnSiteName(siteName) + getSvnFilePath(siteName) + getPackagePath(filename) + "/" + filename + "?root=TS_ECC";
    }

    private String getSvnFilePath(String siteName) {
        List<ConfigProperty.SvnRepositoryPath> svnRepositoryPaths = configProperty.getSvnRepositoryPaths();
        for (ConfigProperty.SvnRepositoryPath svnRepositoryPath: svnRepositoryPaths) {
            if (siteName.equals(svnRepositoryPath.getSiteName())) {
                return svnRepositoryPath.getSvnPath();
            }
        }

        return "/trunk/enomixSrc";
    }
}
