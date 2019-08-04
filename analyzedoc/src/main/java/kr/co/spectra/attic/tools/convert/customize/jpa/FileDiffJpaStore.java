package kr.co.spectra.attic.tools.convert.customize.jpa;

import kr.co.spectra.attic.tools.convert.customize.jpa.jpo.FileDiffJpo;
import kr.co.spectra.attic.tools.convert.customize.jpa.repository.FileDiffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class FileDiffJpaStore {

    @Autowired
    private FileDiffRepository fileDiffRepository;

    public void create(FileDiffJpo fileDiffJpo) {
        fileDiffRepository.save(fileDiffJpo);
    }

}
