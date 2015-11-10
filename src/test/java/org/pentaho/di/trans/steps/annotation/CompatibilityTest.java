/*!
 * PENTAHO CORPORATION PROPRIETARY AND CONFIDENTIAL
 *
 * Copyright 2002 - 2014 Pentaho Corporation (Pentaho). All rights reserved.
 *
 * NOTICE: All information including source code contained herein is, and
 * remains the sole property of Pentaho and its licensors. The intellectual
 * and technical concepts contained herein are proprietary and confidential
 * to, and are trade secrets of Pentaho and may be covered by U.S. and foreign
 * patents, or patents in process, and are protected by trade secret and
 * copyright laws. The receipt or possession of this source code and/or related
 * information does not convey or imply any rights to reproduce, disclose or
 * distribute its contents, or to manufacture, use, or sell anything that it
 * may describe, in whole or in part. Any reproduction, modification, distribution,
 * or public display of this information without the express written authorization
 * from Pentaho is strictly prohibited and in violation of applicable laws and
 * international treaties. Access to the source code contained herein is strictly
 * prohibited to anyone except those individuals and entities who have executed
 * confidentiality and non-disclosure agreements or other agreements with Pentaho,
 * explicitly covering such access.
 */
package org.pentaho.di.trans.steps.annotation;


import org.junit.Before;
import org.junit.Test;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.agilebi.modeler.models.annotations.util.XMLUtil;
import org.pentaho.metadata.model.concept.types.AggregationType;
import org.pentaho.metastore.api.IMetaStore;

