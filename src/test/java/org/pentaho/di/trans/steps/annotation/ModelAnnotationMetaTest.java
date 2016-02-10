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

package org.pentaho.di.trans.steps.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.any;

import org.mockito.Mockito;
import org.pentaho.agilebi.modeler.models.annotations.CreateAttribute;
import org.pentaho.agilebi.modeler.models.annotations.CreateDimensionKey;
import org.pentaho.agilebi.modeler.models.annotations.CreateMeasure;
import org.pentaho.agilebi.modeler.models.annotations.LinkDimension;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroupXmlReader;
import org.pentaho.agilebi.modeler.models.annotations.data.ColumnMapping;
import org.pentaho.agilebi.modeler.models.annotations.data.DataProvider;

import static org.mockito.Mockito.*;
import static org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation.Type.*;
import static org.pentaho.agilebi.modeler.models.annotations.util.XMLUtil.asDOMNode;
import static org.pentaho.agilebi.modeler.models.annotations.util.XMLUtil.compactPrint;

import org.pentaho.di.core.KettleClientEnvironment;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.StringObjectId;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepPartitioningMeta;
import org.pentaho.di.trans.steps.databaselookup.DatabaseLookupMeta;
import org.pentaho.metadata.model.concept.types.AggregationType;
import org.pentaho.metadata.model.concept.types.DataType;
import org.pentaho.metastore.api.IMetaStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Rowell Belen
 */
public class ModelAnnotationMetaTest {

  private StepMeta stepMeta;
  private StepDataInterface stepDataInterface;
  private TransMeta transMeta;
  private Trans trans;
  private StepPartitioningMeta stepPartitioningMeta;
  private ModelAnnotationMeta modelAnnotationMeta;
  private IMetaStore metaStore;

  @Before
  public void init() throws Exception {
    modelAnnotationMeta = spy( new ModelAnnotationMeta() );
    doNothing().when( modelAnnotationMeta ).logError( anyString() );

    metaStore = mock( IMetaStore.class );
    stepMeta = mock( StepMeta.class );
    stepDataInterface = mock( StepDataInterface.class );
    transMeta = mock( TransMeta.class );
    trans = mock( Trans.class );
    stepPartitioningMeta = mock( StepPartitioningMeta.class );
    if ( !KettleClientEnvironment.isInitialized() ) {
      KettleClientEnvironment.init();
    }
  }

  @Test
  public void testGetStepData() {
    assertTrue( modelAnnotationMeta.getStepData() instanceof ModelAnnotationData );
  }

  @Test
  public void testGetStep() {
    when( stepMeta.getName() ).thenReturn( "model annotation step" );
    when( stepMeta.getTargetStepPartitioningMeta() ).thenReturn( stepPartitioningMeta );
    when( transMeta.findStep( anyString() ) ).thenReturn( stepMeta );
    assertTrue(
      modelAnnotationMeta
        .getStep( stepMeta, stepDataInterface, 0, transMeta, trans ) instanceof ModelAnnotationStep );
  }

  @Test
  public void testGetXmlNoFields() throws Exception {
    modelAnnotationMeta.setDefault();
    String xml = modelAnnotationMeta.getXML();
    xml = XMLHandler.openTag( "step" ) + xml + XMLHandler.closeTag( "step" );
    assertEquals( compactPrint( xml ),
      "<step><category/><targetOutputStep/><annotations><sharedDimension>N</sharedDimension>"
        + "<description/></annotations></step>" );
  }

  @Test
  public void testNotifications() throws Exception {
    DatabaseLookupMeta dlm = new DatabaseLookupMeta();
    StepMeta sm1 = new StepMeta( "dlm", dlm );
    TransMeta trm = new TransMeta();
    trm.addStep( sm1 );
    ModelAnnotationMeta mam = new ModelAnnotationMeta();
    mam.setTargetOutputStep( "dlm" );
    StepMeta sm2 = new StepMeta( "mam", mam );
    trm.addStep( sm2 );

    StepMeta sm1Clone = new StepMeta( "dlm11", dlm );
    sm1.setChanged( true );
    trm.setChanged( true );
    trm.addOrReplaceStep( sm1Clone );
    trm.notifyAllListeners( sm1, sm1Clone );

    assertEquals( mam.getTargetOutputStep(), "dlm11" );
  }

