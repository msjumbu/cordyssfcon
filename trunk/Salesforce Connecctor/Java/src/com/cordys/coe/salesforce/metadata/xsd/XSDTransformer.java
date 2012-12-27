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

/**
 * \brief Transformer for \ref XSDSchema objects to increase access level.
 *
 * Depending on the desired level of comfort when working with schemas
 * and the performance needed from the \ref XSDParser, it is possible
 * to only parse a schema minimally and resolve all the cross-references
 * later.
 * The transformer class can augment \ref XSDParser::PARSER_FLAT schemas
 * to \ref XSDParser::PARSER_FLAT_INCLUDES by fetching all listed includes
 * and imports and letting them be parsed and the results be appended to
 * the schema object. It can then augment further to \ref
 * XSDParser::PARSER_TREE by resolving all references to elements,
 * attributes and types as well as base type references within types
 * to the proper objects.
 * Finally, augmentation can lead to \ref XSDParser::PARSER_MERGED by
 * transforming all attribute groups into their individual attributes,
 * similarly for element groups, and by detaching all shared type
 * information so that each element will get its proper type object and
 * thus each type object can refer to its parent element, which allows
 * for easy reverse tree traversal.
 *
 * Another feature of the transformer is to get rid of all of the
 * top-level definitions not needed for a certain element, returning a
 * shallow copy of the schema without changing its access level.
 *
 * @author Josef Spillner <js177634@inf.tu-dresden.de>
 */
public class XSDTransformer extends XMLBase
{
	// Top-level XSDAttributes in a schema
	private ArrayList<XSDAttribute> allattributes = null;
	// Top-level XSDElements in a schema
	private ArrayList<XSDElement> allelements = null;
	// Top-level XSDSequences (element and attribute groups) in a schema
	private ArrayList<XSDSequence> allgroups = null;
	// Top-level XSDTypes (complex and simple) in a schema
	private ArrayList<XSDType> alltypes = null;

	/**
	 * Default constructor.
	 *
	 * Produces a transformer object which can then be used to
	 * advance the level of access on schema objects.
	 */
	public XSDTransformer()
	{
		super();
	}

	// FIXME: copyelement and copytype(_xxx) are not complete yet
	// FIXME: use deref_* internally? must then also include overrides!

	private XSDElement copyelement(XSDElement xsdel)
	{
		XSDElement xsdel2 = new XSDElement();

		xsdel2.setMinOccurs(xsdel.getMinOccurs());
		xsdel2.setMaxOccurs(xsdel.getMaxOccurs());
		xsdel2.setName(xsdel.getName());
		xsdel2.setType(xsdel.getType());

		return xsdel2;
	}

	private XSDType copytype(XSDType t)
	{
		XSDType t2 = new XSDType();

		t2.setName(t.getName());
		//t2.setSequence(t.getSequence());
		t2.setRestriction(t.getRestriction());
		t2.setType(t.getType());

		for(int i = 0; i < t.getAttributes().size(); i++)
		{
			XSDAttribute xsdatt = (XSDAttribute)t.getAttributes().get(i);
			t2.addAttribute(xsdatt);
		}

		XSDSequence xsdseq = new XSDSequence();
		for(int i = 0; i < t.getSequence().getElements().size(); i++)
		{
			XSDElement xsdel = (XSDElement)t.getSequence().getElements().get(i);
			xsdel = copyelement(xsdel);
			xsdel.setParentSequence(xsdseq);
			xsdseq.addElement(xsdel);
		}
		xsdseq.setParentType(t2);

		t2.setSequence(xsdseq);

		return t2;
	}

	// FIXME: from resolver(), merge with copytype() as needed, then remove
	/*private void copytype_xxx(XSDType xsdtype, XSDType xsdtype2)
	{
		xsdtype.setSequence(xsdtype2.getSequence());
		xsdtype.setRestriction(xsdtype2.getRestriction());
		xsdtype.setType(xsdtype2.getType());
		xsdtype.setBaseType(xsdtype2.getBaseType());
		ArrayList attlist = xsdtype2.getAttributes();
		ArrayList mtlist = xsdtype2.getMemberTypes();
		for(int k = 0; k < attlist.size(); k++)
		{
			XSDAttribute att = (XSDAttribute)attlist.get(k);
			xsdtype.addAttribute(att);
		}
		for(int k = 0; k < mtlist.size(); k++)
		{
			XSDType t = (XSDType)mtlist.get(k);
			xsdtype.addMemberType(t);
		}
		xsdtype.setChoice(xsdtype2.getChoice());
	}*/

