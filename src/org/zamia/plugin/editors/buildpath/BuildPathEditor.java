/*
 * Copyright 2004-2009 by the authors indicated in the @author tags.
 * All rights reserved.
 *
 * See the LICENSE file for details.
 *
 */

package org.zamia.plugin.editors.buildpath;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.PaintManager;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.MatchingCharacterPainter;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.editors.text.TextEditor;
import org.zamia.ExceptionLogger;
import org.zamia.SourceLocation;
import org.zamia.ZamiaLogger;
import org.zamia.plugin.editors.ColorManager;
import org.zamia.plugin.editors.ErrorMarkEditor;
import org.zamia.plugin.editors.ZamiaPairMatcher;
import org.zamia.vhdl.ast.VHDLNode;


/**
 * 
 * @author Guenter Bartsch
 * 
 */
public class BuildPathEditor extends ErrorMarkEditor {

	public final static ZamiaLogger logger = ZamiaLogger.getInstance();

	public final static ExceptionLogger el = ExceptionLogger.getInstance();

	protected MatchingCharacterPainter fBracketPainter;

	private PaintManager fPaintManager;

	private static final RGB BRACKETS_COLOR = new RGB(160, 160, 160);

	private final static char[] BRACKETS = { '{', '}', '(', ')', '[', ']' };

	protected AbstractSelectionChangedListener fOutlineSelectionChangedListener = new OutlineSelectionChangedListener();

	public BuildPathEditor() {
		setSourceViewerConfiguration(new BuildPathSourceViewerConfiguration(new BuildPathScanner(), new String[] {"#", ""}, this));
	}

	protected ISourceViewer createSourceViewer(Composite aParent, IVerticalRuler aRuler, int aStyles) {
		ISourceViewer viewer = new ProjectionViewer(aParent, aRuler, getOverviewRuler(), isOverviewRulerVisible(), aStyles);
		// ensure decoration support has been created and configured.
		getSourceViewerDecorationSupport(viewer);
		return viewer;
	}

	public void createPartControl(Composite aParent) {
		super.createPartControl(aParent);

		fPaintManager = new PaintManager(getSourceViewer());

		ISourceViewer sourceViewer = getSourceViewer();

		fBracketPainter = new MatchingCharacterPainter(sourceViewer, new ZamiaPairMatcher(BRACKETS));
		fBracketPainter.setColor(ColorManager.getInstance().getColor(BRACKETS_COLOR));
		fPaintManager.addPainter(fBracketPainter);
	}
	
	class OutlineSelectionChangedListener extends AbstractSelectionChangedListener {

		public void selectionChanged(SelectionChangedEvent aEvent) {
			Object selectedObject;

			ISelection selection = aEvent.getSelection();
			selectedObject = ((IStructuredSelection) selection).getFirstElement();

			if (selectedObject instanceof VHDLNode) {
				VHDLNode io = (VHDLNode) selectedObject;

				SourceLocation location = io.getLocation();
				if (location != null) {

					try {
						int offset = getSourceViewer().getDocument().getLineOffset(location.fLine - 1) + location.fCol - 1;
						selectAndReveal(offset, 1);
					} catch (BadLocationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}
		}
	}

//	public void updateColors() {
//		// FIXME: implement
//	}

}
