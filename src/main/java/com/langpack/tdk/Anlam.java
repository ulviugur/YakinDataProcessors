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
public class Anlam {
    private String anlam_id;
    private String madde_id;
    private String anlam_sira;
    private String fiil;
    private String tipkes;
    private String anlam;
    private String anlam_html;
    private String gos;
    private String gos_kelime;
    private String gos_kultur;
    private List<Ozellik> ozelliklerListe;
    private List<Ornek> orneklerListe;

    // Getters and setters
    // ...
}