  @Test
  public void testGetXmlWithAnnotations() throws Exception {
    // mock data
    ModelAnnotationGroup modelAnnotationGroup = new ModelAnnotationGroup();
    ModelAnnotation<CreateMeasure>
      measure = new ModelAnnotation<CreateMeasure>();
    measure.setName( "myName" );
    CreateMeasure m = new CreateMeasure();
    m.setFormatString( "xxxx" );
    m.setAggregateType( AggregationType.SUM );
    m.setDescription( "some description" );
    m.setField( "col1" );
    measure.setAnnotation( m );
    modelAnnotationGroup.add( measure );
    modelAnnotationGroup.add( measure );

    LinkDimension linkDimension = new LinkDimension();
    linkDimension.setName( "ldName" );
    linkDimension.setSharedDimension( "sharedDimension" );
    linkDimension.setField( "ld" );
    ModelAnnotation ldAnnotation = new ModelAnnotation( linkDimension );
    ldAnnotation.setName( "ld" );
    modelAnnotationGroup.add( ldAnnotation );

    modelAnnotationGroup.setDescription( "Test Description" );

    modelAnnotationMeta.setModelAnnotations( modelAnnotationGroup );
    modelAnnotationMeta.setModelAnnotationCategory( "Category 2" );

    String xml = modelAnnotationMeta.getXML();
    xml = XMLHandler.openTag( "step" ) + xml + XMLHandler.closeTag( "step" );
    assertEquals( compactPrint( xml ), "<step>"
      + "<category>Category 2</category>"
      + "<targetOutputStep/>"
      + "<annotations>"
      + "<annotation>"
      + "<name>myName</name>"
      + "<field>col1</field>"
      + "<type>CREATE_MEASURE</type>"
      + "<properties>"
      + "<property><name>formatString</name><value><![CDATA[xxxx]]></value></property>"
      + "<property><name>hidden</name><value><![CDATA[false]]></value></property>"
      + "<property><name>description</name><value><![CDATA[some description]]></value></property>"
      + "<property><name>aggregateType</name><value><![CDATA[SUM]]></value></property></properties>"
      + "</annotation>"
      + "<annotation>"
      + "<name>myName</name>"
      + "<field>col1</field><type>CREATE_MEASURE</type>"
      + "<properties>"
      + "<property><name>formatString</name><value><![CDATA[xxxx]]></value></property>"
      + "<property><name>hidden</name><value><![CDATA[false]]></value></property>"
      + "<property><name>description</name><value><![CDATA[some description]]></value></property>"
      + "<property><name>aggregateType</name><value><![CDATA[SUM]]></value></property>"
      + "</properties>"
      + "</annotation>"
      + "<annotation>"
      + "<name>ld</name>"
      + "<field>ld</field><type>LINK_DIMENSION</type>"
      + "<properties><property><name>sharedDimension</name><value><![CDATA[sharedDimension]]></value></property>"
      + "<property><name>name</name><value><![CDATA[ldName]]></value></property></properties></annotation>"
      + "<sharedDimension>N</sharedDimension>"
      + "<description>Test Description</description>"
      + "</annotations>"
      + "</step>" );
  }

