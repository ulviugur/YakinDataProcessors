package com.langpack.datachannel;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.langpack.common.ConfigReader;
import com.langpack.common.XLFileIterator;

public class XLChannel extends DataChannel {

	String importFileName = null;
	File channelFile = null;
	XSSFWorkbook wb = null;
	XSSFSheet importSheet = null;
	String importSheetName = null;
	int rowCount = 0;
	int colCount = 0;

	public XLChannel(ConfigReader cfg, String tmpType, String tmpPrefix) throws UnknownDataChannelException {
		super(cfg, tmpType, tmpPrefix);
		importSheetName = cfg.getValue(this.getPrefix() + "." + DataChannel.CONFIG_FIELD_IMPORTSHEET);
	}

	public File getChannelFile() {
		return channelFile;
	}

	public void setChannelFile(File channelFile) {
		this.channelFile = channelFile;
	}

	@Override
	public String getDetails() {
		// TODO Auto-generated method stub
		return String.format("Name=%s : <URL=%s>", this.getName(), channelFile.getAbsolutePath());
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return String.format("Name=%s : <URL=%s>", this.getName(), channelFile.getAbsolutePath());
	}

	@Override
	public boolean initialize() {
		boolean retval = false;
		String param = this.getPrefix() + "." + "File";
		importFileName = cfg.getValue(param);
		channelFile = new File(importFileName);

		importSheetName = cfg.getValue(this.getPrefix() + "." + DataChannel.CONFIG_FIELD_IMPORTSHEET);

		try {
			XLFileIterator XLiter = new XLFileIterator();
			wb = XLiter.openWorkbook(channelFile);

			importSheet = wb.getSheet(importSheetName);
			if (importSheet != null) {
				log4j.info(
						String.format("Opened sheet %s from %s Workbook . ", importSheetName, channelFile.getName()));
			} else {
				log4j.error(String.format("Importsheet [%s] could not be opened from  Workbook [%s] !", importSheetName,
						channelFile.getName()));
				log4j.error("Exitting !!");
				System.exit(-1);
			}

			retval = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return retval;
	}

	@Override
	public void closeChannel() {
		try {
			wb.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Object getNextRow() throws SQLException {
		XSSFRow retval = importSheet.getRow(rowCount);
		rowCount++;
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
