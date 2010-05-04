/**
 * This file Copyright (c) 2005-2009 Aptana, Inc. This program is
 * dual-licensed under both the Aptana Public License and the GNU General
 * Public license. You may elect to use one or the other of these licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT. Redistribution, except as permitted by whichever of
 * the GPL or APL you select, is prohibited.
 *
 * 1. For the GPL license (GPL), you can redistribute and/or modify this
 * program under the terms of the GNU General Public License,
 * Version 3, as published by the Free Software Foundation.  You should
 * have received a copy of the GNU General Public License, Version 3 along
 * with this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Aptana provides a special exception to allow redistribution of this file
 * with certain other free and open source software ("FOSS") code and certain additional terms
 * pursuant to Section 7 of the GPL. You may view the exception and these
 * terms on the web at http://www.aptana.com/legal/gpl/.
 *
 * 2. For the Aptana Public License (APL), this program and the
 * accompanying materials are made available under the terms of the APL
 * v1.0 which accompanies this distribution, and is available at
 * http://www.aptana.com/legal/apl/.
 *
 * You may view the GPL, Aptana's exception and additional terms, and the
 * APL in the file titled license.html at the root of the corresponding
 * plugin containing this source file.
 *
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.css.contentassist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;

import com.aptana.editor.common.AbstractThemeableEditor;
import com.aptana.editor.common.CommonContentAssistProcessor;
import com.aptana.editor.common.contentassist.CommonCompletionProposal;
import com.aptana.editor.common.contentassist.LexemeProvider;
import com.aptana.editor.common.contentassist.UserAgentManager;
import com.aptana.editor.css.Activator;
import com.aptana.editor.css.CSSScopeScanner;
import com.aptana.editor.css.contentassist.index.CSSIndexConstants;
import com.aptana.editor.css.contentassist.model.ElementElement;
import com.aptana.editor.css.contentassist.model.PropertyElement;
import com.aptana.editor.css.contentassist.model.ValueElement;
import com.aptana.editor.css.parsing.lexer.CSSTokenType;
import com.aptana.parsing.lexer.Lexeme;

public class CSSContentAssistProcessor extends CommonContentAssistProcessor
{
	/**
	 * Location
	 */
	private static enum Location
	{
		ERROR, OUTSIDE_RULE, INSIDE_RULE, INSIDE_ARG, INSIDE_PROPERTY, INSIDE_VALUE
	};

	private static final Image ELEMENT_ICON = Activator.getImage("/icons/element.gif");
	private static final Image PROPERTY_ICON = Activator.getImage("/icons/property.gif");

	private IContextInformationValidator _validator;
	private CSSIndexQueryHelper _queryHelper;
	private Lexeme<CSSTokenType> _currentLexeme;

	/**
	 * CSSContentAssistProcessor
	 * 
	 * @param editor
	 */
	public CSSContentAssistProcessor(AbstractThemeableEditor editor)
	{
		super(editor);

		this._queryHelper = new CSSIndexQueryHelper();
	}

	/**
	 * getElementProposals
	 * 
	 * @param proposals
	 * @param offset
	 */
	protected void addAllElementProposals(List<ICompletionProposal> proposals, int offset)
	{
		List<ElementElement> elements = this._queryHelper.getElements();

		if (elements != null)
		{
			for (ElementElement element : elements)
			{
				String name = element.getName();
				int length = name.length();
				String description = element.getDescription();
				Image image = ELEMENT_ICON;
				IContextInformation contextInfo = null;
				String[] userAgents = element.getUserAgentNames();
				Image[] userAgentIcons = UserAgentManager.getInstance().getUserAgentImages(userAgents);

				// build a proposal
				CommonCompletionProposal proposal = new CommonCompletionProposal(name, offset, 0, length, image, name, contextInfo, description);
				proposal.setFileLocation(CSSIndexConstants.METADATA);
				proposal.setUserAgentImages(userAgentIcons);

				// add it to the list
				proposals.add(proposal);
			}
		}
	}

	/**
	 * getAllPropertyProposals
	 * 
	 * @param proposals
	 * @param offset
	 */
	protected void addAllPropertyProposals(List<ICompletionProposal> proposals, int offset)
	{
		List<PropertyElement> properties = this._queryHelper.getProperties();

		if (properties != null)
		{
			for (PropertyElement property : properties)
			{
				String name = property.getName();
				int length = name.length();
				String description = property.getDescription();
				Image image = PROPERTY_ICON;
				IContextInformation contextInfo = null;
				String[] userAgents = property.getUserAgentNames();
				Image[] userAgentIcons = UserAgentManager.getInstance().getUserAgentImages(userAgents);

				// TEMP:
				int replaceLength = 0;
				
				if (this._currentLexeme != null)
				{
					offset = this._currentLexeme.getStartingOffset();
					replaceLength = this._currentLexeme.getLength();
				}
				
				// build a proposal
				CommonCompletionProposal proposal = new CommonCompletionProposal(name, offset, replaceLength, length, image, name, contextInfo, description);
				proposal.setFileLocation(CSSIndexConstants.METADATA);
				proposal.setUserAgentImages(userAgentIcons);

				// add it to the list
				proposals.add(proposal);
			}
		}
	}

	/**
	 * addClasses
	 * 
	 * @param proposals
	 * @param offset
	 */
	protected void addClasses(List<ICompletionProposal> proposals, int offset)
	{
		Map<String, String> classes = this._queryHelper.getClasses(this.getIndex());

		if (classes != null)
		{
			for (Entry<String, String> entry : classes.entrySet())
			{
				String name = "." + entry.getKey();
				int length = name.length();
				String description = null;
				Image image = ELEMENT_ICON;
				IContextInformation contextInfo = null;
				UserAgentManager manager = UserAgentManager.getInstance();
				String[] userAgents = manager.getActiveUserAgentIDs(); // classes can be used by all user agents
				Image[] userAgentIcons = manager.getUserAgentImages(userAgents);
				
				// TEMP:
				int replaceLength = 0;
				
				if (this._currentLexeme != null)
				{
					offset = this._currentLexeme.getStartingOffset();
					replaceLength = this._currentLexeme.getLength();
				}

				CommonCompletionProposal proposal = new CommonCompletionProposal(name, offset, replaceLength, length, image, name, contextInfo, description);
				proposal.setFileLocation(entry.getValue());
				proposal.setUserAgentImages(userAgentIcons);

				// add it to the list
				proposals.add(proposal);
			}
		}
	}

	/**
	 * addIDs
	 * 
	 * @param result
	 * @param offset
	 */
	protected void addIDs(List<ICompletionProposal> proposals, int offset)
	{
		Map<String, String> classes = this._queryHelper.getIDs(this.getIndex());

		if (classes != null)
		{
			for (Entry<String, String> entry : classes.entrySet())
			{
				String name = "#" + entry.getKey();
				int length = name.length();
				String description = null;
				Image image = ELEMENT_ICON;
				IContextInformation contextInfo = null;
				UserAgentManager manager = UserAgentManager.getInstance();
				String[] userAgents = manager.getActiveUserAgentIDs(); // classes can be used by all user agents
				Image[] userAgentIcons = manager.getUserAgentImages(userAgents);
				
				// TEMP:
				int replaceLength = 0;
				
				if (this._currentLexeme != null)
				{
					offset = this._currentLexeme.getStartingOffset();
					replaceLength = this._currentLexeme.getLength();
				}

				CommonCompletionProposal proposal = new CommonCompletionProposal(name, offset, replaceLength, length, image, name, contextInfo, description);
				proposal.setFileLocation(entry.getValue());
				proposal.setUserAgentImages(userAgentIcons);

				// add it to the list
				proposals.add(proposal);
			}
		}
	}

	/**
	 * addInsideRuleProposals
	 * 
	 * @param proposals
	 * @param document
	 * @param offset
	 */
	private void addInsideRuleProposals(List<ICompletionProposal> proposals, LexemeProvider<CSSTokenType> lexemeProvider, int offset)
	{
		Location location = this.getInsideLocation(lexemeProvider, offset);

		switch (location)
		{
			case INSIDE_PROPERTY:
				this.addAllPropertyProposals(proposals, offset);
				break;

			case INSIDE_VALUE:
				this.addPropertyValues(proposals, lexemeProvider, offset);
				break;

			default:
				break;
		}
	}

	/**
	 * addOutsideRuleProposals
	 * 
	 * @param proposals
	 * @param document
	 * @param offset
	 */
	private void addOutsideRuleProposals(List<ICompletionProposal> proposals, LexemeProvider<CSSTokenType> lexemeProvider, int offset)
	{
		switch (this._currentLexeme.getType())
		{
			case CLASS:
				this.addClasses(proposals, offset);
				break;

			case ID:
				this.addIDs(proposals, offset);
				break;

			default:
				this.addAllElementProposals(proposals, offset);
				break;
		}
	}

	/**
	 * addPropertyValues
	 * 
	 * @param proposals
	 * @param lexemeProvider
	 * @param offset
	 */
	private void addPropertyValues(List<ICompletionProposal> proposals, LexemeProvider<CSSTokenType> lexemeProvider, int offset)
	{
		// get property name
		String propertyName = this.getPropertyName(lexemeProvider, offset);

		// lookup value list for property
		PropertyElement property = this._queryHelper.getProperty(propertyName);

		// build proposals from value list
		for (ValueElement value : property.getValues())
		{
			String name = value.getName();
			int length = name.length();
			String description = value.getDescription();
			Image image = PROPERTY_ICON;
			IContextInformation contextInfo = null;
			Image[] userAgentIcons = UserAgentManager.getInstance().getUserAgentImages(property.getUserAgentNames());

			CommonCompletionProposal proposal = new CommonCompletionProposal(name, offset, 0, length, image, name, contextInfo, description);
			proposal.setFileLocation(CSSIndexConstants.METADATA);
			proposal.setUserAgentImages(userAgentIcons);

			// add it to the list
			proposals.add(proposal);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.aptana.editor.common.CommonContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer
	 * , int, char, boolean)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset, char activationChar, boolean autoActivated)
	{
		// tokenize the current document
		IDocument document = viewer.getDocument();
		LexemeProvider<CSSTokenType> lexemeProvider = new LexemeProvider<CSSTokenType>(document, new CSSScopeScanner())
		{
			@Override
			protected CSSTokenType getTypeFromName(String name)
			{
				return CSSTokenType.get(name);
			}
		};

		// store a reference to the lexeme at the current position
		this._currentLexeme = lexemeProvider.getFloorLexeme(offset);

		// first step is to determine if we're inside our outside of a rule
		Location location = this.getLocation(lexemeProvider, offset);

		// create proposal container
		List<ICompletionProposal> result = new ArrayList<ICompletionProposal>();

		switch (location)
		{
			case OUTSIDE_RULE:
				this.addOutsideRuleProposals(result, lexemeProvider, offset);
				break;

			case INSIDE_RULE:
				this.addInsideRuleProposals(result, lexemeProvider, offset);
				break;

			case INSIDE_ARG:
				// TODO: lookup specific property and shows its values
				break;

			default:
				break;
		}

		// sort by display name
		Collections.sort(result, new Comparator<ICompletionProposal>()
		{
			@Override
			public int compare(ICompletionProposal o1, ICompletionProposal o2)
			{
				return o1.getDisplayString().compareToIgnoreCase(o2.getDisplayString());
			}
		});

		// select the current proposal based on the current lexeme
		this.setSelectedProposal(this._currentLexeme.getText(), result);

		// return results
		return result.toArray(new ICompletionProposal[result.size()]);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	@Override
	public char[] getCompletionProposalAutoActivationCharacters()
	{
		return new char[] { ':', '\t', '{', ';' };
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
	 */
	@Override
	public IContextInformationValidator getContextInformationValidator()
	{
		if (this._validator == null)
		{
			this._validator = new CSSContextInformationValidator();
		}

		return this._validator;
	}

	/**
	 * getInsideLocation
	 * 
	 * @param document
	 * @param offset
	 * @return
	 */
	private Location getInsideLocation(LexemeProvider<CSSTokenType> lexemeProvider, int offset)
	{
		Location location = Location.ERROR;

		switch (this._currentLexeme.getType())
		{
			case ELEMENT: // sometimes occurs with partially typed properties
			case PROPERTY:
				location = Location.INSIDE_PROPERTY;
				break;

			case COLON:
			case VALUE:
				location = Location.INSIDE_VALUE;
				break;

			default:
				break;
		}

		return location;
	}

	/**
	 * getLocation
	 * 
	 * @param lexemeProvider
	 * @param offset
	 * @return
	 */
	private Location getLocation(LexemeProvider<CSSTokenType> lexemeProvider, int offset)
	{
		Location result = Location.ERROR;

		if (offset == 0)
		{
			result = Location.OUTSIDE_RULE;
		}
		else
		{
			int index = lexemeProvider.getLexemeFloorIndex(offset);
			
			LOOP: while (index >= 0)
			{
				Lexeme<CSSTokenType> lexeme = lexemeProvider.getLexeme(index);
				
				switch (lexeme.getType())
				{
					case ID:
					case CLASS:
						result = Location.OUTSIDE_RULE;
						break LOOP;
	
					case CURLY_BRACE:
						result = ("{".equals(lexeme.getText())) ? Location.INSIDE_RULE : Location.OUTSIDE_RULE;
						break LOOP;
	
					case PROPERTY:
					case VALUE:
						result = Location.INSIDE_RULE;
						break LOOP;
	
					default:
						break;
				}
				
				index--;
			}
		}

		return result;
	}

	/**
	 * getPropertyName
	 * 
	 * @param document
	 * @param offset
	 * @return
	 */
	private String getPropertyName(LexemeProvider<CSSTokenType> lexemeProvider, int offset)
	{
		String result = null;
		int index = lexemeProvider.getLexemeFloorIndex(offset);

		for (int i = index; i >= 0; i--)
		{
			Lexeme<CSSTokenType> lexeme = lexemeProvider.getLexeme(i);

			if (lexeme.getType() == CSSTokenType.PROPERTY)
			{
				result = lexeme.getText();
				break;
			}
		}

		return result;
	}

	/**
	 * setSelectedProposal
	 * 
	 * @param prefix
	 * @param proposals
	 */
	private void setSelectedProposal(String prefix, List<ICompletionProposal> proposals)
	{
		ICompletionProposal caseSensitiveProposal = null;
		ICompletionProposal caseInsensitiveProposal = null;
		ICompletionProposal suggestedProposal = null;

		for (ICompletionProposal proposal : proposals)
		{
			String displayString = proposal.getDisplayString();
			int comparison = displayString.compareToIgnoreCase(prefix);

			if (comparison >= 0)
			{
				if (displayString.toLowerCase().startsWith(prefix.toLowerCase()))
				{
					caseInsensitiveProposal = proposal;

					if (displayString.startsWith(prefix))
					{
						caseSensitiveProposal = proposal;
						// found a match, so exit loop
						break;
					}
				}
			}
		}

		if (caseSensitiveProposal instanceof CommonCompletionProposal)
		{
			((CommonCompletionProposal) caseSensitiveProposal).setIsDefaultSelection(true);
		}
		else if (caseInsensitiveProposal instanceof CommonCompletionProposal)
		{
			((CommonCompletionProposal) caseInsensitiveProposal).setIsDefaultSelection(true);
		}
		else if (suggestedProposal instanceof CommonCompletionProposal)
		{
			((CommonCompletionProposal) suggestedProposal).setIsSuggestedSelection(true);
		}
		else
		{
			if (proposals.size() > 0)
			{
				ICompletionProposal proposal = proposals.get(0);
				
				if (proposal instanceof CommonCompletionProposal)
				{
					((CommonCompletionProposal) proposal).setIsSuggestedSelection(true);
				}
			}
		}
	}
}
