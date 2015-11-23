/*! ******************************************************************************
 *
 * Pentaho Community Edition Project: data-refinery-pdi-plugin
 *
 * Copyright (C) 2002-2015 by Pentaho : http://www.pentaho.com
 *
 * *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ********************************************************************************/

package org.pentaho.di.core.refinery.model;

import org.pentaho.di.core.ProvidesDatabaseConnectionInformation;
import org.pentaho.di.core.database.Database;
import org.pentaho.di.core.exception.KettleDatabaseException;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.job.entries.build.JobEntryBuildModel;
import org.pentaho.di.job.entry.JobEntryBase;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.pentaho.di.core.row.ValueMetaInterface.*;
import static org.pentaho.di.i18n.BaseMessages.getString;

public class AnalysisModeler {
  private static final Class<?> PKG = JobEntryBuildModel.class;
  private JobEntryBase jobEntry;
  private ProvidesDatabaseConnectionInformation connectionInfo;

  private static Map<String, List<Integer>> analysisTypeMapping = new HashMap<String, List<Integer>>();

  static {
    analysisTypeMapping.put( "String", asList( TYPE_STRING ) );
    analysisTypeMapping.put( "Numeric", asList( TYPE_BIGNUMBER, TYPE_INTEGER, TYPE_NUMBER ) );
    analysisTypeMapping.put( "Boolean", asList( TYPE_BOOLEAN ) );
    analysisTypeMapping.put( "Date", asList( TYPE_DATE ) );
    analysisTypeMapping.put( "Time", asList( TYPE_DATE, TYPE_TIMESTAMP ) );
    analysisTypeMapping.put( "Timestamp", asList( TYPE_TIMESTAMP ) );
  }

  public AnalysisModeler(
      final JobEntryBase jobEntry, final ProvidesDatabaseConnectionInformation connectionInfo ) {
    this.jobEntry = jobEntry;
    this.connectionInfo = connectionInfo;
  }

  public String replaceTableAndSchemaNames( final String schema, final String modelName ) throws KettleException {
    try {
      validateSDRSchema( schema );
      return transformSchema( schema, modelName, getTablename() );
    } catch ( TransformerConfigurationException e ) {
      throw new KettleException( e );
    } catch ( TransformerException e ) {
      throw new KettleException( e );
    }
  }

  private String transformSchema( final String schema, final String modelName, final String tableName )
    throws TransformerException {
    TransformerFactory factory = TransformerFactory.newInstance();
    Source xslt = new StreamSource( new StringReader( constructXsltForSwap( modelName, tableName ) ) );
    Transformer transformer = factory.newTransformer( xslt );
    Source text = new StreamSource( new StringReader( schema ) );
    StringWriter writer = new StringWriter();
    transformer.transform( text, new StreamResult( writer ) );
    return writer.toString();
  }

  private String constructXsltForSwap( final String modelName, final String tableName ) {
    return "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" "
      + "xmlns:fn=\"http://www.w3.org/2005/xpath-functions\">"
      + "<xsl:template match=\"@*|node()\">\n"
      + "<xsl:copy>\n"
      + "    <xsl:apply-templates select=\"@*|node()\"/>\n"
      + "  </xsl:copy>"
      + "</xsl:template>"
      + "<xsl:template match=\"Table/@name\">"
      + "    <xsl:attribute name=\"name\">" + tableName + "</xsl:attribute>"
      + "</xsl:template>"
      + "<xsl:template match=\"Schema/@name\">"
      + "    <xsl:attribute name=\"name\">" + modelName + "</xsl:attribute>"
      + "</xsl:template>"
      + "<xsl:template match=\"Cube/@name\">"
      + "    <xsl:attribute name=\"name\">" + modelName + "</xsl:attribute>"
      + "</xsl:template>"
      + "</xsl:stylesheet>";
  }

  private void validateSDRSchema( final String schema ) throws KettleException {
    try {
      validateSingleTable( schema );
      validateSingleCube( schema );
      validateColumns( schema );
    } catch ( XPathException e ) {
      throw new KettleException( e );
    }
  }