	// Helper for PARSER_MERGED augmentation
	private void detachTypes(XSDElement xsdelement)
	{
		debug("// Detach types for " + xsdelement.getName());

		XSDType t = xsdelement.getType();

		if(t.getName() == null)
		{
			// inline type
			debug("* type for " + xsdelement.getName() + " is inline");
			//return;
		}
		else
		{
			if(t.getParentElement() != null)
			{
				String dets = "* detach " + t.getName() + ": ";
				dets += t.getParentElement().getName() + " / ";
				dets += xsdelement.getName();
				dets += "?";
				debug(dets);

				if(!xsdelement.getName().equals(t.getParentElement().getName()))
				{
					debug("DETACH!");
					t = copytype(t);
					t.setParentElement(xsdelement);
					xsdelement.setType(t);
				}
			}
			else
			{
				if(t.getType() == XSDType.TYPE_COMPLEX)
				{
					String dets = "* assign " + t.getName() + ": ";
					dets += xsdelement.getName();
					debug(dets);
					t.setParentElement(xsdelement);
				}
			}
		}

		if(t.getType() == XSDType.TYPE_COMPLEX)
		{
			XSDSequence xsdseq = t.getSequence();
			if(xsdseq != null)
			{
				ArrayList list = xsdseq.getElements();
				for(int i = 0; i < list.size(); i++)
				{
					XSDElement xsdchild = (XSDElement)list.get(i);
					detachTypes(xsdchild);
				}
			}
		}
	}

	// Helper for PARSER_MERGED augmentation
	private void dissolveGroups(XSDSchema xsdschema)
	{
		debug("// Dissolve groups");

		// Find all types (top-level and inline) first

		checkAll(xsdschema);

		// Now see if they've got attribute groups (if complex)

		for(int i = 0; i < alltypes.size(); i++)
		{
			XSDType xsdtype = alltypes.get(i);

			if(xsdtype.getType() != XSDType.TYPE_COMPLEX)
			{
				continue;
			}

			debug("* complextype: " + xsdtype.getName());

			// Dissolve element groups
			XSDSequence xsdseq = xsdtype.getSequence();
			if(xsdseq != null)
			{
				XSDSequence newseq = new XSDSequence();
				ArrayList elements = xsdseq.getElements();
				for(int j = 0; j < elements.size(); j++)
				{
					XSDElement xsdelement = (XSDElement)elements.get(j);
					XSDType t = xsdelement.getType();

					if(t.getType() == XSDType.TYPE_GROUP)
					{
						XSDSequence xsdgroup = t.getSequence();
						debug("* found group " + xsdgroup.getGroupName());

						ArrayList elements2 = xsdgroup.getElements();
						for(int k = 0; k < elements2.size(); k++)
						{
							XSDElement xsdel = (XSDElement)elements2.get(k);
							newseq.addElement(xsdel);
						}
					}
					else if(t.getType() == XSDType.TYPE_CHOICE)
					{
						// FIXME!
						newseq.addElement(xsdelement);
					}
					else
					{
						newseq.addElement(xsdelement);
					}
				}

				// FIXME: stuff might be lost from xsdseq, check!
				xsdtype.setSequence(newseq);
			}

			// Dissolve attribute groups
			ArrayList<XSDAttribute> newattributes = new ArrayList<XSDAttribute>();
			ArrayList attributes = xsdtype.getAttributes();
			for(int j = 0; j < attributes.size(); j++)
			{
				XSDAttribute xsdattribute = (XSDAttribute)attributes.get(j);
				XSDType t = xsdattribute.getType();

				if(t.getType() == XSDType.TYPE_ATTRIBUTEGROUP)
				{
					XSDSequence xsdgroup = t.getSequence();
					debug("* found attribute group " + xsdgroup.getGroupName());

					ArrayList attributes2 = xsdgroup.getAttributes();
					for(int k = 0; k < attributes2.size(); k++)
					{
						XSDAttribute xsdatt = (XSDAttribute)attributes2.get(k);
						newattributes.add(xsdatt);
					}
				}
				else
				{
					// normal attribute
					newattributes.add(xsdattribute);
				}
			}

			xsdtype.setAttributes(newattributes);
		}

		// FIXME: substitute in complex types, then remove globals
		// FIXME: must dissolve element groups also, both in choices and in sequences!
		// FIXME: doesn't work for choices!!! - but for detached choices? (ie. remove ref)
		xsdschema.clearGroups();
	}

