package kr.co.spectra.attic.tools.convert.customize.jpa.repository;

import kr.co.spectra.attic.tools.convert.customize.jpa.jpo.CustomizeChangeFileJpo;
import kr.co.spectra.attic.tools.convert.customize.model.ItemCount;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;


public interface CustomizeChangedFileRepository extends CrudRepository<CustomizeChangeFileJpo, Long> {
    long countBySiteNameAndFilepathAndCustomizeName(String siteName, String filepath, String customizeName);

    @Query(
            value =
                    "select filename as item, count(filename) as cnt " +
                    "from customize_changed_file where filename <> '' group by filename order by count(filename) desc limit 1",
            nativeQuery = true)
    List<ItemCount> findByFilenameGroupBy();
}
