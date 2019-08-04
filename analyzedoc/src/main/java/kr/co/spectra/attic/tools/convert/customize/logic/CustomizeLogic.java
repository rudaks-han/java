package kr.co.spectra.attic.tools.convert.customize.logic;

import kr.co.spectra.attic.tools.convert.customize.share.Util;
import kr.co.spectra.attic.tools.convert.customize.jpa.CustomizeChangedFileJpaStore;
import kr.co.spectra.attic.tools.convert.customize.jpa.CustomizeItemJpaStore;
import kr.co.spectra.attic.tools.convert.customize.jpa.EERFileJpaStore;
import kr.co.spectra.attic.tools.convert.customize.jpa.jpo.CustomizeChangeFileJpo;
import kr.co.spectra.attic.tools.convert.customize.jpa.jpo.CustomizeItemJpo;
import kr.co.spectra.attic.tools.convert.customize.jpa.jpo.EERFileJpo;
import kr.co.spectra.attic.tools.convert.customize.model.CustomizeChangedFile;
import kr.co.spectra.attic.tools.convert.customize.model.CustomizeItem;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@Component
public class CustomizeLogic {

    @Autowired
    private CustomizeItemJpaStore customizeItemJpaStore;

    @Autowired
    private CustomizeChangedFileJpaStore customizeChangedFileJpaStore;

    @Autowired
    private EERFileJpaStore eerFileJpaStore;

    public void createCustomizeItem(CustomizeItem customizeItem) {

        CustomizeItemJpo customizeItemJpo = null;
        List<HashMap<String, String>> dataList = customizeItem.getDataList();
        if (dataList != null && dataList.size() > 0) {
            for (int i = 0; i < dataList.size(); i++) {
                HashMap<String, String> map = dataList.get(i);

                customizeItemJpo = new CustomizeItemJpo();
                customizeItemJpo.setFilename(customizeItem.getFileName());
                customizeItemJpo.setSiteName(customizeItem.getSiteName());
                customizeItemJpo.setProduct(customizeItem.getProduct());
                customizeItemJpo.setEerVersion(customizeItem.getEerVersion());
                customizeItemJpo.setCustomizeName(map.get("customizeName"));
                customizeItemJpo.setRequirements(map.get("requirements"));
                customizeItemJpo.setJavaChanges(map.get("javaChanges"));
                customizeItemJpo.setDbChanges(map.get("dbChanges"));
                customizeItemJpo.setChangedFiles(map.get("changedFiles"));
                customizeItemJpo.setCustomizeType(map.get("customizeType"));
                customizeItemJpo.setChangedService(map.get("changedService"));
                customizeItemJpo.setDeveloper(map.get("developer"));
                customizeItemJpo.setSolution(map.get("solution"));
                customizeItemJpo.setCause(map.get("cause"));

                long count = customizeItemJpaStore.countBySiteNameAndCustomizeNameAndRequirements(customizeItem.getSiteName(), map.get("customizeName"), map.get("requirements"));
                if (count == 0) {
                    customizeItemJpaStore.create(customizeItemJpo);
                    //Util.printLog("inserted " + map.get("customizeName") + " to DB");
                }
                else {
                    //Util.printLog("#already inserted. " + map.get("customizeName") + " to DB");
                }
            }
        }

    }

    public void createCustomizeChangedFile(CustomizeChangedFile customizeChangedFile) {
        List<HashMap<String, String>> dataList = customizeChangedFile.getDataList();

        if (dataList != null && dataList.size() > 0) {
            for (int i = 0; i < dataList.size(); i++) {
                HashMap<String, String> map = dataList.get(i);

                String changedFiles = map.get("changedFiles");
                if (changedFiles != null) {
                    String[] arChangedFiles = changedFiles.split("\n");

                    for (int j = 0; j < arChangedFiles.length; j++) {

                        if (arChangedFiles[j].indexOf("/") > -1) {

                            String fileName = getFileName(arChangedFiles[j]);
                            String fileExt = FilenameUtils.getExtension(fileName);
                            String fileModifiedType = "";

                            EERFileJpo eerFileJpo = null;

                            if (fileExt != null && fileExt.length() > 0)
                            {
                                eerFileJpo = eerFileJpaStore.findByFilenameAndEerVersion(fileName, customizeChangedFile.getProduct(), customizeChangedFile.getEerVersion());

                                if (eerFileJpo != null) {
                                    fileModifiedType = "MODIFY";
                                } else {
                                    fileModifiedType = "ADD";
                                }
                            }

                            CustomizeChangeFileJpo customizeChangeFileJpo = new CustomizeChangeFileJpo();
                            customizeChangeFileJpo.setSiteName(customizeChangedFile.getSiteName());
                            customizeChangeFileJpo.setProduct(customizeChangedFile.getProduct());
                            customizeChangeFileJpo.setEerVersion(customizeChangedFile.getEerVersion());
                            customizeChangeFileJpo.setFilepath(arChangedFiles[j]);
                            customizeChangeFileJpo.setCustomizeName(map.get("customizeName"));
                            customizeChangeFileJpo.setFilename(fileName);
                            customizeChangeFileJpo.setFileExt(fileExt);
                            customizeChangeFileJpo.setFileModifiedType(fileModifiedType);

                            long count = customizeChangedFileJpaStore.countBySiteNameAndFilepathAndCustomizeName(customizeChangedFile.getSiteName(), arChangedFiles[j], map.get("customizeName"));
                            if (count == 0)
                                customizeChangedFileJpaStore.create(customizeChangeFileJpo);
                        }
                    }
                }
            }
        }
    }

    private String getFileName(String filePath) {
        String fileName = "";
        if (filePath != null && filePath.length() > 0 && filePath.indexOf("/") > -1 && filePath.indexOf(".") > -1) {
            fileName = Util.getFileNameByRegExp(filePath);
        }

        return fileName;
    }
}
