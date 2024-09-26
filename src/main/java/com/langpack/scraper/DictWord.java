package com.langpack.scraper;

public class DictWord {
	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getWordtype() {
		return wordtype;
	}

	public void setWordtype(String wordtype) {
		this.wordtype = wordtype;
	}

	public String getImportTime() {
		return importTime;
	}

	public void setImportTime(String importTime) {
		this.importTime = importTime;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getURL() {
		return URL;
	}

	public void setURL(String uRL) {
		URL = uRL;
	}

	public String getMarkAsDeleted() {
		return markAsDeleted;
	}

	public void setMarkAsDeleted(String markAsDeleted) {
		this.markAsDeleted = markAsDeleted;
	}

	public String getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(String updateTime) {
		this.updateTime = updateTime;
	}

	public String getLevel2() {
		return level2;
	}

	public void setLevel2(String level2) {
		this.level2 = level2;
	}

	public DictWord(String word) {
		this.setWord(word);
	}

	public boolean isFound() {
		if (level2 == null || level2.contains("Sonuç bulunamadı")) {
			return false;
		} else {
			return true;
		}
	}

	public String getProcessed() {
		return processed;
	}

	public void setProcessed(String processed) {
		this.processed = processed;
	}

	public String getFound() {
		return found;
	}

	public void setFound(String found) {
		this.found = found;
	}

	@Override
	public String toString() {
		return "DictWord [word=" + word + ", id=" + id + ", source=" + source + ", URL=" + URL + ", level2=" + level2
				+ ", wordtype=" + wordtype + ", importTime=" + importTime + ", markAsDeleted=" + markAsDeleted
				+ ", updateTime=" + updateTime + ", processed=" + processed + ", found=" + found + "]";
	}

	String word = null;
	String id = null;
	String source = null;
	String URL = null;
	String level2 = null;
	String wordtype = null;
	String importTime = null;
	String markAsDeleted = null;
	String updateTime = null;
	String processed = null;
	String found = null;
}
