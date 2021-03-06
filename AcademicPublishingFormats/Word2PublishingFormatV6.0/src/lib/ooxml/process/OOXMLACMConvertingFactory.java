package lib.ooxml.process;

import java.io.File;
import java.math.BigInteger;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.docx4j.TraversalUtil;
import org.docx4j.dml.CTPositiveSize2D;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.finders.ClassFinder;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.DocumentSettingsPart;
import org.docx4j.openpackaging.parts.WordprocessingML.FootnotesPart;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.openpackaging.parts.WordprocessingML.NumberingDefinitionsPart;
import org.docx4j.openpackaging.parts.WordprocessingML.StyleDefinitionsPart;
import org.docx4j.vml.CTGroup;
import org.docx4j.wml.Body;
import org.docx4j.wml.CTFootnotes;
import org.docx4j.wml.CTFtnEdn;
import org.docx4j.wml.CTLanguage;
import org.docx4j.wml.CTSettings;
import org.docx4j.wml.JcEnumeration;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.P;
import org.docx4j.wml.PPr;
import org.docx4j.wml.PPrBase.Ind;
import org.docx4j.wml.PPrBase.PStyle;
import org.docx4j.wml.ParaRPr;
import org.docx4j.wml.Pict;
import org.docx4j.wml.R;
import org.docx4j.wml.RPr;
import org.docx4j.wml.STTblLayoutType;
import org.docx4j.wml.SdtBlock;
import org.docx4j.wml.SectPr;
import org.docx4j.wml.Style;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.TblGridCol;
import org.docx4j.wml.Text;

import base.AppEnvironment;
import base.UserMessage;
import db.AcademicFormatStructureDefinition;
import lib.AbstractPublishingStyle;
import lib.ooxml.author.Authors;
import lib.ooxml.converter.OOXMLConverterACM;
import lib.ooxml.identifier.OOXMLIdentifier;
import lib.ooxml.ref.ACMNumbering;
import lib.ooxml.style.OOXMLACMStyle;
import lib.ooxml.tool.OOXMLConvertTool;
import tools.LogUtils;

public class OOXMLACMConvertingFactory implements OOXMLConvertingInterface, Runnable{

	private OOXMLACMStyle acmStyle;
	
	private String sourceFileFormat;
	
	private int titleIndex = 0;
	
	private File convertedFile;
	private WordprocessingMLPackage wordMLPackage;
	private MainDocumentPart documentPart;
	private StyleDefinitionsPart stylePart;
	private NumberingDefinitionsPart numberingPart;
	private AbstractPublishingStyle publishingStyle;
	
	public OOXMLACMConvertingFactory(File convertedFile, WordprocessingMLPackage wordMLPackage, MainDocumentPart documentPart, StyleDefinitionsPart stylePart, AbstractPublishingStyle publishingStyle){
		this.convertedFile = convertedFile;
		this.wordMLPackage = wordMLPackage;
		this.documentPart = documentPart;
		this.stylePart = stylePart;
		this.numberingPart = documentPart.getNumberingDefinitionsPart();
		this.publishingStyle = publishingStyle;
		AppEnvironment.getInstance().setNumberingPart(numberingPart);
	}
	
	@Override
	public UserMessage adaptAbstract(MainDocumentPart documentPart, P p, int docContentIndex) {
		// TODO Auto-generated method stub
		UserMessage msg = new UserMessage();
		
		try {
			PPr pPr = OOXMLConvertTool.getPPr(p, true, true);
			
			removePSetting(pPr);
			
			List<Object> pContents = p.getContent();
			for(Object o : pContents){
				if(o instanceof R){
					R r = (R)o;
					r.setRPr(null);
				}
			}
			
			if((OOXMLConvertTool.isPTextStartWithString(p, "Abstract", true)
					|| OOXMLConvertTool.isPTextStartWithString(p, "Abstract.", true))
					&& p.toString().length()>"abstract".length()+2){
				R newR = new R();
				Text text = new Text();
				text.setValue("Abstract");
				newR.getContent().add(text);
				P newP = new P();
				newP.getContent().add(newR);
				newP.setPPr(new PPr());
				newP.getPPr().setPStyle(new PStyle());
				newP.getPPr().getPStyle().setVal(acmStyle.getStyleMap().get(OOXMLACMStyle.STYLENAME_ABSTRACTHEADER).getStyleId());
				
				documentPart.getContent().add(docContentIndex, newP);
				
				String s = p.toString();
				s = s.substring(s.indexOf("Abstract")+"Abstract".length()+2, s.length());
				p.getContent().clear();
				
				newR = new R();
				text = new Text();
				text.setValue(s);
				newR.getContent().add(text);
				p.getContent().add(newR);
				
				pPr.setPStyle(new PStyle());
				pPr.getPStyle().setVal(acmStyle.getStyleMap().get(OOXMLACMStyle.STYLENAME_ABSTRACT).getStyleId());
				p.setPPr(pPr);
				
			} else {
				PStyle pStyle = OOXMLConvertTool.getPStyle(pPr, true);
				pStyle.setVal(acmStyle.getStyleMap().get(OOXMLACMStyle.STYLENAME_ABSTRACTHEADER).getStyleId());
				pPr.setPStyle(pStyle);
				
				P abstractTextP = OOXMLConvertTool.getNextP(documentPart, docContentIndex);
				abstractTextP.setPPr(new PPr());
				OOXMLConvertTool.removeAllRPROfP(abstractTextP);
				abstractTextP.getPPr().setPStyle(new PStyle());
				abstractTextP.getPPr().getPStyle().setVal(acmStyle.getStyleMap().get(OOXMLACMStyle.STYLENAME_ABSTRACT).getStyleId());
			}
			
			LogUtils.log("abstract of doc was adapted.");
			
			msg.setMessageCode(UserMessage.MESSAGE_ABSTRACT_ADAPT_FINISH);
		} catch (Exception e) {
			// TODO: handle exception
			msg.setMessageDetails(e.getMessage());
		}
		
		return msg;
	}

