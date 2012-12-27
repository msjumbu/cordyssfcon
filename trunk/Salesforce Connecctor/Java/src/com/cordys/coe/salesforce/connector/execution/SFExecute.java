/**
 * 
 */
package com.cordys.coe.salesforce.connector.execution;

import java.io.IOException;
import java.util.Iterator;

import com.eibus.soap.BodyBlock;
import com.eibus.xml.nom.Node;
import com.eibus.xml.xpath.XPath;
import com.sforce.soap.partner.DeleteResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.SaveResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.bind.XmlObject;

/**
 * @author Senthil Kumar Murugesan
 * 
 */
public class SFExecute {

	PartnerConnection connection = null;
	BodyBlock request = null;
	BodyBlock response = null;

	public SFExecute(PartnerConnection connection, BodyBlock request,
			BodyBlock response) {
		this.connection = connection;
		this.request = request;
		this.response = response;
	}

	public void execute() throws ConnectionException, IOException {
		int impl = request.getMethodDefinition().getImplementation();
		int responseXML = response.getXMLNode();
		int requestXML = request.getXMLNode();

		String operation = Node.getData(XPath.getFirstMatch(".//Operation",
				null, impl));
		if (operation.equals("get"))
			executeQuery(responseXML, impl);
		if (operation.equals("create"))
			executeCreate(requestXML, responseXML, impl);
		if (operation.equals("update"))
			executeUpdate(requestXML, responseXML, impl);
		if (operation.equals("delete"))
			executeDelete(requestXML, responseXML, impl);
	}

	@SuppressWarnings("deprecation")
	private void executeCreate(int requestXML, int responseXML, int impl)
			throws ConnectionException {
		SObject[] newSObject = new SObject[1];
		SObject so = new SObject();
		newSObject[0] = so;
		String type = Node.getData(XPath
				.getFirstMatch(".//SObject", null, impl));

		so.setType(type);
		int objNode = XPath.getFirstMatch(".//" + type, null, requestXML);
		int field = Node.getFirstChildElement(objNode);
		while (field > 0) {
//			if (Node.getLocalName(field).equals("Id")
//					|| Node.getLocalName(field).equals("CreatedDate")
//					|| Node.getLocalName(field).equals("CreatedById")
//					|| Node.getLocalName(field).equals("LastModifiedDate")
//					|| Node.getLocalName(field).equals("LastModifiedById")
//					|| Node.getLocalName(field).equals("SystemModstamp")
//					|| Node.getLocalName(field).equals("IsDeleted")) {
//				field = Node.getNextElement(field);
//				continue;
//			}
			String fieldName = Node.getLocalName(field);
			int fieldDef = XPath.getFirstMatch(".//fields[name='" + fieldName
					+ "']", null, impl);
			boolean creatable = Boolean.parseBoolean(Node.getDataWithDefault(
					XPath.getFirstMatch(".//createable", null, fieldDef),
					"false"));
			if (!creatable) continue;
			Object fieldValue = getValueObject(fieldName,
					Node.getDataWithDefault(field, ""), fieldDef);
			so.setField(fieldName, fieldValue);
			field = Node.getNextElement(field);
		}

		SaveResult[] saveResults = connection.create(newSObject);

		int respNode = Node.clone(objNode, true);
		Node.appendToChildren(respNode, responseXML);

		for (int i = 0; i < saveResults.length; i++) {
			if (saveResults[i].isSuccess()) {
				String ns = Node.getNamespaceURI(respNode);
				Node.setDataElementNS(respNode, "Id", ns,
						saveResults[i].getId());
			} else {
				com.sforce.soap.partner.Error[] errors = saveResults[i]
						.getErrors();
				for (int j = 0; j < errors.length; j++) {
					response.createSOAPFault("Server", errors[i].getMessage());
				}
			}
		}
	}

	private Object getValueObject(String fieldName, String fieldValue,
			int fieldDef) {

		// TODO: picklist, datetime

		int fieldType = XPath.getFirstMatch(".//type", null, fieldDef);
		String type = Node.getDataWithDefault(fieldType, "");
		Object retObj = null;
		switch (type) {
		case "_boolean":
			retObj = Boolean.parseBoolean(fieldValue);
			break;
		case "id":
		case "url":
		case "phone":
		case "textarea":
		case "email":
		case "string":
		case "reference":
		case "picklist":
			retObj = fieldValue;
			break;
		case "_int":
			retObj = Integer.parseInt(fieldValue);
			break;
		case "_double":
			retObj = Double.parseDouble(fieldValue);
		default:
			break;
		}

		return retObj;
	}

