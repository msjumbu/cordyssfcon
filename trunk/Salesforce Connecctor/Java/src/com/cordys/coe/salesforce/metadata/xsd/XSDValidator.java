// XSD4J - XML Schema library for Java
//
// Copyright (C) 2006 Josef Spillner <js177634@inf.tu-dresden.de>
// Published under GNU General Public License conditions.
//
// This file is part of the XSD4J library.
// It has been created under a student deployment contract
// at the Chair of Computer Networks, Faculty for Computer Sciences,
// Dresden University of Technology.
// See http://dynvocation.selfip.net/xsd4j/ for more information.

package com.cordys.coe.salesforce.metadata.xsd;

import java.util.*;

import org.w3c.dom.*;

// FIXME: also, check for fixed vs. default consistency etc.
// FIXME: instance validation vs. schema verification!

/**
 * \brief Validator class for XML schema trees.
 *
 * Once a tree has been obtained from an \ref XSDParser object,
 * which means that it has already passed internal validation,
 * some additional validation might be wanted by the application.
 *
 * Warning: This class is not working yet.
 *
 * @author Josef Spillner <js177634@inf.tu-dresden.de>
 */
public class XSDValidator extends XMLBase
{
	/**
	 * Default constructor.
	 *
	 * Returns a new validator object which may then be used to
	 * validate schema tree objects.
	 */
	public XSDValidator()
	{
	}

	/**
	 * Validates an XML instance against the whole XML Schema tree.
	 *
	 * This method takes both an XML schema and a perceived instance
	 * thereof and performs a validation.
	 *
	 * @param schema XML schema tree object
	 * @param instance XML DOM element describing an instance of the schema
	 * @return \b true if the instance matches the schema, or \b false otherwise
	 */
	public boolean validate(XSDSchema schema, Element instance)
	{
		boolean valid = true;
		boolean tmpvalid;

		NodeList list = instance.getChildNodes();
		for(int i = 0; i < list.getLength(); i++)
		{
			Node node = list.item(i);
			if(node.getNodeType() != Node.ELEMENT_NODE)
			{
				continue;
			}

			Element el = (Element)node;
			String tree = treepath_xml(el);
			System.out.println("TreePath: " + tree);

			XSDElement xsdelement = treepath2element(schema, tree);
			if(xsdelement != null)
			{
				if(xsdelement.getType().getType() == XSDType.TYPE_COMPLEX)
				{
					System.out.println("-> ok (complex type)");
				}
				else
				{
					tmpvalid = validateElement(el, xsdelement);
					if(!tmpvalid) valid = false;
					if(tmpvalid)
					{
						System.out.println("-> ok");
					}
					else
					{
						System.out.println("-> invalid (type mismatch)");
					}
				}
			}
			else
			{
				System.out.println("-> invalid (not in schema)");
			}

			tmpvalid = validate(schema, el);
			if(!tmpvalid) valid = false;
		}

		return valid;
	}

	private boolean validateElement(Element el, XSDElement xsdelement)
	{
		String content = textvalue(el);
		System.out.println("## Content: " + content);

		XSDType xsdtype = xsdelement.getType();
		int t = xsdtype.getType();

		if(t == XSDType.TYPE_BOOLEAN)
		{
			// true, false, 1, 0
			if((content.equals("true"))
			|| (content.equals("false"))
			|| (content.equals("1"))
			|| (content.equals("0")))
			{
				// roger
			}
			else
			{
				return false;
			}
		}

		return true;
	}

	private XSDElement treepath2element(XSDSequence schema, String tree)
	{
		String sections[] = tree.split("/");
		String section = sections[1];
		String remainder = tree.substring(section.length() + 1);
		//System.out.println("# DEBUG: " + sections.length);
		//System.out.println("# Need: " + section);
		//System.out.println("# Remainder: " + remainder);

		ArrayList list = schema.getElements();
		for(int i = 0; i < list.size(); i++)
		{
			XSDElement xsdelement = (XSDElement)list.get(i);
			//System.out.println("# " + xsdelement.getName());
			if(xsdelement.getName().equals(section))
			{
				if(remainder.length() == 0)
				{
					return xsdelement;
				}
				else
				{
					XSDType xsdtype = xsdelement.getType();
					if(xsdtype.getType() != XSDType.TYPE_COMPLEX)
					{
						return null;
					}
					else
					{
						XSDSequence xsdseq = xsdtype.getSequence();
						return treepath2element(xsdseq, remainder);
					}
				}
			}
		}
		return null;
	}

