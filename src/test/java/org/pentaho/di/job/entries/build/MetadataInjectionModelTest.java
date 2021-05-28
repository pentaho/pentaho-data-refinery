/*! ******************************************************************************
 *
 * Pentaho Community Edition Project: data-refinery-pdi-plugin
 *
 * Copyright (C) 2002-2018 by Hitachi Vantara : http://www.pentaho.com
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

package org.pentaho.di.job.entries.build;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
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
import org.pentaho.di.core.refinery.model.DswModeler;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobHopMeta;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entries.special.JobEntrySpecial;
import org.pentaho.di.job.entries.trans.JobEntryTrans;
import org.pentaho.di.job.entry.JobEntryCopy;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.metainject.MetaInjectMeta;
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

    Map<Class<?>, String> classMap = new HashMap<>();
    classMap.put( StepMetaInterface.class, "org.pentaho.di.trans.steps.metainject.MetaInjectMeta" );

    PluginRegistry.addPluginType( StepPluginType.getInstance() );

    StepPluginType.getInstance().handlePluginAnnotation(
      MetaInjectMeta.class,
      MetaInjectMeta.class.getAnnotation( org.pentaho.di.core.annotations.Step.class ),
      Collections.emptyList(), false, null );

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
    String dbDir = "./target/test-db/MetadataInjectionModelTest-H2-DB";
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
    trans.parameters = new String[0];
    trans.parameterFieldNames = new String[0];
    trans.parameterValues = new String[0];
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
    buildJobEntry.setModeler( new DswModeler() );
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
  @Ignore
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
