package kr.co.spectra.attic.tools.convert.customize.jpa.jpo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "eer_file", indexes = { @Index(name = "idx_eer_file_filename", columnList = "product,eerVersion,filename" )})
@Getter
@Setter
@NoArgsConstructor
public class EERFileJpo {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String product;
    private String eerVersion;
    @Column(name = "filename")
    private String filename;
    private String filepath;
    private long fileSize;
    private String fileExt;
    private int fileLine;

    public EERFileJpo(String product, String eerVersion, String filename, String filepath, long fileSize, String fileExt, int fileLine) {
        this.product = product;
        this.eerVersion = eerVersion;
        this.filename = filename;
        this.filepath = filepath;
        this.fileSize = fileSize;
        this.fileExt = fileExt;
        this.fileLine = fileLine;
    }
}
