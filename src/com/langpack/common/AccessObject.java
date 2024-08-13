package com.langpack.common;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;

public class AccessObject {
	private TreeMap<String, Table> tables = new TreeMap<>();
	private TreeMap<String, ArrayList<String>> tableColumnNames = new TreeMap<>();

	public static final Logger log4j = LogManager.getLogger("AccessObjects");
	private File ADFile = null;
	private Database db = null;
	private Set<String> tableNames = null;

	public Table getTable(String tmpTableName) {
		return tables.get(tmpTableName);
	}

	public Database getDBLink() {
		return db;
	}

	public AccessObject(File tmpFile) {
		ADFile = tmpFile;
		log4j.info("Loading file : " + ADFile);

		try {
			db = DatabaseBuilder.open(ADFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	// public void loadDataset() {

	// DB Tables : Alternative, Building, DeliveryService, Information, Locality,
	// Organization, PostalCode, Province, Street, SubBuilding
	// Street table >> stlStreetLevelID, stlStreetID, stlLocalityID, stlProvinceID,
	// stlPostalCodeID, stsPreDescriptor, stsPreDirectional,
	// stsName, stsPostDirectional, stsPostDescriptor, stsHouseRange,
	// stsSupplementaryInfo, stlDependence, styLanguage, stlType, stlNumberType,
	// styLevel, stsSpecialUse

	// Locality table >> lolLocalityLevelID, lolLocalityID, lolMasterLocalityID,
	// lolProvinceID, loyProvinceUsedInAddress, losPreDescriptor, losName,
	// losPostDescriptor,
	// lobPostDescriptorUsedInAddress, losSortingCode, lolStreetDir,
	// lobAddressRelevant, loyLanguage, lolType, loyLevel

	// PostalCode table >> pclPostalCodeID, pcsName, pcsAddOn, pclTableMarker,
	// pclLocalityID
	// }

	public void loadAllTables() {
		try {
			tableNames = db.getTableNames();
			for (String tmpName : tableNames) {
				Table tmpTable = db.getTable(tmpName);
				tables.put(tmpName, tmpTable);
				ArrayList<String> tmpColumnNameArray = getColumnNames(tmpTable);
				tableColumnNames.put(tmpName, tmpColumnNameArray);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log4j.info("Completed loading tables ..");
	}

	public ArrayList<String> getColumnNames(Table tmpTable) {
		ArrayList<String> retval = new ArrayList<>();
		List<? extends Column> tmpColumns = tmpTable.getColumns();
		for (Column tmpColumn : tmpColumns) {
			String tmpName = tmpColumn.getName();
			retval.add(tmpName);
		}
		return retval;
	}

	public void printTableColumns(String tableName) {
		Table table = null;
		try {
			table = db.getTable(tableName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<? extends Column> columns = table.getColumns();
		for (Column column : columns) {
			log4j.info(column.getName());
		}
	}

	public void printDBTables() {
		Set<String> tmpTableNamesSet = null;
		try {
			tmpTableNamesSet = db.getTableNames();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (Object column : tmpTableNamesSet) {
			log4j.info("TableName : " + column);
		}
	}

	public ArrayList<Object[]> getTableContent(Table tmpTable) {
		ArrayList<Object[]> retval = new ArrayList<>();
		Row row = null;
		try {
			while ((row = tmpTable.getNextRow()) != null) {
				Object[] mappedData = new Object[row.size()];
				int i = 0;
				for (String key : row.keySet()) {
					Object tmp = row.get(key);
					mappedData[i] = tmp;
					i++;
				}
				retval.add(mappedData);
				// log4j.info(String.format("Row added : %s", retval.size()));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return retval;
	}

	public void exportAccessContent(Table tmpTable, FileExporter tmpExporter) {
		String line = "";
		String strValue = null;

		// write column headers
		ArrayList<String> colNames = getColumnNames(tmpTable);
		for (String colName : colNames) {
			strValue = colName;
			line = line + strValue + ";";
		}
		line = line.substring(0, line.length() - 1);
		tmpExporter.writeLineToFile(line);

		int count = 0;
		// write data
		/*
		 * MapperModel model = new MapperAccessEN(); ArrayList<Object[]> content =
		 * getTableContent(tmpTable, model); for (int i = 0; i < content.size(); i++) {
		 * Object[] row = content.get(i); line = ""; for (int j = 0; j < row.length;
		 * j++) { Object value = row[j]; if (value == null) { strValue=""; } else {
		 * strValue=value.toString(); } line = line + strValue + ";"; } line =
		 * line.substring(0, line.length() -1);
		 * log4j.debug(String.format("Line [%s] : Writing : %s",count, line));
		 * tmpExporter.writeLineToFile(line); }
		 */
	}
}
