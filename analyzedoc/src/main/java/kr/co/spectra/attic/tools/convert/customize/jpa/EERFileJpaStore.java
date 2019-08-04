package kr.co.spectra.attic.tools.convert.customize.jpa;

import kr.co.spectra.attic.tools.convert.customize.jpa.jpo.EERFileJpo;
import kr.co.spectra.attic.tools.convert.customize.jpa.repository.EERFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EERFileJpaStore {

    @Autowired
    private EERFileRepository eerFileRepository;

    public void create(EERFileJpo eerFileJpo) {
        eerFileRepository.save(eerFileJpo);
    }

    public EERFileJpo findByFilenameAndEerVersion(String filename, String product, String eerVersion) {
        return eerFileRepository.findTop1ByFilenameAndProductAndEerVersion(filename, product, eerVersion);
    }

    public Iterable<EERFileJpo> findAll() {
        return eerFileRepository.findAll();
    }

    public List<EERFileJpo> findByFilenameAndProductAndEerVersion(String filename, String product, String eerVersion) {
        return eerFileRepository.findByFilenameAndProductAndEerVersion(filename, product, eerVersion);
    }
}