  @Test
  public void testGetXmlWithDataProviders() throws Exception {
    // mock data
    ModelAnnotationGroup modelAnnotationGroup = new ModelAnnotationGroup();
    modelAnnotationGroup.setSharedDimension( true );

    DataProvider dp1 = new DataProvider();
    dp1.setName( "dp1Name" );

    DataProvider dp2 = new DataProvider();
    dp2.setName( "dp2Name" );

    ColumnMapping cm1 = new ColumnMapping();
    cm1.setName( "cm1name" );
    cm1.setColumnDataType( DataType.BOOLEAN );
    ColumnMapping cm2 = new ColumnMapping();
    cm2.setName( "cm2name" );
    cm2.setColumnDataType( DataType.DATE );
    dp2.setColumnMappings( Arrays.asList( new ColumnMapping[] { cm1, cm2 } ) );

    List<DataProvider> dataProviders = new ArrayList<DataProvider>();
    dataProviders.add( dp1 );
    dataProviders.add( dp2 );

    modelAnnotationGroup.setDataProviders( dataProviders );
    modelAnnotationMeta.setModelAnnotations( modelAnnotationGroup );

    String xml = modelAnnotationMeta.getXML();
    xml = XMLHandler.openTag( "step" ) + xml + XMLHandler.closeTag( "step" );
    assertEquals( compactPrint( ""
      + "<step>"
      + "  <category />"
      + "  <targetOutputStep/>"
      + "  <annotations>"
      + "    <sharedDimension>Y</sharedDimension>"
      + "    <description/>"
      + "    <data-providers>"
      + "      <data-provider>"
      + "        <name>dp1Name</name>"
      + "        <schemaName />"
      + "        <tableName />"
      + "        <databaseMetaRef />"
      + "      </data-provider>"
      + "      <data-provider>"
      + "        <name>dp2Name</name>"
      + "        <schemaName />"
      + "        <tableName />"
      + "        <databaseMetaRef />"
      + "        <column-mappings>"
      + "          <column-mapping>"
      + "            <name>cm1name</name>"
      + "            <columnName />"
      + "            <dataType>BOOLEAN</dataType>"
      + "          </column-mapping>"
      + "          <column-mapping>"
      + "            <name>cm2name</name>"
      + "            <columnName />"
      + "            <dataType>DATE</dataType>"
      + "          </column-mapping>"
      + "        </column-mappings>"
      + "      </data-provider>"
      + "    </data-providers>"
      + "  </annotations>"
      + "</step>" ), compactPrint( xml ) );
  }

  @Test
  public void testLoadXml() throws Exception {
    modelAnnotationMeta.loadXML( asDOMNode( getSampleXml() ), null, metaStore );

    ModelAnnotationGroup modelAnnotationGroup = modelAnnotationMeta.getModelAnnotations();
    assertTrue( modelAnnotationGroup.size() == 2 );
    assertTrue( modelAnnotationGroup.get( 0 ).getType() == CREATE_MEASURE );
    assertTrue( modelAnnotationGroup.get( 0 ).getAnnotation().getField().equals( "col1" ) );

    assertTrue( modelAnnotationGroup.get( 1 ).getType() == LINK_DIMENSION );
    assertTrue( modelAnnotationGroup.get( 1 ).getAnnotation().getField().equals( "ld" ) );
    assertTrue( modelAnnotationGroup.get( 1 ).getName().equals( "ld" ) );

    assertEquals( "Category 1", modelAnnotationMeta.getModelAnnotationCategory() );

    verify( modelAnnotationMeta, times( 1 ) ).readDataFromMetaStore( any( IMetaStore.class ) );
  }

  @Test
  public void testLoadXmlNoCategory() throws Exception {
    modelAnnotationMeta.loadXML( asDOMNode( getSampleXmlNoFields() ), null, metaStore );
    verify( modelAnnotationMeta, times( 0 ) ).readDataFromMetaStore( any( IMetaStore.class ) ); // not called
  }

  @Test
  public void testLoadXmlWithDataProviders() throws Exception {
    modelAnnotationMeta.loadXML( asDOMNode( getSampleXmlWithDataProviders() ), null, metaStore );

    ModelAnnotationGroup modelAnnotationGroup = modelAnnotationMeta.getModelAnnotations();
    assertTrue( modelAnnotationGroup.size() == 0 );
    assertTrue( modelAnnotationGroup.getDataProviders().size() == 2 );
    assertTrue( modelAnnotationGroup.getDataProviders().get( 0 ).getColumnMappings().size() == 0 );
    assertTrue( modelAnnotationGroup.getDataProviders().get( 1 ).getColumnMappings().size() == 2 );
  }

  @Test( expected = KettleException.class )
  public void testLoadXmlException() throws Exception {

    doThrow( new RuntimeException() ).when( modelAnnotationMeta )
      .setModelAnnotations( any( ModelAnnotationGroup.class ) );

    modelAnnotationMeta.loadXML( null, null, metaStore );

  }