  private void validateColumns( final String schema ) throws XPathExpressionException, KettleException {
    ArrayList<String> notFound = new ArrayList<String>();
    ArrayList<String> incompatibleTypes = new ArrayList<String>();
    List<ValueMetaInterface> fieldsInDatabase = getFieldsInDatabase();
    NodeList levelColumnAttributes = getNodeList( schema, "//Level | //Measure" );
    for ( int i = 0; i < levelColumnAttributes.getLength(); i++ ) {
      Node node = levelColumnAttributes.item( i );
      Node schemaColumnNode = node.getAttributes().getNamedItem( "column" );
      String schemaColumn = schemaColumnNode.getTextContent();
      boolean foundColumn = false;
      for ( ValueMetaInterface valueMetaInterface : fieldsInDatabase ) {
        if (  valueMetaInterface.getName().equals( schemaColumn ) ) {
          foundColumn = true;
          Node type = node.getAttributes().getNamedItem( "type" );
          if ( type != null ) {
            List<Integer> validTypes = analysisTypeMapping.get( type.getTextContent() );
            if ( !validTypes.contains( valueMetaInterface.getType() ) ) {
              incompatibleTypes.add( schemaColumnNode.getTextContent() );
            }
          }
        }
      }
      if ( !foundColumn ) {
        notFound.add( schemaColumnNode.getTextContent() );
      }
    }
    if ( !notFound.isEmpty()  || !incompatibleTypes.isEmpty() ) {
      throw new KettleException(
        getString( PKG, "AnalysisModeler.ColumnValidation",
          msgIfNotEmpty( notFound, "AnalysisModeler.SelectModelColumnNotFound" ),
          msgIfNotEmpty( incompatibleTypes, "AnalysisModeler.SelectModelColumnTypeMismatch" ) ) );
    }
  }

  private String msgIfNotEmpty( final ArrayList<String> notFound, final String msgKey ) {
    if ( notFound.isEmpty() ) {
      return "";
    }
    return getString( PKG, msgKey, notFound.toString() );
  }

  private void validateSingleTable( final String schema ) throws XPathExpressionException, KettleException {
    NodeList tableNameAttributes = getNodeList( schema, "//Table/@name" );
    if ( !modelTablesAreSupported( tableNameAttributes ) ) {
      throw new KettleException( getString( PKG, "AnalysisModeler.SelectModelErrorMultipleTables" ) );
    }
  }

  private void validateSingleCube( final String schema ) throws XPathExpressionException, KettleException {
    NodeList tableNameAttributes = getNodeList( schema, "//Cube" );
    if ( tableNameAttributes.getLength() != 1 ) {
      throw new KettleException( getString( PKG, "AnalysisModeler.SelectModelErrorMultipleCubes" ) );
    }
  }

  List<ValueMetaInterface> getFieldsInDatabase() throws KettleDatabaseException {
    Database database = new Database( jobEntry, connectionInfo.getDatabaseMeta() );
    try {
      database.connect();
      RowMetaInterface tableFields = database.getTableFields( getTablename() );
      return tableFields.getValueMetaList();
    } finally {
      database.disconnect();
    }
  }

  private String getTablename() {
    return jobEntry.environmentSubstitute( connectionInfo.getTableName() );
  }

  private NodeList getNodeList( final String schema, final String xPathExpression ) throws XPathExpressionException {
    XPathFactory factory = XPathFactory.newInstance();
    XPath xpath = factory.newXPath();
    return (NodeList) xpath.compile( xPathExpression )
        .evaluate( new InputSource( new StringReader( schema ) ), XPathConstants.NODESET );
  }

  private boolean modelTablesAreSupported( final NodeList tableNameAttributes ) {
    int length = tableNameAttributes.getLength();
    String uniqueTableName;
    if ( length > 0 ) {
      uniqueTableName = tableNameAttributes.item( 0 ).getTextContent();
    } else {
      return false;
    }
    for ( int i = 1; i < length; i++ ) {
      if ( !tableNameAttributes.item( i ).getTextContent().equals( uniqueTableName ) ) {
        return false;
      }
    }
    return true;
  }
}