	/**
	 * Augment the access level to a schema object.
	 *
	 * The transformer makes it possible to advance one or several
	 * levels of access to a schema object. The lowest level
	 * (\ref XSDParser::PARSER_FLAT) can be augmented to fetch all
	 * missing include files (\ref XSDParser::PARSER_FLAT_INCLUDES).
	 * Once this is done, type and base type references as well as
	 * element references can be resolved to build up an object
	 * tree (\ref XSDParser::PARSER_TREE).
	 * Finally, if wanted, \ref XSDParser::PARSER_MERGED can be used
	 * to heavily change the structure so that all groups and attribute
	 * groups are replaced by their simple containments.
	 *
	 * It is only possible to advance one level, but never to go back,
	 * which would make this method return \b false.
	 * Returning \b false might also happen if include files cannot
	 * be loaded or if types or references are missing during resolving
	 * them.
	 *
	 * For convenience, advancing several levels at once is supported.
	 *
	 * @param xsdschema Schema object whose level should be advanced
	 * @param level The desired new access level
	 *
	 * @return Whether or not the augmentation succeeded
	 */
	public boolean augment(XSDSchema xsdschema, int level)
	{
		if(xsdschema == null)
		{
			debug("Error: augmentation on null schema");
			return false;
		}

		int oldlevel = xsdschema.getLevel();

		if(oldlevel > level)
		{
			debug("Error: backwards augmentation");
			return false;
		}
		else if(oldlevel == level)
		{
			return true;
		}


		// Advance all the levels until the requested one recursively
		boolean ret = augment(xsdschema, level - 1);
		if(ret == false)
		{
			return false;
		}

		debug("=== Augmentation ===");
		debug("From level " + (level - 1) + " to level " + level);

		if(level == XSDParser.PARSER_FLAT_INCLUDES)
		{
			return augmentFlatIncludes(xsdschema);
		}
		else if(level == XSDParser.PARSER_TREE)
		{
			return augmentTree(xsdschema);
		}
		else if(level == XSDParser.PARSER_MERGED)
		{
			return augmentMerged(xsdschema);
		}
		else
		{
			return false;
		}
	}

