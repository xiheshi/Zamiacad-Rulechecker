package org.zamia.plugin.tool.vhdl;

import java.io.IOException;

import org.zamia.ZamiaException;
import org.zamia.ZamiaLogger;
import org.zamia.analysis.ast.ASTDeclarationSearch;
import org.zamia.plugin.tool.vhdl.manager.ToolManager;
import org.zamia.vhdl.ast.DeclarativeItem;
import org.zamia.vhdl.ast.DiscreteRange;
import org.zamia.vhdl.ast.InterfaceDeclaration;
import org.zamia.vhdl.ast.InterfaceList;
import org.zamia.vhdl.ast.Name;
import org.zamia.vhdl.ast.Range;
import org.zamia.vhdl.ast.SignalDeclaration;
import org.zamia.vhdl.ast.TypeDeclaration;
import org.zamia.vhdl.ast.TypeDefinition;
import org.zamia.vhdl.ast.TypeDefinitionConstrainedArray;
import org.zamia.vhdl.ast.TypeDefinitionEnum;
import org.zamia.vhdl.ast.TypeDefinitionRecord;
import org.zamia.vhdl.ast.TypeDefinitionSubType;
import org.zamia.vhdl.ast.VHDLNode;

public abstract class Declaration {

	public final static ZamiaLogger logger = ZamiaLogger.getInstance();
	
	protected RegisterTypeE type = RegisterTypeE.NAN;

	protected String typeS = "";

	protected int fLeft = 0;
	
	protected int fRight = 0;
	
	protected boolean fAscending = true;
	
	protected Name name;

	protected TypeDefinitionRecord record;

	protected int range;
	
	public Name getName() {
		return name;
	}
	
	public int getRangeNb() {
		return range;
	}
	
	public abstract String toString();

	public abstract VHDLNode getVhdlNode();

	public RegisterTypeE getType() {
		return type;
	}
	
	public String getTypeS() {
		return typeS;
	}
	
	public int getLeft() {
		return fLeft;
	}
	
	public int getRight() {
		return fRight;
	}
	
	public boolean isAscending() {
		return fAscending;
	}
	
	public String getRange() {
		return fLeft + (fAscending ? " to " : " downto " ) +fRight;
	}
	
	public int getIndex() {
		if (isVector()) {
			if (toString().equalsIgnoreCase(getVectorName())) { return -1;}
			
			int indexOf = toString().indexOf("(");
			if (indexOf == -1) { return -1;}
			
			String substring = toString().substring(0, indexOf);
			
			indexOf = substring.indexOf("(");
			if (indexOf == -1) { return -1;}

			try {
				
				return Integer.valueOf(toString().substring(0,indexOf));
			} catch (NumberFormatException e) {
				return -1;
			}
			
		} else {
			return -1;
		}

	}

	public int getIndexMin() {
		int indexMin = 0;
		if (isAscending()) {
			indexMin = getLeft();
		} else {
			indexMin = getRight();
		}
		return indexMin;
	}

	public int getIndexMax() {
		int indexMax = 0;
		if (isAscending()) {
			indexMax = getRight();
		} else {
			indexMax = getLeft();
		}
		return indexMax;
	}

	public boolean isVector() {
		return type == RegisterTypeE.VECTOR;
	}

	public boolean isDiscrete() {
		return type == RegisterTypeE.DISCRETE;
	}

	public boolean isPartOfVector() {
		return type == RegisterTypeE.VECTOR_PART;
	}
	
	public boolean isArray() {
		return type == RegisterTypeE.ARRAY;
	}
	