	@Override
	public UserMessage adaptKeyWord(MainDocumentPart documentPart, P p, int docContentIndex) {
		// TODO Auto-generated method stub
		UserMessage msg = new UserMessage();
		
		try {
			PPr pPr = OOXMLConvertTool.getPPr(p, true, true);
			
			removePSetting(pPr);
			
			List<Object> pContents = p.getContent();
			for(Object o : pContents){
				if(o instanceof R){
					R r = (R)o;
					r.setRPr(null);
				}
			}
			
			if((OOXMLConvertTool.isPTextStartWithString(p, "Keywords", true)
					|| OOXMLConvertTool.isPTextStartWithString(p, "Keywords.", true))
					&& p.toString().length()>"Keywords".length()+2){
				R newR = new R();
				Text text = new Text();
				text.setValue("Keywords");
				newR.getContent().add(text);
				P newP = new P();
				newP.getContent().add(newR);
				newP.setPPr(new PPr());
				newP.getPPr().setPStyle(new PStyle());
				newP.getPPr().getPStyle().setVal(acmStyle.getStyleMap().get(OOXMLACMStyle.STYLENAME_KEYWORDHEAD).getStyleId());
				
				documentPart.getContent().add(docContentIndex, newP);
				
				String s = p.toString();
				s = s.substring("Keywords".length()+2, s.length());
				p.getContent().clear();
				
				newR = new R();
				text = new Text();
				text.setValue(s);
				newR.getContent().add(text);
				p.getContent().add(newR);
				
				pPr.setPStyle(new PStyle());
				pPr.getPStyle().setVal(acmStyle.getStyleMap().get(OOXMLACMStyle.STYLENAME_KEYWORD).getStyleId());
				p.setPPr(pPr);
				
			} else {
				PStyle pStyle = OOXMLConvertTool.getPStyle(pPr, true);
				pStyle.setVal(acmStyle.getStyleMap().get(OOXMLACMStyle.STYLENAME_KEYWORDHEAD).getStyleId());
				pPr.setPStyle(pStyle);

				P keywordTextP = OOXMLConvertTool.getNextP(documentPart, docContentIndex);
				keywordTextP.setPPr(new PPr());
				OOXMLConvertTool.removeAllRPROfP(keywordTextP);
				keywordTextP.getPPr().setPStyle(new PStyle());
				keywordTextP.getPPr().getPStyle().setVal(acmStyle.getStyleMap().get(OOXMLACMStyle.STYLENAME_ABSTRACT).getStyleId());
			}
			
			msg.setMessageCode(UserMessage.MESSAGE_KEYWORD_ADAPT_FINISH);
			LogUtils.log(msg.getMessage());
		} catch (Exception e) {
			// TODO: handle exception
			msg.setMessageDetails(e.getMessage());
		}
		
		return msg;
	}

	@Override
	public UserMessage adaptNumberFile(MainDocumentPart documentPart) {
		// TODO Auto-generated method stub
		UserMessage msg = new UserMessage();
//		documentPart.getBibliographyPart();
		NumberingDefinitionsPart numberingPart = documentPart.getNumberingDefinitionsPart();
		if(numberingPart!=null){
			numberingPart.getAbstractListDefinitions();
		}
		ACMNumbering.adaptNumberingFile(wordMLPackage, documentPart);
		return msg;
	}

	@Override
	public UserMessage adaptFontStyleClass(File convertedFile, WordprocessingMLPackage wordMLPackage, MainDocumentPart documentPart,
			StyleDefinitionsPart stylePart) {
		// TODO Auto-generated method stub
		UserMessage msg = new UserMessage();
		
		int index = 0;
		
		while(index < acmStyle.getStyleList().size()){
			try {
				if(OOXMLConvertTool.isStyleExist(acmStyle.getStyleList().get(index), stylePart)){
					Style style = stylePart.getStyleById(acmStyle.getStyleList().get(index).getStyleId());
					int styleIndex = style!=null?OOXMLConvertTool.getStyleDefinitionIndex(style, stylePart):-1;
					if(styleIndex>=0){
//						stylePart.getContents().getStyle().remove(styleIndex);
						stylePart.getContents().getStyle().set(styleIndex, style);
					}
				} else {
//					documentPart.addStyledParagraphOfText(acmStyle.getStyleList().get(index).getStyleId(), "doc "+acmStyle.getStyleList().get(index).getName().getVal());
//					stylePart.getContents().getStyle().add(acmStyle.getStyleList().get(index));
					ObjectFactory factory = Context.getWmlObjectFactory ();
					Style newStyle = factory.createStyle();
					newStyle.setCustomStyle(true);
					newStyle.setBasedOn(acmStyle.getStyleList().get(index).getBasedOn());
					newStyle.setLink(acmStyle.getStyleList().get(index).getLink());
					newStyle.setName(acmStyle.getStyleList().get(index).getName());
					newStyle.setNext(acmStyle.getStyleList().get(index).getNext());
					newStyle.setPPr(acmStyle.getStyleList().get(index).getPPr());
					newStyle.setQFormat(acmStyle.getStyleList().get(index).getQFormat());
					newStyle.setRPr(acmStyle.getStyleList().get(index).getRPr());
					newStyle.setStyleId(acmStyle.getStyleList().get(index).getStyleId());
					newStyle.setTblPr(acmStyle.getStyleList().get(index).getTblPr());
					newStyle.setTcPr(acmStyle.getStyleList().get(index).getTcPr());
					newStyle.setTrPr(acmStyle.getStyleList().get(index).getTrPr());
					newStyle.setType(acmStyle.getStyleList().get(index).getType());
//					newStyle.setUiPriority(value);
					stylePart.getJaxbElement().getStyle().add(newStyle);
				}
			} catch (Docx4JException e) {
				// TODO Auto-generated catch block
				LogUtils.log("ERROR while adapting style." + e.getMessage());
			}
			index++;
		}
		
		saveChange(convertedFile, wordMLPackage);
		
		LogUtils.log("font class will be modified.");
		
		return msg;
	}