  @Test
  public void testCreateModelAnnotation() throws Exception {

    try {

      ModelAnnotationGroupXmlReader.create( null, "" );
    } catch ( Exception e ) {
      assertNotNull( e );
    }

    try {
      ModelAnnotationGroupXmlReader.create( "MEASURE", "" );
    } catch ( Exception e ) {
      assertNotNull( e );
    }

    ModelAnnotation<?> modelAnnotation =
        ModelAnnotationGroupXmlReader.create( "CREATE_ATTRIBUTE", "" );
    assertTrue( modelAnnotation.getAnnotation() instanceof CreateAttribute );

    modelAnnotation =
        ModelAnnotationGroupXmlReader.create( "CREATE_MEASURE", "" );
    assertTrue( modelAnnotation.getAnnotation() instanceof CreateMeasure );
  }

  @Test
  public void testSaveLoadXmlWithCreateDimKey() throws Exception {

    CreateDimensionKey createDimKey = new CreateDimensionKey();
    createDimKey.setName( "1" );
    createDimKey.setDimension( "d" );
    createDimKey.setField( "1" );
    ModelAnnotation<CreateDimensionKey> dimKeyAnno = new ModelAnnotation<CreateDimensionKey>( createDimKey );
    ModelAnnotationGroup mg = new ModelAnnotationGroup( dimKeyAnno );

    ModelAnnotationMeta modelAnnotationMeta = new ModelAnnotationMeta();
    modelAnnotationMeta.setModelAnnotations( mg );

    final String xmlizedStep =
      "<step>" + "  <name>Model Annotation Step</name>" + "  <type>FieldMetadataAnnotation</type>"
        + modelAnnotationMeta.getXML() + "</step>";

    ModelAnnotationMeta loadedMeta = new ModelAnnotationMeta();
    loadedMeta.loadXML( asDOMNode( xmlizedStep ), null, metaStore );

    ModelAnnotationGroup loadedMg = loadedMeta.getModelAnnotations();

    CreateDimensionKey loadedCreateDimKey = (CreateDimensionKey) loadedMg.get( 0 ).getAnnotation();
    assertEquals( createDimKey.getName(), loadedCreateDimKey.getName() );
    assertEquals( createDimKey.getDimension(), loadedCreateDimKey.getDimension() );
  }

  @Test
  public void testSaveAnnotationsToRep() throws Exception {
    CreateAttribute createAttribute = new CreateAttribute();
    createAttribute.setField( "GenderField" );
    createAttribute.setHierarchy( "Gender" );
    createAttribute.setName( "Gender" );

    CreateMeasure createMeasure = new CreateMeasure();
    createMeasure.setName( "Total Sales" );
    createMeasure.setField( "sales" );
    createMeasure.setAggregateType( AggregationType.SUM );

    ModelAnnotation<CreateAttribute> attributeAnnotation = new ModelAnnotation<>( createAttribute );
    attributeAnnotation.setName( "12345" );
    ModelAnnotation<CreateMeasure> measureAnnotation = new ModelAnnotation<>( createMeasure );
    measureAnnotation.setName( "54321" );
    ModelAnnotationGroup modelAnnotations = new ModelAnnotationGroup( attributeAnnotation, measureAnnotation );
    modelAnnotations.setDescription( "aDescription" );
    ModelAnnotationMeta meta = new ModelAnnotationMeta();
    meta.setSharedDimension( false );
    meta.setModelAnnotations( modelAnnotations );

    Repository rep = Mockito.mock( Repository.class );
    StringObjectId transId = new StringObjectId( "transId" );
    StringObjectId stepId = new StringObjectId( "stepId" );
    meta.saveRep( rep, metaStore, transId, stepId );

    verify( rep ).saveStepAttribute( transId, stepId, "CATEGORY_NAME", null );
    verify( rep ).saveStepAttribute( transId, stepId, "TARGET_OUTPUT_STEP", null );

    verify( rep ).saveStepAttribute( transId, stepId, 0, "ANNOTATION_NAME", "12345" );
    verify( rep ).saveStepAttribute( transId, stepId, 0, "ANNOTATION_FIELD_NAME", "GenderField" );
    verify( rep ).saveStepAttribute( transId, stepId, 0, "ANNOTATION_TYPE", CREATE_ATTRIBUTE.toString() );
    verify( rep ).saveStepAttribute( transId, stepId, 0, "PROPERTY_VALUE_field", "GenderField" );
    verify( rep ).saveStepAttribute( transId, stepId, 0, "PROPERTY_VALUE_unique", "false" );
    verify( rep ).saveStepAttribute( transId, stepId, 0, "PROPERTY_VALUE_hierarchy", "Gender" );
    verify( rep ).saveStepAttribute( transId, stepId, 0, "PROPERTY_VALUE_name", "Gender" );
    verify( rep ).saveStepAttribute( transId, stepId, 0, "PROPERTY_VALUE_hidden", "false" );

    verify( rep ).saveStepAttribute( transId, stepId, 1, "ANNOTATION_NAME", "54321" );
    verify( rep ).saveStepAttribute( transId, stepId, 1, "ANNOTATION_FIELD_NAME", "sales" );
    verify( rep ).saveStepAttribute( transId, stepId, 1, "ANNOTATION_TYPE", CREATE_MEASURE.toString() );
    verify( rep ).saveStepAttribute( transId, stepId, 1, "PROPERTY_VALUE_field", "sales" );
    verify( rep ).saveStepAttribute( transId, stepId, 1, "PROPERTY_VALUE_name", "Total Sales" );
    verify( rep ).saveStepAttribute( transId, stepId, 1, "PROPERTY_VALUE_aggregateType", "SUM" );
    verify( rep ).saveStepAttribute( transId, stepId, 1, "PROPERTY_VALUE_hidden", "false" );

    verify( rep ).saveStepAttribute( transId, stepId, "SHARED_DIMENSION", false );
    verify( rep ).saveStepAttribute( transId, stepId, "DESCRIPTION", "aDescription" );
    verifyNoMoreInteractions( rep );
  }

