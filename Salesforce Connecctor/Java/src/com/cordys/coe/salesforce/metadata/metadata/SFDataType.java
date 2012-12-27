package com.cordys.coe.salesforce.metadata.metadata;

import java.util.ArrayList;
/**
 * @author Senthil Kumar Murugesan
 * 
 */
public enum SFDataType {
	NUM(new String[] {"0"}),
    DATE(new String[] {"date"}),
    TIME(new String[] {"0"}),
    INT(new String[] {"integer"}),
    UNSIGNED_BYTE(new String[] {"0"}),
    FLOAT(new String[] {"decimal"}),
    BINARY(new String[] {"0"}),
    STRING(new String[] {"string"}),
    UNKNOWN(new String[] {"0"});

	private String dataType; 
	
	private ArrayList<String> m_sfDataTypes;
	
	SFDataType (String[] types) {
		m_sfDataTypes = new ArrayList<String>();
		for (String bancsType : types)
			m_sfDataTypes.add(bancsType);
	}

    public static SFDataType mapSFDataType(String sfDataType)
    {
    	SFDataType[] temp = SFDataType.values();

        for (SFDataType dataType : temp)
        {
            if (dataType.matchSF(sfDataType))
            {
                return dataType;
            }
        }

        return UNKNOWN;
    }
    
    private boolean matchSF(String sfDataType)
    {
        return m_sfDataTypes.contains(sfDataType);
    }

	
}
