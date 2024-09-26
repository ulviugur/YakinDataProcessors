package com.langpack.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class YakinMeaningItem {

	private String word;
	private String syls;
	private String word_type;
	private String word_type2;
	private String chapter_name;
	private String meaning_id;
	private String meaning;
	private String lang_code;
	private String lang_content;
	private String import_time;
	private String chapter_id;
	private String combi_type;
	private String linked_combis;
	private String mark_as_deleted;

}