	@Override
	public UserMessage removeUselessPageBreaks(WordprocessingMLPackage wordMLPackage, MainDocumentPart documentPart) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserMessage convert2Format(File convertedFile, WordprocessingMLPackage wordMLPackage, MainDocumentPart documentPart, StyleDefinitionsPart stylePart) {
		// TODO Auto-generated method stub
		UserMessage msg = new UserMessage();
		try {
//			OOXMLConvertTool.sourceFormat = null;
			NumberingDefinitionsPart numberingPart = documentPart.getNumberingDefinitionsPart();
			List<Object> docContentList = documentPart.getContent();
			acmStyle = new OOXMLACMStyle();
			acmStyle.setStyleMap(AppEnvironment.getInstance().getStylePool().getStyles().get(AcademicFormatStructureDefinition.ACM).getStyleMap());
			
			OOXMLConverterACM converter = new OOXMLConverterACM();
			OOXMLIdentifier identifier = new OOXMLIdentifier();
			
			converter.removeDefaultHeader(documentPart);
			
			int docContentIndex = 0;
			while(docContentIndex<docContentList.size()){
				Object o = docContentList.get(docContentIndex);
				if(o instanceof P) {
					P p = (P) o;
					int headingLvl = 0;
					if(identifier.isKeywordIdentified()){
						headingLvl = identifier.getHeadingLvl(p, numberingPart, stylePart);
					}
					if(identifier.isTitle(p, stylePart)){
						converter.adaptTitle(documentPart, p);
//						converter.appendLayoutPara(documentPart, Context.getWmlObjectFactory().createP(), docContentIndex, 1);
//						docContentIndex++;
					} else if(identifier.isAbstractHeader(p, stylePart)){
						converter.appendLayoutPara(documentPart, p, docContentIndex-1, 1);
						docContentIndex++;
						converter.adaptAbstract(documentPart, p, docContentIndex);
						docContentIndex++;
					} else if(identifier.isKeywordHeader(p, stylePart)){
						converter.adaptKeyWord(documentPart, p, docContentIndex);
						docContentIndex++;
					} else if(identifier.isAcknowledgmentHeader(p, numberingPart, stylePart)){
						converter.adaptAcknowledgment(documentPart, p, docContentIndex);
						docContentIndex++;
					} else if(identifier.isReferenceHeader(p, numberingPart, stylePart)) {
						converter.adaptLiteratureHeader(documentPart, p);
					} else if(headingLvl>0){
						switch (headingLvl) {
						case 1:
							converter.adaptHeading1(p);
							break;
						case 2:
							converter.adaptHeading2(p);
							break;
						case 3:
							converter.adaptHeading3(documentPart, p, docContentIndex);
							break;
						case 4:
							converter.adaptHeading4(documentPart, p, docContentIndex);
							break;
						default:
							break;
						}
					} else if(identifier.isFigureCaption(p, stylePart, numberingPart)){
						converter.adaptFigureCaption(documentPart, p);
					} else if(identifier.isAppendix(p, stylePart)){
//						converter.appendLayoutPara(documentPart, p, docContentIndex-1, 1);
//						docContentIndex++;
						converter.adaptAppendixHeader(documentPart, p, docContentIndex);
					} else if(identifier.isAppendixHeaderIdentified() && docContentIndex>documentPart.getContent().size()-2){
						converter.appendLayoutPara(documentPart, p, docContentIndex, 1);
//					} else if(identifier.isReferencesHeaderIdentified()){
//						adaptLiterature(documentPart, p);
					}
					
					
//					if(OOXMLConvertTool.isImage(p)){
//						System.out.println("test is image");
//					} else 
/*					if(OOXMLConvertTool.isTitle(p, stylePart)){
						titleIndex = docContentIndex;
						adaptTitle(documentPart, p);
						appendLayoutPara(documentPart, p, docContentIndex, 1);
						docContentIndex++;
						saveChange(convertedFile, wordMLPackage);
					} else if(OOXMLConvertTool.isAbstract(p, stylePart)) {
						adaptAbstract(documentPart, p, docContentIndex);
						saveChange(convertedFile, wordMLPackage);
					} else if(OOXMLConvertTool.isKeywords(p, stylePart)) {
						adaptKeyWord(documentPart, p, docContentIndex);
						saveChange(convertedFile, wordMLPackage);
					} else if(OOXMLConvertTool.isSectionHeader1(p, stylePart, documentPart)){
						adaptHeading1(p);
						saveChange(convertedFile, wordMLPackage);
					} else if(OOXMLConvertTool.isSectionHeader2(p, stylePart, documentPart)) {
						adaptHeading2(p);
						saveChange(convertedFile, wordMLPackage);
					} else if(OOXMLConvertTool.isSectionHeader3(p, stylePart, documentPart)) {
						adaptHeading3(documentPart, p, docContentIndex);
						saveChange(convertedFile, wordMLPackage);
					} else if(OOXMLConvertTool.isSectionHeader4(p, stylePart, documentPart)) {
						adaptHeading4(documentPart, p, docContentIndex);
						saveChange(convertedFile, wordMLPackage);
					} else if(OOXMLConvertTool.isCaption(p, documentPart, stylePart)){
						adaptCaption(documentPart, p);
						saveChange(convertedFile, wordMLPackage);
					} else if(OOXMLConvertTool.isTableCaption(p, stylePart)){
						adaptTableCaption(documentPart, p);
						saveChange(convertedFile, wordMLPackage);
					} else if(OOXMLConvertTool.isAcknowledgReferenceHeader(p, stylePart, documentPart)){
						adaptLiteratureHeader(documentPart, p);
						saveChange(convertedFile, wordMLPackage);
					} else if(OOXMLConvertTool.isReferenceText(p, stylePart, documentPart)){
						adaptLiterature(documentPart, p);
						saveChange(convertedFile, wordMLPackage);
					} else {
						removePSetting(p.getPPr());
					}*/
				} else if(o instanceof Tbl){
					Tbl tbl = (Tbl)o;
					
				}
				docContentIndex++;
				AppEnvironment.getInstance().getGui().getProgressBar().setValue(docContentIndex*100/(6+docContentList.size()));
			}
			
			converter.adaptFootNote(convertedFile, wordMLPackage);
			AppEnvironment.getInstance().getGui().getProgressBar().setValue((++docContentIndex)*100/(6+docContentList.size()));
			adaptImages(documentPart);
			AppEnvironment.getInstance().getGui().getProgressBar().setValue((++docContentIndex)*100/(6+docContentList.size()));
			converter.adaptFontStyleClass(convertedFile, wordMLPackage, documentPart, stylePart);
			AppEnvironment.getInstance().getGui().getProgressBar().setValue((++docContentIndex)*100/(6+docContentList.size()));
			converter.adaptPageLayout(documentPart, wordMLPackage);
			AppEnvironment.getInstance().getGui().getProgressBar().setValue((++docContentIndex)*100/(6+docContentList.size()));
			adaptNumberFile(documentPart);
			AppEnvironment.getInstance().getGui().getProgressBar().setValue((++docContentIndex)*100/(6+docContentList.size()));
			adaptTable(documentPart);
			AppEnvironment.getInstance().getGui().getProgressBar().setValue((++docContentIndex)*100/(6+docContentList.size()));
			
			saveChange(convertedFile, wordMLPackage);
		} catch (Exception e) {
			// TODO: handle exception
			msg.setMessageDetails(e.getMessage());
			LogUtils.log(msg.getMessage());
		}
		
		msg.setMessageCode(UserMessage.MESSAGE_GENERAL_OK);
		
		return msg;
	}

	@Override
	public UserMessage adaptTitle(MainDocumentPart documentPart, P p) {
		// TODO Auto-generated method stub
		UserMessage msg = new UserMessage();
		try {
			PPr pPr = OOXMLConvertTool.getPPr(p, true, true);
			
			removePSetting(pPr);
			
			List<Object> pContents = p.getContent();
			for(Object o : pContents){
				if(o instanceof R){
					R r = (R)o;
					r.setRPr(Context.getWmlObjectFactory().createRPr());
					r.getRPr().setLang(new CTLanguage());
					r.getRPr().getLang().setVal("en-US");
				}
			}
			
			PStyle pStyle = OOXMLConvertTool.getPStyle(pPr, true);
			pStyle.setVal(acmStyle.getStyleMap().get(OOXMLACMStyle.STYLENAME_TITLE).getStyleId());
			pPr.setPStyle(pStyle);
			
			LogUtils.log("Title of doc was adapted.");
			
			msg.setMessageCode(UserMessage.MESSAGE_TITLE_ADAPT_FINISH);
		} catch (Exception e) {
			// TODO: handle exception
			msg.setMessageDetails(e.getMessage());
		}
		
		return msg;
	}

	@Override
	public UserMessage saveChange(File convertedFile, WordprocessingMLPackage wordMLPackage) {
		// TODO Auto-generated method stub
		UserMessage msg = new UserMessage();
		try {
			wordMLPackage.save(convertedFile);
			msg.setMessageDetails("The Changes have been saved.");
		} catch (Exception e) {
			// TODO: handle exception
			msg.setMessageDetails(e.getMessage());
		}
		return msg;
	}

	@Override
	public UserMessage removePSetting(PPr pPr) {
		// TODO Auto-generated method stub
		UserMessage msg = new UserMessage();
		if(pPr!=null){
			pPr.setInd(null);
			pPr.setJc(null);
			pPr.setNumPr(null);
			pPr.setSectPr(null);
			pPr.setTabs(null);
			pPr.setTextAlignment(null);
			
			pPr.setRPr(new ParaRPr());
			
			pPr.getRPr().setLang(new CTLanguage());
			pPr.getRPr().getLang().setVal("en-US");
		}
		
		return msg;
	}

