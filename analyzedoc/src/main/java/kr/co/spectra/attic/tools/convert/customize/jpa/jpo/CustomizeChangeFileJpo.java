package kr.co.spectra.attic.tools.convert.customize.jpa.jpo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "customize_changed_file", indexes = { @Index(name = "idx_changed_file_customize_name", columnList = "site_name,customize_name" )})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomizeChangeFileJpo {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    @Column(name = "site_name")
    private String siteName;
    private String product;
    private String eerVersion;
    @Column(length = 1000, name = "customize_name")
    private String customizeName;
    @Column(length = 1000)
    private String filepath;
    private String filename;
    private String fileExt;
    private String fileModifiedType;
}
