/**
 * 
 */
package com.cordys.coe.salesforce.metadata;

import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;

/**
 * @author Senthil Kumar Murugesan
 *
 */
public abstract class ASFObjectInfo {
	
	private PartnerConnection partnerConnection = null;
	
	public ASFObjectInfo(PartnerConnection connection) {
		setPartnerConnection(connection);
	}
	
	public abstract int listObjects(int responseNode) throws ConnectionException;
	
	public abstract int objectDefinition(String[] sObjectTypes, int responseNode) throws ConnectionException;
	
	public abstract int process(int requestNode, int responseNode) throws ConnectionException;

	public PartnerConnection getPartnerConnection() {
		return partnerConnection;
	}

	public void setPartnerConnection(PartnerConnection partnerConnection) {
		this.partnerConnection = partnerConnection;
	}

}
