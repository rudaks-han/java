package kr.co.spectra.attic.tools.convert;

import kr.co.spectra.attic.tools.convert.customize.ConfigProperty;
import kr.co.spectra.attic.tools.convert.customize.FileDiffComparer;
import kr.co.spectra.attic.tools.convert.customize.jpa.jpo.EERFileJpo;
import kr.co.spectra.attic.tools.convert.customize.logic.CustomizeChangedFileLogic;
import kr.co.spectra.attic.tools.convert.customize.logic.EERFileLogic;
import kr.co.spectra.attic.tools.convert.customize.model.ItemCount;
import kr.co.spectra.attic.tools.convert.customize.share.Util;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(false)
@ComponentScan
public class SvnDiffTest {
    @Autowired
    private ConfigProperty configProperty;

    @Autowired
    private CustomizeChangedFileLogic customizeChangedFileLogic;

    @Autowired
    private EERFileLogic eerFileLogic;

    private List<String> diffFileList;

    @Autowired
    private FileDiffComparer fileDiffComparer;

    @Before
    public void setup() {
        fileDiffComparer.setConfigProperty(configProperty);

        getDiffFileList();
    }

    public void getDiffFileList() {
        diffFileList = new ArrayList<>();

        List<ItemCount> list  = customizeChangedFileLogic.findByFilenameGroupBy();
        for (int i=0; i<list.size(); i++) {
            ItemCount itemCount = (ItemCount) list.get(i);
            System.out.println(itemCount.getItem());
            diffFileList.add(itemCount.getItem());
        }
    }

    @Test
    public void diffTest() {
        Collection<File> files = Util.listFiles(configProperty.getInputFilePath(), configProperty.getInputFileExtension());

        String _siteName = "";
        int loop = 1;
        int totalCount = files.size();

        for (File file : files) {
            if (file.getName().startsWith("~")) // 임시파일
                continue;

            String siteName = Util.getSiteNameInFilePath(file, configProperty.getInputFilePath());
            String eerFullVersion = Util.getEERVersionInPath(siteName);
            String product = eerFullVersion.split("_")[0];
            String eerVersion = eerFullVersion.split("_")[1];

            if (_siteName.equals(siteName))
                continue;

            Util.printPrettyLog(siteName);

            boolean testFlag = false;
            if (testFlag) {
                fileDiffComparer.execute(product, eerVersion, siteName, "ticketWindow.js");
            } else {

                /*for (String filename : diffFileList) {
                    List<EERFileJpo> eerFileJpos = eerFileLogic.findByFilenameAndProductAndEerVersion(filename, product, eerVersion);
                    System.out.println(">> filename : " + filename);
                    for (EERFileJpo eerFileJpo : eerFileJpos) {
                        System.out.println("filepath : " + eerFileJpo.getFilepath());
                    }
                }*/

                List<ConfigProperty.FilenameToPackage> filenameToPackages = configProperty.getFilenameToPackages();
                for (ConfigProperty.FilenameToPackage filenameToPackage: filenameToPackages) {
                    String filename = filenameToPackage.getFilename();

                    fileDiffComparer.execute(product, eerVersion, siteName, filename);
                }
            }

            _siteName = siteName;
            loop++;
        }
    }
}