	@Override
	public UserMessage adaptHeading1(P p) {
		// TODO Auto-generated method stub
		UserMessage msg = new UserMessage();
		
		try {
			PPr pPr = OOXMLConvertTool.getPPr(p, true, true);
			
			removePSetting(pPr);
			
			List<Object> pContents = p.getContent();
			for(Object o : pContents){
				if(o instanceof R){
					R r = (R)o;
					r.setRPr(new RPr());
					r.getRPr().setLang(new CTLanguage());
					r.getRPr().getLang().setVal("en-US");
				}
			}
			
			PStyle pStyle = OOXMLConvertTool.getPStyle(pPr, true);
			pStyle.setVal(acmStyle.getStyleMap().get(OOXMLACMStyle.STYLENAME_HEADING1).getStyleId());
			pPr.setPStyle(pStyle);
			
			OOXMLConvertTool.removeHeadingNum(1, p);
			
			LogUtils.log("Heading1 of doc was adapted.");
			
			msg.setMessageCode(UserMessage.MESSAGE_HEADING1_ADAPT_FINISH);
		} catch (Exception e) {
			// TODO: handle exception
			msg.setMessageDetails(e.getMessage());
		}
		
		return msg;
	}

	@Override
	public UserMessage adaptHeading2(P p) {
		// TODO Auto-generated method stub
		UserMessage msg = new UserMessage();
		
		try {
			PPr pPr = OOXMLConvertTool.getPPr(p, true, true);
			
			removePSetting(pPr);
			
			List<Object> pContents = p.getContent();
			for(Object o : pContents){
				if(o instanceof R){
					R r = (R)o;
					r.setRPr(new RPr());
					r.getRPr().setLang(new CTLanguage());
					r.getRPr().getLang().setVal("en-US");
				}
			}
			
			PStyle pStyle = OOXMLConvertTool.getPStyle(pPr, true);
			pStyle.setVal(acmStyle.getStyleMap().get(OOXMLACMStyle.STYLENAME_HEADING2).getStyleId());
			pPr.setPStyle(pStyle);
			
			OOXMLConvertTool.removeHeadingNum(2, p);
			
			LogUtils.log("Heading2 of doc was adapted.");
			
			msg.setMessageCode(UserMessage.MESSAGE_HEADING2_ADAPT_FINISH);
		} catch (Exception e) {
			// TODO: handle exception
			msg.setMessageDetails(e.getMessage());
		}
		
		return msg;
	}