  @Test
  public void testSaveSharedDimensionToRep() throws Exception {
    CreateAttribute createAttribute = new CreateAttribute();
    createAttribute.setField( "GenderField" );
    createAttribute.setHierarchy( "Gender" );
    createAttribute.setName( "Gender" );

    ModelAnnotation<CreateAttribute> attributeAnnotation = new ModelAnnotation<>( createAttribute );
    attributeAnnotation.setName( "12345" );
    ModelAnnotationGroup modelAnnotations = new ModelAnnotationGroup( attributeAnnotation );
    modelAnnotations.setDescription( "aDescription" );
    modelAnnotations.setSharedDimension( true );
    DataProvider dataProvider = new DataProvider();
    dataProvider.setName( "sample" );
    dataProvider.setTableName( "salesTable" );
    dataProvider.setSchemaName( "sampleSchema" );
    dataProvider.setDatabaseMetaNameRef( "ref" );
    ColumnMapping columnMapping = new ColumnMapping();
    columnMapping.setColumnName( "sex" );
    columnMapping.setName( "Gender" );
    columnMapping.setColumnDataType( DataType.STRING );
    dataProvider.setColumnMappings( Collections.singletonList( columnMapping ) );
    modelAnnotations.setDataProviders( Collections.singletonList( dataProvider ) );
    ModelAnnotationMeta meta = new ModelAnnotationMeta();
    meta.setSharedDimension( true );
    meta.setModelAnnotations( modelAnnotations );

    Repository rep = Mockito.mock( Repository.class );
    StringObjectId transId = new StringObjectId( "transId" );
    StringObjectId stepId = new StringObjectId( "stepId" );
    meta.saveRep( rep, metaStore, transId, stepId );

    verify( rep ).saveStepAttribute( transId, stepId, "CATEGORY_NAME", null );
    verify( rep ).saveStepAttribute( transId, stepId, "TARGET_OUTPUT_STEP", null );

    verify( rep ).saveStepAttribute( transId, stepId, 0, "ANNOTATION_NAME", "12345" );
    verify( rep ).saveStepAttribute( transId, stepId, 0, "ANNOTATION_FIELD_NAME", "GenderField" );
    verify( rep ).saveStepAttribute( transId, stepId, 0, "ANNOTATION_TYPE", CREATE_ATTRIBUTE.toString() );
    verify( rep ).saveStepAttribute( transId, stepId, 0, "PROPERTY_VALUE_field", "GenderField" );
    verify( rep ).saveStepAttribute( transId, stepId, 0, "PROPERTY_VALUE_unique", "false" );
    verify( rep ).saveStepAttribute( transId, stepId, 0, "PROPERTY_VALUE_hierarchy", "Gender" );
    verify( rep ).saveStepAttribute( transId, stepId, 0, "PROPERTY_VALUE_name", "Gender" );
    verify( rep ).saveStepAttribute( transId, stepId, 0, "PROPERTY_VALUE_hidden", "false" );

    verify( rep ).saveStepAttribute( transId, stepId, "SHARED_DIMENSION", true );
    verify( rep ).saveStepAttribute( transId, stepId, "DESCRIPTION", "aDescription" );

    verify( rep ).saveStepAttribute( transId, stepId, 0, "DP_NAME", "sample" );
    verify( rep ).saveStepAttribute( transId, stepId, 0, "DP_SCHEMA_NAME", "sampleSchema" );
    verify( rep ).saveStepAttribute( transId, stepId, 0, "DP_TABLE_NAME", "salesTable" );
    verify( rep ).saveStepAttribute( transId, stepId, 0, "DP_DATABASE_META_NAME_REF", "ref" );
    verify( rep ).saveStepAttribute( transId, stepId, 0, "CM_NAME_0", "Gender" );
    verify( rep ).saveStepAttribute( transId, stepId, 0, "CM_COLUMN_NAME_0", "sex" );
    verify( rep ).saveStepAttribute( transId, stepId, 0, "CM_DATA_TYPE_0", "STRING" );
    verify( rep ).saveStepAttribute( transId, stepId, "CM_COUNT_0", 1 );
    verifyNoMoreInteractions( rep );

  }

