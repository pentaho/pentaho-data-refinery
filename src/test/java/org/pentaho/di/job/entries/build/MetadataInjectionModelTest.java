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
package org.pentaho.di.job.entries.build;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.di.core.KettleClientEnvironment;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.database.Database;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.extension.ExtensionPoint;
import org.pentaho.di.core.extension.ExtensionPointPluginType;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.plugins.StepPluginType;
import org.pentaho.di.core.refinery.extension.DataRefineryTransFinishListener;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobHopMeta;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entries.special.JobEntrySpecial;
import org.pentaho.di.job.entries.trans.JobEntryTrans;
import org.pentaho.di.job.entry.JobEntryCopy;
import org.pentaho.metadata.model.Domain;
import org.pentaho.metadata.model.LogicalModel;
import org.pentaho.metadata.model.concept.types.AggregationType;
import org.pentaho.metadata.model.olap.OlapCube;
import org.pentaho.metadata.util.XmiParser;

public class MetadataInjectionModelTest {
  private Job job;
  private JobEntryTrans trans;
  private DatabaseMeta databaseMeta;
  private JobEntryBuildModel buildJobEntry;
  private JobEntryCopy buildCopy;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    if ( !KettleClientEnvironment.isInitialized() ) {
      KettleClientEnvironment.init();
    }
    PluginRegistry.addPluginType( StepPluginType.getInstance() );

    ExtensionPoint epAnnotation = DataRefineryTransFinishListener.class.getAnnotation( ExtensionPoint.class );
    ExtensionPointPluginType.getInstance().handlePluginAnnotation(
        DataRefineryTransFinishListener.class, epAnnotation, Collections.<String>emptyList(), false, null );

    PluginRegistry.init();
    if ( !Props.isInitialized() ) {
      Props.init( 0 );
    }
  }

  @Before
  public void setUp() throws Exception {

    // DB Setup
    String dbDir = "target/test-db/MetadataInjectionModelTest-H2-DB";
    File file = new File( dbDir + ".h2.db" );
    if ( file.exists() ) {
      file.delete();
    }
    databaseMeta = new DatabaseMeta( "myh2", "H2", "Native", null, dbDir, null, "sa", null );
    Database db = new Database( null, databaseMeta );
    db.connect();
    db.execStatement( "DROP TABLE IF EXISTS steel;" );
    db.execStatement( "CREATE TABLE steel(territory VARCHAR(2147483647), sales DOUBLE, quantity DOUBLE );" );
    db.disconnect();

    // Job Setup
    job = new Job( null, new JobMeta() );
    // Add start job entry
    JobEntrySpecial start = new JobEntrySpecial( "START", true, false );
    JobEntryCopy startCopy = new JobEntryCopy( start );
    startCopy.setDrawn();
    job.getJobMeta().addJobEntry( startCopy );
    start.setParentJob( job );

    // Call transformation to load table
    trans = new JobEntryTrans();
    trans.setName( "Call Transformation" );
    JobEntryCopy transCopy = new JobEntryCopy( trans );
    transCopy.setDrawn();
    job.getJobMeta().addJobEntry( transCopy );
    trans.setParentJob( job );
    trans.setFileName( getClass().getResource( "/53 Metadata Inject.ktr" ).getPath() );

    JobHopMeta hop = new JobHopMeta( startCopy, transCopy );
    job.getJobMeta().addJobHop( hop );

    // Add Build Model job entry
    buildJobEntry = new JobEntryBuildModel( "Build Model", "Builds model on a metadata injected step" );
    buildJobEntry.setPluginId( JobEntryBuildModel.PLUGIN_ID );

    buildJobEntry.setOutputStep( "Metadata Injected Table Output" );
    buildCopy = new JobEntryCopy( buildJobEntry );
    buildCopy.setDrawn();
    job.getJobMeta().addJobEntry( buildCopy );
    buildJobEntry.setParentJob( job );

    JobHopMeta hop2 = new JobHopMeta( transCopy, buildCopy );
    job.getJobMeta().addJobHop( hop2 );
  }

  @After
  public void tearDown() throws Exception {

  }

  /**
   * Verifies that we can auto-model on a table output step which was defined via metadata injection.
   * The underlying physical table contains more columns than the stream fields that are being injected in.
   *
   * @throws Exception
   */
  @Test
  public void testJobEntry() throws Exception {

    buildJobEntry.setModelName( "Territory-Sales Model" );
    job.run();

    assertTrue( job.getResult().getResult() );

    String xmi = job.getVariable( "JobEntryBuildModel.XMI.Territory-Sales Model" );
    assertTrue( xmi, xmi.contains( "<CWM:Description body=\"STEEL\" name=\"target_table\"" ) );
    assertTrue( xmi, xmi.contains( "<CWMOLAP:Schema name=\"MODEL_1_OLAP\"" ) );

    // Verify we can parse the XMI and read the cube. 
    XmiParser parser = new XmiParser();
    Domain d = parser.parseXmi( new ByteArrayInputStream( xmi.getBytes( "UTF-8" ) ) );
    assertEquals( "Territory-Sales Model", d.getLogicalModels().get( 0 ).getName().getLocalizedString( "en" ) );
    assertEquals( "Territory-Sales Model_OLAP", d.getLogicalModels().get( 1 ).getName().getLocalizedString( "en" ) );

    assertEquals( 2, d.getLogicalModels().size() );
    @SuppressWarnings( "unchecked" )
    List<OlapCube> cubes =
        (List<OlapCube>) d.getLogicalModels().get( 1 ).getProperty( LogicalModel.PROPERTY_OLAP_CUBES );
    assertEquals( "Territory-Sales Model", cubes.get( 0 ).getName() );

    // Verify model has only one Sales measure even though the underlying table has two numeric columns
    assertEquals( 1, cubes.get( 0 ).getOlapMeasures().size() );
    assertEquals( "Sales", cubes.get( 0 ).getOlapMeasures().get( 0 ).getName() );
    assertEquals( AggregationType.SUM, cubes.get( 0 ).getOlapMeasures().get( 0 ).getLogicalColumn()
        .getAggregationType() );

    assertEquals( "myh2", job.getVariable( "JobEntryBuildModel.DatabaseConnection.Territory-Sales Model" ) );
    assertEquals( "true", job.getVariable( "JobEntryBuildModel.XMI.DSW.Territory-Sales Model" ) );

  }

  @Test
  public void testGetOutputStepList() throws Exception {

    String[] steps = buildJobEntry.getOutputStepList( job.getJobMeta() );

    assertEquals( 1, steps.length );
    assertEquals( "Metadata Injected Table Output", steps[0] );
  }
}
