
package org.zamia.plugin.tool.vhdl.rules.impl.std;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.w3c.dom.Element;
import org.zamia.ZamiaProject;
import org.zamia.plugin.tool.vhdl.ClockSignal;
import org.zamia.plugin.tool.vhdl.EntityException;
import org.zamia.plugin.tool.vhdl.HdlArchitecture;
import org.zamia.plugin.tool.vhdl.HdlEntity;
import org.zamia.plugin.tool.vhdl.HdlFile;
import org.zamia.plugin.tool.vhdl.NodeInfo;
import org.zamia.plugin.tool.vhdl.NodeType;
import org.zamia.plugin.tool.vhdl.Process;
import org.zamia.plugin.tool.vhdl.ResetSignal;
import org.zamia.plugin.tool.vhdl.manager.ResetSignalManager;
import org.zamia.plugin.tool.vhdl.rules.RuleE;
import org.zamia.plugin.tool.vhdl.rules.impl.PositionE;
import org.zamia.plugin.tool.vhdl.rules.impl.RuleManager;
import org.zamia.util.Pair;
import org.zamia.vhdl.ast.Architecture;
import org.zamia.vhdl.ast.Entity;
import org.zamia.vhdl.ast.SequentialProcess;

public class RuleSTD_00300 extends RuleManager {

	// Name of reset signal

	RuleE rule = RuleE.STD_00300;

	private List<ResetSignal> listReset;

	public Pair<Integer, String> Launch(ZamiaProject zPrj, String ruleId) {

		String fileName = ""; 

		listReset = new ArrayList<ResetSignal>();

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
			return new Pair<Integer, String> (WRONG_PARAM,"");
		}

		PositionE position = PositionE.valueOf(((String) listParam1.get(2)).toUpperCase());
		List<String> listPrefix = new ArrayList<String>();
		List<Object> listParam2 = xmlParameterFileConfig.get(1);
		for (int i = 2; i < listParam2.size(); i++) {
			listPrefix.add((String) listParam2.get(i));
		}

		Map<String, HdlFile> hdlFiles;
		try {

			hdlFiles = ResetSignalManager.getResetSignal();
		} catch (EntityException e) {
			logger.error("some exception message RuleSTD_00300", e);
			return new Pair<Integer, String> (RuleManager.NO_BUILD,"");
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
						if (processItem.getListClockSignal() == null) { continue;}
						for (ClockSignal clockSignalItem : processItem.getListClockSignal()) {
							if (clockSignalItem.getListResetSignal() == null) { continue;}
							for (ResetSignal resetSignalItem : clockSignalItem.getListResetSignal()) {
								cmpt++;
								addResetEntityArchi(resetSignalItem);
							}
						}
					}
				}
			}
		}

		//		if (cmpt == 0) {
		//			JOptionPane.showMessageDialog(null, "<html>No reset Signal Find</html>", "Warning",
		//                    JOptionPane.WARNING_MESSAGE);
		//
		//		}

		for (ResetSignal resetSignal : listReset) {
			if (!nameValide(resetSignal.toString(), listPrefix, position)) {
				cmptViolation++;
				addViolation(racine, resetSignal, "nameInvalid", resetSignal.getFileName(), resetSignal.getEntity(),
						resetSignal.getArchitecture(), resetSignal.getProcess(), resetSignal.getClockSignal());
				//(racine, resetSignal, "nameInvalid", resetSignal.getFileName(), resetSignal.getEntity(), resetSignal.getArchitecture(), resetSignal.getProcess());

			}
		}

		if (cmptViolation != 0) {
			fileName = createReportFile(ruleId, rule.getRuleName(), rule.getType());
		}
		//		ZamiaErrorObserver.updateAllMarkers(zPrj);
		return new Pair<Integer, String> (cmptViolation, fileName);
	}


	private void addResetEntityArchi(ResetSignal resetSignalItem) {
		if (listReset.contains(resetSignalItem)) {
			return;
		}

		listReset.add(resetSignalItem);
	}


	private void addViolation(Element racine, ResetSignal resetSignalItem, String error, 
			String hdlFileName, Entity entity, Architecture architecture, SequentialProcess processItem,
			ClockSignal clockSignalItem) {
		Element resetSignalElement = document.createElement(NodeType.RESET_SIGNAL.toString());
		racine.appendChild(resetSignalElement);

		resetSignalElement.appendChild(NewElement(document, "violationType"
				, error));


		resetSignalElement.appendChild(NewElement(document, NodeType.RESET_SIGNAL.toString()+NodeInfo.NAME.toString()
				, resetSignalItem.toString()));

		Element resetSignalLocationElement = document.createElement(NodeType.RESET_SIGNAL.toString()+NodeInfo.LOCATION.toString());
		resetSignalElement.appendChild(resetSignalLocationElement);

		resetSignalLocationElement.appendChild(NewElement(document, NodeType.FILE.toString()+NodeInfo.NAME.toString()
				, hdlFileName));

		resetSignalLocationElement.appendChild(NewElement(document, NodeType.ENTITY.toString()+NodeInfo.NAME.toString()
				, entity.getId()));

		resetSignalLocationElement.appendChild(NewElement(document, NodeType.ARCHITECTURE.toString()+NodeInfo.NAME.toString()
				, architecture.getId()));

		resetSignalLocationElement.appendChild(NewElement(document, NodeType.PROCESS.toString()+NodeInfo.NAME.toString()
				, processItem.getLabel() != null ? processItem.getLabel() : "unnamed process"));

		resetSignalLocationElement.appendChild(NewElement(document, NodeType.RESET_SIGNAL.toString()+NodeInfo.NAME.toString()
				, clockSignalItem.toString()));

		resetSignalLocationElement.appendChild(NewElement(document, NodeType.RESET_SIGNAL.toString()+NodeInfo.LOCATION.toString()
				, String.valueOf(resetSignalItem.getLocation().fLine)));

	}




}

