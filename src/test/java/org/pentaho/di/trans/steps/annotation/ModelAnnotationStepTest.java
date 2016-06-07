/*! ******************************************************************************
 *
 * Pentaho Community Edition Project: data-refinery-pdi-plugin
 *
 * Copyright (C) 2002-2016 by Pentaho : http://www.pentaho.com
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.*;

import org.pentaho.agilebi.modeler.models.annotations.CreateAttribute;
import org.pentaho.agilebi.modeler.models.annotations.CreateMeasure;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationManager;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.logging.LogLevel;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.value.ValueMetaNumber;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.entries.build.JobEntryBuildModel;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaDataCombi;
import org.pentaho.metadata.model.concept.types.AggregationType;
import org.pentaho.metastore.api.IMetaStore;

import java.util.Collections;

public class ModelAnnotationStepTest {
  @Test
  public void testPutsAnnotationGroupIntoTheExtensionMap() throws Exception {
    StepDataInterface stepDataInterface = new ModelAnnotationData();

    ModelAnnotationStep modelAnnotation = spy( createOneShotStep( stepDataInterface, null, null ) );
    ModelAnnotationMeta modelAnnotationMeta = new ModelAnnotationMeta();
    CreateAttribute ca1 = new CreateAttribute();
    ca1.setField( "f" );
    ModelAnnotation<?> annotationMock1 = new ModelAnnotation<CreateAttribute>( ca1 );
    ModelAnnotationGroup modelAnnotations = new ModelAnnotationGroup( annotationMock1 );
    modelAnnotationMeta.setModelAnnotations( modelAnnotations );
    doNothing().when( modelAnnotation ).putRow( null, new Object[]{} );

    modelAnnotation.processRow( modelAnnotationMeta, stepDataInterface );
    ModelAnnotationGroup actualAnnotations =
        (ModelAnnotationGroup) modelAnnotation.getExtensionDataMap().get( JobEntryBuildModel.KEY_MODEL_ANNOTATIONS );
    assertEquals( 1, actualAnnotations.size() );
    assertSame( annotationMock1, actualAnnotations.get( 0 ) );
    CreateAttribute ca2 = new CreateAttribute();
    ca2.setField( "f" );
    ModelAnnotation<?> annotationMock2 = new ModelAnnotation<CreateAttribute>( ca2 );
    modelAnnotations.add( annotationMock2 );
    modelAnnotation.first = true;

    modelAnnotation.processRow( modelAnnotationMeta, stepDataInterface );
    actualAnnotations =
        (ModelAnnotationGroup) modelAnnotation.getExtensionDataMap().get( JobEntryBuildModel.KEY_MODEL_ANNOTATIONS );
    assertEquals( 3, actualAnnotations.size() );
    assertSame( annotationMock1, actualAnnotations.get( 0 ) );
    assertSame( annotationMock1, actualAnnotations.get( 1 ) );
    assertSame( annotationMock2, actualAnnotations.get( 2 ) );
  }

  @Test
  public void testPutsAnnotationGroupIntoTheExtensionMapNoJob() throws Exception {
    StepDataInterface stepDataInterface = new ModelAnnotationData();

    ModelAnnotationStep modelAnnotation =
      spy( createOneShotStep( stepDataInterface, null, null, false, new Object[] {} ) );
    ModelAnnotationMeta modelAnnotationMeta = new ModelAnnotationMeta();
    CreateAttribute ca1 = new CreateAttribute();
    ca1.setField( "f" );
    ModelAnnotation<?> annotationMock1 = new ModelAnnotation<CreateAttribute>( ca1 );
    ModelAnnotationGroup modelAnnotations = new ModelAnnotationGroup( annotationMock1 );
    modelAnnotationMeta.setModelAnnotations( modelAnnotations );
    doNothing().when( modelAnnotation ).putRow( null, new Object[]{} );

    modelAnnotation.processRow( modelAnnotationMeta, stepDataInterface );
    ModelAnnotationGroup actualAnnotations =
        (ModelAnnotationGroup) modelAnnotation.getExtensionDataMap().get( JobEntryBuildModel.KEY_MODEL_ANNOTATIONS );
    assertEquals( 1, actualAnnotations.size() );
    assertSame( annotationMock1, actualAnnotations.get( 0 ) );
    CreateAttribute ca2 = new CreateAttribute();
    ca2.setField( "f" );
    ModelAnnotation<?> annotationMock2 = new ModelAnnotation<CreateAttribute>( ca2 );
    modelAnnotations.add( annotationMock2 );
    modelAnnotation.first = true;

    modelAnnotation.processRow( modelAnnotationMeta, stepDataInterface );
    actualAnnotations =
        (ModelAnnotationGroup) modelAnnotation.getExtensionDataMap().get( JobEntryBuildModel.KEY_MODEL_ANNOTATIONS );
    assertEquals( 3, actualAnnotations.size() );
    assertSame( annotationMock1, actualAnnotations.get( 0 ) );
    assertSame( annotationMock1, actualAnnotations.get( 1 ) );
    assertSame( annotationMock2, actualAnnotations.get( 2 ) );
  }

  @Test
  public void testReadsMetaStoreAnnotationGroup() throws Exception {
    final String groupName = "someGroup";

    // metastored annotations
    CreateAttribute ca = new CreateAttribute();
    ca.setField( "f1" );
    CreateMeasure cm = new CreateMeasure();
    cm.setField( "f2" );
    ModelAnnotation<CreateAttribute> a1 = new ModelAnnotation<CreateAttribute>( ca );
    ModelAnnotation<CreateMeasure> a2 = new ModelAnnotation<CreateMeasure>( cm );
    ModelAnnotationGroup modelAnnotations = new ModelAnnotationGroup( a1, a2 );
    modelAnnotations.setName( groupName );

    // 'metastore'
    IMetaStore metaStore = mock( IMetaStore.class );
    final ModelAnnotationManager manager = mock( ModelAnnotationManager.class );
    when( manager.readGroup( groupName, metaStore ) ).thenReturn( modelAnnotations );

    //step
    StepDataInterface stepDataInterface = new ModelAnnotationData();
    ModelAnnotationStep modelAnnotation = spy( createOneShotStep( stepDataInterface, metaStore, manager ) );
    RowMeta rowMeta = new RowMeta();
    rowMeta.addValueMeta( new ValueMetaString( "f1" ) );
    rowMeta.addValueMeta( new ValueMetaNumber( "f2" ) );
    when( modelAnnotation.getInputRowMeta() ).thenReturn( rowMeta );
    doNothing().when( modelAnnotation ).putRow( rowMeta, new Object[]{} );

    // set up a linked group in the meta
    ModelAnnotationMeta modelAnnotationMeta = new ModelAnnotationMeta();
    modelAnnotationMeta.setDefault();
    ModelAnnotationGroup linkedGroup = new ModelAnnotationGroup();
    linkedGroup.setName( groupName );
    modelAnnotationMeta.setModelAnnotations( linkedGroup );
    modelAnnotationMeta.setModelAnnotationCategory( groupName );

    // run
    modelAnnotation.processRow( modelAnnotationMeta, stepDataInterface );
    ModelAnnotationGroup actualAnnotations =
        (ModelAnnotationGroup) modelAnnotation.getExtensionDataMap().get( JobEntryBuildModel.KEY_MODEL_ANNOTATIONS );

    for ( int i = 0; i < modelAnnotations.size(); i++ ) {
      assertEquals( modelAnnotations.get( i ), actualAnnotations.get( i ) );
    }
  }

  @Test
  public void testReadsMetaStoreAnnotationGroupNoJob() throws Exception {
    final String groupName = "someGroup";

    // metastored annotations
    CreateAttribute ca = new CreateAttribute();
    ca.setField( "f1" );
    CreateMeasure cm = new CreateMeasure();
    cm.setField( "f2" );
    ModelAnnotation<CreateAttribute> a1 = new ModelAnnotation<CreateAttribute>( ca );
    ModelAnnotation<CreateMeasure> a2 = new ModelAnnotation<CreateMeasure>( cm );
    ModelAnnotationGroup modelAnnotations = new ModelAnnotationGroup( a1, a2 );
    modelAnnotations.setName( groupName );

    // 'metastore'
    IMetaStore metaStore = mock( IMetaStore.class );
    final ModelAnnotationManager manager = mock( ModelAnnotationManager.class );
    when( manager.readGroup( groupName, metaStore ) ).thenReturn( modelAnnotations );

    //step
    StepDataInterface stepDataInterface = new ModelAnnotationData();
    ModelAnnotationStep modelAnnotation = spy( createOneShotStep( stepDataInterface, metaStore, manager, false,
      new Object[]{} ) );
    RowMeta rowMeta = new RowMeta();
    rowMeta.addValueMeta( new ValueMetaString( "f1" ) );
    rowMeta.addValueMeta( new ValueMetaNumber( "f2" ) );
    when( modelAnnotation.getInputRowMeta() ).thenReturn( rowMeta );
    doNothing().when( modelAnnotation ).putRow( rowMeta, new Object[]{} );


    // set up a linked group in the meta
    ModelAnnotationMeta modelAnnotationMeta = new ModelAnnotationMeta();
    modelAnnotationMeta.setDefault();
    ModelAnnotationGroup linkedGroup = new ModelAnnotationGroup();
    linkedGroup.setName( groupName );
    modelAnnotationMeta.setModelAnnotations( linkedGroup );
    modelAnnotationMeta.setModelAnnotationCategory( groupName );

    // run
    modelAnnotation.processRow( modelAnnotationMeta, stepDataInterface );
    ModelAnnotationGroup actualAnnotations =
        (ModelAnnotationGroup) modelAnnotation.getExtensionDataMap().get( JobEntryBuildModel.KEY_MODEL_ANNOTATIONS );

    for ( int i = 0; i < modelAnnotations.size(); i++ ) {
      assertEquals( modelAnnotations.get( i ), actualAnnotations.get( i ) );
    }
  }

  @Test
  public void testMetaStoreAnnotationGroupNotThere() throws Exception {
    final String groupName = "someGroup";

    // 'metastore'
    IMetaStore metaStore = mock( IMetaStore.class );
    final ModelAnnotationManager manager = mock( ModelAnnotationManager.class );
    when( manager.readGroup( groupName, metaStore ) ).thenReturn( null );

    // step
    StepDataInterface stepDataInterface = new ModelAnnotationData();
    ModelAnnotationStep modelAnnotation = createOneShotStep( stepDataInterface, metaStore, manager );

    // set up a linked group in the meta
    ModelAnnotationMeta modelAnnotationMeta = new ModelAnnotationMeta();
    modelAnnotationMeta.setDefault();
    ModelAnnotationGroup linkedGroup = new ModelAnnotationGroup();
    linkedGroup.setName( groupName );
    modelAnnotationMeta.setModelAnnotations( linkedGroup );
    modelAnnotationMeta.setModelAnnotationCategory( groupName );

    // run
    try {
      modelAnnotation.processRow( modelAnnotationMeta, stepDataInterface );
      Assert.fail();
    } catch ( KettleException e ) {
      assertEquals( "Shared annotation group someGroup is not found.", e.getMessage().trim() );
    }
  }

  @Test
  public void testMetaStoreSharedDimensionNotThere() throws Exception {
    final String groupName = "someGroup";

    // 'metastore'
    IMetaStore metaStore = mock( IMetaStore.class );
    final ModelAnnotationManager manager = mock( ModelAnnotationManager.class );
    when( manager.readGroup( groupName, metaStore ) ).thenReturn( null );

    // step
    StepDataInterface stepDataInterface = new ModelAnnotationData();
    ModelAnnotationStep modelAnnotation = createOneShotStep( stepDataInterface, metaStore, manager );

    // set up a linked group in the meta
    ModelAnnotationMeta modelAnnotationMeta = new ModelAnnotationMeta();
    modelAnnotationMeta.setDefault();
    modelAnnotationMeta.setSharedDimension( true );
    ModelAnnotationGroup sharedAnnotations = new ModelAnnotationGroup();
    sharedAnnotations.setName( groupName );
    modelAnnotationMeta.setModelAnnotations( sharedAnnotations );
    modelAnnotationMeta.setModelAnnotationCategory( groupName );
    modelAnnotationMeta.setTargetOutputStep( "step name" );

    // run
    try {
      modelAnnotation.processRow( modelAnnotationMeta, stepDataInterface );
      Assert.fail();
    } catch ( KettleException e ) {
      assertEquals( "Shared dimension someGroup is not found.", e.getMessage().trim() );
    }
  }
  @Test
  public void testFailNonNumericMeasure() throws Exception {
    ModelAnnotationGroup group = new ModelAnnotationGroup();

    CreateAttribute attr = new CreateAttribute();
    attr.setName( "a1" );
    attr.setDimension( "d" );
    attr.setHierarchy( "h" );
    attr.setField( "f1" );
    ModelAnnotation<CreateAttribute> attribute = new ModelAnnotation<CreateAttribute>( attr );
    group.add( attribute );

    CreateMeasure cm = new CreateMeasure();
    cm.setName( "measure1" );
    cm.setField( "f2" );
    ModelAnnotation<CreateMeasure> measure = new ModelAnnotation<CreateMeasure>( cm );
    group.add( measure );

    StepDataInterface stepDataInterface = new ModelAnnotationData();
    ModelAnnotationMeta modelAnnotationMeta = new ModelAnnotationMeta();
    modelAnnotationMeta.setDefault();
    modelAnnotationMeta.setModelAnnotations( group );

    // run ok
    ModelAnnotationStep modelAnnotation = spy( createOneShotStep( stepDataInterface, null, null ) );
    RowMeta okMeta = new RowMeta();
    okMeta.addValueMeta( new ValueMetaString( "f1" ) );
    okMeta.addValueMeta( new ValueMetaNumber( "f2" ) );
    when( modelAnnotation.getInputRowMeta() ).thenReturn( okMeta );
    doNothing().when( modelAnnotation ).putRow( okMeta, new Object[]{} );

    modelAnnotation.processRow( modelAnnotationMeta, stepDataInterface );

    // fail
    RowMeta badMeta = new RowMeta();
    badMeta.addValueMeta( new ValueMetaString( "f1" ) );
    badMeta.addValueMeta( new ValueMetaString( "f2" ) );
    modelAnnotation = spy( createOneShotStep( stepDataInterface, null, null ) );
    when( modelAnnotation.getInputRowMeta() ).thenReturn( badMeta );
    doNothing().when( modelAnnotation ).putRow( badMeta, new Object[]{} );
    try {
      modelAnnotation.processRow( modelAnnotationMeta, stepDataInterface );
      Assert.fail( "not validated" );
    } catch ( KettleException e ) {
      // ok
    }

    // count is fine for non-numeric
    modelAnnotation = spy( createOneShotStep( stepDataInterface, null, null ) );
    when( modelAnnotation.getInputRowMeta() ).thenReturn( badMeta );
    doNothing().when( modelAnnotation ).putRow( badMeta, new Object[]{} );
    cm.setAggregateType( AggregationType.COUNT );
    modelAnnotation.processRow( modelAnnotationMeta, stepDataInterface );

    // count distinct is fine for non-numeric
    modelAnnotation = spy( createOneShotStep( stepDataInterface, null, null ) );
    when( modelAnnotation.getInputRowMeta() ).thenReturn( badMeta );
    doNothing().when( modelAnnotation ).putRow( badMeta, new Object[]{} );
    cm.setAggregateType( AggregationType.COUNT_DISTINCT );
    modelAnnotation.processRow( modelAnnotationMeta, stepDataInterface );

    // fail. does not support maximum
    modelAnnotation = spy( createOneShotStep( stepDataInterface, null, null ) );
    when( modelAnnotation.getInputRowMeta() ).thenReturn( badMeta );
    doNothing().when( modelAnnotation ).putRow( badMeta, new Object[]{} );
    cm.setAggregateType( AggregationType.MAXIMUM );
    try {
      modelAnnotation.processRow( modelAnnotationMeta, stepDataInterface );
      Assert.fail( "not validated" );
    } catch ( KettleException e ) {
      Assert.assertNotNull( e );
    }
  }

  @Test
  public void testOutputStepIsEmpty() throws Exception {

    // step
    StepDataInterface stepDataInterface = new ModelAnnotationData();
    ModelAnnotationStep modelAnnotation = createOneShotStep( stepDataInterface, null, null );

    // set up a linked group in the meta
    ModelAnnotationMeta modelAnnotationMeta = new ModelAnnotationMeta();
    modelAnnotationMeta.setDefault();
    modelAnnotationMeta.setModelAnnotationCategory( "someGroup" );
    modelAnnotationMeta.setSharedDimension( true );

    // run
    try {
      modelAnnotation.processRow( modelAnnotationMeta, stepDataInterface );
      Assert.fail();
    } catch ( KettleException e ) {
      assertEquals( "Please select a valid data provider step.", e.getMessage().trim() );
    }
  }

  @Test
  public void testAnnotationsOnlyWrittenOnValidRow() throws Exception {
    StepDataInterface stepDataInterface = new ModelAnnotationData();
    ModelAnnotationStep modelAnnotation = createOneShotStep( stepDataInterface, null, null, true, null );
    modelAnnotation.processRow( null, null );
    assertNull( modelAnnotation.getTrans().getExtensionDataMap().get( JobEntryBuildModel.KEY_MODEL_ANNOTATIONS ) );
  }

  @Test
  public void testOutputStepIsMissing() throws Exception {

    // step
    StepDataInterface stepDataInterface = new ModelAnnotationData();
    ModelAnnotationStep modelAnnotation = createOneShotStep( stepDataInterface, null, null );

    // set up a linked group in the meta
    ModelAnnotationMeta modelAnnotationMeta = new ModelAnnotationMeta();
    modelAnnotationMeta.setDefault();
    modelAnnotationMeta.setTargetOutputStep( "MissingStep" );
    modelAnnotationMeta.setModelAnnotationCategory( "someGroup" );
    modelAnnotationMeta.setSharedDimension( true );

    // run
    try {
      modelAnnotation.processRow( modelAnnotationMeta, stepDataInterface );
      Assert.fail();
    } catch ( KettleException e ) {
      assertEquals( "Please select a valid data provider step.", e.getMessage().trim() );
    }
  }

  private ModelAnnotationStep createOneShotStep( StepDataInterface stepDataInterface, IMetaStore metaStore,
      final ModelAnnotationManager manager ) {
    return createOneShotStep( stepDataInterface, metaStore, manager, true, new Object[] {} );
  }
  private ModelAnnotationStep createOneShotStep( StepDataInterface stepDataInterface, IMetaStore metaStore,
                                                 final ModelAnnotationManager manager, boolean createJob,
                                                 final Object[] fakeRow ) {
    StepMeta stepMeta = mock( StepMeta.class );
    TransMeta transMeta = mock( TransMeta.class );
    final Trans trans = mock( Trans.class );
    when( stepMeta.getName() ).thenReturn( "someName" );
    when( transMeta.findStep( "someName" ) ).thenReturn( stepMeta );
    Job job = null;
    if ( createJob ) {
      job = mock( Job.class );
      when( trans.getParentJob() ).thenReturn( job );
    } else {
      when( trans.getParentJob() ).thenReturn( null );
    }
    StepMetaDataCombi stepMetaDataCombi = new StepMetaDataCombi();
    stepMetaDataCombi.stepname = "step name";
    when( trans.getSteps() ).thenReturn( Collections.singletonList( stepMetaDataCombi ) );
    ModelAnnotationStep modelAnnotation = new ModelAnnotationStep( stepMeta, stepDataInterface, 1, transMeta, trans ) {
      @Override public Object[] getRow() throws KettleException {
        return fakeRow;
      }

      @Override public Trans getTrans() {
        return trans;
      }

      @Override
      protected ModelAnnotationManager getModelAnnotationsManager( ModelAnnotationMeta meta ) {
        return manager;
      }
    };
    modelAnnotation.setLogLevel( LogLevel.BASIC );
    modelAnnotation.setMetaStore( metaStore );
    if ( createJob ) {
      when( job.getExtensionDataMap() ).thenReturn( modelAnnotation.getExtensionDataMap() );
    } else {
      when( trans.getExtensionDataMap() ).thenReturn( modelAnnotation.getExtensionDataMap() );
    }
    return modelAnnotation;
  }

}
