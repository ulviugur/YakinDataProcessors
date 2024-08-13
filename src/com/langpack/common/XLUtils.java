package com.langpack.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class XLUtils {
	public static final Logger log4j = LogManager.getLogger("XLUtils");

	public static boolean markCell(XSSFSheet sheet, int rowNo, int column, CellStyle style) {
		XSSFRow tmpRow = sheet.getRow(rowNo);
		if (tmpRow == null) {
			tmpRow = sheet.createRow(rowNo);
		}

		if (tmpRow == null) {
			log4j.warn("Could not read a new row ..");
			return false;
		} else {
			Cell cellTmp = tmpRow.createCell(column);
			setCellStyle(style, cellTmp);
			return true;
		}
	}

	public static ArrayList<String> readRow(XSSFSheet sheet, int rowId, int noColumns) {
		ArrayList<String> retval = new ArrayList<>();
		XSSFRow rowObject = sheet.getRow(rowId);
		if (rowObject == null) {
			log4j.warn("Could not read a new row ..");
			return null;
		}
		int col = 0;
		for (col = 0; col < noColumns; col++) {
			Cell tmpCell = rowObject.getCell(col);
			if (tmpCell == null) {
				retval.add(null); // null values are empty
			} else {
				String value = GlobalUtils.getCellContentAsString(tmpCell);
				retval.add(value);
			}
		}
		return retval;
	}

	public static ArrayList<String> readRow(XSSFRow rowObject, int noColumns) {
		ArrayList<String> retval = new ArrayList<>();
		if (rowObject == null) {
			log4j.warn("Could not read a new row ..");
			return null;
		}
		int col = 0;
		for (col = 0; col < noColumns; col++) {
			Cell tmpCell = rowObject.getCell(col);
			if (tmpCell == null) {
				retval.add(null); // null values are empty
			} else {
				String value = GlobalUtils.getCellContentAsString(tmpCell);
				retval.add(value);
			}
		}
		return retval;
	}

	private static void setCellStyle(CellStyle style, Cell... tmpCells) {
		for (Cell tmp : tmpCells) {
			tmp.setCellStyle(style);
		}
	}

	public static boolean changeCellValue(XSSFSheet sheet, int rowNo, int column, String value,
			CellStyle cellStyleNew) {
		log4j.info(String.format("Changing cell %s, {%s %s} = %s", sheet.getSheetName(), rowNo, column, value));
		XSSFRow tmpRow = sheet.getRow(rowNo);
		if (tmpRow == null) {
			tmpRow = sheet.createRow(rowNo);
		}

		if (tmpRow == null) {
			log4j.warn("Could not read a new row ..");
			return false;
		} else {
			Cell cellTmp = tmpRow.createCell(column);
			cellTmp.setCellValue(value);
			setCellStyle(cellStyleNew, cellTmp);
			return true;
		}
	}

	public static boolean changeCellValue(XSSFSheet sheet, int rowNo, int column, String value) {
		log4j.info(String.format("Changing cell %s, {%s %s} = %s", sheet.getSheetName(), rowNo, column, value));
		XSSFRow tmpRow = sheet.getRow(rowNo);
		if (tmpRow == null) {
			tmpRow = sheet.createRow(rowNo);
		}

		if (tmpRow == null) {
			log4j.warn("Could not read a new row ..");
			return false;
		} else {
			Cell cellTmp = tmpRow.createCell(column);
			cellTmp.setCellValue(value);
			return true;
		}
	}

	public static ArrayList<ArrayList<String>> getSheetData(XSSFSheet sheet, int skipLines, int noColumns) {
		if (sheet == null) {
			log4j.info("Sheet is not known, cannot continue ..");
			return null;
		}

		ArrayList<ArrayList<String>> retval = new ArrayList<>();

		XSSFRow rowObject = null;
		int currentRowId = skipLines - 1; // row number start with 0 on the API level

		while (true) {
			ArrayList<String> rowData = new ArrayList<>();
			rowObject = sheet.getRow(currentRowId);
			if (rowObject == null) {
				log4j.warn("Could not read a new row after row : " + currentRowId);
				break;
			}
			for (int i = 0; i < noColumns; i++) {
				Cell cellTmp = rowObject.getCell(i);
				String value = GlobalUtils.getCellContentAsString(cellTmp);
				rowData.add(value);
			}
			retval.add(rowData);
			currentRowId++;
		}
		return retval;
	}

	public static XSSFSheet getSheet(File file, String sheetName) {
		FileInputStream fsIP = null;
		XSSFWorkbook wb = null;
		XSSFSheet sheet = null;

		try {
			fsIP = new FileInputStream(file);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// Access the workbook
		log4j.info(String.format("Loading file %s ..", file.getAbsoluteFile()));
		try {
			wb = new XSSFWorkbook(fsIP);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sheet = wb.getSheet(sheetName);
		return sheet;
	}

	public static XSSFSheet getSheet(String filePath, String sheetName) {
		File readFile = new File(filePath);
		XSSFSheet retval = getSheet(readFile, sheetName);
		return retval;
	}
}
