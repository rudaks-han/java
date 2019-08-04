package kr.co.spectra.attic.tools.convert.customize.jpa;

import kr.co.spectra.attic.tools.convert.customize.jpa.jpo.CustomizeChangeFileJpo;
import kr.co.spectra.attic.tools.convert.customize.jpa.repository.CustomizeChangedFileRepository;
import kr.co.spectra.attic.tools.convert.customize.model.ItemCount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CustomizeChangedFileJpaStore {

    @Autowired
    private CustomizeChangedFileRepository customizeChangedFileRepository;

    public long countBySiteNameAndFilepathAndCustomizeName(String siteName, String filepath, String customizeName) {
        return customizeChangedFileRepository.countBySiteNameAndFilepathAndCustomizeName(siteName, filepath, customizeName);
    }

    public void create(CustomizeChangeFileJpo customizeChangeFileJpo) {
        customizeChangedFileRepository.save(customizeChangeFileJpo);
    }

    public List<ItemCount> findByFilenameGroupBy() {
        return customizeChangedFileRepository.findByFilenameGroupBy();
    }
}
