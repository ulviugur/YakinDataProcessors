package com.langpack.datachannel;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import com.langpack.common.ConfigReader;
import com.langpack.common.TextFileReader;

public class FileChannel extends DataChannel {
	File channelFile = null;
	TextFileReader reader = null;
	String seperator = ";";

	public FileChannel(ConfigReader cfg, String tmpType, String tmpPrefix) throws UnknownDataChannelException {
		super(cfg, tmpType, tmpPrefix);
	}

	public File getChannelFile() {
		return channelFile;
	}

	public void setChannelFile(File channelFile) {
		this.channelFile = channelFile;
	}

	public TextFileReader getReader() {
		return reader;
	}

	public void setReader(TextFileReader reader) {
		this.reader = reader;
	}

	@Override
	public String getDetails() {
		// TODO Auto-generated method stub
		return String.format("Name=%s : <URL=%s>", this.getName(), reader.getFileName());
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return String.format("Name=%s : <URL=%s>", this.getName(), reader.getFileName());
	}

	@Override
	public boolean initialize() {
		boolean retval = false;

		String importFileName = cfg.getValue(this.getPrefix() + "." + "File");
		channelFile = new File(importFileName);
		try {
			reader = new TextFileReader(channelFile);
			retval = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return retval;
	}

	@Override
	public void closeChannel() {
		reader.closeFile();
	}

	@Override
	public Object getNextRow() throws SQLException {
		ArrayList<String> retval = null;
		try {
			String line = reader.readLine();
			if (line != null) {
				String[] fields = line.split(seperator);
				retval = new ArrayList<>();
				for (String tmp : fields) {
					retval.add(tmp);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return retval;

	}

	@Override
	public Connection getDbConn() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDbConn(Connection dbConn) {
		// TODO Auto-generated method stub

	}
}
