package com.test.up_soft;

import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ParsingXMLElements extends DefaultHandler 
{
	private Map<String, String> element;	//存储XML元素键值
    private String elementName;				//前一个元素名称
    
    public Map<String, String> getElement()
    {
    	return element;
    }
    
    public void startDocument() throws SAXException
    {
    	System.out.println("*******开始解析文档*******");
    	element = new HashMap<String, String>();
    }

    public void endDocument() throws SAXException
    { 
    	System.out.println("*******文档解析结束*******");
    }

    public void startPrefixMapping( String prefix, String uri )
    {
    	System.out.println(" 前缀映射: " + prefix +" 开始!"+ " 它的URI是:" + uri);
    }

    public void endPrefixMapping( String prefix )
    {
    	System.out.println(" 前缀映射: " + prefix + " 结束!");
    }

    // public void processingInstruction( String target, String instruction )throwsSAXException{}

    // public void ignorableWhitespace( char[] chars, int start, int length ) throwsSAXException {}

    // public void skippedEntity( String name ) throws SAXException {}

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
    {
    	System.out.println("*******开始解析" + localName + "元素*******"); 
    	elementName = localName;
    }

    public void endElement(String namespaceURI,String localName,String fullName )throws SAXException
    {
	    System.out.println("******" + localName + "元素解析结束********");
    }

    public void characters( char[] chars, int start, int length )throws SAXException
    {
    	//将元素内容加到Map中
    	if(null != elementName)
    	{
    		element.put(elementName, String.valueOf(chars, start, length).replace("\\r", "\r").replace("\\n", "\n"));
    		elementName = null;
    		System.out.println("元素值：" + element.get(elementName));
    	}
    }
}