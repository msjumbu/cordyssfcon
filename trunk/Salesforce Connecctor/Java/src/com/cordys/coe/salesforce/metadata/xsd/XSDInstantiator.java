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

import javax.xml.namespace.*;

import org.w3c.dom.*;

/**
 * \brief Instantiator class for schema expressions.
 *
 * The goal of this class is to be able to produce XML instance documents
 * whose structure is determined by the source schema.
 * This is needed to create initial instances for XForms models, for
 * example.
 * Smart values are used to honour restrictions on simple types.
 *
 * @author Josef Spillner <js177634@inf.tu-dresden.de>
 */
public class XSDInstantiator extends XMLBase
{
	/**
	 * Default constructor.
	 *
	 * Creates an empty instantiator object.
	 */
	public XSDInstantiator()
	{
		super();
	}

	/**
	 * Creates an instance of a complete schema.
	 *
	 * This method iterates over all top-level elements of a schema
	 * and converts them to XML instance data.
	 *
	 * @param xsdschema Schema to convert to an instance
	 *
	 * @return XML document containing the instances below the root element
	 *
	 * @see create
	 */
	public Document createall(XSDSchema xsdschema)
	{
		Document doc = XSDCommon.createDocument();

		Element xml_root = doc.createElement("INSTANCE-ROOT");
		doc.appendChild(xml_root);

		ArrayList list = xsdschema.getElements();
		for(int i = 0; i < list.size(); i++)
		{
			XSDElement xsdelement = (XSDElement)list.get(i);

			create(xsdelement, doc, xml_root, xsdschema.getTargetNamespace());
		}

		return doc;
	}