	/**
	 * Creates a partial copy of a schema.
	 *
	 * While a schema contains a lot of top-level elements, attributes
	 * and so on, sometimes only one element and all of its dependencies
	 * are of interest.
	 * This method takes an element and returns a schema from it which
	 * resembles the element's parent schema, however all entries not
	 * referenced recursively by the element are discarded.
	 * Also, all probably merged-in XSD built-in types will be discarded.
	 * The element's namespace URI will become the schema's target
	 * namespace.
	 *
	 * Note that the contents of the returned schema are shallow copies,
	 * so modifying the partial schema will also modify the original one.
	 *
	 * Warning: The parent schema must be of level \ref
	 * XSDParser.PARSER_TREE at least to make this method work.
	 *
	 * @param xsdelement Element to which the partial schema should be restricted
	 *
	 * @return Partial schema as a shallow copy from the element's parent schema
	 */
	public XSDSchema copyPartial(XSDElement xsdelement, XSDSchema xsdschema)
	{
		reset();

		checkElement(xsdelement);

		XSDSchema xsdpartial = new XSDSchema();
		xsdpartial.setTargetNamespace(xsdelement.getName().getNamespaceURI());
		xsdpartial.setLevel(XSDParser.PARSER_TREE);
		// FIXME: what if PARSER_MERGED was input?

		String ns_xsd = XSDCommon.NAMESPACE_XSD;

		// Only top-level entries are considered
		for(int i = 0; i < allelements.size(); i++)
		{
			XSDElement xsdel = allelements.get(i);
			if(xsdel.getName() != null)
			{
				if(findElement(xsdschema.getElements(), xsdel.getName()) != null)
				{
					xsdpartial.addElement(xsdel);
				}
			}
		}

		for(int i = 0; i < allattributes.size(); i++)
		{
			XSDAttribute xsdatt = allattributes.get(i);
			if(xsdatt.getName() != null)
			{
				if(findAttribute(xsdschema.getAttributes(), xsdatt.getName()) != null)
				{
					xsdpartial.addAttribute(xsdatt);
				}
			}
		}

		for(int i = 0; i < alltypes.size(); i++)
		{
			XSDType xsdtype = alltypes.get(i);
			if(xsdtype.getName() != null)
			{
				if(xsdtype.getName().getNamespaceURI() != ns_xsd)
				{
					if(findType(xsdschema.getTypes(), xsdtype.getName()) != null)
					{
						xsdpartial.addType(xsdtype);
					}
				}
			}
		}

		for(int i = 0; i < allgroups.size(); i++)
		{
			XSDSequence xsdgroup = allgroups.get(i);
			if(xsdgroup.getGroupName() != null)
			{
				if(findGroup(xsdschema.getGroups(), xsdgroup.getGroupName()) != null)
				{
					xsdpartial.addGroup(xsdgroup);
				}
			}
		}

		debug("*Partial statistics*");
		debug(" - Global elements:   " + xsdpartial.getElements().size());
		debug(" -   Orig elements:   " + xsdschema.getElements().size());
		debug(" - Global attributes: " + xsdschema.getAttributes().size());
		debug(" -   Orig attributes: " + xsdschema.getAttributes().size());
		debug(" - Global types     : " + xsdschema.getTypes().size());
		debug(" -   Orig types     : " + xsdschema.getTypes().size());
		debug(" - Global groups    : " + xsdpartial.getGroups().size());
		debug(" -   Orig groups    : " + xsdschema.getGroups().size());

		return xsdschema;
	}

	// Advances from PARSER_TREE to PARSER_MERGED
	private boolean augmentMerged(XSDSchema xsdschema)
	{
		dissolveGroups(xsdschema);

		// FIXME: attributes as well?
		ArrayList elements = xsdschema.getElements();
		for(int i = 0; i < elements.size(); i++)
		{
			detachTypes((XSDElement)elements.get(i));
		}

		xsdschema.setLevel(XSDParser.PARSER_MERGED);

		return true;
	}

	// Advances from PARSER_FLAT to PARSER_FLAT_INCLUDES
	private boolean augmentFlatIncludes(XSDSchema xsdschema)
	{
		ArrayList includes = xsdschema.getIncludes();
		ArrayList imports = xsdschema.getImports();

		if((includes.size() > 0) || (imports.size() > 0))
		{
			XSDParser parser = new XSDParser();
			xsdschema = parser.parseIncludes(xsdschema);
			debug(parser.getDebug());
		}

		if(xsdschema != null)
		{
			xsdschema.setLevel(XSDParser.PARSER_FLAT_INCLUDES);
			return true;
		}
		else
		{
			return false;
		}
	}

	// See augmentTree()
	private void anytypehack()
	{
		for(int i = 0; i < allelements.size(); i++)
		{
			XSDElement xsdelement = allelements.get(i);
			if((xsdelement.getTypeRef() == null) && (xsdelement.getRef() == null))
			{
				if(xsdelement.getType() == null)
				{
					debug("-- anytype for element " + xsdelement.getName());

					XSDType xsdtype = new XSDType();
					xsdtype.setType(XSDType.TYPE_ANY_TYPE);
					xsdelement.setType(xsdtype);
				}
			}
		}

		for(int i = 0; i < allattributes.size(); i++)
		{
			XSDAttribute xsdattribute = allattributes.get(i);
			if((xsdattribute.getTypeRef() == null) && (xsdattribute.getRef() == null))
			{
				if(xsdattribute.getType() == null)
				{
					debug("-- anytype for attribute " + xsdattribute.getName());

					XSDType xsdtype = new XSDType();
					xsdtype.setType(XSDType.TYPE_ANY_TYPE);
					xsdattribute.setType(xsdtype);
				}
			}
		}
	}

