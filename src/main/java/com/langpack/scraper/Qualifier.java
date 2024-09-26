package com.langpack.scraper;

public class Qualifier {
	public String getQualifierString() {
		return qualifierString;
	}

	public void setQualifierString(String qualifierString) {
		this.qualifierString = qualifierString;
	}

	public int getSkipQualifiers() {
		return skipQualifiers;
	}

	public void setSkipQualifiers(int skipQualifiers) {
		this.skipQualifiers = skipQualifiers;
	}

	public String getBlockStartQualifier() {
		return blockStartQualifier;
	}

	public void setBlockStartQualifier(String blockQualifier) {
		this.blockStartQualifier = blockQualifier;
	}

	public String getBlockEndQualifier() {
		return blockEndQualifier;
	}

	public void setBlockEndQualifier(String blockEndQualifier) {
		this.blockEndQualifier = blockEndQualifier;
	}

	public int getSearchDirection() {
		return searchDirection;
	}

	public void setSearchDirection(int searchDirection) {
		this.searchDirection = searchDirection;
	}

	// look for qualifier string
	private String qualifierString = null;

	// skip n of them
	private int skipQualifiers = 0;

	// when you find the qualifierString, look for the blockStartQualifier
	private String blockStartQualifier = null;

	// qualifies the end of the block
	private String blockEndQualifier = null;

	// when you find the block qualifier, which way to go "F" / "B"
	private int searchDirection = -1;

	// Go FORWARD or BACKWARD to find the blockQualifier
	public static final int GO_FORWARD = 1;
	public static final int GO_BACKWARD = -1;

	// There are two sequential searches :
	// 1- Primary Key
	// 2- Block Key

	// _qualifierStr : look for the qualifying string to know what we are looking
	// for (primary key search)
	// _skipQualifiers : skip the qualifier n times before you parse the block (# of
	// skip on primary key searches)
	// _blockStartQualifierString : start the block from this qualifier onwards.
	// Look into the search direction from the _qualifierString
	// _blockEndQualifierString : find the end of the block through this qualifier
	// _searchDirection : search for the qualifierStr in the GO_FORWARD or
	// GO_BACKWARD direction

	public Qualifier(String _qualifierStr, int _skipQualifiers, String _blockStartQualifierString,
			String _blockEndQualifierString, int _searchDirection) {

		this.setQualifierString(_qualifierStr);
		this.setSkipQualifiers(_skipQualifiers);
		this.setBlockStartQualifier(_blockStartQualifierString);
		this.setBlockEndQualifier(_blockEndQualifierString);

		// _searchDirection => block start search direction
		this.setSearchDirection(_searchDirection);
	}

	@Override
	public String toString() {
		String retval = String.format(
				"QualifierString : %s\n, SkipQualifiers : %s\n, BlockStartQualifier : %s\n, BlockEndQualifier : %s\n,",
				qualifierString, skipQualifiers, blockStartQualifier, blockEndQualifier);
		if (searchDirection == GO_FORWARD) {
			retval += " SearchDirection : FORWARD";
		} else if (searchDirection == GO_BACKWARD) {
			retval += " SearchDirection : BACKWARD";
		} else {
			retval += " SearchDirection : NOT SET";
		}
		return retval;
	}
}