  @Test
  public void testReadAnnotationFromRep() throws Exception {
    ModelAnnotationMeta meta = new ModelAnnotationMeta();
    StringObjectId stepId = new StringObjectId( "stepId" );
    Repository rep = mock( Repository.class );
    when( rep.getStepAttributeString( stepId, "CATEGORY_NAME" ) ).thenReturn( "aCategory" );
    when( rep.getStepAttributeString( stepId, "TARGET_OUTPUT_STEP" ) ).thenReturn( "target" );
    when( rep.countNrStepAttributes( stepId, "ANNOTATION_FIELD_NAME" ) ).thenReturn( 1 );

    when( rep.getStepAttributeString( stepId, 0, "ANNOTATION_NAME" ) ).thenReturn( "12345" );
    when( rep.getStepAttributeString( stepId, 0, "ANNOTATION_FIELD_NAME" ) ).thenReturn( "Gender" );
    when( rep.getStepAttributeString( stepId, 0, "ANNOTATION_TYPE" ) ).thenReturn( CREATE_ATTRIBUTE.toString() );

    meta.readRep( rep, metaStore, stepId, Collections.<DatabaseMeta>emptyList() );
    assertEquals( "aCategory", meta.getModelAnnotationCategory() );
    assertEquals( "target", meta.getTargetOutputStep() );
    ModelAnnotationGroup modelAnnotations = meta.getModelAnnotations();
    assertEquals( "12345", modelAnnotations.get( 0 ).getName() );
    assertEquals( "Gender", modelAnnotations.get( 0 ).getAnnotation().getField() );
    assertEquals( CREATE_ATTRIBUTE, modelAnnotations.get( 0 ).getType() );
  }

  @Test
  public void testInvalidNamesThrowException() throws Exception {
    assertValidName( "a valid name123" );
    assertInvalidName( "<invalid" );
    assertInvalidName( ">invalid" );
    assertInvalidName( "invalid:" );
    assertInvalidName( "inva*lid" );
    assertInvalidName( "invali?d" );
    assertInvalidName( "in|valid" );
    assertInvalidName( "in\tvalid" );
    assertInvalidName( "in\rvalid" );
    assertInvalidName( "invalid\n" );
  }