	// Advances from PARSER_FLAT_INCLUDES to PARSER_TREE
	private boolean augmentTree(XSDSchema xsdschema)
	{
		// Find all top-level and inline entries first

		checkAll(xsdschema);

		ArrayList elements = xsdschema.getElements();
		ArrayList attributes = xsdschema.getAttributes();
		ArrayList types = xsdschema.getTypes();
		ArrayList groups = xsdschema.getGroups();

		debug("*Tree level augmentation statistics*");
		debug(" - Global elements:   " + elements.size());
		debug(" -  Total elements:   " + allelements.size());
		debug(" - Global attributes: " + attributes.size());
		debug(" -  Total attributes: " + allattributes.size());
		debug(" - Global types     : " + types.size());
		debug(" -  Total types     : " + alltypes.size());
		debug(" - Global groups    : " + groups.size());
		debug(" -  Total groups    : " + allgroups.size());

		// Make sure all elements and attributes have anytype or a reference
		// FIXME: hack?
		anytypehack();

		// Resolve all type references
		for(int i = 0; i < alltypes.size(); i++)
		{
			// Base type for types like lists, derived/extended etc.
			XSDType xsdtype = alltypes.get(i);
			if(xsdtype.getBaseRef() != null)
			{
				XSDType xsdref = findType(types, xsdtype.getBaseRef());
				if(xsdref != null)
				{
					xsdtype.setBaseType(xsdref);
				}
				else
				{
					return false;
				}
			}

			// Member types for unions
			ArrayList memberrefs = xsdtype.getMemberRefs();
			for(int j = 0; j < memberrefs.size(); j++)
			{
				QName memberref = (QName)memberrefs.get(j);
				XSDType xsdref = findType(types, memberref);
				if(xsdref != null)
				{
					xsdtype.addMemberType(xsdref);
				}
				else
				{
					return false;
				}
			}
		}

		// Resolve all element types
		for(int i = 0; i < allelements.size(); i++)
		{
			XSDElement xsdelement = allelements.get(i);
			if(xsdelement.getTypeRef() != null)
			{
				XSDType xsdref = findType(types, xsdelement.getTypeRef());
				if(xsdref != null)
				{
					xsdelement.setType(xsdref);
				}
				else
				{
					return false;
				}
			}
		}

		// Resolve all attribute types
		for(int i = 0; i < allattributes.size(); i++)
		{
			XSDAttribute xsdattribute = allattributes.get(i);
			if(xsdattribute.getTypeRef() != null)
			{
				XSDType xsdref = findType(types, xsdattribute.getTypeRef());
				if(xsdref != null)
				{
					xsdattribute.setType(xsdref);
				}
				else
				{
					return false;
				}
			}
		}

		// Resolve references to other elements
		for(int i = 0; i < allelements.size(); i++)
		{
			XSDElement xsdelement = allelements.get(i);
			if(xsdelement.getRef() != null)
			{
				XSDElement xsdref = findElement(elements, xsdelement.getRef());
				if(xsdref != null)
				{
					deref_element(xsdref, xsdelement);
				}
				else
				{
					return false;
				}
			}
		}

		// Resolve references to other attributes
		for(int i = 0; i < allattributes.size(); i++)
		{
			XSDAttribute xsdattribute = allattributes.get(i);
			if(xsdattribute.getRef() != null)
			{
				XSDAttribute xsdref = findAttribute(attributes, xsdattribute.getRef());
				if(xsdref != null)
				{
					deref_attribute(xsdref, xsdattribute);
				}
				else
				{
					return false;
				}
			}
		}

		// Resolve references to other groups
		for(int i = 0; i < allgroups.size(); i++)
		{
			XSDSequence xsdgroup = allgroups.get(i);
			if(xsdgroup.getGroupRef() != null)
			{
				XSDSequence xsdref = findGroup(groups, xsdgroup.getGroupRef());
				if(xsdref != null)
				{
					deref_group(xsdref, xsdgroup);
				}
				else
				{
					return false;
				}
			}
		}

		xsdschema.setLevel(XSDParser.PARSER_TREE);

		return true;
	}

