package com.langpack.tdk;

import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

public class Madde {
    private String madde_id;
    private String kac;
    private String kelime_no;
    private String cesit;
    private String anlam_gor;
    private String on_taki;
    private String on_taki_html;
    private String madde;
    private String madde_html;
    private String cesit_say;
    private String anlam_say;
    private String taki;
    private String cogul_mu;
    private String ozel_mi;
    private String egik_mi;
    private String lisan_kodu;
    private String lisan;
    private String telaffuz_html;
    private String lisan_html;
    private String telaffuz;
    private String birlesikler;
    private String font;
    private String madde_duz;
    private String gosterim_tarihi;
    private List<Anlam> anlamlarListe;
    private List<Atasozu> atasozu;

}
