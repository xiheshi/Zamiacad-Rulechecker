/* 
 * Copyright 2009 by the authors indicated in the @author tags. 
 * All rights reserved. 
 * 
 * See the LICENSE file for details.
 * 
 * Created by Guenter Bartsch on Nov 7, 2009
 */
package org.zamia.plugin.build;

import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.zamia.ERManager;
import org.zamia.ErrorObserver;
import org.zamia.ExceptionLogger;
import org.zamia.SourceFile;
import org.zamia.SourceLocation;
import org.zamia.ZamiaException;
import org.zamia.ZamiaLogger;
import org.zamia.ZamiaProject;
import org.zamia.plugin.ZamiaPlugin;
import org.zamia.plugin.ZamiaProjectMap;


/**
 * 
 * @author Guenter Bartsch
 *
 */

public class ZamiaErrorObserver implements ErrorObserver {

	private static final ZamiaLogger logger = ZamiaLogger.getInstance();

	private static final ExceptionLogger el = ExceptionLogger.getInstance();

	private IProject fProject;

	public ZamiaErrorObserver(IProject aProject) {
		fProject = aProject;
	}

	static class MarkDeleter implements IResourceVisitor {

		private String fMarkerType;

		public MarkDeleter(String aMarkerType) {
			fMarkerType = aMarkerType;
		}

		public boolean visit(IResource aResource) {
			if (aResource instanceof IFile) {
				IFile file = (IFile) aResource;
				try {
					file.deleteMarkers(fMarkerType, false, IResource.DEPTH_ZERO);
				} catch (CoreException e) {
					el.logException(e);
				}
			}
			// return true to continue visiting children.
			return true;
		}
	}

	static void deleteAllMarkers(IProject aProject) {
		try {
			aProject.accept(new MarkDeleter(null));
		} catch (Throwable e) {
			el.logException(e);
		}
	}

	@Override
	public void notifyCleaned(ZamiaProject aZPrj) {
		logger.debug("ZamiaErrorObserver: notifyCleaned() %s", aZPrj);
		deleteAllMarkers(fProject);
	}

	static void addMarker(IProject aProject, ZamiaException aError) {
		SourceLocation location = aError.getLocation();

		if (location != null && location.fSF != null) {
			SourceFile sf = location.fSF;

			IFile file = ZamiaPlugin.getIFile(sf, aProject);

			if (file != null) {
				//addMarker(file, err.getMessage(), location.fLine, err.getCat() != ExCat.EXTERNAL ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING);
				addMarker(file, aError.getMessage(), location.fLine, aError.isError() ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING);
			}
		}
	}
	
	@Override
	public void notifyErrorAdded(ZamiaProject aZPrj, ZamiaException aError) {
		logger.debug("ZamiaErrorObserver: notifyErrorAdded() %s : %s", aZPrj, aError);
		addMarker(fProject, aError);
	}

	@Override
	public void notifyErrorsChanged(ZamiaProject aZPrj, SourceFile aSF) {

		logger.debug("ZamiaErrorObserver: notifyErrorsChanged() %s : %s", aZPrj, aSF);

		final IFile file = ZamiaPlugin.getIFile(aSF, fProject);

		if (file != null && file.exists()) { // file will not exist if it was renamed. By the time this called, errors are removed from ZDB. But, I do not know about eclipse markers, linked to the old file name. How to remove them?

			new WorkspaceJob("Delete marker notification") {

				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
					try {
						file.deleteMarkers(null, true, IResource.DEPTH_ZERO);
					} catch (CoreException e) {
						el.logException(e);
					}
					return Status.OK_STATUS;
				}
				
			}; 

			ERManager erm = aZPrj.getERM();

			int n = erm.getNumErrors(aSF);
			for (int i = 0; i < n; i++) {

				ZamiaException err = erm.getError(aSF, i);

				SourceLocation location = err.getLocation();

				if (location != null) {
					//addMarker(file, err.getMessage(), location.fLine, err.getCat() != ExCat.EXTERNAL ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING);
					addMarker(file, err.getMessage(), location.fLine, IMarker.SEVERITY_ERROR);
				}
			}
		}
	}

	public static void updateAllMarkers(ZamiaProject aZPrj) {
		IProject prj = ZamiaProjectMap.getProject(aZPrj);

		deleteAllMarkers(prj);

		ERManager erm = aZPrj.getERM();

		int n = erm.getNumErrors();
		for (int i = 0; i < n; i++) {
			ZamiaException error = erm.getError(i);
			logger.info("ZamiaErrorObserver: updating markers %3d/%3d: %s", i + 1, n, error);
			addMarker(prj, error);
		}
	}
	
	@Override
	public void notifyErrorsChanged(ZamiaProject aZPrj) {

		logger.debug("ZamiaErrorObserver: notifyErrorsChanged() %s", aZPrj);

		updateAllMarkers(aZPrj);
	}

	private static void addMarker(IFile aFile, String aMessage, int aLineNumber, int aSeverity) {
		
		for (IFile f : ((ZamiaProjectMap.EclipseProjectFileIterator)(ZamiaProjectMap.getZamiaProject(aFile.getProject()).fBasePath)).getFiles(aFile))
		
		try {
			IMarker marker = f.createMarker(IMarker.PROBLEM);
			// IMarker marker = file.createMarker(markerType_);
			marker.setAttribute(IMarker.MESSAGE, aMessage);
			marker.setAttribute(IMarker.SEVERITY, aSeverity);
			if (aLineNumber < 1) {
				aLineNumber = 1;
			}
			marker.setAttribute(IMarker.LINE_NUMBER, aLineNumber);
		} catch (CoreException e) {
			el.logException(e);
		}
	}
}
