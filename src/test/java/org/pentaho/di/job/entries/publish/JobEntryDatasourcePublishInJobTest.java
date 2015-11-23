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

package org.pentaho.di.job.entries.publish;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.di.core.KettleClientEnvironment;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.plugins.StepPluginType;
import org.pentaho.di.core.refinery.publish.agilebi.BiServerConnection;
import org.pentaho.di.core.refinery.publish.model.DataSourcePublishModel;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobHopMeta;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entries.special.JobEntrySpecial;
import org.pentaho.di.job.entries.success.JobEntrySuccess;
import org.pentaho.di.job.entry.JobEntryCopy;

/**
 * Tests the JobEntryDatasourcePublish by setting up a Job and running it.
 * 
 * @author Benny
 *
 */
public class JobEntryDatasourcePublishInJobTest {
  private Job job;
  private JobEntryDatasourcePublish publishJobEntry;
  private JobEntryCopy publishCopy;
  private DataSourcePublishModel model;
  private BiServerConnection serverModel;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    if ( !KettleClientEnvironment.isInitialized() ) {
      KettleClientEnvironment.init();
    }
    PluginRegistry.addPluginType( StepPluginType.getInstance() );
    PluginRegistry.init();
    if ( !Props.isInitialized() ) {
      Props.init( 0 );
    }
  }

  @Before
  public void setUp() throws Exception {
    model = new DataSourcePublishModel();
    serverModel = new BiServerConnection();
    model.setBiServerConnection( serverModel );

    // Job Setup
    job = new Job( null, new JobMeta() );
    // Add start job entry
    JobEntrySpecial start = new JobEntrySpecial( "START", true, false );
    JobEntryCopy startCopy = new JobEntryCopy( start );
    startCopy.setDrawn();
    job.getJobMeta().addJobEntry( startCopy );
    start.setParentJob( job );

    // Add Publish job entry
    publishJobEntry = new JobEntryDatasourcePublish();
    publishJobEntry.setName( "Publish Me" );
    publishJobEntry.setDataSourcePublishModel( model );
    publishCopy = new JobEntryCopy( publishJobEntry );
    publishCopy.setDrawn();
    job.getJobMeta().addJobEntry( publishCopy );
    publishJobEntry.setParentJob( job );

    JobHopMeta hop2 = new JobHopMeta( startCopy, publishCopy );
    job.getJobMeta().addJobHop( hop2 );
  }

  @After
  public void tearDown() throws Exception {

  }

  @Test
  public void testErrorHop() throws Exception {
    JobEntrySuccess jobEntrySuccess = new JobEntrySuccess();
    JobEntryCopy copy = new JobEntryCopy( jobEntrySuccess );
    copy.setDrawn();
    job.getJobMeta().addJobEntry( copy );
    jobEntrySuccess.setParentJob( job );

    // Create a error hop that leads to the job success
    JobHopMeta hop2 = new JobHopMeta( publishCopy, copy );
    hop2.setEvaluation( false );
    job.getJobMeta().addJobHop( hop2 );

    job.run();
    // Publish job entry will fail but it'll follow the error hop to the job success entry
    // and thus the whole job will succeed with no errors.
    assertTrue( job.getResult().getResult() );
    assertEquals( 0, job.getResult().getNrErrors() );

    // Now switch the hop to success so that the job fails immediately in the publish job entry
    hop2.setEvaluation( true );
    job.run();
    assertFalse( job.getResult().getResult() );
    assertEquals( 1, job.getResult().getNrErrors() );
  }

}
