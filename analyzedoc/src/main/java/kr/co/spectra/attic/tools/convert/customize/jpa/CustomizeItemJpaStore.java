package kr.co.spectra.attic.tools.convert.customize.jpa;

import kr.co.spectra.attic.tools.convert.customize.jpa.jpo.CustomizeItemJpo;
import kr.co.spectra.attic.tools.convert.customize.jpa.repository.CustomizeItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class CustomizeItemJpaStore {

    @Autowired
    private CustomizeItemRepository customizeItemRepository;

    public long countBySiteNameAndCustomizeNameAndRequirements(String siteName, String customizeName, String requirements) {
        return customizeItemRepository.countBySiteNameAndCustomizeNameAndRequirements(siteName, customizeName, requirements);
    }

    public void create(CustomizeItemJpo customizeItemJpo) {
        customizeItemRepository.save(customizeItemJpo);
    }
}
