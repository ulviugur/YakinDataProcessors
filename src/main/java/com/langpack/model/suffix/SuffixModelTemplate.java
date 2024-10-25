package com.langpack.model.suffix;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SuffixModelTemplate {
	String postag;
	String engDesc;
	String turkishDesc;
	SuffixType sfxType;
}
