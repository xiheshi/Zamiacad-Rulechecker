package org.zamia.plugin.tool.vhdl.rules.impl.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

import org.w3c.dom.Element;
import org.zamia.ZamiaProject;
import org.zamia.plugin.tool.vhdl.EntityException;
import org.zamia.plugin.tool.vhdl.HdlArchitecture;
import org.zamia.plugin.tool.vhdl.HdlEntity;
import org.zamia.plugin.tool.vhdl.HdlFile;
import org.zamia.plugin.tool.vhdl.NodeInfo;
import org.zamia.plugin.tool.vhdl.NodeType;
import org.zamia.plugin.tool.vhdl.Process;
import org.zamia.plugin.tool.vhdl.manager.ProcessManager;
import org.zamia.plugin.tool.vhdl.rules.RuleE;
import org.zamia.plugin.tool.vhdl.rules.impl.PositionE;
import org.zamia.plugin.tool.vhdl.rules.impl.RuleManager;
import org.zamia.util.Pair;

public class RuleGEN_01200 extends RuleManager {

	// Identification of process label
	
	RuleE rule = RuleE.GEN_01200;

	public Pair<Integer, String> Launch(ZamiaProject zPrj, String ruleId) {
		String fileName = "";
// default param
		List<List<Object>> listParam = new ArrayList<List<Object>>();
		List<Object> param = new ArrayList<Object>(); 
		param.add("position");
		param.add(PositionE.class);
		listParam.add(param);

		param = new ArrayList<Object>(); 
		param.add("partName");
		param.add(String.class);
		listParam.add(param);

		
		List<List<Object>> xmlParameterFileConfig = getXmlParameterFileConfig(zPrj, ruleId, listParam);
		if (xmlParameterFileConfig == null) {
			// wrong param
			logger.info("Rule Checker: wrong parameter for rules "+rule.getIdReq()+ ".");
			return new Pair<Integer, String> (WRONG_PARAM, "");
		}
		
		// get param
		List<Object> listParam1 = xmlParameterFileConfig.get(0);

		if (!PositionE.exist((String) listParam1.get(2))) {
			logger.info("Rule Checker: wrong parameter for rules "+rule.getIdReq()+ ".");
			return new Pair<Integer, String> (WRONG_PARAM, "");
		}

		PositionE position = PositionE.valueOf(((String) listParam1.get(2)).toUpperCase());
		List<String> listPrefix = new ArrayList<String>();
		List<Object> listParam2 = xmlParameterFileConfig.get(1);
		for (int i = 2; i < listParam2.size(); i++) {
			listPrefix.add((String) listParam2.get(i));
		}

		Map<String, HdlFile> hdlFiles;
		try {
			hdlFiles = ProcessManager.getProcess();
		} catch (EntityException e) {
			logger.error("some exception message RuleGEN_01200", e);
			return new Pair<Integer, String> (RuleManager.NO_BUILD, "");
		}

		Element racine = initReportFile(ruleId, rule.getType(), rule.getRuleName());

		Integer cmptViolation = 0;
		Integer cmpt = 0;
		for(Entry<String, HdlFile> entry : hdlFiles.entrySet()) {
			HdlFile hdlFile = entry.getValue();
			if (hdlFile.getListHdlEntity() == null) { continue;}
			for (HdlEntity hdlEntityItem : hdlFile.getListHdlEntity()) {
				if (hdlEntityItem.getListHdlArchitecture() == null) { continue;}
				for (HdlArchitecture hdlArchitectureItem : hdlEntityItem.getListHdlArchitecture()) {
					if (hdlArchitectureItem.getListProcess() == null) { continue;}
					for (Process processItem : hdlArchitectureItem.getListProcess()) {
						cmpt++;
						if (processItem.getSequentialProcess().getLabel() == null) {
							cmptViolation++;

							addViolation(racine, processItem, "unnamed process", hdlFile, hdlEntityItem, hdlArchitectureItem);
							//							erm.addError(new ZamiaException("unnamed process" , processItem.getSequentialProcess().getLocation()));
							// erreur label NULL
						} else {
							// test label correspond a la regle
							if (!nameValide(processItem.getLabel(), listPrefix, position)) {
								cmptViolation++;
								addViolation(racine, processItem, "invalid process label", hdlFile, hdlEntityItem, hdlArchitectureItem);
								//								erm.addError(new ZamiaException("unvalide process label" , processItem.getSequentialProcess().getLocation()));
								// erreur label pas bon
							}
						}
					}
				}
			}
		}
		if (cmpt == 0) {
			JOptionPane.showMessageDialog(null, "<html>No process Find</html>", "Warning",
                    JOptionPane.WARNING_MESSAGE);

		}
		if (cmptViolation != 0) {
			fileName = createReportFile(ruleId, rule.getRuleName(), rule.getType());
		}
		//		ZamiaErrorObserver.updateAllMarkers(zPrj);
		return new Pair<Integer, String> (cmptViolation, fileName);
	}


	private void addViolation(Element racine, Process processItem, String error, HdlFile hdlFile, HdlEntity hdlEntityItem, HdlArchitecture hdlArchitectureItem) {
		Element processElement = document.createElement(NodeType.PROCESS.toString());
		racine.appendChild(processElement);

		processElement.appendChild(NewElement(document, "violationType", error));
		
		if (processItem.getSequentialProcess().getLabel() != null) {
			
			processElement.appendChild(NewElement(document, NodeType.PROCESS.toString()+NodeInfo.NAME.toString(), 
					processItem.getLabel()));
		}

		Element processLocationElement = document.createElement(NodeType.PROCESS.toString()+NodeInfo.LOCATION.toString());
		processElement.appendChild(processLocationElement);
		
		processLocationElement.appendChild(NewElement(document, NodeType.FILE.toString()+NodeInfo.NAME.toString()
				, hdlFile.getLocalPath()));

		processLocationElement.appendChild(NewElement(document, NodeType.ENTITY.toString()+NodeInfo.NAME.toString()
				, hdlEntityItem.getEntity().getId()));

		processLocationElement.appendChild(NewElement(document, NodeType.ARCHITECTURE.toString()+NodeInfo.NAME.toString()
				, hdlArchitectureItem.getArchitecture().getId()));

		processLocationElement.appendChild(NewElement(document, NodeType.PROCESS.toString()+NodeInfo.LOCATION.toString()
				, String.valueOf(processItem.getLocation().fLine)));

	}


}