	protected void setType(HdlEntity hdlEntity, HdlArchitecture hdlArchitecture) {

		// Try to find type from signals declared at architecture level
		int numChildren = hdlArchitecture.getArchitecture().getNumChildren();
		
		for (int i = 0; i < numChildren; i++) {
			VHDLNode child = hdlArchitecture.getArchitecture().getChild(i);

			if (child instanceof SignalDeclaration) {
				SignalDeclaration signal = (SignalDeclaration) child;
				String signalId = signal.getId();
				String signalType = signal.getType().toString();
				if (signalId.equalsIgnoreCase(getVectorName())) {
					if (findGenericType(signalType, signal, hdlEntity, hdlArchitecture, false) // search first generic type
							|| searchOtherType(hdlEntity, hdlArchitecture, signalType)	// then other types
							|| searchSubType(signal, hdlEntity, hdlArchitecture)) {		// then subtype
						return;	// type was found
					} else {
						type = RegisterTypeE.UNKNOWN_TYPE;
						typeS = signalType;
						range = 1;
					}
				}  else if (signalId.equalsIgnoreCase(getRecordName()) 
						&& searchOtherType(hdlEntity, hdlArchitecture, signal.getType().toString())) {
					return;
				}
			}
		}
		numChildren = hdlEntity.getEntity().getNumChildren();

		// If type declaration wasn't found in architecture, search at entity level
		for (int i = 0; i < numChildren; i++) {
			VHDLNode child = hdlEntity.getEntity().getChild(i);
			if (child instanceof InterfaceList) {
				InterfaceList interfaceList = (InterfaceList) child;
				int numSubChildren = interfaceList.getNumChildren();
				for (int j = 0; j < numSubChildren; j++) {
					VHDLNode subChild = child.getChild(j);
					if (subChild instanceof InterfaceDeclaration) {
						InterfaceDeclaration interfaceDec = (InterfaceDeclaration) subChild;
						if (interfaceDec.getId().equalsIgnoreCase(getVectorName())) {
							String searchedType = interfaceDec.getType().toString();
							if(searchedType != null && 
									(findGenericType(searchedType, interfaceDec, hdlEntity, hdlArchitecture, false)	// search first generic type
									|| searchOtherType(hdlEntity, hdlArchitecture, searchedType)				// then other types
									|| searchSubType(interfaceDec, hdlEntity, hdlArchitecture))) {				// then subtype
								return;	// type was found
							} else {
								type = RegisterTypeE.UNKNOWN_TYPE;
								typeS = searchedType;
								range = 1;
							}
						}
					}
				}
			}
		}
	}	
	
	/*
	 * Determine basic type for both type and subtype
	 * By default check (STD_LOGIC, STD_LOGIC_VECTOR, STATE_ARRAY_TYPE) for regular type
	 * Or if 'isSubtype' parameter is checked, checks (STD_LOGIC, STD_LOGIC_VECTOR)
	 * return true if type is found, else false
	 */
	private boolean findGenericType(String signalType, VHDLNode node, HdlEntity hdlEntity, HdlArchitecture hdlArchitecture, boolean isSubtype) {
		if (signalType.equalsIgnoreCase("STD_LOGIC")) {

			type = RegisterTypeE.DISCRETE;
			typeS = signalType;
			range = 1;
		} else if (signalType.contains("STD_LOGIC_VECTOR")) {
			if(getVectorName().equalsIgnoreCase(toString())) {
				type = RegisterTypeE.VECTOR;
				typeS = signalType;
				range = getRangeVector();
				getSignalVectorRange(node, hdlEntity, hdlArchitecture);
			} else {
				type = RegisterTypeE.VECTOR_PART;
				typeS = signalType;
				String argument = toString().replace(getVectorName(), "").replace("(", "").replace(")", "");
				try {
					determineVectorProperties(argument);
				} catch (NumberFormatException e) {
					getSignalVectorRange(node, hdlEntity, hdlArchitecture);
				}
			}
		} else if (signalType.contains("STATE_ARRAY_TYPE") && !isSubtype) {
			type = RegisterTypeE.STATE_ARRAY_TYPE;
			typeS = signalType;
			range = 1;
		} else {
			return false;
		}
		return true;
	}	
	
	/*
	 * Handle subtype by searching referenced generic type 
	 * return true if type is found, else false
	 */
	private boolean searchSubType(VHDLNode node, HdlEntity hdlEntity, HdlArchitecture hdlArchitecture) {
		try {

			TypeDefinitionSubType subtype = null;
			if((node instanceof InterfaceDeclaration) && ((InterfaceDeclaration) node).getType() instanceof TypeDefinitionSubType) {
				subtype = (TypeDefinitionSubType)((InterfaceDeclaration) node).getType();
			} else if ((node instanceof SignalDeclaration) && ((SignalDeclaration) node).getType() instanceof TypeDefinitionSubType)  {
				subtype = (TypeDefinitionSubType)((SignalDeclaration) node).getType();
			}
			
			if(subtype != null) {
				DeclarativeItem declaration = ASTDeclarationSearch.search(subtype.getName(), ToolManager.getZamiaProject());

				if (declaration instanceof TypeDeclaration) {
					TypeDefinition td = ((TypeDeclaration) declaration).getType();
					return findGenericType(td.toString(), declaration, hdlEntity, hdlArchitecture, false) ;
				}
			}
		} catch (IOException|ZamiaException e) {
			logger.debug(e.toString());
		}
		return false;
	}
	/**
	 * Other type not supported by findGenericType
	 */
	private boolean searchOtherType(HdlEntity hdlEntity, HdlArchitecture hdlArchitecture, String otherType) {
		int numChildren = hdlArchitecture.getArchitecture().getNumChildren();
		for (int i = 0; i < numChildren; i++) {
			VHDLNode child = hdlArchitecture.getArchitecture().getChild(i);
//			if (child != null) {
//				System.out.println("child "+child.getClass().getSimpleName());
//			}
			if (child instanceof TypeDeclaration) {
				TypeDeclaration typeDec = (TypeDeclaration) child;
				if (typeDec.getId().equalsIgnoreCase(otherType)) {
					int numChildren2 = child.getNumChildren();
					for (int j = 0; j < numChildren2; j++) {
						VHDLNode child2 = child.getChild(j);
						if (child2 instanceof TypeDefinitionRecord) {
							type = RegisterTypeE.RECORD;
							typeS = otherType;
							record = (TypeDefinitionRecord) child2;
							range = child2.getNumChildren();
							return true;
						} else if (child2 instanceof TypeDefinitionEnum) {
							type = RegisterTypeE.ENUMERATION;
							typeS = otherType;
							range = 1;
							return true;
						} else if (child2 instanceof TypeDefinitionConstrainedArray) {
							TypeDefinitionConstrainedArray typeArray = (TypeDefinitionConstrainedArray) child2;
							TypeDefinition elementType = typeArray.getElementType();
							System.out.println("elementType "+elementType.toString());
							DiscreteRange discreteRange = (DiscreteRange) typeArray.getChild(1);
							setRange(discreteRange.getRange(), hdlEntity, hdlArchitecture);
							type = RegisterTypeE.ARRAY;
							typeS = child2.toString();
							range = getRangeVector();
							return true;
						} else if (child2 != null) {
							System.out.println("child2 "+child2.toString()+ "  loc "+child2.getLocation());
							System.out.println("child2 "+child2.getClass().getSimpleName());
						}
//						System.out.println("child "+child2.getClass().getSimpleName());
					}
				}
			}
		}

		return false;
	}
	
