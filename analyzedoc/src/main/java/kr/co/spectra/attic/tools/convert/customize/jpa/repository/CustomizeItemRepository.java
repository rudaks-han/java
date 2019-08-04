package kr.co.spectra.attic.tools.convert.customize.jpa.repository;

import kr.co.spectra.attic.tools.convert.customize.jpa.jpo.CustomizeItemJpo;
import org.springframework.data.repository.CrudRepository;

public interface CustomizeItemRepository extends CrudRepository<CustomizeItemJpo, Long> {
    long countBySiteNameAndCustomizeNameAndRequirements(String siteName, String customizeName, String requirements);
}
