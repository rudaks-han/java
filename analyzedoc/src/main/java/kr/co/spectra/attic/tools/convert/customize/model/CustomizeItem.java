package kr.co.spectra.attic.tools.convert.customize.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomizeItem {
    private String fileName;
    private String siteName;
    private String product;
    private String eerVersion;
    private List<HashMap<String, String>> dataList;
}
