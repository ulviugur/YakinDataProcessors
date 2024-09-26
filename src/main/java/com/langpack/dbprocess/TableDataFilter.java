package com.langpack.dbprocess;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;

public interface TableDataFilter {
	public boolean shouldUpdate(ResultSet rs);

	public String getSelectSQL();

	public void updateRecord(Connection conn, ResultSet rs, String[] updateParameters);

	public void loadExternalSources(Connection dbconn, File inputFile);
}