	/**
	 * Creates an instance of a schema element.
	 *
	 * This method generates an initial XML instance from a specific element
	 * from a schema. The type of the element and possibly further
	 * elements and attributes from the type are taken into account,
	 * so that all restrictions on the type are fulfilled by the instance.
	 *
	 * @param xsdelement Schema element to generate instance from
	 * @param doc XML document to put XML instance data in
	 * @param parent XML root node for the XML instance data
	 * @param namespace Namespace for the resulting instance
	 */
	public void create(XSDElement xsdelement, Document doc, Element parent, String namespace)
	{
		String ns_xsi = XSDCommon.NAMESPACE_XSI;
		String ns_i = namespace;

		XSDType t = xsdelement.getType();

		if(t.getType() == XSDType.TYPE_COMPLEX)
		{
			Element el;
			if(xsdelement.getQualified())
			{
				el = doc.createElementNS(ns_i, typename(xsdelement.getName()));
			}
			else
			{
				el = doc.createElement(xsdelement.getName().getLocalPart());
			}
			parent.appendChild(el);

			XSDSequence xsdseq = t.getSequence();
			if(xsdseq != null)
			{
				ArrayList list = xsdseq.getElements();
				for(int i = 0; i < list.size(); i++)
				{
					XSDElement xsdchild = (XSDElement)list.get(i);
					create(xsdchild, doc, el, namespace);
				}
			}

			// FIXME: how does SOAP handle (typed) attributes?
			ArrayList attributes = t.getAttributes();
			for(int i = 0; i < attributes.size(); i++)
			{
				XSDAttribute xsdatt = (XSDAttribute)attributes.get(i);

				createattribute(xsdatt, el, ns_i);
			}

			// Simple content or complex extension, or complex restriction
			XSDType basetype = t.getBaseType();
			if(basetype != null)
			{
				if(basetype.getType() == XSDType.TYPE_COMPLEX)
				{
					xsdseq = basetype.getSequence();
					if((xsdseq != null) && (!t.getRestricted()))
					{
						ArrayList list = xsdseq.getElements();
						for(int i = 0; i < list.size(); i++)
						{
							XSDElement xsdchild = (XSDElement)list.get(i);
							create(xsdchild, doc, el, namespace);
						}
					}

					attributes = basetype.getAttributes();
					for(int i = 0; i < attributes.size(); i++)
					{
						XSDAttribute xsdatt = (XSDAttribute)attributes.get(i);

						createattribute(xsdatt, el, ns_i);
					}
				}
				else
				{
					String smartinstance = smartvalue(basetype);
					if(smartinstance != null)
					{
						Text smart = doc.createTextNode(smartinstance);
						el.appendChild(smart);
					}
				}
			}
		}
		else if(t.getType() == XSDType.TYPE_CHOICE)
		{
			// FIXME: always use first one?
			XSDChoice xsdchoice = t.getChoice();
			ArrayList groups = xsdchoice.getGroups();
			ArrayList elements = xsdchoice.getElements();
			if(groups.size() > 0)
			{
				XSDSequence seq = (XSDSequence)groups.get(0);
				ArrayList seqelements = seq.getElements();

				for(int i = 0; i < seqelements.size(); i++)
				{
					XSDElement elem = (XSDElement)seqelements.get(i);
					create(elem, doc, parent, namespace);
				}
			}
			else if(elements.size() > 0)
			{
				XSDElement elem = (XSDElement)elements.get(0);
				create(elem, doc, parent, namespace);
			}
		}
		else if(t.getType() == XSDType.TYPE_GROUP)
		{
			XSDSequence xsdgroup = t.getSequence();
			ArrayList seqelements = xsdgroup.getElements();
			for(int i = 0; i < seqelements.size(); i++)
			{
				XSDElement elem = (XSDElement)seqelements.get(i);
				create(elem, doc, parent, namespace);
			}
		}
		else
		{
			QName nname = xsdelement.getName();
			QName nqtype = xsdelement.getType().getName();

			if(xsdelement.getMaxOccurs() == 0)
			{
				return;
			}
			// FIXME: handle minOccurs > 1

			Element el;
			if(xsdelement.getQualified())
			{
				el = doc.createElementNS(ns_i, typename(nname));
			}
			else
			{
				el = doc.createElement(nname.getLocalPart());
			}

			if(nqtype != null)
			{
				el.setAttributeNS(ns_xsi, "type", typename(nqtype));
			}
			parent.appendChild(el);

			String smartinstance = null;
			if(xsdelement.getFixedValue() != null)
			{
				smartinstance = xsdelement.getFixedValue();
			}
			else if(xsdelement.getDefaultValue() != null)
			{
				smartinstance = xsdelement.getDefaultValue();
			}
			else
			{
				smartinstance = smartvalue(xsdelement.getType());
			}
			if(smartinstance != null)
			{
				Text smart = doc.createTextNode(smartinstance);
				el.appendChild(smart);
			}

			if(xsdelement.getNillable())
			{
				el.setAttributeNS(ns_xsi, "nil", "false");
			}
		}
	}

	private void createattribute(XSDAttribute xsdatt, Element el, String ns_i)
	{
		if(xsdatt.getType().getType() == XSDType.TYPE_ATTRIBUTEGROUP)
		{
			XSDSequence group = xsdatt.getType().getSequence();
			ArrayList attributes = group.getAttributes();
			for(int i = 0; i < attributes.size(); i++)
			{
				XSDAttribute xsdatt2 = (XSDAttribute)attributes.get(i);
				createattribute(xsdatt2, el, ns_i);
			}
			return;
		}

		if(xsdatt.getUse() == XSDAttribute.USE_PROHIBITED)
		{
			return;
		}

		String smartinstance = null;
		if(xsdatt.getFixedValue() != null)
		{
			smartinstance = xsdatt.getFixedValue();
		}
		else if(xsdatt.getDefaultValue() != null)
		{
			smartinstance = xsdatt.getDefaultValue();
		}
		else
		{
			smartinstance = smartvalue(xsdatt.getType());
		}
		if(smartinstance == null)
		{
			smartinstance = "";
		}

		if(xsdatt.getQualified())
		{
			el.setAttributeNS(ns_i, typename(xsdatt.getName()), smartinstance);
		}
		else
		{
			el.setAttribute(xsdatt.getName().getLocalPart(), smartinstance);
			// FIXME: smart attributes
		}
	}