	// Returns the attribute object for a XSD attribute reference string
	private XSDAttribute findAttribute(ArrayList attributes, QName ref)
	{
		for(int j = 0; j < attributes.size(); j++)
		{
			XSDAttribute refattribute = (XSDAttribute)attributes.get(j);
			if(ref.equals(refattribute.getName()))
			{
				debug("+ attribute " + ref);
				return refattribute;
			}
		}

		debug("Error: attribute " + ref + " not found");
		return null;
	}

	// Returns the element object for a XSD element reference string
	private XSDElement findElement(ArrayList elements, QName ref)
	{
		for(int j = 0; j < elements.size(); j++)
		{
			XSDElement refelement = (XSDElement)elements.get(j);
			if(ref.equals(refelement.getName()))
			{
				debug("+ element " + ref);
				return refelement;
			}
		}

		// Built-in elements from the XSD specification
		String ns_xsd = XSDCommon.NAMESPACE_XSD;
		if(ref.getNamespaceURI().equals(ns_xsd))
		{
			// FIXME: non-public, only for bootstrapping?
			if(ref.getLocalPart().equals("annotation"))
			{
				XSDType xsdtype = new XSDType();
				XSDElement xsdelement = new XSDElement();
				xsdelement.setName(ref);
				xsdelement.setType(xsdtype);
				return xsdelement;
			}
		}

		debug("Error: element " + ref + " not found");
		return null;
	}

	// Returns the type object for a XSD type reference string
	// For built-in types, creates type object ad-hoc!
	private XSDType findType(ArrayList types, QName ref)
	{
		// Built-in types from the XSD specification
		String ns_xsd = XSDCommon.NAMESPACE_XSD;
		if(ref.getNamespaceURI().equals(ns_xsd))
		{
			int typetype = XSDCommon.nameToType(ref.getLocalPart());
			if(typetype != XSDType.TYPE_INVALID)
			{
				XSDType xsdtype = new XSDType();
				xsdtype.setType(typetype);
				xsdtype.setName(new QName(ns_xsd, ref.getLocalPart()));
				debug("+ type (built-in) " + ref);
				return xsdtype;
			}

			// Look in derived list, might be non-public utility types
			// FIXME: this will be normal for most types once bootstrapping works
			for(int j = 0; j < types.size(); j++)
			{
				XSDType reftype = (XSDType)types.get(j);
				if(ref.equals(reftype.getName()))
				{
					debug("+ type (built-in/non-public) " + ref);
					return reftype;
				}
			}

			debug("Error: type (built-in) " + ref + " not found");
			return null;
		}

		// Derived types listed in the schema
		for(int j = 0; j < types.size(); j++)
		{
			XSDType reftype = (XSDType)types.get(j);
			if(ref.equals(reftype.getName()))
			{
				debug("+ type " + ref);
				return reftype;
			}
		}

		debug("Error: type " + ref + " not found");
		return null;
	}

	// Returns the group object for a XSD group reference string
	private XSDSequence findGroup(ArrayList groups, QName ref)
	{
		for(int j = 0; j < groups.size(); j++)
		{
			XSDSequence refgroup = (XSDSequence)groups.get(j);
			if(ref.equals(refgroup.getGroupName()))
			{
				debug("+ group " + ref);
				return refgroup;
			}
		}

		debug("Error: group " + ref + " not found");
		return null;
	}

