/*
 * Copyright 2007-2010 by the authors indicated in the @author tags.
 * All rights reserved.
 *
 * See the LICENSE file for details.
 *
 */

package org.zamia.plugin.editors;

import java.util.Map;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.zamia.ASTNode;
import org.zamia.ToplevelPath;
import org.zamia.analysis.SourceLocation2AST;
import org.zamia.analysis.ast.ASTDeclarationSearch;
import org.zamia.analysis.ig.IGReferencesSearchThrough;
import org.zamia.analysis.ig.IGReferencesSearchThrough.SearchResultEntrySite;
import org.zamia.instgraph.IGObject;
import org.zamia.plugin.ZamiaPlugin;
import org.zamia.plugin.editors.ShowReferencesDialog.Option;
import org.zamia.vhdl.ast.DeclarativeItem;
import org.zamia.vhdl.ast.InterfaceDeclaration;
import org.zamia.vhdl.ast.SignalDeclaration;

/**
 * 
 * @author Guenter Bartsch
 * 
 */

public class ShowReferencesAction extends StaticAnalysisAction {

	private boolean[] values;
	public void run(IAction a) {

		NewSearchUI.activateSearchResultView();

		try {
			processSelection();

			IWorkbenchWindow window = ZamiaPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
			if (window == null) {
				logger.error("ShowReferencesAction: Internal error: window == null.");
				return;
			}

			Shell shell = window.getShell();
			if (fLocation == null) {
				showError("Failed to determine source location.", shell);
				return;
			}

			ASTNode nearest = SourceLocation2AST.findNearestASTNode(fLocation, true, fZPrj);
			if (nearest == null) {
				showError("Couldn't map caret position to syntax tree.", shell);
				return;
			}

			DeclarativeItem decl = ASTDeclarationSearch.search(nearest, fZPrj);
			if (decl == null) {
				showError("Couldn't find declaration of\n" + nearest, shell);
				return;
			}

			String jobText = "Search for " + decl + "\nLocation: " + fLocation + "\nPath: " + fPath;
			boolean usePath = decl instanceof SignalDeclaration || decl instanceof InterfaceDeclaration;
			ShowReferencesDialog dlg = new ShowReferencesDialog(window.getShell(), jobText, usePath ? fPath : null, values);

			if (dlg.open() == Window.OK) {
				NewSearchUI.runQueryInBackground(new ExtendedReferencesSearchQuery(this, dlg.isSearchUp(), dlg.isSearchDown(), false, 
						usePath && dlg.getValue(Option.UsePath), dlg.getValue(Option.WritesOnly), dlg.getValue(Option.ReadsOnly), dlg.getValue(Option.AssignThrough)));
			}

			values = dlg.values;
		} catch (Exception e) {
			el.logException(e);
			showError("Catched exception:\n" + e + "\nSee log for details.", (Shell) null);
		}
	}

	private void showError(String aMsg, Shell shell) {
		logger.error("ShowReferencesAction: Error: %s", aMsg);
		ZamiaPlugin.showError(shell, "ShowReferencesAction: Error", aMsg, "");
	}

	
}


class ExtendedReferencesSearchQuery extends ReferencesSearchQuery {

	private boolean fFollowAssignments;

	public ExtendedReferencesSearchQuery(StaticAnalysisAction aSAA,
			boolean aSearchUpward, boolean aSearchDownward, boolean aDeclOnly,
			boolean aUsePath, boolean aWritersOnly, boolean aReadersOnly, boolean aFollowAssignments) {
		super(aSAA, aSearchUpward, aSearchDownward, aDeclOnly, aUsePath, aWritersOnly,
				aReadersOnly);
		fFollowAssignments = aFollowAssignments;
	}

	@Override
	protected void igSearch(IGObject object, ToplevelPath path) {
		
		if (fFollowAssignments) {
			IGReferencesSearchThrough rs = new IGReferencesSearchThrough(fZPrj);
	
	
			Map<IGObject, SearchResultEntrySite> searches = rs.assignmentThroughSearch(object, path, fSearchUpward, fSearchDownward, fWritersOnly, fReadersOnly);
	
			for (IGObject key : searches.keySet()) {
				mergeResults(key, searches.get(key).restOfResults);
			}
		} else 
			super.igSearch(object, path);
	}
}
