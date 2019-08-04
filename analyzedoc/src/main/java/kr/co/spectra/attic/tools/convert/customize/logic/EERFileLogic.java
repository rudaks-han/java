package kr.co.spectra.attic.tools.convert.customize.logic;

import kr.co.spectra.attic.tools.convert.customize.jpa.EERFileJpaStore;
import kr.co.spectra.attic.tools.convert.customize.jpa.jpo.EERFileJpo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EERFileLogic {

    @Autowired
    private EERFileJpaStore eerFileJpaStore;

    public void create(EERFileJpo eerFileJpo) {
        eerFileJpaStore.create(eerFileJpo);
    }

    public Iterable<EERFileJpo> findAll() {
        return eerFileJpaStore.findAll();
    }

    public Map<String, Map<String, EERFileJpo>> getAllEERFile() {
        List<EERFileJpo> eerFileJpoList = new ArrayList<>();
        this.findAll().forEach(eerFileJpoList::add);

        // {3.0.1: {"test.txt": "EERFileJpo"}}
        Map<String, Map<String, EERFileJpo>> rootMap = new HashMap<>();

        int i = 0;
        System.out.println(eerFileJpoList.size());
        for (EERFileJpo eerFileJpo : eerFileJpoList) {
            String rootMapId = eerFileJpo.getProduct() + "_" + eerFileJpo.getEerVersion();
            if (rootMapId == null) {
                Map<String, EERFileJpo> eerFileMap = new HashMap<>();
                eerFileMap.put(eerFileJpo.getFilename(), eerFileJpo);

                rootMap.put(rootMapId, eerFileMap);
            } else {
                Map<String, EERFileJpo> eerFileMap = rootMap.get(rootMapId);
                eerFileMap.put(eerFileJpo.getFilename(), eerFileJpo);

                rootMap.put(rootMapId, eerFileMap);
            }

            System.out.println(i);
            i++;
        }

        return rootMap;
    }

    public List<EERFileJpo> findByFilenameAndProductAndEerVersion(String filename, String product, String eerVersion) {
        return eerFileJpaStore.findByFilenameAndProductAndEerVersion(filename, product, eerVersion);
    }
}
