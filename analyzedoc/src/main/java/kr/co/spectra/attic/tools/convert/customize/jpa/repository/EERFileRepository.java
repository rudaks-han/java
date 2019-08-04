package kr.co.spectra.attic.tools.convert.customize.jpa.repository;

import kr.co.spectra.attic.tools.convert.customize.jpa.jpo.EERFileJpo;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface EERFileRepository extends CrudRepository<EERFileJpo, Long> {
    EERFileJpo findTop1ByFilenameAndProductAndEerVersion(String filename, String product, String eerVersion);

    List<EERFileJpo> findByFilenameAndProductAndEerVersion(String filename, String product, String eerVersion);
}
