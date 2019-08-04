package kr.co.spectra.attic.tools.convert.customize.logic;

import kr.co.spectra.attic.tools.convert.customize.jpa.CustomizeChangedFileJpaStore;
import kr.co.spectra.attic.tools.convert.customize.model.ItemCount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomizeChangedFileLogic {

    @Autowired
    private CustomizeChangedFileJpaStore customizeChangedFileJpaStore;

    public List<ItemCount> findByFilenameGroupBy() {
        List<ItemCount> list = customizeChangedFileJpaStore.findByFilenameGroupBy();

        return list;
    }
}