	@SuppressWarnings("deprecation")
	private void executeUpdate(int requestXML, int responseXML, int impl)
			throws ConnectionException {
		SObject[] newSObject = new SObject[1];
		SObject so = new SObject();
		newSObject[0] = so;
		String type = Node.getData(XPath
				.getFirstMatch(".//SObject", null, impl));

		so.setType(type);
		int objNode = XPath.getFirstMatch(".//" + type, null, requestXML);
		int field = Node.getFirstChildElement(objNode);
		while (field > 0) {
			if (Node.getLocalName(field).equals("CreatedDate")
					|| Node.getLocalName(field).equals("CreatedById")
					|| Node.getLocalName(field).equals("LastModifiedDate")
					|| Node.getLocalName(field).equals("LastModifiedById")
					|| Node.getLocalName(field).equals("SystemModstamp")
					|| Node.getLocalName(field).equals("IsDeleted")) {
				field = Node.getNextElement(field);
				continue;
			}
			if (Node.getLocalName(field).equals("Id")) {
				so.setId(Node.getDataWithDefault(field, ""));
			}
			so.setField(Node.getLocalName(field),
					Node.getDataWithDefault(field, ""));
			field = Node.getNextElement(field);
		}

		SaveResult[] saveResults = connection.update(newSObject);

		int respNode = Node.clone(objNode, true);
		Node.appendToChildren(respNode, responseXML);

		for (int i = 0; i < saveResults.length; i++) {
			if (saveResults[i].isSuccess()) {
				Node.setDataElement(respNode, "Id", saveResults[i].getId());
			} else {
				com.sforce.soap.partner.Error[] errors = saveResults[i]
						.getErrors();
				for (int j = 0; j < errors.length; j++) {
					response.createSOAPFault("Server", errors[i].getMessage());
				}
			}
		}

	}

	@SuppressWarnings("deprecation")
	private void executeDelete(int requestXML, int responseXML, int impl)
			throws ConnectionException {
		String[] delID = new String[1];
		String type = Node.getData(XPath
				.getFirstMatch(".//SObject", null, impl));

		int objNode = XPath.getFirstMatch(".//" + type, null, requestXML);
		int field = Node.getFirstChildElement(objNode);
		while (field > 0) {
			if (Node.getLocalName(field).equals("Id")) {
				delID[0] = Node.getDataWithDefault(field, "");
			}
			field = Node.getNextElement(field);
		}

		DeleteResult[] delResults = connection.delete(delID);

		for (int i = 0; i < delResults.length; i++) {
			if (delResults[i].isSuccess()) {
				Node.setDataElement(responseXML, "result", "success");
			} else {
				com.sforce.soap.partner.Error[] errors = delResults[i]
						.getErrors();
				for (int j = 0; j < errors.length; j++) {
					response.createSOAPFault("Server", errors[i].getMessage());
				}
			}
		}

	}

	private void executeQuery(int responseXML, int impl)
			throws ConnectionException {
		String queryString = Node.getData(XPath.getFirstMatch(".//query", null,
				impl));
		String type = Node.getData(XPath
				.getFirstMatch(".//SObject", null, impl));
		QueryResult qr = connection.query(queryString);
		if (qr.getSize() <= 0) {
			return;
		}

		SObject[] records = qr.getRecords();
		for (SObject record : records) {
			int QueryRespNode = Node.createElement(type, responseXML);
			Iterator<XmlObject> r = record.getChildren();
			while (r.hasNext()) {
				XmlObject child = r.next();
				String fieldName = child.getName().getLocalPart();
				if (fieldName.equals("type"))
					continue;
				String value = "";
				Object obj = record.getField(fieldName);
				if (obj != null)
					value = record.getField(fieldName).toString();

				if (!value.isEmpty())
					Node.setDataElement(QueryRespNode, fieldName, value);
			}
		}
	}

}
