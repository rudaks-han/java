package kr.co.spectra.attic.tools.convert;

import kr.co.spectra.attic.tools.convert.customize.*;
import kr.co.spectra.attic.tools.convert.customize.logic.CustomizeLogic;
import kr.co.spectra.attic.tools.convert.customize.logic.EERFileLogic;
import kr.co.spectra.attic.tools.convert.customize.model.CustomizeChangedFile;
import kr.co.spectra.attic.tools.convert.customize.model.CustomizeItem;
import kr.co.spectra.attic.tools.convert.customize.share.ExcelWriter;
import kr.co.spectra.attic.tools.convert.customize.share.Util;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.util.*;

@RunWith(SpringJUnit4ClassRunner.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(false)
@ComponentScan
public class WordToExcelConverterTest {

    @Autowired
    private ConfigProperty configProperty;

    @Autowired
    private CustomizeLogic customizeLogic;

    @Autowired
    private EERFileLogic eerFileLogic;

    @Test
    public void executeWordParseTest() {
        Collection<File> files = Util.listFiles(configProperty.getInputFilePath(), configProperty.getInputFileExtension());

        int totalCount = files.size();
        int loop = 1;

        for (File file : files) {
            if (file.getName().startsWith("~")) // 임시파일
                continue;

            String fileName = file.getName();
            String siteName = Util.getSiteNameInFilePath(file, configProperty.getInputFilePath());
            String eerFullVersion = Util.getEERVersionInPath(siteName);
            String product = eerFullVersion.split("_")[0];
            String eerVersion = eerFullVersion.split("_")[1];
            String outputFileName = Util.getOutputFilename(file, "xls");

            /*if (!"LGU_PLUS_EER_3.0.2".equals(siteName)) {
                continue;
            }*/

            Util.printPrettyLog("[" + loop + "/" + totalCount + "] " + siteName);

            WordExtractor wordExtractor = new WordExtractor(configProperty.getExcelColumns());
            List<HashMap<String, String>> dataList = wordExtractor.parseWordDocument(file.getAbsolutePath());

            if (configProperty.isWriteToExcel()) {
                ExcelWriter excelWriter = new ExcelWriter(siteName, configProperty.getExcelColumns(), dataList);
                excelWriter.write(configProperty.getOutputFilePath() + "/" + siteName + "/" + outputFileName);
            }

            if (configProperty.isWriteToDb()) {

                customizeLogic.createCustomizeItem(new CustomizeItem(fileName, siteName, product, eerVersion, dataList));

                CustomizeChangedFile customizeChangedFile = new CustomizeChangedFile();
                customizeChangedFile.setSiteName(siteName);
                customizeChangedFile.setProduct(product);
                customizeChangedFile.setEerVersion(eerVersion);
                customizeChangedFile.setFilename(fileName);
                customizeChangedFile.setDataList(dataList);
                customizeLogic.createCustomizeChangedFile(customizeChangedFile);
            }

            loop++;
        }


    }
}