import java.io.Serializable;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class CompatibilityTest {

  private ModelAnnotationMeta modelAnnotationMeta;
  private IMetaStore metaStore;

  @Before
  public void init() throws Exception {
    modelAnnotationMeta = spy( new ModelAnnotationMeta() );
    doNothing().when( modelAnnotationMeta ).logError( anyString() );

    metaStore = mock( IMetaStore.class );
  }


  @Test
  public void testLoadXml() throws Exception {
    modelAnnotationMeta.loadXML( XMLUtil.asDOMNode( getTransWithLocalAnnotations() ), null, metaStore );

    ModelAnnotationGroup modelAnnotationGroup = modelAnnotationMeta.getModelAnnotations();
    assertTrue( modelAnnotationGroup.size() == 3 );

    ModelAnnotation m1 = modelAnnotationGroup.get( 0 );
    assertTrue( m1.getType() == ModelAnnotation.Type.CREATE_ATTRIBUTE );
    assertTrue( m1.getField().equals( "county" ) );

    Map<String, Serializable> p1 = m1.describeAnnotation();
    assertEquals( 5, p1.size() );
    assertEquals( "county", p1.get( "field" ) );
    assertEquals( "Geography", p1.get( "dimension" ) );
    assertEquals( false, p1.get( "unique" ) );
    assertEquals( "County", p1.get( "name" ) );
    assertEquals( "Geography", p1.get( "hierarchy" ) );

    ModelAnnotation m2 = modelAnnotationGroup.get( 1 );
    assertTrue( m2.getType() == ModelAnnotation.Type.CREATE_ATTRIBUTE );
    assertTrue( m2.getField().equals( "city" ) );

    Map<String, Serializable> p2 = m2.describeAnnotation();
    assertEquals( 6, p2.size() );
    assertEquals( "city", p2.get( "field" ) );
    assertEquals( "Geography", p2.get( "dimension" ) );
    assertEquals( false, p2.get( "unique" ) );
    assertEquals( "City", p2.get( "name" ) );
    assertEquals( "Geography", p2.get( "hierarchy" ) );
    assertEquals( "County", p2.get( "parentAttribute" ) );

    ModelAnnotation m3 = modelAnnotationGroup.get( 2 );
    assertTrue( m3.getType() == ModelAnnotation.Type.CREATE_ATTRIBUTE );
    assertTrue( m3.getField().equals( "state" ) );

    Map<String, Serializable> p3 = m3.describeAnnotation();
    assertEquals( 6, p3.size() );
    assertEquals( "state", p3.get( "field" ) );
    assertEquals( "Geography", p3.get( "dimension" ) );
    assertEquals( false, p3.get( "unique" ) );
    assertEquals( "State", p3.get( "name" ) );
    assertEquals( "Geography", p3.get( "hierarchy" ) );
    assertEquals( "City", p3.get( "parentAttribute" ) );
  }

  @Test
  public void testLoadXmlWithMeasureAndLinkDimension() throws Exception {
    modelAnnotationMeta.loadXML( XMLUtil.asDOMNode( getStepXmlWithMeasureAndLinkDimension() ), null, metaStore );

    ModelAnnotationGroup modelAnnotationGroup = modelAnnotationMeta.getModelAnnotations();
    assertTrue( modelAnnotationGroup.size() == 3 );
    assertEquals( "Category 2", modelAnnotationMeta.getModelAnnotationCategory() );

    ModelAnnotation m1 = modelAnnotationGroup.get( 0 );
    assertTrue( m1.getType() == ModelAnnotation.Type.CREATE_MEASURE );
    assertTrue( m1.getField().equals( "col1" ) );

    Map<String, Serializable> p1 = m1.describeAnnotation();
    assertEquals( 4, p1.size() );
    assertEquals( "col1", p1.get( "field" ) );
    assertEquals( "xxxx", p1.get( "formatString" ) );
    assertEquals( "some description", p1.get( "description" ) );
    assertEquals( AggregationType.SUM, p1.get( "aggregateType" ) );

    // skip the 2nd one

    ModelAnnotation m2 = modelAnnotationGroup.get( 2 );
    assertTrue( m2.getType() == ModelAnnotation.Type.LINK_DIMENSION );
    assertTrue( m2.getField().equals( "ld" ) );

    Map<String, Serializable> p2 = m2.describeAnnotation();
    assertEquals( 3, p2.size() );
    assertEquals( "ld", p2.get( "field" ) );
    assertEquals( "sharedDimension", p2.get( "sharedDimension" ) );
    assertEquals( "ldName", p2.get( "name" ) );
  }

  private String getStepXmlWithMeasureAndLinkDimension() {
    return "<step>"
      + "<category>Category 2</category>"
      + "<targetOutputStep/>"
      + "<annotations>"
      + "<annotation>"
      + "<name>myName</name>"
      + "<field>col1</field>"
      + "<type>CREATE_MEASURE</type>"
      + "<properties>"
      + "<property><name>formatString</name><value>xxxx</value></property>"
      + "<property><name>description</name><value>some description</value></property>"
      + "<property><name>aggregateType</name><value>SUM</value></property></properties>"
      + "</annotation>"
      + "<annotation>"
      + "<name>myName</name>"
      + "<field>col1</field><type>CREATE_MEASURE</type>"
      + "<properties>"
      + "<property><name>formatString</name><value>xxxx</value></property>"
      + "<property><name>description</name><value>some description</value></property>"
      + "<property><name>aggregateType</name><value>SUM</value></property>"
      + "</properties>"
      + "</annotation>"
      + "<annotation>"
      + "<name>ld</name>"
      + "<field>ld</field><type>LINK_DIMENSION</type>"
      + "<properties><property><name>sharedDimension</name><value>sharedDimension</value></property>"
      + "<property><name>name</name><value>ldName</value></property></properties></annotation>"
      + "<sharedDimension>N</sharedDimension>"
      + "</annotations>"
      + "</step>";
  }

  private String getTransWithLocalAnnotations() {
    return ""
      + "<step>\n"
      + "  <name>Annotate One</name>\n"
      + "  <type>FieldMetadataAnnotation</type>\n"
      + "  <description/>\n"
      + "  <distribute>Y</distribute>\n"
      + "  <custom_distribution/>\n"
      + "  <copies>1</copies>\n"
      + "  <partitioning>\n"
      + "    <method>none</method>\n"
      + "    <schema_name/>\n"
      + "  </partitioning>\n"
      + "  <annotations>\n"
      + "    <annotation>\n"
      + "      <field>county</field>\n"
      + "      <type>CREATE_ATTRIBUTE</type>\n"
      + "      <properties>\n"
      + "        <property>\n"
      + "          <name>dimension</name>\n"
      + "          <value>Geography</value>\n"
      + "        </property>\n"
      + "        <property>\n"
      + "          <name>unique</name>\n"
      + "          <value>false</value>\n"
      + "        </property>\n"
      + "        <property>\n"
      + "          <name>name</name>\n"
      + "          <value>County</value>\n"
      + "        </property>\n"
      + "        <property>\n"
      + "          <name>hierarchy</name>\n"
      + "          <value>Geography</value>\n"
      + "        </property>\n"
      + "      </properties>\n"
      + "    </annotation>\n"
      + "    <annotation>\n"
      + "      <field>city</field>\n"
      + "      <type>CREATE_ATTRIBUTE</type>\n"
      + "      <properties>\n"
      + "        <property>\n"
      + "          <name>dimension</name>\n"
      + "          <value>Geography</value>\n"
      + "        </property>\n"
      + "        <property>\n"
      + "          <name>unique</name>\n"
      + "          <value>false</value>\n"
      + "        </property>\n"
      + "        <property>\n"
      + "          <name>parentAttribute</name>\n"
      + "          <value>County</value>\n"
      + "        </property>\n"
      + "        <property>\n"
      + "          <name>name</name>\n"
      + "          <value>City</value>\n"
      + "        </property>\n"
      + "        <property>\n"
      + "          <name>hierarchy</name>\n"
      + "          <value>Geography</value>\n"
      + "        </property>\n"
      + "      </properties>\n"
      + "    </annotation>\n"
      + "    <annotation>\n"
      + "      <field>state</field>\n"
      + "      <type>CREATE_ATTRIBUTE</type>\n"
      + "      <properties>\n"
      + "        <property>\n"
      + "          <name>dimension</name>\n"
      + "          <value>Geography</value>\n"
      + "        </property>\n"
      + "        <property>\n"
      + "          <name>unique</name>\n"
      + "          <value>false</value>\n"
      + "        </property>\n"
      + "        <property>\n"
      + "          <name>parentAttribute</name>\n"
      + "          <value>City</value>\n"
      + "        </property>\n"
      + "        <property>\n"
      + "          <name>name</name>\n"
      + "          <value>State</value>\n"
      + "        </property>\n"
      + "        <property>\n"
      + "          <name>hierarchy</name>\n"
      + "          <value>Geography</value>\n"
      + "        </property>\n"
      + "      </properties>\n"
      + "    </annotation>\n"
      + "  </annotations>\n"
      + "  <cluster_schema/>\n"
      + "  <remotesteps>\n"
      + "    <input/>\n"
      + "    <output/>\n"
      + "  </remotesteps>\n"
      + "  <GUI>\n"
      + "    <xloc>320</xloc>\n"
      + "    <yloc>192</yloc>\n"
      + "    <draw>Y</draw>\n"
      + "  </GUI>\n"
      + "</step>";
  }
}
