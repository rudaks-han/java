package kr.co.spectra.attic.tools.convert.customize.logic;

import kr.co.spectra.attic.tools.convert.customize.jpa.FileDiffJpaStore;
import kr.co.spectra.attic.tools.convert.customize.jpa.jpo.FileDiffJpo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileDiffLogic {

    @Autowired
    private FileDiffJpaStore fileDiffJpaStore;

    public void create(FileDiffJpo fileDiffJpo) {
        fileDiffJpaStore.create(fileDiffJpo);
    }
}
