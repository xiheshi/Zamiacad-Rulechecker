package org.zamia.plugin.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.handlers.HandlerUtil;

@SuppressWarnings("deprecation")
public class SetBuildPathHandler extends AbstractHandler{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ITreeSelection v = (ITreeSelection) HandlerUtil.getVariable(event, "selection");
		try {
			((IFile) v.getFirstElement()).touch(null);
		} catch (CoreException e1) {
			throw new ExecutionException("Failed to touch " + v, e1);
		}
		return null;
	}

}