	private void determineVectorProperties(String vectorStr) {
		// case downto
		if (vectorStr.indexOf("downto") != -1) {
			int index = (vectorStr.indexOf("downto"));
			fAscending = false;
			fLeft = Integer.valueOf(vectorStr.substring(0, index).trim());
			fRight = Integer.valueOf(vectorStr.substring(index+6, vectorStr.length()).trim());
			range = getRangeVector();						
		}
		// case to
		else if (vectorStr.indexOf("to") != -1) {
			int index = vectorStr.indexOf("to");
			fAscending = true;
			fLeft = Integer.valueOf(vectorStr.substring(0, index).trim());
			fRight = Integer.valueOf(vectorStr.substring(index+2, vectorStr.length()).trim());
			range = getRangeVector();
		}
		// case discrete
		else {
			fAscending = true;
			fLeft = Integer.valueOf(vectorStr.trim());
			fRight = Integer.valueOf(vectorStr.trim());
			range = 1;
		}
	}
	
	public String getRecordName() {
		int indexOf = toString().indexOf(".");
		if (indexOf == -1) { return toString();}
		
		return toString().substring(0, indexOf);
	}

	private int getRangeVector() {
		int indexMax = 0;
		int indexMin = 0;
		if (isAscending()) {
			indexMax = getRight();
			indexMin = getLeft();
		} else {
			indexMax = getLeft();
			indexMin = getRight();
		}
		return (indexMax - indexMin +1);
	}
	

	private void setRange(Range range, HdlEntity hdlEntity, HdlArchitecture hdlArchitecture) {
		fAscending = range.isAscending();
		fLeft = ToolManager.getOp(range.getLeft(), hdlEntity, hdlArchitecture);
		fRight = ToolManager.getOp(range.getRight(), hdlEntity, hdlArchitecture);
	}
	
	private void getSignalVectorRange(VHDLNode signal, HdlEntity hdlEntity, HdlArchitecture hdlArchitecture) {
		int numChildren = signal.getNumChildren();
		for (int i = 0; i < numChildren; i++) {
			VHDLNode child = signal.getChild(i);
			if (child instanceof TypeDefinitionSubType) {
				searchRangeInTypeDefinitionSubType(child, hdlEntity, hdlArchitecture);
			}
		}
	}

	private void searchRangeInTypeDefinitionSubType(VHDLNode child, HdlEntity hdlEntity, HdlArchitecture hdlArchitecture) {
		VHDLNode name = child.getChild(0);
		VHDLNode nameExtRange = name.getChild(0);
		VHDLNode childNameExtRange = nameExtRange.getChild(0);
		if (childNameExtRange instanceof Range) {
			Range range = (Range) childNameExtRange;
			fLeft = ToolManager.getOp(range.getLeft(), hdlEntity, hdlArchitecture);
			fRight = ToolManager.getOp(range.getRight(), hdlEntity, hdlArchitecture);
			fAscending = range.isAscending();
		}
	}

	public String getVectorName() {
		return ToolManager.getVectorName(toString());
	}
	
}
