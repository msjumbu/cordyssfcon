/**
 * 
 */
package com.cordys.coe.salesforce.connector;

import java.io.IOException;

import com.cordys.coe.salesforce.connector.execution.SFExecute;
import com.cordys.coe.salesforce.metadata.SFObjects;
import com.eibus.soap.ApplicationTransaction;
import com.eibus.soap.BodyBlock;
import com.eibus.util.logger.CordysLogger;
import com.sforce.ws.ConnectionException;

/**
 * @author Senthil Kumar Murugesan
 * 
 */
public class SFTransaction implements ApplicationTransaction {
	
	//static final String USERNAME = "senthilkumar@gmail.com";
	//static final String PASSWORD = "p0cs1998r8rhnPtX9jupZNUCXgqljXtu";


	private String implType = "";
	CordysLogger logger = null;
	SalesforceConnector connector = null;

	public SFTransaction(SalesforceConnector connector) {
		this.connector = connector;
		this.logger = SalesforceConnector.logger;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.eibus.soap.ApplicationTransaction#abort()
	 */
	@Override
	public void abort() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.eibus.soap.ApplicationTransaction#canProcess(java.lang.String)
	 */
	@Override
	public boolean canProcess(String implementationType) {
		if (logger.isDebugEnabled()) {
			logger.debug("canProcess(String) - start");
		}
		
		this.implType = implementationType;
		
		boolean returnboolean = "SalesforceSOAP".equals(implType) || "SalesforceMetadata".equals(implType);
		return returnboolean;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.eibus.soap.ApplicationTransaction#commit()
	 */
	@Override
	public void commit() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.eibus.soap.ApplicationTransaction#process(com.eibus.soap.BodyBlock,
	 * com.eibus.soap.BodyBlock)
	 */
	@SuppressWarnings("deprecation")
	@Override
	public boolean process(BodyBlock requestBlock, BodyBlock responseBlock) {
		
		int requestNode = requestBlock.getXMLNode();
		int responseNode = responseBlock.getXMLNode();

		String methodName = requestBlock.getMethodDefinition().getMethodName();
		String reqOrg = requestBlock.getSOAPTransaction().getIdentity().getUserOrganization();

		if (implType.equals("SalesforceMetadata")) {
			SFObjects sfo = new SFObjects(SalesforceConnector.getConnection(), methodName);
			try {
				sfo.process(requestNode, responseNode);
			} catch (ConnectionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			SFExecute sfe = new SFExecute(SalesforceConnector.getConnection(), requestBlock, responseBlock);
			try {
				sfe.execute();
			} catch (ConnectionException e) {
				responseBlock.createSOAPFault("failed", e.getMessage());
			} catch (IOException e) {
				responseBlock.createSOAPFault("failed", e.getMessage());
			}
		}
		return false;
	}

	

}