	// Recursively finds all top-level and inline entries
	private void checkAll(XSDSchema xsdschema)
	{
		reset();

		ArrayList types = xsdschema.getTypes();
		for(int i = 0; i < types.size(); i++)
		{
			checkType((XSDType)types.get(i));
		}

		ArrayList elements = xsdschema.getElements();
		for(int i = 0; i < elements.size(); i++)
		{
			checkElement((XSDElement)elements.get(i));
		}

		ArrayList attributes = xsdschema.getAttributes();
		for(int i = 0; i < attributes.size(); i++)
		{
			checkAttribute((XSDAttribute)attributes.get(i));
		}

		ArrayList groups = xsdschema.getGroups();
		for(int i = 0; i < groups.size(); i++)
		{
			checkGroup((XSDSequence)groups.get(i));
		}
	}

	// Recursively finds child entries of this pseudo-type
	private void checkChoice(XSDChoice xsdchoice)
	{
		ArrayList elements = xsdchoice.getElements();
		for(int i = 0; i < elements.size(); i++)
		{
			checkElement((XSDElement)elements.get(i));
		}

		ArrayList groups = xsdchoice.getGroups();
		for(int i = 0; i < groups.size(); i++)
		{
			checkGroup((XSDSequence)groups.get(i));
		}
	}

	// Recursively finds this type and child entries
	private void checkType(XSDType xsdtype)
	{
		alltypes.add(xsdtype);

		if(xsdtype.getType() == XSDType.TYPE_UNION)
		{
			ArrayList members = xsdtype.getMemberTypes();
			for(int i = 0; i < members.size(); i++)
			{
				XSDType member = (XSDType)members.get(i);
				checkType(member);
			}
		}
		else if(xsdtype.getType() == XSDType.TYPE_LIST)
		{
			if(xsdtype.getBaseType() != null)
			{
				checkType(xsdtype.getBaseType());
			}
		}
		else if(xsdtype.getType() == XSDType.TYPE_COMPLEX)
		{
			XSDSequence xsdseq = xsdtype.getSequence();
			if(xsdseq != null)
			{
				ArrayList elements = xsdseq.getElements();
				for(int i = 0; i < elements.size(); i++)
				{
					checkElement((XSDElement)elements.get(i));
				}
			}

			ArrayList attributes = xsdtype.getAttributes();
			for(int i = 0; i < attributes.size(); i++)
			{
				checkAttribute((XSDAttribute)attributes.get(i));
			}
		}
	}

	// Recursively finds this group and child entries
	private void checkGroup(XSDSequence xsdgroup)
	{
		allgroups.add(xsdgroup);

		if(xsdgroup.getGroupAttribute())
		{
			ArrayList attributes = xsdgroup.getAttributes();
			for(int i = 0; i < attributes.size(); i++)
			{
				checkAttribute((XSDAttribute)attributes.get(i));
			}
		}
		else
		{
			ArrayList elements = xsdgroup.getElements();
			for(int i = 0; i < elements.size(); i++)
			{
				checkElement((XSDElement)elements.get(i));
			}
		}
	}

	// Finds this attribute
	private void checkAttribute(XSDAttribute xsdattribute)
	{
		XSDType xsdtype = xsdattribute.getType();
		if(xsdtype != null)
		{
			checkType(xsdtype);

			if(xsdtype.getType() == XSDType.TYPE_ATTRIBUTEGROUP)
			{
				checkGroup(xsdtype.getSequence());
				return;
			}
		}

		allattributes.add(xsdattribute);
	}

	// Recursively finds this element and child entries
	private void checkElement(XSDElement xsdelement)
	{
		XSDType xsdtype = xsdelement.getType();
		if(xsdtype != null)
		{
			checkType(xsdtype);

			if(xsdtype.getType() == XSDType.TYPE_CHOICE)
			{
				checkChoice(xsdtype.getChoice());
				return;
			}
			else if(xsdtype.getType() == XSDType.TYPE_GROUP)
			{
				checkGroup(xsdtype.getSequence());
				return;
			}
		}

		allelements.add(xsdelement);
	}

	// Empties the lists of found top-level entries
	private void reset()
	{
		allattributes = new ArrayList<XSDAttribute>();
		allelements = new ArrayList<XSDElement>();
		allgroups = new ArrayList<XSDSequence>();
		alltypes = new ArrayList<XSDType>();
	}

