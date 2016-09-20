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

package org.pentaho.di.job.entries.publish;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.database.model.DatabaseAccessType;
import org.pentaho.database.model.DatabaseConnection;
import org.pentaho.di.core.database.BaseDatabaseMeta;
import org.pentaho.di.core.KettleClientEnvironment;
import org.pentaho.di.core.ProvidesDatabaseConnectionInformation;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.database.DatabaseInterface;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.refinery.DataRefineryConfig;
import org.pentaho.di.core.refinery.publish.agilebi.BiServerConnection;
import org.pentaho.di.core.refinery.publish.agilebi.ModelServerPublish;
import org.pentaho.di.core.refinery.publish.model.DataSourceAclModel;
import org.pentaho.di.core.refinery.publish.model.DataSourcePublishModel;
import org.pentaho.di.core.refinery.publish.util.PublishRestUtil;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobHopMeta;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entries.build.JobEntryBuildModel;
import org.pentaho.di.job.entry.JobEntryCopy;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.ui.job.entries.common.ConnectionValidator;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.matches;

/**
 * @author Rowell Belen
 */
public class JobEntryDatasourcePublishTest {

  private JobEntryDatasourcePublish jobEntryDatasourcePublish;
  private Job parentJob;
  private JobMeta jobMeta;
  private JobHopMeta jobHopMeta;
  private JobHopMeta jobHopMeta2;
  private JobEntryCopy jobEntryCopy;
  private JobEntryCopy jobEntryCopy2;
  private JobEntryInterface jobEntry;
  private JobEntryInterface jobEntry2;
  private JobEntryBuildModel jobEntryBuildModel;
  private DatabaseMeta databaseMeta;
  private Result result;
  private BiServerConnection biServerConnection;
  private ModelServerPublish modelServerPublish;
  private PublishRestUtil publishRestUtil;
  private DatabaseConnection databaseConnection;
  private ConnectionValidator connectionValidator;
  private ProvidesDatabaseConnectionInformation connectionInfo;

  @Before
  public void init() throws Exception {

    KettleClientEnvironment.init();
    parentJob = mock( Job.class );
    jobMeta = mock( JobMeta.class );
    jobHopMeta = mock( JobHopMeta.class );
    jobHopMeta2 = mock( JobHopMeta.class );
    jobEntryCopy = mock( JobEntryCopy.class );
    jobEntryCopy2 = mock( JobEntryCopy.class );
    jobEntry = mock( JobEntryInterface.class );
    jobEntry2 = mock( JobEntryInterface.class );
    jobEntryBuildModel = mock( JobEntryBuildModel.class );
    databaseMeta = mock( DatabaseMeta.class );
    connectionInfo = mock( ProvidesDatabaseConnectionInformation.class );
    result = mock( Result.class );
    biServerConnection = mock( BiServerConnection.class );
    modelServerPublish = mock( ModelServerPublish.class );
    publishRestUtil = mock( PublishRestUtil.class );
    connectionValidator = mock( ConnectionValidator.class );

    databaseConnection = new DatabaseConnection();
    databaseConnection.setId( UUID.randomUUID().toString() );

    jobEntryDatasourcePublish = new JobEntryDatasourcePublish();
  }

  @Test
  public void testDiscoverDatabaseMeta() throws Exception {

    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );

    when( datasourcePublishSpy.getParentJob() ).thenReturn( parentJob );
    when( parentJob.getJobMeta() ).thenReturn( jobMeta );

    // can't find build model entry
    doReturn( null ).when( datasourcePublishSpy ).findPrecedingBuildModelJobEntry( jobMeta, datasourcePublishSpy );
    assertNull( datasourcePublishSpy.discoverDatabaseMeta( jobMeta ) );

    doReturn( jobEntryBuildModel ).when( datasourcePublishSpy ).findPrecedingBuildModelJobEntry( jobMeta,
        datasourcePublishSpy );

    // can't find database meta
    when( jobEntryBuildModel.getConnectionInfo() ).thenReturn( connectionInfo );
    when( jobEntryBuildModel.clone() ).thenReturn( jobEntryBuildModel );
    when( connectionInfo.getDatabaseMeta() ).thenReturn( null );
    assertNull( datasourcePublishSpy.discoverDatabaseMeta( jobMeta ) );

