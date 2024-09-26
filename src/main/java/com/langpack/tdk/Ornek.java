package com.langpack.tdk;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Ornek {
    private String ornek_id;
    private String anlam_id;
    private String ornek_sira;
    private String ornek;
    private String kac;
    private String yazar_id;
    private String yazar_vd;
    private List<Yazar> yazar;

    // Getters and setters
    // ...
}
