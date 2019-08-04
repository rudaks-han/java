package kr.co.spectra.attic.tools.convert.customize.jpa.jpo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "customize_item", indexes = { @Index(name = "idx_customize_item_customize_name", columnList = "site_name,customize_name" )})
@Getter
@Setter
@NoArgsConstructor
public class CustomizeItemJpo {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String filename;
    @Column(name = "site_name")
    private String siteName;
    private String product;
    private String eerVersion;
    @Column(length = 1000, name = "customize_name")
    private String customizeName;
    @Column(length = 50000)
    private String requirements;
    @Column(length = 50000)
    private String javaChanges;
    @Column(length = 50000)
    private String dbChanges;
    @Column(length = 50000)
    private String changedFiles;
    private String customizeType;
    private String changedService;
    private String developer;
    private String solution;
    private String cause;

}