    // found database meta
    when( jobEntryBuildModel.getConnectionInfo() ).thenReturn( connectionInfo );
    when( jobEntryBuildModel.getConnectionInfo().getDatabaseMeta() ).thenReturn( databaseMeta );
    assertEquals( datasourcePublishSpy.discoverDatabaseMeta( jobMeta ), databaseMeta );
  }

  @Test
  public void testGetXml() {
    DataSourcePublishModel model = new DataSourcePublishModel();
    model.setModelName( "myLogicalModel" );
    model.setOverride( true );

    BiServerConnection biServerModel = new BiServerConnection();
    biServerModel.setName( "default" );
    biServerModel.setUrl( "http://localhost:8080/pentaho" );
    biServerModel.setUserId( "admin" );
    biServerModel.setPassword( "password" );
    model.setBiServerConnection( biServerModel );

    jobEntryDatasourcePublish.setDataSourcePublishModel( model );
    assertTrue( StringUtils.contains( jobEntryDatasourcePublish.getXML(),
        "<logical_model>myLogicalModel</logical_model>" ) );
    assertTrue( StringUtils.contains( jobEntryDatasourcePublish.getXML(), "<override>Y</override>" ) );
    assertTrue( StringUtils.contains( jobEntryDatasourcePublish.getXML(), "<ba_server_name>default</ba_server_name>" ) );

    model.setBiServerConnection( null );
    assertFalse( StringUtils.contains( jobEntryDatasourcePublish.getXML(), "<ba_server_name>default</ba_server_name>" ) );

    jobEntryDatasourcePublish.setDataSourcePublishModel( null );
    assertFalse( StringUtils.contains( jobEntryDatasourcePublish.getXML(),
        "<logical_model>myLogicalModel</logical_model>" ) );
  }

  @Test
  public void testLoadXml() throws Exception {
    jobEntryDatasourcePublish.loadXML( null, null, null, null, null );
  }

  @Test
  public void testLoadRep() throws Exception {

    ObjectId id_jobentry = mock( ObjectId.class );
    Repository rep = mock( Repository.class );

    jobEntryDatasourcePublish.loadRep( rep, null, id_jobentry, null, null );
    verify( rep ).getJobEntryAttributeString( id_jobentry, JobEntryDatasourcePublish.Fields.BASERVER_NAME );
    verify( rep ).getJobEntryAttributeString( id_jobentry, JobEntryDatasourcePublish.Fields.BASERVER_URL );
    verify( rep ).getJobEntryAttributeString( id_jobentry, JobEntryDatasourcePublish.Fields.BASERVER_USERID );
    verify( rep ).getJobEntryAttributeString( id_jobentry, JobEntryDatasourcePublish.Fields.BASERVER_PASSWORD );
    verify( rep ).getJobEntryAttributeString( id_jobentry, JobEntryDatasourcePublish.Fields.LOGICAL_MODEL );
    verify( rep ).getJobEntryAttributeBoolean( id_jobentry, JobEntryDatasourcePublish.Fields.OVERRIDE );
  }

  @Test
  public void testSaveRep() throws Exception {

    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );
    DataSourcePublishModel dataSourcePublishModel = new DataSourcePublishModel();
    dataSourcePublishModel.setBiServerConnection( new BiServerConnection() );
    dataSourcePublishModel.setAccessType( "User" );
    dataSourcePublishModel.setUserOrRole( "batman" );
    datasourcePublishSpy.setDataSourcePublishModel( dataSourcePublishModel );

    ObjectId id_jobentry = mock( ObjectId.class );
    Repository rep = mock( Repository.class );

    doReturn( id_jobentry ).when( datasourcePublishSpy ).getObjectId();

    datasourcePublishSpy.saveRep( rep, null, id_jobentry );

    verify( rep ).saveJobEntryAttribute( id_jobentry, id_jobentry, JobEntryDatasourcePublish.Fields.BASERVER_NAME, "" );
    verify( rep ).saveJobEntryAttribute(
        id_jobentry, id_jobentry, JobEntryDatasourcePublish.Fields.ACL_ACCESS_TYPE, "User" );
    verify( rep ).saveJobEntryAttribute(
        id_jobentry, id_jobentry, JobEntryDatasourcePublish.Fields.ACL_USER_OR_ROLE, "batman" );
  }

  @Test( expected = KettleException.class )
  public void testPublishDatabaseMetaPublishError() throws Exception {
    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );

    when( modelServerPublish.connectionNameExists( anyString() ) ).thenReturn( databaseConnection );
    final DatabaseInterface databaseInterface = mock( DatabaseInterface.class );
    when( databaseMeta.getDatabaseInterface() ).thenReturn( databaseInterface );
    when( databaseInterface.getPluginId() ).thenReturn( "Oracle" );
    datasourcePublishSpy.publishDatabaseMeta( modelServerPublish, databaseMeta, false );
  }

  @Test
  public void testCannotPublishKettleThinLocal() throws Exception {
    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );

    when( modelServerPublish.connectionNameExists( anyString() ) ).thenReturn( databaseConnection );
    when( modelServerPublish.publishDataSource( anyBoolean(), anyString() ) ).thenReturn( true );
    final DatabaseInterface databaseInterface = mock( DatabaseInterface.class );
    when( databaseMeta.getDatabaseInterface() ).thenReturn( databaseInterface );
    when( databaseInterface.getPluginId() ).thenReturn( "KettleThin" );
    Map<String, String> extraOptions = new HashMap<>();
    extraOptions.put( "KettleThin.local", "true" );
    when( databaseMeta.getExtraOptions() ).thenReturn( extraOptions );
    try {
      datasourcePublishSpy.publishDatabaseMeta( modelServerPublish, databaseMeta, false );
      fail( "expected Exception" );
    } catch ( KettleException e ) {
      assertEquals( "We weren't able to publish the requested Pentaho Data Service connection. Make sure you are connected to a Pentaho Repository.",
        e.getMessage().trim() );
    }
  }

  @Test
  public void testCanPublishKettleThinRepository() throws Exception {
    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );

    when( modelServerPublish.connectionNameExists( anyString() ) ).thenReturn( databaseConnection );
    when( modelServerPublish.publishDataSource( anyBoolean(), anyString() ) ).thenReturn( true );
    final DatabaseInterface databaseInterface = mock( DatabaseInterface.class );
    Properties props = new Properties();
    when( databaseInterface.getAttributes() ).thenReturn( props );
    when( databaseMeta.getDatabaseInterface() ).thenReturn( databaseInterface );
    when( databaseInterface.getPluginId() ).thenReturn( "KettleThin" );
    when( databaseMeta.getExtraOptions() ).thenReturn( new HashMap<String, String>() );
    try {
      assertTrue( databaseMeta.getDatabaseInterface().getAttributes()
          .getProperty( BaseDatabaseMeta.ATTRIBUTE_FORCE_IDENTIFIERS_TO_LOWERCASE ) == null );
      datasourcePublishSpy.publishDatabaseMeta( modelServerPublish, databaseMeta, false );
      assertFalse( databaseMeta.isForcingIdentifiersToLowerCase() );
    } catch ( KettleException e ) {
      fail( "did not expect exception " + e.getMessage() );
    }
  }

  @Test
  public void testPublishDatabaseMetaPublishSuccess() throws Exception {
    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );

    when( modelServerPublish.connectionNameExists( anyString() ) ).thenReturn( databaseConnection );
    when( modelServerPublish.publishDataSource( anyBoolean(), anyString() ) ).thenReturn( true );
    final DatabaseInterface databaseInterface = mock( DatabaseInterface.class );
    when( databaseMeta.getDatabaseInterface() ).thenReturn( databaseInterface );
    when( databaseInterface.getPluginId() ).thenReturn( "Oracle" );
    datasourcePublishSpy.publishDatabaseMeta( modelServerPublish, databaseMeta, false );
  }

  @Test
  public void testPublishDatabaseMetaPublishForceOverrideUpdate() throws Exception {
    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );

    when( modelServerPublish.connectionNameExists( anyString() ) ).thenReturn( databaseConnection );
    when( modelServerPublish.publishDataSource( anyBoolean(), anyString() ) ).thenReturn( true );
    final DatabaseInterface databaseInterface = mock( DatabaseInterface.class );
    when( databaseMeta.getDatabaseInterface() ).thenReturn( databaseInterface );
    when( databaseInterface.getPluginId() ).thenReturn( "Oracle" );
    datasourcePublishSpy.publishDatabaseMeta( modelServerPublish, databaseMeta, true );
  }

  @Test
  public void testPublishDatabaseMetaPublishForceOverrideAdd() throws Exception {
    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );

    when( modelServerPublish.connectionNameExists( anyString() ) ).thenReturn( null );
    when( modelServerPublish.publishDataSource( anyBoolean(), anyString() ) ).thenReturn( true );
    final DatabaseInterface databaseInterface = mock( DatabaseInterface.class );
    when( databaseMeta.getDatabaseInterface() ).thenReturn( databaseInterface );
    when( databaseInterface.getPluginId() ).thenReturn( "Oracle" );
    datasourcePublishSpy.publishDatabaseMeta( modelServerPublish, databaseMeta, true );
  }

  @Test
  public void testPublishMetadataXmiError() throws Exception {
    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );

    // suppress log basic
    doNothing().when( datasourcePublishSpy ).logBasic( anyString() );

    when( datasourcePublishSpy.getParentJob() ).thenReturn( parentJob );
    when( parentJob.getVariable( "JobEntryBuildModel.XMI.logicalModel" ) ).thenReturn( null );

    DataSourcePublishModel model = new DataSourcePublishModel();
    model.setModelName( "logicalModel" );

    datasourcePublishSpy.setDataSourcePublishModel( model );

    datasourcePublishSpy.publishMetadataXmi( "logicalModel", modelServerPublish, false );
  }

  @Test( expected = KettleException.class )
  public void testPublishMetadataXmiFail() throws Exception {
    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );

    // suppress log basic
    doNothing().when( datasourcePublishSpy ).logBasic( anyString() );

    when( datasourcePublishSpy.getParentJob() ).thenReturn( parentJob );
    when( parentJob.getVariable( "JobEntryBuildModel.XMI.logicalModel" ) ).thenReturn( "<test></test>" );

    DataSourcePublishModel model = new DataSourcePublishModel();
    model.setModelName( "logicalModel" );
    model.setBiServerConnection( biServerConnection );

    datasourcePublishSpy.setDataSourcePublishModel( model );

    when( modelServerPublish.publishMetaDataFile( any( InputStream.class ), anyString() ) ).thenThrow(
        new KettleException() );

    datasourcePublishSpy.publishMetadataXmi( "logicalModel", modelServerPublish, false );
  }

  @Test
  public void testPublishMetadataXmi() throws Exception {
    final String modelName = "logicalModel";
    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );

    // suppress log basic
    doNothing().when( datasourcePublishSpy ).logBasic( anyString() );

    when( datasourcePublishSpy.getParentJob() ).thenReturn( parentJob );
    when( parentJob.getVariable( "JobEntryBuildModel.XMI.logicalModel" ) ).thenReturn( "<test></test>" );

    DataSourcePublishModel model = new DataSourcePublishModel();
    model.setModelName( modelName );
    model.setBiServerConnection( biServerConnection );

    datasourcePublishSpy.setDataSourcePublishModel( model );
    when( modelServerPublish.publishMetaDataFile( any( InputStream.class ), matches( modelName ) ) ).thenReturn(
        ModelServerPublish.PUBLISH_SUCCESS );

    datasourcePublishSpy.publishMetadataXmi( modelName, modelServerPublish, false );
  }

  @Test
  public void testExecuteErrorMissingDatabaseMeta() throws Exception {

    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );

    when( datasourcePublishSpy.getParentJob() ).thenReturn( parentJob );
    when( datasourcePublishSpy.getPublishRestUtil() ).thenReturn( publishRestUtil );

    // suppress log basic
    doNothing().when( datasourcePublishSpy ).logBasic( anyString() );

    // return mocks
    when( datasourcePublishSpy.getModelServerPublish() ).thenReturn( modelServerPublish );

    doReturn( null ).when( datasourcePublishSpy ).discoverDatabaseMeta( any( JobMeta.class ) );

    DataSourcePublishModel model = new DataSourcePublishModel();
    model.setModelName( "logicalModel" );
    model.setBiServerConnection( biServerConnection );

    datasourcePublishSpy.setDataSourcePublishModel( model );

    datasourcePublishSpy.execute( result, 0 );
  }

  @Test
  public void testExecuteFailPentahoServer() throws Exception {

    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );

    doReturn( false ).when( publishRestUtil ).isPentahoServer( any( BiServerConnection.class ) );

    when( datasourcePublishSpy.getPublishRestUtil() ).thenReturn( publishRestUtil );

    DataSourcePublishModel model = new DataSourcePublishModel();
    model.setBiServerConnection( biServerConnection );
    datasourcePublishSpy.setDataSourcePublishModel( model );
    datasourcePublishSpy.execute( result, 0 );

    verify( result ).setResult( false );
  }

  @Test
  public void testExecuteFailUserInfoProvided() throws Exception {

    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );

    doReturn( true ).when( publishRestUtil ).isPentahoServer( any( BiServerConnection.class ) );
    doReturn( false ).when( publishRestUtil ).isUserInfoProvided( any( BiServerConnection.class ) );

    when( datasourcePublishSpy.getPublishRestUtil() ).thenReturn( publishRestUtil );

    DataSourcePublishModel model = new DataSourcePublishModel();
    model.setBiServerConnection( biServerConnection );
    datasourcePublishSpy.setDataSourcePublishModel( model );
    datasourcePublishSpy.execute( result, 0 );

    verify( result ).setResult( false );
  }

  @Test
  public void testExecuteFailUnauthenticatedUser() throws Exception {

    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );

    doReturn( true ).when( publishRestUtil ).isPentahoServer( any( BiServerConnection.class ) );
    doReturn( true ).when( publishRestUtil ).isUserInfoProvided( any( BiServerConnection.class ) );
    doReturn( true ).when( publishRestUtil ).isUnauthenticatedUser( any( BiServerConnection.class ) );

    when( datasourcePublishSpy.getPublishRestUtil() ).thenReturn( publishRestUtil );

    DataSourcePublishModel model = new DataSourcePublishModel();
    model.setBiServerConnection( biServerConnection );
    datasourcePublishSpy.setDataSourcePublishModel( model );
    datasourcePublishSpy.execute( result, 0 );

    verify( result ).setResult( false );
  }

  @Test
  public void testExecuteFailEarly() throws Exception {

    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );

    // suppress log basic
    when( datasourcePublishSpy.getPublishRestUtil() ).thenReturn( publishRestUtil );
    doReturn( true ).when( publishRestUtil ).isUnauthenticatedUser( any( BiServerConnection.class ) );

    // return mocks
    when( datasourcePublishSpy.getModelServerPublish() ).thenReturn( modelServerPublish );
    when( datasourcePublishSpy.getPublishRestUtil() ).thenReturn( publishRestUtil );

    DataSourcePublishModel model = new DataSourcePublishModel();
    model.setBiServerConnection( biServerConnection );

    datasourcePublishSpy.setDataSourcePublishModel( model );

    datasourcePublishSpy.execute( result, 0 );
    verify( result ).setResult( false );
  }

  @Test
  public void testExecute() throws Exception {

    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );

    when( datasourcePublishSpy.getParentJob() ).thenReturn( parentJob );

    // suppress log basic
    doNothing().when( datasourcePublishSpy ).logBasic( anyString() );

    // return mocks
    when( datasourcePublishSpy.getModelServerPublish() ).thenReturn( modelServerPublish );
    when( datasourcePublishSpy
        .getConnectionValidator( any( BiServerConnection.class ) ) ).thenReturn( connectionValidator );

    doReturn( true ).when( connectionValidator ).isPentahoServer();
    doReturn( true ).when( connectionValidator ).isUserInfoProvided();
    doReturn( true ).when( connectionValidator ).canConnect();

    doReturn( databaseMeta ).when( datasourcePublishSpy ).discoverDatabaseMeta( any( JobMeta.class ) );

    DataSourcePublishModel model = new DataSourcePublishModel();
    model.setModelName( "logicalModel" );
    model.setBiServerConnection( biServerConnection );
    model.setAccessType( "UsEr" );
    model.setUserOrRole( "suzy" );

    datasourcePublishSpy.setDataSourcePublishModel( model );

    doNothing().when( datasourcePublishSpy ).publishDatabaseMeta( any( ModelServerPublish.class ),
        any( DatabaseMeta.class ), anyBoolean() );
    doNothing().when( datasourcePublishSpy ).publishMetadataXmi( anyString(), any( ModelServerPublish.class ),
        anyBoolean() );
    doNothing().when( datasourcePublishSpy ).publishMondrianSchema( anyString(), any( ModelServerPublish.class ),
        anyBoolean() );

    datasourcePublishSpy.execute( result, 0 );
    verify( modelServerPublish ).setAclModel( argThat( matchesUser( "suzy" ) ) );
    verify( datasourcePublishSpy ).publishDatabaseMeta( any( ModelServerPublish.class ), any( DatabaseMeta.class ),
        anyBoolean() );

    model.setAccessType( "role" );
    model.setUserOrRole( "" );
    Result r1 = datasourcePublishSpy.execute( new Result( 0 ), 0 );
    assertTrue( r1.getNrErrors() == 1 );
    assertFalse( r1.getResult() ); // Failed

    model.setAccessType( "user" );
    model.setUserOrRole( null );
    Result r2 = datasourcePublishSpy.execute( new Result( 0 ), 0 );
    assertTrue( r1.getNrErrors() == 1 );
    assertFalse( r2.getResult() ); // Failed

    model.setAccessType( "user" );
    model.setUserOrRole( "admin" );
    Result r3 = datasourcePublishSpy.execute( new Result( 0 ), 0 );
    assertTrue( r3.getResult() ); // Success
  }

  @Test
  public void testRollback() throws Exception {

    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );

    when( datasourcePublishSpy.getParentJob() ).thenReturn( parentJob );

    // suppress log basic
    doNothing().when( datasourcePublishSpy ).logBasic( anyString() );

    // return mocks
    when( datasourcePublishSpy.getModelServerPublish() ).thenReturn( modelServerPublish );
    when( datasourcePublishSpy
        .getConnectionValidator( any( BiServerConnection.class ) ) ).thenReturn( connectionValidator );

    doReturn( true ).when( connectionValidator ).isPentahoServer();
    doReturn( true ).when( connectionValidator ).isUserInfoProvided();
    doReturn( true ).when( connectionValidator ).canConnect();

    doReturn( databaseMeta ).when( datasourcePublishSpy ).discoverDatabaseMeta( any( JobMeta.class ) );

    DataSourcePublishModel model = new DataSourcePublishModel();
    model.setModelName( "logicalModel" );
    model.setBiServerConnection( biServerConnection );

    datasourcePublishSpy.setDataSourcePublishModel( model );

    doNothing().when( datasourcePublishSpy ).deleteDatabaseMeta( any( ModelServerPublish.class ),
        any( DatabaseMeta.class ) );
    doNothing().when( datasourcePublishSpy ).deleteXMI( any( ModelServerPublish.class ), anyString(),
        anyString() );
    doNothing().when( datasourcePublishSpy ).publishDatabaseMeta( any( ModelServerPublish.class ),
        any( DatabaseMeta.class ), anyBoolean() );
    doNothing().when( datasourcePublishSpy ).publishMetadataXmi( anyString(), any( ModelServerPublish.class ),
        anyBoolean() );
    doThrow( new KettleException() ).when( datasourcePublishSpy ).publishMondrianSchema( anyString(),
        any( ModelServerPublish.class ), anyBoolean() );

    model.setAccessType( "user" );
    model.setUserOrRole( "admin" );
    Result r3 = datasourcePublishSpy.execute( new Result( 0 ), 0 );
    assertTrue( !r3.getResult() ); // Success
    verify( datasourcePublishSpy, times( 1 ) ).deleteDatabaseMeta( any( ModelServerPublish.class ),
        any( DatabaseMeta.class ) );
    verify( datasourcePublishSpy, times( 1 ) ).deleteXMI( any( ModelServerPublish.class ), anyString(),
        anyString() );
  }

  @Test
  public void testExecuteDoubleSlash() throws Exception {
    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );
    when( datasourcePublishSpy.getParentJob() ).thenReturn( parentJob );
    // suppress log basic
    doNothing().when( datasourcePublishSpy ).logBasic( anyString() );

    // return mocks
    when( datasourcePublishSpy.getModelServerPublish() ).thenReturn( modelServerPublish );
    when( datasourcePublishSpy
        .getConnectionValidator( any( BiServerConnection.class ) ) ).thenReturn( connectionValidator );

    doReturn( true ).when( connectionValidator ).isPentahoServer();
    doReturn( true ).when( connectionValidator ).isUserInfoProvided();

    doReturn( databaseMeta ).when( datasourcePublishSpy ).discoverDatabaseMeta( any( JobMeta.class ) );

    BiServerConnection biServerConnection = new BiServerConnection();
    biServerConnection.setUrl( "http://localhost:8080/pentaho//" );
    DataSourcePublishModel model = new DataSourcePublishModel();
    model.setModelName( "logicalModel" );
    model.setBiServerConnection( biServerConnection );
    model.setAccessType( "UsEr" );
    model.setUserOrRole( "suzy" );

    datasourcePublishSpy.setDataSourcePublishModel( model );

    doNothing().when( datasourcePublishSpy ).publishDatabaseMeta( any( ModelServerPublish.class ),
        any( DatabaseMeta.class ), anyBoolean() );
    doNothing().when( datasourcePublishSpy ).publishMetadataXmi( anyString(), any( ModelServerPublish.class ),
        anyBoolean() );
    doNothing().when( datasourcePublishSpy ).publishMondrianSchema( anyString(), any( ModelServerPublish.class ),
        anyBoolean() );

    assertTrue( biServerConnection.getUrl().endsWith( "//" ) );

    datasourcePublishSpy.execute( result, 0 );
    verify( modelServerPublish ).setAclModel( argThat( matchesUser( "suzy" ) ) );
    verify( datasourcePublishSpy ).publishDatabaseMeta( any( ModelServerPublish.class ), any( DatabaseMeta.class ),
        anyBoolean() );
    assertFalse( biServerConnection.getUrl().endsWith( "//" ) );
  }

  private Matcher<DataSourceAclModel> matchesUser( final String userName ) {
    return new BaseMatcher<DataSourceAclModel>() {
      @Override public boolean matches( final Object item ) {
        DataSourceAclModel acl = (DataSourceAclModel) item;
        return acl.getUsers().size() == 1
            && acl.getUsers().get( 0 ).equals( userName )
            && acl.getRoles() == null;
      }

      @Override public void describeTo( final Description description ) {

      }
    };
  }

  @Test
  public void testExecuteError() throws Exception {

    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );

    when( datasourcePublishSpy.getParentJob() ).thenReturn( parentJob );

    // suppress log basic
    doNothing().when( datasourcePublishSpy ).logBasic( anyString() );

    // return mocks
    when( datasourcePublishSpy.getModelServerPublish() ).thenReturn( modelServerPublish );
    when( datasourcePublishSpy
        .getConnectionValidator( any( BiServerConnection.class ) ) ).thenReturn( connectionValidator );
    doReturn( databaseConnection ).when( modelServerPublish ).connectionNameExists( anyString() );

    doReturn( true ).when( connectionValidator ).isPentahoServer();
    doReturn( true ).when( connectionValidator ).isUserInfoProvided();
    doReturn( true ).when( connectionValidator ).canConnect();

    doReturn( databaseMeta ).when( datasourcePublishSpy ).discoverDatabaseMeta( any( JobMeta.class ) );

    DataSourcePublishModel model = new DataSourcePublishModel();
    model.setModelName( "logicalModel" );
    model.setBiServerConnection( biServerConnection );
    model.setOverride( false );

    datasourcePublishSpy.setDataSourcePublishModel( model );

    datasourcePublishSpy.execute( result, 0 );
    assertEquals( result.getResult(), false );
  }

  @Test
  public void testExecuteJNDIFail() throws Exception {

    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );

    when( datasourcePublishSpy.getParentJob() ).thenReturn( parentJob );

    // suppress log basic
    doNothing().when( datasourcePublishSpy ).logBasic( anyString() );

    // return mocks
    when( datasourcePublishSpy.getModelServerPublish() ).thenReturn( modelServerPublish );
    when( datasourcePublishSpy
        .getConnectionValidator( any( BiServerConnection.class ) ) ).thenReturn( connectionValidator );

    doReturn( true ).when( connectionValidator ).isPentahoServer();
    doReturn( true ).when( connectionValidator ).isUserInfoProvided();
    doReturn( true ).when( connectionValidator ).canConnect();

    doReturn( databaseMeta ).when( datasourcePublishSpy ).discoverDatabaseMeta( any( JobMeta.class ) );
    when( databaseMeta.getAccessType() ).thenReturn( DatabaseAccessType.JNDI.ordinal() ); // not allowed

    DataSourcePublishModel model = new DataSourcePublishModel();
    model.setModelName( "logicalModel" );
    model.setBiServerConnection( biServerConnection );

    datasourcePublishSpy.setDataSourcePublishModel( model );

    datasourcePublishSpy.execute( result, 0 );
    assertEquals( result.getResult(), false );
    verify( modelServerPublish, times( 0 ) ).connectionNameExists( anyString() ); // statement not reached
  }

  @Test
  public void testDiscoverLogicalModel() throws Exception {
    JobEntryDatasourcePublish spy = spy( jobEntryDatasourcePublish );

    doReturn( jobEntryBuildModel ).when( spy ).findPrecedingBuildModelJobEntry( jobMeta, spy );
    when( jobEntryBuildModel.getModelName() ).thenReturn( "logical model" );

    String logicalModel = spy.discoverModelName( jobMeta );
    assertTrue( logicalModel.equalsIgnoreCase( "logical model" ) );
  }

  @Test
  public void testGetPreviousJobEntry() throws KettleException {

    JobEntryDatasourcePublish spy = spy( jobEntryDatasourcePublish );

    JobEntryInterface previousJobEntry = spy.getPreviousJobEntry( jobMeta, jobEntryDatasourcePublish );
    assertNull( previousJobEntry );

    // empty hops
    List<JobHopMeta> jobHops = new ArrayList<JobHopMeta>();
    when( jobMeta.getJobhops() ).thenReturn( jobHops ); // empty
    previousJobEntry = spy.getPreviousJobEntry( jobMeta, jobEntryDatasourcePublish );
    assertNull( previousJobEntry );

    // found
    when( jobHopMeta.getToEntry() ).thenReturn( jobEntryCopy );
    when( jobHopMeta.getFromEntry() ).thenReturn( jobEntryCopy );
    when( jobHopMeta.isEnabled() ).thenReturn( true );
    when( jobEntryCopy.getEntry() ).thenReturn( jobEntry );
    jobHops.add( jobHopMeta );
    when( jobMeta.getJobhops() ).thenReturn( jobHops );
    previousJobEntry = spy.getPreviousJobEntry( jobMeta, jobEntry );
    assertTrue( previousJobEntry == jobEntry );

    // not found
    previousJobEntry = spy.getPreviousJobEntry( jobMeta, jobEntryDatasourcePublish );
    assertNull( previousJobEntry );
  }

  @Test
  public void testGetPreviousJobEntries() throws KettleException {

    JobEntryDatasourcePublish spy = spy( jobEntryDatasourcePublish );

    JobEntryInterface previousJobEntry = spy.getPreviousJobEntry( jobMeta, jobEntryDatasourcePublish );
    assertNull( previousJobEntry );

    // empty hops
    List<JobHopMeta> jobHops = new ArrayList<JobHopMeta>();
    when( jobMeta.getJobhops() ).thenReturn( jobHops ); // empty
    previousJobEntry = spy.getPreviousJobEntry( jobMeta, jobEntryDatasourcePublish );
    assertNull( previousJobEntry );

    // found
    when( jobHopMeta.getToEntry() ).thenReturn( jobEntryCopy );
    when( jobHopMeta.getFromEntry() ).thenReturn( jobEntryCopy );
    when( jobHopMeta.isEnabled() ).thenReturn( false );
    when( jobEntryCopy.getEntry() ).thenReturn( jobEntry );
    jobHops.add( jobHopMeta );
    when( jobHopMeta2.getToEntry() ).thenReturn( jobEntryCopy );
    when( jobHopMeta2.getFromEntry() ).thenReturn( jobEntryCopy2 );
    when( jobHopMeta2.isEnabled() ).thenReturn( true );
    when( jobEntryCopy2.getEntry() ).thenReturn( jobEntry2 );
    jobHops.add( jobHopMeta2 );
    when( jobMeta.getJobhops() ).thenReturn( jobHops );
    previousJobEntry = spy.getPreviousJobEntry( jobMeta, jobEntry );
    assertTrue( previousJobEntry == jobEntry2 );

    // not found
    previousJobEntry = spy.getPreviousJobEntry( jobMeta, jobEntryDatasourcePublish );
    assertNull( previousJobEntry );
  }

  @Test
  public void testFindPrecedingBuildModelJobEntry() throws Exception {

    JobEntryDatasourcePublish spy = spy( jobEntryDatasourcePublish );

    doReturn( null ).when( spy ).getPreviousJobEntry( jobMeta, jobEntry );
    assertNull( spy.findPrecedingBuildModelJobEntry( jobMeta, jobEntry ) );

    doReturn( jobEntryBuildModel ).when( spy ).getPreviousJobEntry( jobMeta, jobEntry );
    assertEquals( spy.findPrecedingBuildModelJobEntry( jobMeta, jobEntry ), jobEntryBuildModel );

    doReturn( jobEntryDatasourcePublish ).when( spy ).getPreviousJobEntry( jobMeta, jobEntry );
    assertNull( spy.findPrecedingBuildModelJobEntry( jobMeta, jobEntry ) );
  }

  @Test
  public void testXmlRoundtripEmptyBIServerConnectionElements() throws Exception {
    // dialog assumes fields are never null
    DataSourcePublishModel model = new DataSourcePublishModel();
    BiServerConnection biServerConnection = new BiServerConnection();
    biServerConnection.setUrl( "" );
    biServerConnection.setUserId( "" );
    biServerConnection.setPassword( "" );
    model.setBiServerConnection( biServerConnection );
    jobEntryDatasourcePublish.setDataSourcePublishModel( model );
    final String entryXml = "<entry>" + jobEntryDatasourcePublish.getXML() + "</entry>";
    // load back
    jobEntryDatasourcePublish = new JobEntryDatasourcePublish();
    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Node entryNode = builder.parse( IOUtils.toInputStream( entryXml, "UTF-8" ) ).getDocumentElement();
    jobEntryDatasourcePublish.loadXML( entryNode, null, null, null, null );

    BiServerConnection bism = jobEntryDatasourcePublish.getDataSourcePublishModel().getBiServerConnection();
    assertNotNull( bism.getUrl() );
    assertNotNull( bism.getPassword() );
    assertNotNull( bism.getUserId() );
  }

  @Test
  public void testCheckDswId() {

    String result;
    JobEntryDatasourcePublish spy = spy( jobEntryDatasourcePublish );

    result = spy.checkDswId( "test.xmi" );
    assertEquals( result, "test.xmi" );

    spy.checkDswId( "test" );
    assertEquals( result, "test.xmi" );

    result = spy.checkDswId( "test.XMI" );
    assertEquals( result, "test.xmi" );
  }

  @Test( expected = KettleException.class )
  public void testPublishDswXmi() throws Exception {

    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );

    String xmiString = "<xmi></xmi>";
    String modelName = "MyModel";
    ModelServerPublish modelServerPublish = mock( ModelServerPublish.class );

    // publish not called
    when( datasourcePublishSpy.getParentJob() ).thenReturn( parentJob );
    when( parentJob.getVariable( "JobEntryBuildModel.XMI.MyModel" ) ).thenReturn( null );
    datasourcePublishSpy.publishDswXmi( modelName, modelServerPublish, true );
    verify( modelServerPublish, times( 0 ) ).publishDsw( any( InputStream.class ), anyString() );

    // publish, mock success
    when( parentJob.getVariable( "JobEntryBuildModel.XMI.MyModel" ) ).thenReturn( xmiString );
    when( datasourcePublishSpy.checkDswId( modelName ) ).thenReturn( "MyModel.xmi" );
    when( modelServerPublish.publishDsw( any( InputStream.class ), anyString() ) )
        .thenReturn( ModelServerPublish.PUBLISH_SUCCESS );
    datasourcePublishSpy.publishDswXmi( modelName, modelServerPublish, true );
    verify( modelServerPublish, times( 1 ) ).publishDsw( any( InputStream.class ), anyString() );

    // publish, mock error, throws exception
    when( modelServerPublish.publishDsw( any( InputStream.class ), anyString() ) )
        .thenReturn( ModelServerPublish.PUBLISH_FAILED );
    datasourcePublishSpy.publishDswXmi( modelName, modelServerPublish, true );
    verify( modelServerPublish, times( 1 ) ).publishDsw( any( InputStream.class ), anyString() );
  }

  @Test( expected = KettleException.class )
  public void testPublishMondrianSchema() throws Exception {

    DataRefineryConfig config = new DataRefineryConfig();
    assertNotNull( config );

    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );

    String datasource = "ds";
    String mondrianSchema = "<xml></xml>";
    String modelName = "MyModel";
    ModelServerPublish modelServerPublish = mock( ModelServerPublish.class );

    // publish not called
    when( datasourcePublishSpy.getParentJob() ).thenReturn( parentJob );
    when( parentJob.getVariable( "JobEntryBuildModel.Mondrian.Schema.MyModel" ) ).thenReturn( null );
    when( parentJob.getVariable( "JobEntryBuildModel.Mondrian.Datasource.MyModel" ) ).thenReturn( null );
    datasourcePublishSpy.publishMondrianSchema( modelName, modelServerPublish, true );
    verify( modelServerPublish, times( 0 ) )
        .publishMondrianSchema( any( InputStream.class ), anyString(), anyString(), anyBoolean() );

    // publish not called - missing datasource
    when( parentJob.getVariable( "JobEntryBuildModel.Mondrian.Schema.MyModel" ) ).thenReturn( modelName );
    when( parentJob.getVariable( "JobEntryBuildModel.Mondrian.Datasource.MyModel" ) ).thenReturn( null );
    datasourcePublishSpy.publishMondrianSchema( modelName, modelServerPublish, true );
    verify( modelServerPublish, times( 0 ) )
        .publishMondrianSchema( any( InputStream.class ), anyString(), anyString(), anyBoolean() );

    // publish not called - missing model
    when( parentJob.getVariable( "JobEntryBuildModel.Mondrian.Schema.MyModel" ) ).thenReturn( null );
    when( parentJob.getVariable( "JobEntryBuildModel.Mondrian.Datasource.MyModel" ) ).thenReturn( datasource );
    datasourcePublishSpy.publishMondrianSchema( modelName, modelServerPublish, true );
    verify( modelServerPublish, times( 0 ) )
        .publishMondrianSchema( any( InputStream.class ), anyString(), anyString(), anyBoolean() );


    // publish, mock success
    when( parentJob.getVariable( "JobEntryBuildModel.Mondrian.Schema.MyModel" ) ).thenReturn( mondrianSchema );
    when( parentJob.getVariable( "JobEntryBuildModel.Mondrian.Datasource.MyModel" ) ).thenReturn( datasource );
    when( modelServerPublish.publishMondrianSchema( any( InputStream.class ), anyString(), anyString(), anyBoolean() ) )
        .thenReturn( ModelServerPublish.PUBLISH_SUCCESS );
    datasourcePublishSpy.publishMondrianSchema( modelName, modelServerPublish, true );
    verify( modelServerPublish, times( 1 ) )
        .publishMondrianSchema( any( InputStream.class ), anyString(), anyString(), anyBoolean() );


    // publish, mock error, throws exception
    when( modelServerPublish.publishMondrianSchema( any( InputStream.class ), anyString(), anyString(), anyBoolean() ) )
        .thenReturn( ModelServerPublish.PUBLISH_FAILED );
    datasourcePublishSpy.publishMondrianSchema( modelName, modelServerPublish, true );
    verify( modelServerPublish, times( 1 ) )
        .publishMondrianSchema( any( InputStream.class ), anyString(), anyString(), anyBoolean() );
    datasourcePublishSpy.publishMondrianSchema( modelName, modelServerPublish, true );
    verify( modelServerPublish, times( 0 ) )
        .publishMondrianSchema( any( InputStream.class ), anyString(), anyString(), anyBoolean() );
  }

  @Test
  public void testInvalidAccessTypeThrowsException() throws Exception {
    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );

    when( datasourcePublishSpy
        .getConnectionValidator( any( BiServerConnection.class ) ) ).thenReturn( connectionValidator );


    DataSourcePublishModel model = new DataSourcePublishModel();
    model.setModelName( "myLogicalModel" );
    model.setOverride( true );
    model.setAccessType( "Other" );

    BiServerConnection biServerConnection1 = new BiServerConnection();
    biServerConnection1.setUserId( "a" );
    biServerConnection1.setPassword( "b" );
    model.setBiServerConnection( biServerConnection1 );

    datasourcePublishSpy.setDataSourcePublishModel( model );
    datasourcePublishSpy.execute( result, 0 );
    verify( datasourcePublishSpy ).logError( eq( "\nAccess Type 'other' not recognized\n" ), isA( KettleException.class ) );
  }

  @Test
  public void testEmptyAccessTypeDefaultsToEveryone() throws Exception {
    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );

    when( datasourcePublishSpy
        .getConnectionValidator( any( BiServerConnection.class ) ) ).thenReturn( connectionValidator );
    when( datasourcePublishSpy.getModelServerPublish() ).thenReturn( modelServerPublish );

    DataSourcePublishModel model = new DataSourcePublishModel();
    model.setModelName( "myLogicalModel" );
    model.setOverride( true );
    model.setAccessType( null );

    BiServerConnection biServerConnection1 = new BiServerConnection();
    biServerConnection1.setUserId( "a" );
    biServerConnection1.setPassword( "b" );
    model.setBiServerConnection( biServerConnection1 );

    datasourcePublishSpy.setDataSourcePublishModel( model );
    try {
      datasourcePublishSpy.execute( result, 0 );
    } catch ( Exception e ) {
      //expect an exception because this test doesn't set up all the mocks to get us through the end
      //only care about setting acl
    }
    verify( modelServerPublish ).setAclModel( argThat( matchesEveryoneAcl() ) );
  }

  @Test
  public void testSaveRepEmpty() throws Exception {

    JobEntryDatasourcePublish datasourcePublish = new JobEntryDatasourcePublish();

    ObjectId id_jobentry = mock( ObjectId.class );
    Repository rep = mock( Repository.class );

    // ok if no exception
    datasourcePublish.saveRep( rep, null, id_jobentry );
  }

  private Matcher<DataSourceAclModel> matchesEveryoneAcl() {
    return new BaseMatcher<DataSourceAclModel>() {
      @Override public boolean matches( final Object item ) {
        DataSourceAclModel aclModel = (DataSourceAclModel) item;
        return aclModel.getRoles() == null && aclModel.getUsers() == null;
      }

      @Override public void describeTo( final Description description ) {

      }
    };
  }
}
