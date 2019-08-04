package kr.co.spectra.attic.tools.convert;

import kr.co.spectra.attic.tools.convert.customize.ConfigProperty;
import kr.co.spectra.attic.tools.convert.customize.share.Util;
import kr.co.spectra.attic.tools.convert.customize.jpa.jpo.EERFileJpo;
import kr.co.spectra.attic.tools.convert.customize.logic.EERFileLogic;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RunWith(SpringJUnit4ClassRunner.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(false)
@ComponentScan
public class EERFileJpoTest {

    @Autowired
    private ConfigProperty configProperty;

    @Autowired
    private EERFileLogic eerFileLogic;

    @Test
    public void insertEERFile() {

        String sourceDir = configProperty.getSvnPath();
        //String sourceDir = "/Users/rudaks/_WORK/_SVN/SuperTalk/supertalk_tags/eer/tags/1.2.0";
        String regexp = ".*(ecc|eer)/tags/([0-9]\\.[0-9]\\.[0-9])(.*)";

        Collection<File> files = FileUtils.listFiles(new File(sourceDir), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);

        for (File file : files) {

            String filename = file.getName();
            String absolutePath = file.getAbsolutePath();
            long filesize = file.length();
            String fileExt = FilenameUtils.getExtension(filename);

            String eerVersion = "";
            String filePath = "";
            String product = "";
            int fileLineCount = 0;

            Pattern infoPattern = Pattern.compile(regexp);
            Matcher infoMatcher = infoPattern.matcher(absolutePath);

            boolean matched = false;
            if (infoMatcher.find()) {
                product = infoMatcher.group(1).toUpperCase();
                eerVersion = infoMatcher.group(2);
                filePath = infoMatcher.group(3);

                String mimeType = Util.getFileMimeType(absolutePath);

                if (mimeType.startsWith("text/"))
                    fileLineCount = Util.getFileLines(absolutePath);
                else
                    fileLineCount = -1;

                matched = true;
            }

            if (matched) {
                EERFileJpo eerFileJpo = new EERFileJpo(product, eerVersion, filename, filePath, filesize, fileExt, fileLineCount);
                eerFileLogic.create(eerFileJpo);
            }
        }


    }
}
