/*!
 * PENTAHO CORPORATION PROPRIETARY AND CONFIDENTIAL
 *
 * Copyright 2015 Pentaho Corporation (Pentaho). All rights reserved.
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
package org.pentaho.di.core.refinery.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.pentaho.agilebi.modeler.models.annotations.CreateAttribute;
import org.pentaho.agilebi.modeler.models.annotations.CreateMeasure;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationManager;
import org.pentaho.agilebi.modeler.models.annotations.data.ColumnMapping;
import org.pentaho.agilebi.modeler.models.annotations.data.DataProvider;
import org.pentaho.di.core.KettleClientEnvironment;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.database.Database;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.extension.ExtensionPoint;
import org.pentaho.di.core.extension.ExtensionPointPluginType;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.plugins.PluginRegistry;
import static org.pentaho.di.core.refinery.test.TransTestUtil.createTableH2;
import static org.pentaho.di.core.refinery.test.TransTestUtil.getRowMeta;
import static org.pentaho.di.core.refinery.test.TransTestUtil.getTransAsJobEntry;
import static org.pentaho.di.core.refinery.test.TransTestUtil.newH2Db;
import static org.pentaho.di.core.refinery.test.TransTestUtil.runTransformation;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaInteger;
import org.pentaho.di.core.row.value.ValueMetaNumber;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.variables.Variables;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobHopMeta;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entries.build.JobEntryBuildModel;
import org.pentaho.di.job.entries.special.JobEntrySpecial;
import org.pentaho.di.job.entries.trans.JobEntryTrans;
import org.pentaho.di.job.entry.JobEntryCopy;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransHopMeta;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaDataCombi;
import org.pentaho.di.trans.steps.annotation.ModelAnnotationMeta;
import org.pentaho.di.trans.steps.tableoutput.TableOutput;
import org.pentaho.di.trans.steps.tableoutput.TableOutputMeta;
import org.pentaho.metastore.api.IMetaStore;
import org.pentaho.metastore.stores.memory.MemoryMetaStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class DataRefineryTransFinishListenerTest {

  private static final String H2DB = "mem:testdb";

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    KettleClientEnvironment.init();

    ExtensionPoint epAnnotation = DataRefineryTransFinishListener.class.getAnnotation( ExtensionPoint.class );
    ExtensionPointPluginType.getInstance().handlePluginAnnotation(
        DataRefineryTransFinishListener.class, epAnnotation, Collections.<String>emptyList(), false, null );

    PluginRegistry.init();
    if ( !Props.isInitialized() ) {
      Props.init( 0 );
    }
  }

  @Test
  public void testTransFinishListenerTabelOutputDataProvider() throws Exception {

    IMetaStore metaStore = new MemoryMetaStore();

    ModelAnnotationGroup sharedGroup = new ModelAnnotationGroup();
    sharedGroup.setSharedDimension( true );
    sharedGroup.setName( "sharedGroup" );
    ModelAnnotationManager annotationManager = new ModelAnnotationManager( true );
    annotationManager.createGroup( sharedGroup, metaStore );

    // database
    RowMetaInterface rowMetaDb = getRowMeta(
        new ValueMetaInteger( "id" ),
        new ValueMetaString( "type" ),
        new ValueMetaString( "city" ),
        new ValueMetaString( "name" )
    );
    DatabaseMeta dbMeta = newH2Db( "myh2", H2DB );

    VariableSpace vars = Variables.getADefaultVariableSpace();

    // Transformation
    TransMeta transMeta = new TransMeta( vars );
    transMeta.setMetaStore( metaStore );

    // Annotating step
    ModelAnnotationMeta annotationMeta = new ModelAnnotationMeta();
    annotationMeta.setDefault();
    annotationMeta.setModelAnnotationCategory( sharedGroup.getName() );
    annotationMeta.setTargetOutputStep( "${targetOutputStep}" );
    transMeta.setVariable( "targetOutputStep", "Out" );
    annotationMeta.setSharedDimension( true );
    StepMeta annotStepMeta = new StepMeta( "Annotating", annotationMeta );
    transMeta.addOrReplaceStep( annotStepMeta );

    TableOutputMeta tableOutMeta = new TableOutputMeta();

    // Out step
    tableOutMeta.setDefault();
    tableOutMeta.setDatabaseMeta( dbMeta );
    tableOutMeta.setTableName( "store" );
    tableOutMeta.setSpecifyFields( true );
    tableOutMeta.setFieldStream( new String[] { "Store ID", "Store Type", "Store City", "Store Name" } );
    tableOutMeta.setFieldDatabase( new String[] { "id", "type", "city", "name" } );
    StepMeta outStepMeta = new StepMeta( "Out", tableOutMeta );
    transMeta.addOrReplaceStep( outStepMeta );

    // Annotating -> Out
    transMeta.addTransHop( new TransHopMeta( annotStepMeta, outStepMeta ) );

    RowMetaInterface rowMetaStream = getRowMeta(
        new ValueMetaInteger( "Store ID" ),
        new ValueMetaString( "Store Type" ),
        new ValueMetaString( "Store City" ),
        new ValueMetaString( "Store Name" )
    );
    Database db = createTableH2( dbMeta, "store", rowMetaDb );
    Result result;
    try {
      result = runTransformation( transMeta, annotStepMeta, rowMetaStream,
          new Object[] { 1L, "type 1", "burbclave 1", "store 1" },
          new Object[] { 2L, "type 1", "burbclave 1", "store 2" } );
    } finally {
      // release db from mem
      db.disconnect();
    }
    assertEquals( "transformation errors", 0, result.getNrErrors() );

    ModelAnnotationGroup group = annotationManager.readGroup( "sharedGroup", metaStore );
    assertEquals( "providers", 1, group.getDataProviders().size() );

    DataProvider provider = group.getDataProviders().get( 0 );
    assertEquals( "column mappings", 4, provider.getColumnMappings().size() );

    for ( int i = 0; i < rowMetaDb.size(); i++ ) {
      ValueMetaInterface dbValMeta = rowMetaDb.getValueMeta( i );
      ValueMetaInterface streamValMeta = rowMetaStream.getValueMeta( i );
      for ( ColumnMapping colMap : provider.getColumnMappings() ) {
        if ( colMap.getColumnName().equals( dbValMeta.getName() ) ) {
          assertEquals( streamValMeta.getName(), colMap.getName() );
          provider.getColumnMappings().remove( colMap );
          break;
        }
      }
    }
    assertTrue( "match all", provider.getColumnMappings().isEmpty() );
  }

  /**
   * stream fields only
   */
  @Test
  public void testTransFinishListenerNoSpecifyFields() throws Exception {
    IMetaStore metaStore = new MemoryMetaStore();

    ModelAnnotationGroup sharedGroup = new ModelAnnotationGroup();
    sharedGroup.setSharedDimension( true );
    sharedGroup.setName( "sharedGroup" );
    ModelAnnotationManager
        annotationManager =
        new ModelAnnotationManager( true );
    annotationManager.createGroup( sharedGroup, metaStore );

    // database
    RowMetaInterface rowMetaDb = getRowMeta(
        new ValueMetaInteger( "id" ),
        new ValueMetaString( "type" ),
        new ValueMetaString( "city" ),
        new ValueMetaString( "name" )
    );

    DatabaseMeta dbMeta = newH2Db( "myh2", H2DB );

    // Transformation
    TransMeta transMeta = new TransMeta( Variables.getADefaultVariableSpace() );
    transMeta.setMetaStore( metaStore );

    // Annotating step
    ModelAnnotationMeta annotationMeta = new ModelAnnotationMeta();
    annotationMeta.setDefault();
    annotationMeta.setModelAnnotationCategory( sharedGroup.getName() );
    annotationMeta.setSharedDimension( true );
    annotationMeta.setTargetOutputStep( "${targetOutputStep}" );
    transMeta.setVariable( "targetOutputStep", "Out" );
    StepMeta annotStepMeta = new StepMeta( "Annotating", annotationMeta );
    transMeta.addOrReplaceStep( annotStepMeta );

    TableOutputMeta tableOutMeta = new TableOutputMeta();

    // Out step
    tableOutMeta.setDefault();
    tableOutMeta.setDatabaseMeta( dbMeta );
    tableOutMeta.setTableName( "store" );
    StepMeta outStepMeta = new StepMeta( "Out", tableOutMeta );
    transMeta.addOrReplaceStep( outStepMeta );

    // Annotating -> Out
    transMeta.addTransHop( new TransHopMeta( annotStepMeta, outStepMeta ) );

    Database db = createTableH2( dbMeta, "store", rowMetaDb );
    Result result;
    try {
      result = runTransformation( transMeta, annotStepMeta, rowMetaDb,
          new Object[] { 1L, "type 1", "burbclave 1", "store 1" },
          new Object[] { 2L, "type 1", "burbclave 1", "store 2" } );
    } finally {
      // release db from mem
      db.disconnect();
    }

    assertEquals( "transformation errors", 0, result.getNrErrors() );

    ModelAnnotationGroup group = annotationManager.readGroup( "sharedGroup", metaStore );
    assertEquals( "providers", 1, group.getDataProviders().size() );

    DataProvider provider = group.getDataProviders().get( 0 );
    assertEquals( "column mappings", 4, provider.getColumnMappings().size() );

    for ( int i = 0; i < rowMetaDb.size(); i++ ) {
      ValueMetaInterface dbValMeta = rowMetaDb.getValueMeta( i );
      for ( ColumnMapping colMap : provider.getColumnMappings() ) {
        if ( colMap.getColumnName().equals( dbValMeta.getName() ) ) {
          assertEquals( dbValMeta.getName(), colMap.getName() );
          provider.getColumnMappings().remove( colMap );
          break;
        }
      }
    }
    assertTrue( "match all", provider.getColumnMappings().isEmpty() );
  }

  @Test
  public void testJobListenersSetCombi() throws Exception {
    IMetaStore metaStore = new MemoryMetaStore();

    ModelAnnotationGroup group = new ModelAnnotationGroup();
    //sharedGroup.setSharedDimension( true );
    CreateAttribute attr1 = new CreateAttribute();
    attr1.setDimension( "dim" );
    attr1.setHierarchy( "hie" );
    attr1.setName( "F-1" );
    attr1.setField( "f1" );
    ModelAnnotation<CreateAttribute> annF1 = new ModelAnnotation<CreateAttribute>( attr1 );
    group.add( annF1 );
    CreateMeasure measure = new CreateMeasure();
    measure.setName( "M" );
    measure.setField( "f2" );
    ModelAnnotation<CreateMeasure> annF2 = new ModelAnnotation<CreateMeasure>( measure );
    group.add( annF2 );
    group.setName( "group" );
    ModelAnnotationManager annotationManager = new ModelAnnotationManager();
    annotationManager.createGroup( group, metaStore );
    // database
    RowMetaInterface rowMetaDb = getRowMeta(
        new ValueMetaString( "f1" ),
        new ValueMetaNumber( "f2" )
    );
    DatabaseMeta dbMeta = newH2Db( "myh2", H2DB );
    Database db = createTableH2( dbMeta, "fact1", rowMetaDb );

    // Transformation
    TransMeta transMeta = new TransMeta();

    // Annotating step
    ModelAnnotationMeta annotationMeta = new ModelAnnotationMeta();
    annotationMeta.setDefault();
    annotationMeta.setModelAnnotations( group );
    StepMeta annotStepMeta = new StepMeta( "Annotating", annotationMeta );
    transMeta.addOrReplaceStep( annotStepMeta );

    // Out step
    TableOutputMeta tableOutMeta = new TableOutputMeta();
    tableOutMeta.setDefault();
    tableOutMeta.setDatabaseMeta( dbMeta );
    tableOutMeta.setTableName( "fact1" );
    StepMeta outStepMeta = new StepMeta( "Out", tableOutMeta );
    transMeta.addOrReplaceStep( outStepMeta );

    // Annotating -> Out
    transMeta.addTransHop( new TransHopMeta( annotStepMeta, outStepMeta ) );

    // jobMeta
    JobMeta jobMeta = new JobMeta();
    jobMeta.setMetaStore( metaStore );

    // start
    JobEntrySpecial start = new JobEntrySpecial( "START", true, false );
    JobEntryCopy startCopy = new JobEntryCopy( start );
    startCopy.setDrawn();
    jobMeta.addJobEntry( startCopy );

    // trans entry
    JobEntryTrans jeTrans = getTransAsJobEntry( transMeta, annotStepMeta, rowMetaDb,
        new Object[] { "a", 42d } );
    jeTrans.setName( "trans" );
    JobEntryCopy jeTransCopy = new JobEntryCopy( jeTrans );
    jeTransCopy.setDrawn();
    jobMeta.addJobEntry( jeTransCopy );

    // start -> trans
    jobMeta.addJobHop( new JobHopMeta( startCopy, jeTransCopy ) );

    // buildmodel
    JobEntryBuildModel jeBuildModel = new JobEntryBuildModel( "buildmodel", "builds models" );
    jeBuildModel.setPluginId( JobEntryBuildModel.PLUGIN_ID );
    jeBuildModel.setOutputStep( outStepMeta.getName() );
    jeBuildModel.setModelName( "Das Model" );
    JobEntryCopy jeBuildModelCopy = new JobEntryCopy( jeBuildModel );
    jeBuildModelCopy.setDrawn();
    jobMeta.addJobEntry( jeBuildModelCopy );

    // trans -> buildmodel
    jobMeta.addJobHop( new JobHopMeta( jeTransCopy, jeBuildModelCopy ) );

    Job job = new Job( null, jobMeta );
    transMeta.setParentVariableSpace( job );
    job.run();
    db.disconnect();

    assertEquals( "job errors", 0, job.getResult().getNrErrors() );
    StepMetaDataCombi outCombi =
        (StepMetaDataCombi) job.getExtensionDataMap().get( "JobEntryBuildModel.OutputStep.buildmodel" );
    assertNotNull( "combi not there", outCombi );
    assertEquals( tableOutMeta, outCombi.meta );

  }

  @Test
  public void testStepMetaIsInExtensionMapAndDuplicateThrowsException() throws Exception {
    // former BuildModelJobStartListenerTest+BuildModelStepFinishedListenerTest
    DataRefineryTransFinishListener listener = new DataRefineryTransFinishListener();
    LogChannelInterface logChannel = mock( LogChannelInterface.class );
    StepMetaDataCombi stepMetaData = new StepMetaDataCombi();
    stepMetaData.stepname = "cosmic output";
    Trans trans = mock( Trans.class );
    Job job = mock( Job.class );
    JobMeta meta = new JobMeta();
    addModelEntry( meta, "cosmic build model", "cosmic output" );
    addModelEntry( meta, "build less impactful model", "${boring output}" );
    when( trans.getParentJob() ).thenReturn( job );
    when( job.getJobMeta() ).thenReturn( meta );
    when( job.environmentSubstitute( "cosmic output" ) ).thenReturn( "cosmic output" );
    when( job.environmentSubstitute( "${boring output}" ) ).thenReturn( "cosmic output" );
    HashMap<String, Object> actualMap = new HashMap<String, Object>();
    when( job.getExtensionDataMap() ).thenReturn( actualMap );
    TableOutput tableOutput = mock( TableOutput.class );
    stepMetaData.step = tableOutput;
    ArrayList<StepMetaDataCombi> combis = new ArrayList<StepMetaDataCombi>( 1 );
    combis.add( stepMetaData );
    when( trans.getSteps() ).thenReturn( combis );
    when( tableOutput.getTrans() ).thenReturn( trans );
    listener.callExtensionPoint( logChannel, trans );
    assertEquals( 2, actualMap.size() );
    assertEquals( stepMetaData, actualMap.get( "JobEntryBuildModel.OutputStep.cosmic build model" ) );
    assertEquals( stepMetaData, actualMap.get( "JobEntryBuildModel.OutputStep.build less impactful model" ) );
    listener.callExtensionPoint( logChannel, trans );

    verify( logChannel ).logError( matches(
        "\\s*Unable to auto-model because more than one step with the same name was found: cosmic output\\s*" ) );
  }

  private void addModelEntry(
      final JobMeta meta, final String modelStepName, final String outputStepName ) {
    final JobEntryCopy copy = new JobEntryCopy();
    JobEntryBuildModel jeBuildModel = new JobEntryBuildModel( modelStepName, "who cares" );
    jeBuildModel.setOutputStep( outputStepName );
    jeBuildModel.setPluginId( JobEntryBuildModel.PLUGIN_ID );
    copy.setEntry( jeBuildModel );
    meta.addJobEntry( copy );
  }

}
