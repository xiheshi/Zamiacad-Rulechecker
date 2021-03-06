package org.zamia.plugin.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.zamia.plugin.preferences.PreferenceConstants;

public class PythonEditor extends ErrorMarkEditor {
	
	static class Scanner extends RuleBasedScanner {

		{
			IToken keyword = VHDLScanner.token(PreferenceConstants.P_KEYWORD);
			IToken others = VHDLScanner.token(PreferenceConstants.P_DEFAULT);
			setDefaultReturnToken(others);

			IWordDetector wd = new IWordDetector() {

				public boolean isWordPart(char character) {
					return Character.isLetterOrDigit(character);
				}
			
				public boolean isWordStart(char character) {
					return Character.isLetterOrDigit(character);
				}
			};

			List<IRule> rules = new ArrayList<IRule>();
//			rules[1] = new WhitespaceRule(new XMLWhitespaceDetector());
			WordRule wr = new WordRule(wd, others, false);
			
			String[] keywords = { "and","del","from","not","while","as","elif","global","or","with","assert","else","if","pass","yield","break","except","import","print","class","exec","in","raise","continue","finally","is","return","def","for","lambda","try" };

			for (int i = 0; i < keywords.length; i++) {
				wr.addWord(keywords[i], keyword);
			}

			rules.add(wr);
			
			IToken comment = VHDLScanner.getCommentToken();
			IToken string = VHDLScanner.getStringToken();
			
			rules.add(new SingleLineRule("'", "'", string));
			rules.add(new SingleLineRule("\"", "\"", string)); // curiously, this already covers the multiline strings
			rules.add(new MultiLineRule("\"\"\"", "\"\"\"", string));
			rules.add(new MultiLineRule("'''", "'''", string));
			rules.add(new EndOfLineRule("#", comment));

			setRules(rules.toArray(new IRule[rules.size()]));
			
			
		}
	}

	public static class PythonConfiguration extends SourceViewerConfiguration {

		public PythonConfiguration() {}
		
		public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
			return new String[] {IDocument.DEFAULT_CONTENT_TYPE,};
		}

		public String[] getDefaultPrefixes(ISourceViewer sourceViewer, String contentType) {
			return new String[] {"#", ""};
		}
		
//		public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
//			if (doubleClickStrategy == null)
//				doubleClickStrategy = new XMLDoubleClickStrategy();
//			return doubleClickStrategy;
//		}

		public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
			PresentationReconciler reconciler = new PresentationReconciler();

			DefaultDamagerRepairer dr = new DefaultDamagerRepairer(new Scanner());
			reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
			reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
			
			return reconciler;
		}
		
	}
	
	public PythonEditor() {
		setSourceViewerConfiguration(new PythonConfiguration());
	}
	
}