	// Resolves an element reference, produces a shallow copy
	private void deref_element(XSDElement xsdfrom, XSDElement xsdto)
	{
		// minOccurs, maxOccurs are overridden in elements
		xsdto.setName(xsdfrom.getName());
		xsdto.setType(xsdfrom.getType());
		xsdto.setAnnotation(xsdfrom.getAnnotation());
		xsdto.setQualified(xsdfrom.getQualified());
		xsdto.setNillable(xsdfrom.getNillable());
		xsdto.setFixedValue(xsdfrom.getFixedValue());
		xsdto.setDefaultValue(xsdfrom.getDefaultValue());
	}

	// Resolves an attribute reference, produces a shallow copy
	private void deref_attribute(XSDAttribute xsdfrom, XSDAttribute xsdto)
	{
		// use is overridded in attributes
		xsdto.setName(xsdfrom.getName());
		xsdto.setType(xsdfrom.getType());
		xsdto.setAnnotation(xsdfrom.getAnnotation());
		xsdto.setQualified(xsdfrom.getQualified());
		xsdto.setFixedValue(xsdfrom.getFixedValue());
		xsdto.setDefaultValue(xsdfrom.getDefaultValue());
	}

	// Resolves a group reference, producing a shallow copy
	private void deref_group(XSDSequence xsdfrom, XSDSequence xsdto)
	{
		//xsdto.setElements(xsdfrom.getElements());
		ArrayList elements = xsdfrom.getElements();
		for(int i = 0; i < elements.size(); i++)
		{
			XSDElement xsdelement = (XSDElement)elements.get(i);
			xsdto.addElement(xsdelement);
		}
		xsdto.setParentType(xsdfrom.getParentType());
		xsdto.setGroupName(xsdfrom.getGroupName());
		xsdto.setAll(xsdfrom.getAll());
		//xsdto.setAttributes(xsdfrom.getAttributes());
		ArrayList attributes = xsdfrom.getAttributes();
		for(int i = 0; i < attributes.size(); i++)
		{
			XSDAttribute xsdattribute = (XSDAttribute)attributes.get(i);
			xsdto.addAttribute(xsdattribute);
		}
		xsdto.setGroupAttribute(xsdfrom.getGroupAttribute());
	}

	// Resolves a type reference, producing a shallow copy
//	private void debase_type(XSDType xsdfrom, XSDType xsdto)
//	{
//		xsdto.setBaseRef(xsdfrom.getBaseRef());
//		xsdto.setBaseType(xsdfrom.getBaseType());
//		// FIXME: why not just setMembers/setMemberRefs/setAttributes??
//		// FIXME: answer: because there's no such convenience method!
//		//xsdto.setMemberRefs(xsdfrom.getMemberRefs());
//		//xsdto.setMembers(xsdfrom.getMembers());
//		ArrayList memberrefs = xsdfrom.getMemberRefs();
//		ArrayList members = xsdfrom.getMemberTypes();
//		for(int i = 0; i < memberrefs.size(); i++)
//		{
//			QName memberref = (QName)memberrefs.get(i);
//			xsdto.addMemberRef(memberref);
//		}
//		for(int i = 0; i < members.size(); i++)
//		{
//			XSDType xsdtype = (XSDType)members.get(i);
//			xsdto.addMemberType(xsdtype);
//		}
//
//		xsdto.setSequence(xsdfrom.getSequence());
//		xsdto.setRestriction(xsdfrom.getRestriction());
//		//xsdto.setAttributes(xsdfrom.getAttributes());
//		ArrayList attributes = xsdfrom.getAttributes();
//		for(int i = 0; i < attributes.size(); i++)
//		{
//			XSDAttribute xsdattribute = (XSDAttribute)attributes.get(i);
//			xsdto.addAttribute(xsdattribute);
//		}
//		xsdto.setChoice(xsdfrom.getChoice());
//
//		xsdto.setRestricted(xsdfrom.getRestricted());
//		xsdto.setMixed(xsdfrom.getMixed());
//	}
}