  private void assertInvalidName( final String name ) {
    ModelAnnotationMeta modelAnnotationMeta = new ModelAnnotationMeta();
    try {
      modelAnnotationMeta.checkValidName( name );
    } catch ( KettleException e ) {
      assertEquals( "We can't create an Annotation Group with any of the following characters in the name:\n"
        + " \\ / : * ? \" < > | \\t \\r \\n", e.getMessage().trim() );
      modelAnnotationMeta.setSharedDimension( true );
      try {
        modelAnnotationMeta.checkValidName( name );
      } catch ( KettleException e1 ) {
        assertEquals( "We can't create a Shared Dimension with any of the following characters in the name:\n"
          + " \\ / : * ? \" < > | \\t \\r \\n", e1.getMessage().trim() );
        return;
      }
    }
    fail( "should have been invalid" );
  }

  private void assertValidName( final String name ) throws KettleException {
    ModelAnnotationMeta modelAnnotationMeta = new ModelAnnotationMeta();
    modelAnnotationMeta.checkValidName( name );
  }

  private String getSampleXml() {
    return "<step>"
      + "  <name>Model Annotation Step</name>"
      + "  <type>FieldMetadataAnnotation</type>"
      + "  <description />"
      + "  <distribute>Y</distribute>"
      + "  <custom_distribution />"
      + "  <copies>1</copies>"
      + "  <partitioning>"
      + "    <method>none</method>"
      + "    <schema_name />"
      + "  </partitioning>"
      + "  <category>Category 1</category>"
      + "<annotations>"
      + "<annotation>"
      + "<field>col1</field><type>CREATE_MEASURE</type>"
      + "<properties>"
      + "<property><name>formatString</name><value>xxxx</value></property>"
      + "<property><name>hidden</name><value>false</value></property>"
      + "<property><name>description</name><value>some description</value></property>"
      + "<property><name>aggregateType</name><value>SUM</value></property>"
      + "</properties>"
      + "</annotation>"
      + "<annotation>"
      + "<name>ld</name><field>ld</field><type>LINK_DIMENSION</type>"
      + "<properties>"
      + "<property><name>sharedDimension</name><value>sharedDimension</value></property>"
      + "<property><name>name</name><value>ldName</value></property>"
      + "</properties></annotation>"
      + "</annotations>"
      + "  <cluster_schema />"
      + "  <remotesteps>"
      + "    <input />"
      + "    <output />"
      + "  </remotesteps>"
      + "  <GUI>"
      + "    <xloc>506</xloc>"
      + "    <yloc>175</yloc>"
      + "    <draw>Y</draw>"
      + "  </GUI>"
      + "</step>";
  }

  private String getSampleXmlNoFields() {
    return "<step>"
      + "  <name>Model Annotation Step</name>"
      + "  <type>FieldMetadataAnnotation</type>"
      + "  <description />"
      + "  <distribute>Y</distribute>"
      + "  <custom_distribution />"
      + "  <copies>1</copies>"
      + "  <partitioning>"
      + "    <method>none</method>"
      + "    <schema_name />"
      + "  </partitioning>"
      + "  <cluster_schema />"
      + "  <remotesteps>"
      + "    <input />"
      + "    <output />"
      + "  </remotesteps>"
      + "  <GUI>"
      + "    <xloc>506</xloc>"
      + "    <yloc>175</yloc>"
      + "    <draw>Y</draw>"
      + "  </GUI>"
      + "</step>";
  }

  private String getSampleXmlWithDataProviders() {
    return ""
      + "<step>"
      + "  <category />"
      + "  <annotations>"
      + "    <sharedDimension>Y</sharedDimension>"
      + "    <data-providers>"
      + "      <data-provider>"
      + "        <name>dp1Name</name>"
      + "        <schemaName />"
      + "        <tableName />"
      + "        <databaseMetaRef />"
      + "      </data-provider>"
      + "      <data-provider>"
      + "        <name>dp2Name</name>"
      + "        <schemaName />"
      + "        <tableName />"
      + "        <databaseMetaRef />"
      + "        <column-mappings>"
      + "          <column-mapping>"
      + "            <name>cm1name</name>"
      + "            <columnName />"
      + "          </column-mapping>"
      + "          <column-mapping>"
      + "            <name>cm2name</name>"
      + "            <columnName />"
      + "          </column-mapping>"
      + "        </column-mappings>"
      + "      </data-provider>"
      + "    </data-providers>"
      + "  </annotations>"
      + "</step>";
  }
}
