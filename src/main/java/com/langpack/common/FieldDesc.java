package com.langpack.common;

public class FieldDesc {
	public static String FIELD_COUNTRY_CODE = "country_code";
	public static String FIELD_COUNTRY_NAME = "country_name";
	public static String FIELD_PROVINCE_NAME = "province_name";
	public static String FIELD_LARGE_AREA_NAME = "large_area_name";
	public static String FIELD_DEPARTMENT_NAME = "department_name";
	public static String FIELD_AREA_NAME = "area_name";
	public static String FIELD_LOCALITY_NAME = "locality_name";
	public static String FIELD_STREET_BASE_NAME = "street_base_name";
	public static String FIELD_POSTAL_CODE = "postal_code";
	public static String FIELD_EXPORT = "export";
	public static String FIELD_TYPE = "type";
	public static String FIELD_ADDITIONAL = "_additional";

	public FieldDesc() {

	}

	public FieldDesc(String _field, String _value) {
		this(_field, _value, false);
	}

	public FieldDesc(String _field, String _value, boolean _escape) {
		setFieldName(_field);
		setFieldValue(_value);
		setFieldEscape(_escape);
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getFieldValue() {
		String _tmp = null;
		if (getFieldEscape()) {
			_tmp = GlobalUtils.escapeSpecialCharacters(fieldValue);
		} else {
			_tmp = fieldValue;
		}
		return _tmp;
	}

	public void setFieldValue(String fieldValue) {
		this.fieldValue = fieldValue;
	}

	public boolean getFieldEscape() {
		return fieldEscape;
	}

	public void setFieldEscape(boolean fieldEscape) {
		this.fieldEscape = fieldEscape;
	}

	private String fieldName = null;
	private String fieldValue = null;
	private boolean fieldEscape = false; // if we want to escape single quotes or any other characters in the field
											// value
}