package kr.co.spectra.attic.tools.convert.customize.jpa.repository;

import kr.co.spectra.attic.tools.convert.customize.jpa.jpo.EERFileJpo;
import kr.co.spectra.attic.tools.convert.customize.jpa.jpo.FileDiffJpo;
import org.springframework.data.repository.CrudRepository;

public interface FileDiffRepository extends CrudRepository<FileDiffJpo, Long> {
}