	private boolean adaptHeading3(P p, MainDocumentPart documentPart, int docContentIndex, String sectionStr, String regex){
		boolean result = false;
		try {
			if(p!=null){
				sectionStr = OOXMLConvertTool.removeSectionNumStringOfHeading(sectionStr, regex);
				
				if(sectionStr!=null && sectionStr.length()>0){
					if(OOXMLConvertTool.removeSectionFromText(p, documentPart.getStyleDefinitionsPart(), regex, false, true)){	// IEEE -> ACM
						
					} else {
						OOXMLConvertTool.removeSectionFromText(p, documentPart.getStyleDefinitionsPart(), regex, true, false);	// Springer -> ACM
					}
					
					R newR = Context.getWmlObjectFactory().createR();
					Text text = Context.getWmlObjectFactory().createText();
					text.setValue(sectionStr);
					newR.getContent().add(text);
					P newP = Context.getWmlObjectFactory().createP();
					newP.getContent().add(newR);
					newP.setPPr(Context.getWmlObjectFactory().createPPr());
					newP.getPPr().setPStyle(Context.getWmlObjectFactory().createPPrBasePStyle());
					newP.getPPr().getPStyle().setVal(acmStyle.getStyleMap().get(OOXMLACMStyle.STYLENAME_HEADING3).getStyleId());
					documentPart.getContent().add(docContentIndex, newP);
					
					result = true;
				}
				
//				newR = new R();
//				text = new Text();
//				text.setValue(sectionStr);
//				newR.getContent().add(text);
//				p.getContent().add(0, newR);
			}
		} catch (Exception e) {
			// TODO: handle exception
			LogUtils.log(e.getMessage());
		}
		
		return result;
	}
	private boolean adaptHeading4(P p, MainDocumentPart documentPart, int docContentIndex, String sectionStr, String regex){
		boolean result = false;
		try {
			if(p!=null){
				sectionStr = OOXMLConvertTool.removeSectionNumStringOfHeading(sectionStr, regex);
				
				if(sectionStr!=null && sectionStr.length()>0){
					if(OOXMLConvertTool.removeSectionFromText(p, documentPart.getStyleDefinitionsPart(), regex, false, true)){	// IEEE -> ACM
						
					} else {
						OOXMLConvertTool.removeSectionFromText(p, documentPart.getStyleDefinitionsPart(), regex, false, true);	// Springer -> ACM
					}
					
					R newR = Context.getWmlObjectFactory().createR();
					Text text = Context.getWmlObjectFactory().createText();
					text.setValue(sectionStr);
					newR.getContent().add(text);
					P newP = Context.getWmlObjectFactory().createP();
					newP.getContent().add(newR);
					newP.setPPr(Context.getWmlObjectFactory().createPPr());
					newP.getPPr().setPStyle(Context.getWmlObjectFactory().createPPrBasePStyle());
					newP.getPPr().getPStyle().setVal(acmStyle.getStyleMap().get(OOXMLACMStyle.STYLENAME_HEADING4).getStyleId());
					documentPart.getContent().add(docContentIndex, newP);
					
					result = true;
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			LogUtils.log(e.getMessage());
		}
		
		return result;
	}
	
	@Override
	public UserMessage adaptHeading3(MainDocumentPart documentPart, P p, int docContentIndex) {
		// TODO Auto-generated method stub
		UserMessage msg = new UserMessage();
		
		try {
			
			String sectionStr = OOXMLConvertTool.getSectionHeader3String(p, documentPart.getStyleDefinitionsPart());
			if(sectionStr!=null && sectionStr.equals(p.toString())){
				PPr pPr = p.getPPr();
				if(pPr!=null){
					
				} else {
					pPr = Context.getWmlObjectFactory().createPPr();
					p.setPPr(pPr);
				}
				if(pPr.getPStyle()!=null){
					
				} else {
					pPr.setPStyle(Context.getWmlObjectFactory().createPPrBasePStyle());
				}
				pPr.getPStyle().setVal(acmStyle.getStyleMap().get(OOXMLACMStyle.STYLENAME_HEADING3).getStyleId());
			} else {
				String regex = "^\\d\\)\\s";
				if(adaptHeading3(p, documentPart, docContentIndex, sectionStr, regex)){
					
				} else {
					regex = ".*";
					adaptHeading3(p, documentPart, docContentIndex, sectionStr, regex);
				}
			}
//			pPr.setPStyle(new PStyle());
//			pPr.getPStyle().setVal(acmStyle.getStyleMap().get(OOXMLACMStyle.STYLENAME_KEYWORD).getStyleId());
//			p.setPPr(pPr);
			OOXMLConvertTool.removeHeadingNum(3, p);
			
			if(p!=null && p.getPPr()!=null && p.getPPr().getNumPr()!=null){
				p.getPPr().setNumPr(null);
			}
			
			msg.setMessageCode(UserMessage.MESSAGE_HEADING3_ADAPT_FINISH);
			LogUtils.log(msg.getMessage());
		} catch (Exception e) {
			// TODO: handle exception
			msg.setMessageDetails(e.getMessage());
		}
		
		return msg;
	}

	@Override
	public UserMessage adaptCaption(MainDocumentPart documentPart, P p) {
		// TODO Auto-generated method stub
		UserMessage msg = new UserMessage();
		
		try {
			if(p!=null){
				if(p.getPPr()!=null){
					
				} else {
					p.setPPr(Context.getWmlObjectFactory().createPPr());
				}
				if(p.getPPr().getPStyle()!=null){
					
				} else {
					p.getPPr().setPStyle(Context.getWmlObjectFactory().createPPrBasePStyle());
				}
				p.getPPr().getPStyle().setVal(acmStyle.getStyleMap().get(OOXMLACMStyle.STYLENAME_CAPTION).getStyleId());
				p.getPPr().setRPr(Context.getWmlObjectFactory().createParaRPr());
				p.getPPr().setJc(Context.getWmlObjectFactory().createJc());
				p.getPPr().getJc().setVal(JcEnumeration.CENTER);
				OOXMLConvertTool.removeAllRPROfP(p);
				
				OOXMLConvertTool.adaptFirstWord2Special(p, "Fig.", "Figure", null);
				
				msg.setMessageCode(UserMessage.MESSAGE_CAPTION_ADAPT_FINISH);
				LogUtils.log(msg.getMessage());
			}
		} catch (Exception e) {
			// TODO: handle exception
			msg.setMessageDetails(e.getMessage());
		}
		return msg;
	}

	@Override
	public UserMessage adaptHeading4(MainDocumentPart documentPart, P p, int docContentIndex) {
		// TODO Auto-generated method stub
		UserMessage msg = new UserMessage();
		
		try {
			
			String sectionStr = OOXMLConvertTool.getSectionHeader4String(p, documentPart.getStyleDefinitionsPart());
			if(sectionStr!=null && sectionStr.equals(p.toString())){
				PPr pPr = p.getPPr();
				if(pPr!=null){
					
				} else {
					pPr = Context.getWmlObjectFactory().createPPr();
					p.setPPr(pPr);
				}
				if(pPr.getPStyle()!=null){
					
				} else {
					pPr.setPStyle(Context.getWmlObjectFactory().createPPrBasePStyle());
				}
				pPr.getPStyle().setVal(acmStyle.getStyleMap().get(OOXMLACMStyle.STYLENAME_HEADING4).getStyleId());
			} else {
				String regex = "^[a-z]\\)\\s";
				if(adaptHeading4(p, documentPart, docContentIndex, sectionStr, regex)){
					
				} else {
					regex = ".*";
					adaptHeading4(p, documentPart, docContentIndex, sectionStr, regex);
				}
			}
			
			
			if(p!=null && p.getPPr()!=null && p.getPPr().getNumPr()!=null){
				p.getPPr().setNumPr(null);
				p.getPPr().setPStyle(null);
				if(p.getContent()!=null){
					for (int i = 0; i < p.getContent().size(); i++) {
						Object o = p.getContent().get(i);
						if(o instanceof R){
							R r = (R)o;
							if(r.getContent()!=null){
								for (int j = 0; j < r.getContent().size(); j++) {
									Object rO = r.getContent().get(j);
									if(rO instanceof JAXBElement<?>
											&& ((JAXBElement) rO).getValue() instanceof Text){
										Text text = (Text)(((JAXBElement) rO).getValue());
										if(text.getSpace()!=null){
											text.setSpace(null);
										}
									}
								}
							}
						}
					}
				}
			}
			OOXMLConvertTool.removeHeadingNum(4, p);
			
			msg.setMessageCode(UserMessage.MESSAGE_HEADING4_ADAPT_FINISH);
			LogUtils.log(msg.getMessage());
		} catch (Exception e) {
			// TODO: handle exception
			msg.setMessageDetails(e.getMessage());
		}
		
		return msg;
	}

	@Override
	public UserMessage adaptTableCaption(MainDocumentPart documentPart, P p) {
		// TODO Auto-generated method stub
		UserMessage msg = new UserMessage();
		
		try {
			if(p!=null){
				if(p.getPPr()!=null){
					
				} else {
					p.setPPr(Context.getWmlObjectFactory().createPPr());
				}
				if(p.getPPr().getPStyle()!=null){
					
				} else {
					p.getPPr().setPStyle(Context.getWmlObjectFactory().createPPrBasePStyle());
				}
				p.getPPr().getPStyle().setVal(acmStyle.getStyleMap().get(OOXMLACMStyle.STYLENAME_CAPTION).getStyleId());
//				p.getPPr().setRPr(Context.getWmlObjectFactory().createParaRPr());
				p.getPPr().setRPr(null);
				p.getPPr().setJc(Context.getWmlObjectFactory().createJc());
				p.getPPr().getJc().setVal(JcEnumeration.CENTER);
				OOXMLConvertTool.removeAllRPROfP(p);
				
				OOXMLConvertTool.adaptFirstWord2Special(p, "TABLE", "Table", null);
				
				msg.setMessageCode(UserMessage.MESSAGE_TABLECAPTION_ADAPT_FINISH);
				LogUtils.log(msg.getMessage());
			}
		} catch (Exception e) {
			// TODO: handle exception
			msg.setMessageDetails(e.getMessage());
		}
		
		return msg;
	}

	@Override
	public UserMessage adaptLiteratureHeader(MainDocumentPart documentPart, P p) {
		// TODO Auto-generated method stub
		UserMessage msg = new UserMessage();
		
		try {
			if(p!=null){
				PPr pPr = p.getPPr();
				if(pPr!=null){
					
				} else {
					pPr = Context.getWmlObjectFactory().createPPr();
					p.setPPr(pPr);
				}
				if(pPr.getPStyle()!=null){
					
				} else {
					pPr.setPStyle(Context.getWmlObjectFactory().createPPrBasePStyle());
				}
				pPr.getPStyle().setVal(acmStyle.getStyleMap().get(OOXMLACMStyle.STYLENAME_REFERENCEHEAD).getStyleId());
				msg.setMessageCode(UserMessage.MESSAGE_REFERENCEHEADER_ADPAT_FINISH);
				LogUtils.log(msg.getMessage());
			}
		} catch (Exception e) {
			// TODO: handle exception
			msg.setMessageDetails(e.getMessage());
		}
		return msg;
	}

	@Override
	public UserMessage adaptLiterature(MainDocumentPart documentPart, P p) {
		// TODO Auto-generated method stub
		UserMessage msg = new UserMessage();
		
		try {
			if(p!=null){
				PPr pPr = p.getPPr();
				if(pPr!=null){
					
				} else {
					pPr = Context.getWmlObjectFactory().createPPr();
					p.setPPr(pPr);
				}
				if(pPr.getPStyle()!=null){
					
				} else {
					pPr.setPStyle(Context.getWmlObjectFactory().createPPrBasePStyle());
				}
				pPr.getPStyle().setVal(acmStyle.getStyleMap().get(AcademicFormatStructureDefinition.BIBLIOGRAPHY).getStyleId());
				pPr.setInd(new Ind());
				pPr.getInd().setLeft(new BigInteger("360"));
				pPr.getInd().setHanging(new BigInteger("360"));
				
				msg.setMessageCode(UserMessage.MESSAGE_REFERENCE_ADPAT_FINISH);
				if(AppEnvironment.getInstance().isActiveTestMode()){
					LogUtils.log(msg.getMessage());
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			msg.setMessageDetails(e.getMessage());
			LogUtils.log(msg.getMessage());
		}
		
		return msg;
	}
	public UserMessage adaptImages2(MainDocumentPart documentPart){
		UserMessage msg = new UserMessage();
		
		try {
			ClassFinder classFiner = new ClassFinder(Pict.class);
			new TraversalUtil(documentPart.getContent(), classFiner);
			for(Object pictObj : classFiner.results){
				Pict pict = (Pict) pictObj;
				List<Object> pictContentList = pict.getAnyAndAny();
				if(pictContentList!=null){
					for(Object o : pictContentList){
						if(o instanceof CTGroup){
							CTGroup group = (CTGroup) o;
							String styleString = group.getStyle();
							LogUtils.log(styleString);
						} else if(o instanceof JAXBElement<?>){
							if(((JAXBElement<?>) o).getValue() instanceof CTGroup){
								CTGroup group = (CTGroup) ((JAXBElement<?>) o).getValue();
								String styleString = group.getStyle();
								float factor = -1;
								int width = 468;//270;//648;
								if(styleString.indexOf(";width:")<0){
									factor = -2;
								}
								if(styleString!=null && styleString.length()>0){
									String[] temps = styleString.split(";");
									for (int i = 0; i < temps.length; i++) {
										if(temps[i].indexOf("left")==0){
											temps[i] = "left:0";
										} else if(temps[i].indexOf("margin-left:")==0){
											temps[i] = "margin-left:0pt";
										} else if(temps[i].indexOf("margin-top:")==0){
											temps[i] = "margin-top:30pt";
										} else if(temps[i].indexOf("width:")==0) {
											String[] vals = temps[i].split(":");
											if(vals[1].indexOf("pt")>0){
												String val = vals[1].substring(0, vals[1].indexOf("pt"));
												if(Float.parseFloat(val)>width){
													factor = width/Float.parseFloat(val);//-2;
													temps[i] = "width:"+width+"pt";
												} else {
													factor = width/Float.parseFloat(val);
//													LogUtils.log(width+"/"+Float.parseFloat(val)+"="+factor);
													temps[i] = "width:"+width+"pt";
												}
											} else if(vals[1].indexOf("in")>0){
												String val = vals[1].substring(0, vals[1].indexOf("in"));
												if(Float.parseFloat(val)*72>width){
													factor = width/Float.parseFloat(val);//-2;
													temps[i] = "width:"+width+"pt";
												} else {
													factor = width/(Float.parseFloat(val)*72);
													temps[i] = "width:"+width+"pt";
												}
											}
//											LogUtils.log("factor="+factor);
										} else if(temps[i].indexOf("height:")==0) {
											if(factor>0){
												String[] vals = temps[i].split(":");
												if(vals[1].indexOf("pt")>0){
													String val = vals[1].substring(0, vals[1].indexOf("pt"));
													temps[i] = "height:"+factor*Float.parseFloat(val)+"pt";
												} else if(vals[1].indexOf("in")>0){
													String val = vals[1].substring(0, vals[1].indexOf("in"));
//													LogUtils.log(factor+"*"+Float.parseFloat(val)+"*72="+factor*(Float.parseFloat(val)*72));
													temps[i] = "height:"+factor*(Float.parseFloat(val)*72)+"pt";
												}
											}
										}
									}
									styleString="";
									for (int i = 0; i < temps.length; i++) {
										boolean isEnd = (i+1>=temps.length);
										styleString+=temps[i]+(isEnd?"":";");
									}
								}
//								LogUtils.log(styleString);
								group.setStyle(styleString);
							} else {
								LogUtils.log(((JAXBElement) o).getValue().getClass().getName());
							}
						} else {
							LogUtils.log(o.getClass().getName());
						}
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			LogUtils.log(e.getMessage());
		}
		
		try {
			ClassFinder classFiner = new ClassFinder(Inline.class);
			new TraversalUtil(documentPart.getContent(), classFiner);
			for(Object inlineObj : classFiner.results){
				Inline inline = (Inline) inlineObj;
				CTPositiveSize2D extent = inline.getExtent();
				long cx = extent.getCx();
				long cy = extent.getCy();
				if(cx>12240){
					float ratio = (float)(3*914400)/cx;
					extent.setCx(3*914400);
					long calY = (long) (cy*ratio);//(long) (cy*3/cx*914400);
					extent.setCy(calY);
//					LogUtils.log("image size from ("+cx+", "+ cy+") to ("+extent.getCx()+", "+ extent.getCy()+")");
					if(inline.getGraphic()!=null && inline.getGraphic().getGraphicData()!=null 
							&& inline.getGraphic().getGraphicData().getPic()!=null
							&& inline.getGraphic().getGraphicData().getPic().getSpPr()!=null
							&& inline.getGraphic().getGraphicData().getPic().getSpPr().getXfrm()!=null
							&& inline.getGraphic().getGraphicData().getPic().getSpPr().getXfrm().getExt()!=null){
						inline.getGraphic().getGraphicData().getPic().getSpPr().getXfrm().getExt().setCx(extent.getCx());
						inline.getGraphic().getGraphicData().getPic().getSpPr().getXfrm().getExt().setCy(extent.getCy());
//						LogUtils.logShort("image size changed\n");
					}
				} 
			}
//			msg.setMessageCode(UserMessage.MESSAGE_IMAGE_ADAPT_FINISH);
//			LogUtils.log(msg.getMessage());
		} catch (Exception e) {
			// TODO: handle exception
			LogUtils.log(e.getMessage());
		}
		
		return msg;
	}
	@Override
	public UserMessage adaptImages(MainDocumentPart documentPart) {
		// TODO Auto-generated method stub
		UserMessage msg = new UserMessage();
		OOXMLConvertTool.fixImgAlignment(documentPart);
//		OOXMLConvertTool.fixImgWidth(documentPart);
		msg.setMessageCode(UserMessage.MESSAGE_IMAGE_ADAPT_FINISH);
		adaptImages2(documentPart);
		if(AppEnvironment.getInstance().isActiveTestMode()){
			LogUtils.log(msg.getMessage());
		}
		return msg;
	}

	@Override
	public UserMessage adaptPageLayout(MainDocumentPart documentPart, WordprocessingMLPackage wordMLPackage) {
		// TODO Auto-generated method stub
		UserMessage msg = new UserMessage();
		try {
			if(documentPart!=null){
				Body body = wordMLPackage.getMainDocumentPart().getJaxbElement().getBody();
				if(body!=null){
					SectPr bodySectPr = body.getSectPr();
					OOXMLConvertTool.fixPageLayout(bodySectPr, "2", "475", false);
//					OOXMLConvertTool.adaptPageLayout(bodySectPr, "2", "720", "12240", "15840", "1", "1080", "1440", "1080", "1080", "720", "720", "0");
					/*
					 * fix: default header need to remove
					 */
					if(bodySectPr.getEGHdrFtrReferences()!=null){
						bodySectPr.getEGHdrFtrReferences().clear();
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			msg.setMessageDetails(e.getMessage());
			LogUtils.log(msg.getMessage());
		}
		
		return msg;
	}

	@Override
	public UserMessage adaptFootNote(File convertedFile, WordprocessingMLPackage wordMLPackage) {
		// TODO Auto-generated method stub
		UserMessage msg = new UserMessage();
		
		try {
			FootnotesPart footPart = wordMLPackage.getMainDocumentPart().getFootnotesPart();
			if(footPart!=null){
				CTFootnotes footNotes = footPart.getContents();
				if(footNotes!=null){
					List<CTFtnEdn> footNotesList =  footNotes.getFootnote();
					for (int i = 0; i < footNotesList.size(); i++) {
						CTFtnEdn footNote = footNotesList.get(i);
						List<Object> footNoteContent = footNote.getContent();
						if(footNoteContent!=null){
							for (Object o : footNoteContent) {
								if(o instanceof P){
									P p = (P)o;
									if(p.getPPr()!=null){
										p.getPPr().setPStyle(Context.getWmlObjectFactory().createPPrBasePStyle());
										p.getPPr().getPStyle().setVal(acmStyle.getStyleMap().get(OOXMLACMStyle.STYLENAME_FOOTNOTES).getStyleId());
										
										if(p.getPPr().getRPr()!=null){
											p.getPPr().getRPr().setB(null);
											p.getPPr().getRPr().setBCs(null);
											p.getPPr().getRPr().setI(null);
											p.getPPr().getRPr().setICs(null);
											p.getPPr().getRPr().setCaps(null);
											p.getPPr().getRPr().setSmallCaps(null);
											p.getPPr().getRPr().setRFonts(null);
										}
										
									}
								}
							}
						}
					}
				}
				
			}
		} catch (Exception e) {
			// TODO: handle exception
			msg.setMessageDetails(e.getMessage());
			LogUtils.log(msg.getMessage());
		}
		
		return msg;
	}

	@Override
	public UserMessage appendLayoutPara(MainDocumentPart documentPart, P p, int docContentIndex, int cols) {
		// TODO Auto-generated method stub
		UserMessage msg = new UserMessage();
		
		try {
			Object o = documentPart.getContent().get(docContentIndex+1);
			P newP = null;
			if(o instanceof P && ((P)o).toString().length()>0){
				newP = Context.getWmlObjectFactory().createP();
			} else {
				newP = (P)o;
			}
			newP.setPPr(Context.getWmlObjectFactory().createPPr());
			newP.getPPr().setSectPr(Context.getWmlObjectFactory().createSectPr());
//			newP.getPPr().getSectPr().setCols(Context.getWmlObjectFactory().createCTColumns());
//			newP.getPPr().getSectPr().getCols().setNum(new BigInteger("1"));
//			newP.getPPr().getSectPr().getCols().setSpace(new BigInteger("720"));
//			newP.getPPr().getSectPr().setType(Context.getWmlObjectFactory().createSectPrType());
//			newP.getPPr().getSectPr().getType().setVal("continuous");
			OOXMLConvertTool.adaptPageLayout(newP.getPPr().getSectPr(), String.valueOf(cols), "720", "12240", "15840", "1", "1080", "1440", "1080", "1080", "720", "720", "0");
			documentPart.getContent().add(docContentIndex+1, newP);
		} catch (Exception e) {
			// TODO: handle exception
			msg.setMessageDetails(e.getMessage());
			LogUtils.log(msg.getMessage());
		}
		
		return msg;
	}

	@Override
	public UserMessage adaptTable(MainDocumentPart documentPart) {
		// TODO Auto-generated method stub
		UserMessage msg = new UserMessage();
		
		try {
			ClassFinder classFinder = new ClassFinder(Tbl.class);
			new TraversalUtil(documentPart.getContent(), classFinder);
			int index = 0;
			while(index<classFinder.results.size()){
				Tbl tbl = (Tbl) classFinder.results.get(index);
				if(tbl.getTblPr()!=null){
//					tbl.getTblPr().getTblCaption();
					if(tbl.getTblPr().getTblW()!=null
							&& tbl.getTblPr().getTblW().getType()!=null
							&& !tbl.getTblPr().getTblW().getType().equals("auto")
							&& tbl.getTblPr().getTblW().getW()!=null
							&& tbl.getTblPr().getTblW().getW().intValue()>12240/2
							&& tbl.getTblGrid()!=null
							&& tbl.getTblGrid().getGridCol()!=null
							&& tbl.getTblGrid().getGridCol().size()<6){
//						System.out.println("something in 1");
						tbl.getTblPr().getTblW().setW(new BigInteger(""+(12240/2)));
						tbl.getTblPr().setTblLayout(Context.getWmlObjectFactory().createCTTblLayoutType());
						tbl.getTblPr().getTblLayout().setType(STTblLayoutType.AUTOFIT);
					} else if(tbl.getTblGrid()!=null
							&& tbl.getTblGrid().getGridCol()!=null
							&& tbl.getTblGrid().getGridCol().size()<6){
						int calTblW = 0;
						for(TblGridCol col : tbl.getTblGrid().getGridCol()){
							if(col.getW()!=null){
								calTblW += col.getW().intValue();
							}
						}
						int calDiff = (12240/2)-calTblW;
						int calFactor = 0;
						if(calDiff<0){
							calFactor = calDiff/tbl.getTblGrid().getGridCol().size();
							for(TblGridCol col : tbl.getTblGrid().getGridCol()){
								if(col.getW()!=null){
									col.setW(new BigInteger((col.getW().intValue()+calFactor)+""));
								}
							}
//							System.out.println("something in 2");
						}
						
					} else if(tbl.getTblGrid()!=null
							&& tbl.getTblGrid().getGridCol()!=null
							&& tbl.getTblGrid().getGridCol().size()>=6){
						tbl.getTblPr().setTblLayout(Context.getWmlObjectFactory().createCTTblLayoutType());
						tbl.getTblPr().getTblLayout().setType(STTblLayoutType.AUTOFIT);
//						System.out.println("something in 3");
					} else {
//						System.out.println("something in 4");
					}
				}
				index++;
			}
		} catch (Exception e) {
			// TODO: handle exception
			msg.setMessageDetails(e.getMessage());
			LogUtils.log(msg.getMessage());
		}
		
		return msg;
	}

	@Override
	public UserMessage adaptAuthors(MainDocumentPart documentPart, int docContentIndex, int authorCount, Authors authors) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserMessage adaptSettings(MainDocumentPart documentPart) {
		// TODO Auto-generated method stub
		UserMessage msg = new UserMessage();
		DocumentSettingsPart settingPart = documentPart.getDocumentSettingsPart();
		try {
			CTSettings settings = settingPart.getContents();
			settings.setAutoHyphenation(null);
		} catch (Docx4JException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return msg;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		convert2FormatNew(convertedFile, wordMLPackage, documentPart, stylePart);
	}
	
	public UserMessage convert2FormatNew(File convertedFile, WordprocessingMLPackage wordMLPackage, MainDocumentPart documentPart, StyleDefinitionsPart stylePart) {
		// TODO Auto-generated method stub
		UserMessage msg = new UserMessage();
		try {
//			NumberingDefinitionsPart numberingPart = documentPart.getNumberingDefinitionsPart();
			List<Object> docContentList = documentPart.getContent();
			acmStyle = (OOXMLACMStyle) publishingStyle;
			if(AppEnvironment.getInstance().getNewStyleFile()!=null){
				acmStyle.loadStyleFromFile(AppEnvironment.getInstance().getNewStyleFile());
			} else if(AppEnvironment.getInstance().getStylePool().
						getStyles().get(AcademicFormatStructureDefinition.ACM)!=null){
				acmStyle.setStyleMap(AppEnvironment.getInstance().getStylePool().
						getStyles().get(AcademicFormatStructureDefinition.ACM).getStyleMap());	
			}
			
			OOXMLConverterACM converter = new OOXMLConverterACM();
			OOXMLIdentifier identifier = new OOXMLIdentifier();
			identifier.init(stylePart);
			
			converter.removeDefaultHeader(documentPart);
			
			boolean isHeadAdapted = false;
			int docContentIndex = 0;
			while(docContentIndex<docContentList.size()){
				Object o = docContentList.get(docContentIndex);
				if(o instanceof P) {
					P p = (P) o;					
					int headingLvl = 0;
					
					if(AppEnvironment.getInstance().getGui().getIsActiveTestMode().isSelected()){
						LogUtils.log(OOXMLConvertTool.getPText(p));
					}
					if(identifier.isKeywordIdentified()){
						if(OOXMLConvertTool.isIncludeSectPr(p)){
							OOXMLConvertTool.removeSectPr(p);
							if(OOXMLConvertTool.isEmpty(p)){
								OOXMLConvertTool.removeNode(docContentList, docContentIndex);
								continue;
							}
						}
						headingLvl = identifier.getHeadingLvl(p, numberingPart, stylePart);
					}
					if(identifier.isTitle(p, stylePart)){
						converter.adaptTitle(documentPart, p);
						converter.appendLayoutPara(documentPart, Context.getWmlObjectFactory().createP(), docContentIndex, 1);
						isHeadAdapted = true;
//					} else if(identifier.isSubtitle(p, stylePart)) {
//						converter.adaptSubtitle(documentPart, p);
//						converter.appendLayoutPara(documentPart, Context.getWmlObjectFactory().createP(), docContentIndex, 1);
					} else if(identifier.isAbstractHeader(p, stylePart)){
						converter.appendLayoutPara(documentPart, p, docContentIndex-1, 1);
						docContentIndex++;
						converter.adaptAbstract(documentPart, p, docContentIndex);
						isHeadAdapted = true;
					} else if(identifier.isKeywordHeader(p, stylePart)){
						converter.adaptKeyWord(documentPart, p, docContentIndex);
						isHeadAdapted = true;
					} else if(identifier.isAcknowledgmentHeader(p, numberingPart, stylePart)){
						converter.adaptAcknowledgment(documentPart, p, docContentIndex);
						isHeadAdapted = true;
					} else if(identifier.isReferenceHeader(p, numberingPart, stylePart)) {
						converter.adaptLiteratureHeader(documentPart, p);
					} else if(headingLvl>0){
						switch (headingLvl) {
						case 1:
							converter.adaptHeading1(p);
							isHeadAdapted = true;
							break;
						case 2:
							converter.adaptHeading2(p);
							isHeadAdapted = true;
							break;
						case 3:
							converter.adaptHeading3(documentPart, p, docContentIndex);
							isHeadAdapted = true;
							break;
						case 4:
							converter.adaptHeading4(documentPart, p, docContentIndex);
							isHeadAdapted = true;
							break;
						default:
							break;
						}
					} else if(identifier.isFigureCaption(p, stylePart, numberingPart)){
						converter.adaptFigureCaption(documentPart, p);
					} else if(identifier.isTableCaption(p, stylePart, numberingPart)){
						converter.adaptTableCaption(documentPart, p);
					} else if(identifier.isAppendix(p, stylePart)){
						converter.adaptAppendixHeader(documentPart, p, docContentIndex);
					} else if(identifier.isAppendixHeaderIdentified() && docContentIndex>documentPart.getContent().size()-2){
						converter.appendLayoutPara(documentPart, p, docContentIndex, 1);
					} else {
						if(p.getPPr()!=null && p.getPPr().getFramePr()!=null){
							OOXMLConvertTool.fixFramePr(p);
						}
						if(isHeadAdapted){
							OOXMLConvertTool.fixFirstLineEmptySpaces(p, true, null);
						}
						isHeadAdapted = false;
						if(identifier.isAcknowledgeIdentified()){
							OOXMLConvertTool.removeSectPr(p);
						} else if(identifier.isTitleIdentified() && !identifier.isAbstractIdentified()){
							OOXMLConvertTool.replaceSectPr(p, acmStyle.getStyleMap().get(AcademicFormatStructureDefinition.PAGEFORMAT));
							OOXMLConvertTool.setAlignmentCenter(p);// fix p alignment to center
							OOXMLConvertTool.removeInd(p);
						}
						
						if(identifier.isReferencesHeaderIdentified() && !identifier.isAppendixHeaderIdentified()){
							adaptLiterature(documentPart, p);
						} else {
							OOXMLConvertTool.setPStyle(p, acmStyle.getStyleMap().get(AcademicFormatStructureDefinition.NORMAL).getStyleId());
						}
					}
				} else if(o instanceof SdtBlock){	// generated bibliography
					docContentIndex += OOXMLConvertTool.adaptGeneratedReferences((SdtBlock)o, acmStyle.getStyleMap().get(AcademicFormatStructureDefinition.BIBLIOGRAPHYHEADER).getStyleId(), acmStyle.getStyleMap().get(AcademicFormatStructureDefinition.BIBLIOGRAPHY).getStyleId(), docContentIndex, docContentList) - 1;
				} else if(o instanceof Tbl){
					Tbl tbl = (Tbl)o;
				}
				docContentIndex++;
				AppEnvironment.getInstance().getGui().getProgressBar().setValue(docContentIndex*100/(6+docContentList.size()));
			}
			
			converter.adaptFootNote(convertedFile, wordMLPackage);
			AppEnvironment.getInstance().getGui().getProgressBar().setValue((++docContentIndex)*100/(6+docContentList.size()));
			adaptImages(documentPart);
			AppEnvironment.getInstance().getGui().getProgressBar().setValue((++docContentIndex)*100/(6+docContentList.size()));
			converter.adaptFontStyleClass(convertedFile, wordMLPackage, documentPart, stylePart);
			AppEnvironment.getInstance().getGui().getProgressBar().setValue((++docContentIndex)*100/(6+docContentList.size()));
			converter.adaptPageLayout(documentPart, wordMLPackage);
			AppEnvironment.getInstance().getGui().getProgressBar().setValue((++docContentIndex)*100/(6+docContentList.size()));
			adaptNumberFile(documentPart);
			AppEnvironment.getInstance().getGui().getProgressBar().setValue((++docContentIndex)*100/(6+docContentList.size()));
			adaptTable(documentPart);
			AppEnvironment.getInstance().getGui().getProgressBar().setValue((++docContentIndex)*100/(6+docContentList.size()));
			
			saveChange(convertedFile, wordMLPackage);
		} catch (Exception e) {
			// TODO: handle exception
			msg.setMessageDetails(e.getMessage());
			LogUtils.log(msg.getMessage());
		}
		
		msg.setMessageCode(UserMessage.MESSAGE_GENERAL_OK);
		LogUtils.log(msg.getMessage());
		
		return msg;
	}
	
}