	/**
	 * Test method for the validation.
	 *
	 * This method builds an internal instance and tries to validate
	 * it against the given schema afterwards.
	 *
	 * @param schema XML Schema to test the instance against.
	 * @return \b true whether the instance matches the schema, or \b false otherwise
	 * @internal
	 */
	public boolean test(XSDSchema schema)
	{
		Document doc = XSDCommon.createDocument();

		if(doc == null)
		{
			System.out.println("=> ABORT!");
			return false;
		}

		Element parent = doc.createElement("ANONYMOUS");
		try
		{
			Text tn;

			Element comment = doc.createElement("comment");
			tn = doc.createTextNode("FOOO");
			comment.appendChild(tn);
			parent.appendChild(comment);

			Element po = doc.createElement("purchaseOrder");
			po.setAttribute("orderDate", "2006-03-26");
			parent.appendChild(po);

			Element shipto = doc.createElement("shipTo");
			tn = doc.createTextNode("true");
			shipto.appendChild(tn);
			po.appendChild(shipto);

			Element billto = doc.createElement("billTo");
			tn = doc.createTextNode("06-12");
			billto.appendChild(tn);
			po.appendChild(billto);

			Element item1 = doc.createElement("items");
			tn = doc.createTextNode("ITEM1");
			item1.appendChild(tn);
			po.appendChild(item1);

			Element item2 = doc.createElement("items");
			tn = doc.createTextNode("ITEM2");
			item2.appendChild(tn);
			po.appendChild(item2);
		}
		catch(DOMException e)
		{
			System.out.println("=> ABORT!");
			System.out.println(e.toString());
			return false;
		}

		System.out.println();
		System.out.println("[Validator]");
		XSDDumper dumper = new XSDDumper();
		String s = dumper.dumpXML(parent);
		System.out.println(s);
		System.out.println();
		boolean ret = validate(schema, parent);
		System.out.println("[/Validator]");

		return ret;
	}

	private String treepath_xml(Element el)
	{
		Node node = el.getParentNode();
		if(node != null)
		{
			if(node.getNodeType() == Node.ELEMENT_NODE)
			{
				return treepath_xml((Element)node) + "/" + el.getNodeName();
			}
		}

		//return el.getNodeName();
		return "";
	}

	public boolean checker(XSDSchema schema)
	{
		boolean ret = true;

		debug("(-checker-) begin");

		ArrayList list = schema.getElements();
		for(int i = 0; i < list.size(); i++)
		{
			XSDElement xsdelement = (XSDElement)list.get(i);
			XSDType xsdtype = xsdelement.getType();

			if(xsdtype == null)
			{
				debug("error: missing or unknown type '" + xsdelement.getTypeRef() + "'");
				ret = false;
				continue;
			}

			int t = xsdtype.getType();

			if(t == XSDType.TYPE_COMPLEX)
			{
				// skipping...
			}
			else
			{
				//debug("- " + t);
				boolean ordered = true;
				if(t == XSDType.TYPE_STRING) ordered = false;
				if(t == XSDType.TYPE_NORMALIZED_STRING) ordered = false;
				if(t == XSDType.TYPE_TOKEN) ordered = false;
				if(t == XSDType.TYPE_BASE64_BINARY) ordered = false;
				if(t == XSDType.TYPE_HEX_BINARY) ordered = false;
				// FIXME: warning - dangerous >= generalisation
				if(t >= XSDType.TYPE_NAME) ordered = false;

				XSDRestriction xsdres = xsdtype.getRestriction();
				if(xsdres == null)
				{
					continue;
				}
				BitSet res = xsdres.getRestrictions();

				if(ordered)
				{
					if(res.get(XSDRestriction.RESTRICTION_TOTAL_DIGITS))
					{
						// dangerous generalisation
						if(t < XSDType.TYPE_FLOAT)
						{
							debug("error: no total digits allowed");
							ret = false;
						}
					}
					if(res.get(XSDRestriction.RESTRICTION_FRACTION_DIGITS))
					{
						// dangerous generalisation
						if(t < XSDType.TYPE_DECIMAL)
						{
							String fdigits = xsdres.getFractionDigits();
							if(!fdigits.equals("0"))
							{
								debug("error: no fdigits allowed");
								ret = false;
							}
						}
						// dangerous generalisation
						else if(t != XSDType.TYPE_DECIMAL)
						{
							debug("error: no fdigits allowed");
							ret = false;
						}
					}
				}
				if(!ordered)
				{
					if((res.get(XSDRestriction.RESTRICTION_MIN_INCLUSIVE))
					|| (res.get(XSDRestriction.RESTRICTION_MAX_INCLUSIVE))
					|| (res.get(XSDRestriction.RESTRICTION_MIN_EXCLUSIVE))
					|| (res.get(XSDRestriction.RESTRICTION_MAX_EXCLUSIVE))
					|| (res.get(XSDRestriction.RESTRICTION_TOTAL_DIGITS))
					|| (res.get(XSDRestriction.RESTRICTION_FRACTION_DIGITS)))
					{
						debug("error: unapplicable restriction");
						ret = false;
					}
				}

				if(res.get(XSDRestriction.RESTRICTION_WHITE_SPACE))
				{
					if((t != XSDType.TYPE_STRING)
					&& (t != XSDType.TYPE_NORMALIZED_STRING))
					{
						String whitespace = xsdres.getWhiteSpace();
						if(!whitespace.equals("collapse"))
						{
							debug("error: only collapse allowed");
							ret = false;
						}
					}
				}

				if((res.get(XSDRestriction.RESTRICTION_LENGTH))
				|| (res.get(XSDRestriction.RESTRICTION_MIN_LENGTH))
				|| (res.get(XSDRestriction.RESTRICTION_MAX_LENGTH)))
				{
					// dangerous generalisation
					if((t >= XSDType.TYPE_INTEGER)
					&& (t <= XSDType.TYPE_G_DAY))
					{
						debug("error: no length restriction allowed");
						ret = false;
					}
				}
			}
			// all: length, minLength, maxLength, pattern, enumeration, whiteSpace
			// ordered: maxInclusive, maxExclusive, minInclusive, maxInclusive,
        	        //          totalDigits, fractionDigits

			// list: length, minLength, maxLength, pattern, enumeration
			// union: pattern, enumeration
		}

		debug("(-checker-) end");

		return ret;
	}
}

