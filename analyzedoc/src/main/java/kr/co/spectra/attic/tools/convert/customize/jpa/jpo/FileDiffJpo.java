package kr.co.spectra.attic.tools.convert.customize.jpa.jpo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "file_diff")
@Getter
@Setter
@NoArgsConstructor
public class FileDiffJpo {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String siteName;
    private String product;
    private String eerVersion;
    private String filename;
    private String packagePath;
    private long lineCount;
    private long addLineCount;
    private long removeLineCount;
    @Lob
    private String fileContent;

    public FileDiffJpo(String siteName, String product, String eerVersion, String filename, String packagePath, long lineCount, long addLineCount, long removeLineCount, String fileContent) {
        this.siteName = siteName;
        this.product = product;
        this.eerVersion = eerVersion;
        this.filename = filename;
        this.packagePath = packagePath;
        this.lineCount = lineCount;
        this.addLineCount = addLineCount;
        this.removeLineCount = removeLineCount;
        this.fileContent = fileContent;
    }
}