	// This method produces valid values for possibly restricted simple types
	// Sometimes, the empty string is valid, but often it is not!
	private String smartvalue(XSDType xsdtype)
	{
		// FIXME: smartness for non-string defaults!
		// (another valuable concept of WSGUI engines)
		// FIXME: smartness needed for all value-restricted type
		//        where empty string is not a valid value
		// FIXME: always keep in sync with xsdComponent
		int typetype = xsdtype.getType();

		if(typetype == XSDType.TYPE_LIST)
		{
			XSDType basetype = xsdtype.getBaseType();
			String listsmart = smartvalue(basetype);
			return listsmart;
		}
		else if(typetype == XSDType.TYPE_UNION)
		{
			ArrayList membertypes = xsdtype.getMemberTypes();
			if(membertypes.size() > 0)
			{
				XSDType uniontype = (XSDType)membertypes.get(0);
				String unionsmart = smartvalue(uniontype);
				return unionsmart;
			}
		}
		else if((typetype == XSDType.TYPE_INTEGER)
		|| (typetype == XSDType.TYPE_NON_NEGATIVE_INTEGER)
		|| (typetype == XSDType.TYPE_NON_POSITIVE_INTEGER)
		|| (typetype == XSDType.TYPE_LONG)
		|| (typetype == XSDType.TYPE_UNSIGNED_LONG)
		|| (typetype == XSDType.TYPE_INT)
		|| (typetype == XSDType.TYPE_UNSIGNED_INT)
		|| (typetype == XSDType.TYPE_SHORT)
		|| (typetype == XSDType.TYPE_UNSIGNED_SHORT)
		|| (typetype == XSDType.TYPE_BYTE)
		|| (typetype == XSDType.TYPE_UNSIGNED_BYTE)
		|| (typetype == XSDType.TYPE_DECIMAL)
		|| (typetype == XSDType.TYPE_FLOAT)
		|| (typetype == XSDType.TYPE_DOUBLE))
		{
			return "0";
		}
		else if(typetype == XSDType.TYPE_NEGATIVE_INTEGER)
		{
			return "-1";
		}
		else if(typetype == XSDType.TYPE_POSITIVE_INTEGER)
		{
			return "1";
		}
		else if(typetype == XSDType.TYPE_BOOLEAN)
		{
			return "false";
		}
		else if(typetype == XSDType.TYPE_DURATION)
		{
			return "P0D";
		}
		else if(typetype == XSDType.TYPE_DATE_TIME)
		{
			// FIXME: dynamic according to current date?
			return "2006-01-01T00:00:00";
		}
		else if(typetype == XSDType.TYPE_DATE)
		{
			return "2006-01-01";
		}
		else if(typetype == XSDType.TYPE_TIME)
		{
			return "00:00:00";
		}
		else if(typetype == XSDType.TYPE_G_YEAR)
		{
			return "2006";
		}
		else if(typetype == XSDType.TYPE_G_YEAR_MONTH)
		{
			return "2006-01";
		}
		else if(typetype == XSDType.TYPE_G_MONTH)
		{
			return "01"; // --MM
		}
		else if(typetype == XSDType.TYPE_G_MONTH_DAY)
		{
			return "01-01"; // --MM-DD
		}
		else if(typetype == XSDType.TYPE_G_DAY)
		{
			return "01"; // ---DD
		}
		else if(typetype == XSDType.TYPE_NAME)
		{
			return null; // ???
		}
		else if(typetype == XSDType.TYPE_QNAME)
		{
			return null; // ???
		}
		else if(typetype == XSDType.TYPE_NCNAME)
		{
			return null; // ???
		}
		else if(typetype == XSDType.TYPE_ANY_URI)
		{
			return "http://";
		}
		else if(typetype == XSDType.TYPE_LANGUAGE)
		{
			return "de";
		}
		else if(typetype == XSDType.TYPE_ID)
		{
			return null; // ???
		}
		else if(typetype == XSDType.TYPE_IDREF)
		{
			return null; // ???
		}
		else if(typetype == XSDType.TYPE_IDREFS)
		{
			return "x x"; // ???
		}
		else if(typetype == XSDType.TYPE_ENTITY)
		{
			return null; // ???
		}
		else if(typetype == XSDType.TYPE_ENTITIES)
		{
			return "x x"; // ??
		}
		else if(typetype == XSDType.TYPE_NOTATION)
		{
			return null;
		}
		else if(typetype == XSDType.TYPE_NMTOKEN)
		{
			return null; // ???
		}
		else if(typetype == XSDType.TYPE_NMTOKENS)
		{
			return "x x";
		}

		XSDRestriction xsdres = xsdtype.getRestriction();
		if(xsdres != null)
		{
			BitSet res = xsdres.getRestrictions();

			if(res.get(XSDRestriction.RESTRICTION_ENUMERATION))
			{
				ArrayList list = xsdres.getEnumerations();

				if(list.size() > 0)
				{
					String enumeration = (String)list.get(0);
					return enumeration;
				}
			}

			boolean range = false;
			String startrange = null;
			String endrange = null;

			if(res.get(XSDRestriction.RESTRICTION_MIN_INCLUSIVE))
			{
				startrange = xsdres.getMinInclusive();
			}
			if(res.get(XSDRestriction.RESTRICTION_MAX_INCLUSIVE))
			{
				endrange = xsdres.getMaxInclusive();
			}
			if(res.get(XSDRestriction.RESTRICTION_MIN_EXCLUSIVE))
			{
				startrange = xsdres.getMinExclusive();
			}
			if(res.get(XSDRestriction.RESTRICTION_MAX_EXCLUSIVE))
			{
				endrange = xsdres.getMaxExclusive();
			}
			// FIXME: exclude exclusive values but we don't know the type
			if((startrange != null) && (endrange != null))
			{
				range = true;
			}

			if(range)
			{
				String minstring = startrange;
				return minstring;
			}

			if(res.get(XSDRestriction.RESTRICTION_TOTAL_DIGITS))
			{
				Integer digits = new Integer(xsdres.getTotalDigits());
				int totaldigits = digits.intValue();
				// FIXME!
			}
			if(res.get(XSDRestriction.RESTRICTION_FRACTION_DIGITS))
			{
				Integer digits = new Integer(xsdres.getFractionDigits());
				int fractiondigits = digits.intValue();
				// FIXME!
			}

			int minlength = -1;
			int maxlength = -1;

			if(res.get(XSDRestriction.RESTRICTION_LENGTH))
			{
				Integer both = new Integer(xsdres.getLength());
				minlength = both.intValue();
				maxlength = both.intValue();
			}
			if(res.get(XSDRestriction.RESTRICTION_MIN_LENGTH))
			{
				Integer min = new Integer(xsdres.getMinLength());
				minlength = min.intValue();
			}
			if(res.get(XSDRestriction.RESTRICTION_MAX_LENGTH))
			{
				Integer max = new Integer(xsdres.getMaxLength());
				maxlength = max.intValue();
			}

			if(res.get(XSDRestriction.RESTRICTION_PATTERN))
			{
				String instance = null;
				String pattern = xsdres.getPattern();

				// Uncomment below for RegExpInstantiator inclusion

//				RegExpConstraint rec = new RegExpConstraint();
//				rec.addConstraint(RegExpConstraint.MIN_LENGTH, minlength);
//				rec.addConstraint(RegExpConstraint.MAX_LENGTH, maxlength);
//				RegExpInstantiator rei = new RegExpInstantiator();
//				RegExpExpression ree = rei.build(pattern);
//				RegExpExpression reec = rei.constrain(ree, rec);
//				instance = reec.instanceString();

				return instance;
			}

			if(res.get(XSDRestriction.RESTRICTION_WHITE_SPACE))
			{
				// ignore???
			}
		}

		return null;
	}
}

