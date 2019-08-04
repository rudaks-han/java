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
public class CustomizeChangedFile {
    private String siteName;
    private String product;
    private String eerVersion;
    private String customizeName;
    private String filepath;
    private String filename;
    private String fileExt;
    private String fileModifiedType;
    private List<HashMap<String, String>> dataList;
}
