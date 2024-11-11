/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.di.job.entries.publish;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.pentaho.database.model.DatabaseAccessType;
import org.pentaho.database.model.DatabaseConnection;
import org.pentaho.di.core.KettleClientEnvironment;
import org.pentaho.di.core.ProvidesDatabaseConnectionInformation;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.database.BaseDatabaseMeta;
import org.pentaho.di.core.database.DatabaseInterface;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.logging.LogChannelInterface;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
  private DatasourcePublishService publishService;
  private LogChannelInterface log;

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
    log = mock( LogChannelInterface.class );

    databaseConnection = new DatabaseConnection();
    databaseConnection.setId( UUID.randomUUID().toString() );

    publishService = new DatasourcePublishService( log );
    jobEntryDatasourcePublish = new JobEntryDatasourcePublish( spy( publishService ) );
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
    assertTrue(
      StringUtils.contains( jobEntryDatasourcePublish.getXML(), "<ba_server_name>default</ba_server_name>" ) );

    model.setBiServerConnection( null );
    assertFalse(
      StringUtils.contains( jobEntryDatasourcePublish.getXML(), "<ba_server_name>default</ba_server_name>" ) );

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
    DatasourcePublishService datasourcePublishServiceSpy = spy( publishService );

    when( modelServerPublish.connectionNameExists( any() ) ).thenReturn( databaseConnection );
    final DatabaseInterface databaseInterface = mock( DatabaseInterface.class );
    when( databaseMeta.getDatabaseInterface() ).thenReturn( databaseInterface );
    when( databaseInterface.getPluginId() ).thenReturn( "Oracle" );
    datasourcePublishServiceSpy.publishDatabaseMeta( modelServerPublish, databaseMeta, false );
  }

  @Test
  public void testCannotPublishKettleThinLocal() throws Exception {
    DatasourcePublishService datasourcePublishServiceSpy = spy( publishService );

    when( modelServerPublish.connectionNameExists( any() ) ).thenReturn( databaseConnection );
    when( modelServerPublish.publishDataSource( anyBoolean(), anyString() ) ).thenReturn( true );
    final DatabaseInterface databaseInterface = mock( DatabaseInterface.class );
    when( databaseMeta.getDatabaseInterface() ).thenReturn( databaseInterface );
    when( databaseInterface.getPluginId() ).thenReturn( "KettleThin" );
    Map<String, String> extraOptions = new HashMap<>();
    extraOptions.put( "KettleThin.local", "true" );
    when( databaseMeta.getExtraOptions() ).thenReturn( extraOptions );
    try {
      datasourcePublishServiceSpy.publishDatabaseMeta( modelServerPublish, databaseMeta, false );
      fail( "expected Exception" );
    } catch ( KettleException e ) {
      assertEquals(
        "We weren't able to publish the requested Pentaho Data Service connection. Make sure you are connected to a "
          + "Pentaho Repository.",
        e.getMessage().trim() );
    }
  }

  @Test
  public void testCanPublishKettleThinRepository() throws Exception {
    DatasourcePublishService datasourcePublishServiceSpy = spy( publishService );

    when( modelServerPublish.connectionNameExists( any() ) ).thenReturn( databaseConnection );
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
      datasourcePublishServiceSpy.publishDatabaseMeta( modelServerPublish, databaseMeta, false );
      assertFalse( databaseMeta.isForcingIdentifiersToLowerCase() );
    } catch ( KettleException e ) {
      fail( "did not expect exception " + e.getMessage() );
    }
  }

  @Test
  public void testPublishDatabaseMetaPublishSuccess() throws Exception {
    DatasourcePublishService datasourcePublishServiceSpy = spy( publishService );

    when( modelServerPublish.connectionNameExists( any() ) ).thenReturn( databaseConnection );
    when( modelServerPublish.publishDataSource( anyBoolean(), any() ) ).thenReturn( true );
    final DatabaseInterface databaseInterface = mock( DatabaseInterface.class );
    when( databaseMeta.getDatabaseInterface() ).thenReturn( databaseInterface );
    when( databaseInterface.getPluginId() ).thenReturn( "Oracle" );
    datasourcePublishServiceSpy.publishDatabaseMeta( modelServerPublish, databaseMeta, false );
  }

  @Test
  public void testPublishDatabaseMetaPublishForceOverrideUpdate() throws Exception {
    DatasourcePublishService datasourcePublishServiceSpy = spy( publishService );

    when( modelServerPublish.connectionNameExists( any() ) ).thenReturn( databaseConnection );
    when( modelServerPublish.publishDataSource( anyBoolean(), anyString() ) ).thenReturn( true );
    final DatabaseInterface databaseInterface = mock( DatabaseInterface.class );
    when( databaseMeta.getDatabaseInterface() ).thenReturn( databaseInterface );
    when( databaseInterface.getPluginId() ).thenReturn( "Oracle" );
    datasourcePublishServiceSpy.publishDatabaseMeta( modelServerPublish, databaseMeta, true );
  }

  @Test
  public void testPublishDatabaseMetaPublishForceOverrideAdd() throws Exception {
    DatasourcePublishService datasourcePublishServiceSpy = spy( publishService );

    when( modelServerPublish.connectionNameExists( any() ) ).thenReturn( null );
    when( modelServerPublish.publishDataSource( anyBoolean(), any() ) ).thenReturn( true );
    final DatabaseInterface databaseInterface = mock( DatabaseInterface.class );
    when( databaseMeta.getDatabaseInterface() ).thenReturn( databaseInterface );
    when( databaseInterface.getPluginId() ).thenReturn( "Oracle" );
    datasourcePublishServiceSpy.publishDatabaseMeta( modelServerPublish, databaseMeta, true );
  }

  @Test
  public void testPublishMetadataXmiError() throws Exception {
    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );
    DatasourcePublishService datasourcePublishServiceSpy = spy( publishService );

    // suppress log basic
    doNothing().when( datasourcePublishSpy ).logBasic( anyString() );

    when( datasourcePublishSpy.getParentJob() ).thenReturn( parentJob );
    when( parentJob.getVariable( "JobEntryBuildModel.XMI.logicalModel" ) ).thenReturn( null );

    DataSourcePublishModel model = new DataSourcePublishModel();
    model.setModelName( "logicalModel" );

    datasourcePublishSpy.setDataSourcePublishModel( model );
    String xmiString = datasourcePublishSpy.getParentJob().getVariable( "JobEntryBuildModel.XMI." + "logicalModel" );

    datasourcePublishServiceSpy.publishMetadataXmi( "logicalModel", xmiString, modelServerPublish, false );
  }

  @Test( expected = KettleException.class )
  public void testPublishMetadataXmiFail() throws Exception {
    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );
    DatasourcePublishService datasourcePublishServiceSpy = spy( publishService );

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

    String xmiString = datasourcePublishSpy.getParentJob().getVariable( "JobEntryBuildModel.XMI." + "logicalModel" );

    datasourcePublishServiceSpy.publishMetadataXmi( "logicalModel", xmiString, modelServerPublish, false );
  }

  @Test
  public void testPublishMetadataXmi() throws Exception {
    final String modelName = "logicalModel";
    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );
    DatasourcePublishService datasourcePublishServiceSpy = spy( publishService );

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

    String xmiString = datasourcePublishSpy.getParentJob().getVariable( "JobEntryBuildModel.XMI." + "logicalModel" );

    datasourcePublishServiceSpy.publishMetadataXmi( modelName, xmiString, modelServerPublish, false );
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

    DatasourcePublishService datasourcePublishServiceSpy = spy( publishService );
    JobEntryDatasourcePublish datasourcePublishSpy =
      spy( new JobEntryDatasourcePublish( datasourcePublishServiceSpy ) );

    when( datasourcePublishSpy.getParentJob() ).thenReturn( parentJob );
    when( parentJob.getJobMeta() ).thenReturn( jobMeta );

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

    doNothing().when( datasourcePublishServiceSpy ).publishDatabaseMeta( any( ModelServerPublish.class ),
      any( DatabaseMeta.class ), anyBoolean() );
    doNothing().when( datasourcePublishServiceSpy )
      .publishMetadataXmi( anyString(), anyString(), any( ModelServerPublish.class ),
        anyBoolean() );
    doNothing().when( datasourcePublishServiceSpy )
      .publishMondrianSchema( anyString(), anyString(), anyString(), any( ModelServerPublish.class ),
        anyBoolean() );

    datasourcePublishSpy.execute( result, 0 );
    verify( modelServerPublish ).setAclModel( argThat( matchesUser( "suzy" ) ) );
    verify( datasourcePublishServiceSpy )
      .publishDatabaseMeta( any( ModelServerPublish.class ), any( DatabaseMeta.class ),
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

    DatasourcePublishService datasourcePublishServiceSpy = spy( publishService );
    JobEntryDatasourcePublish datasourcePublishSpy =
      spy( new JobEntryDatasourcePublish( datasourcePublishServiceSpy ) );

    when( datasourcePublishSpy.getParentJob() ).thenReturn( parentJob );
    when( parentJob.getJobMeta() ).thenReturn( jobMeta );

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

    doNothing().when( datasourcePublishServiceSpy ).deleteDatabaseMeta( any( ModelServerPublish.class ),
      any( DatabaseMeta.class ) );
    doNothing().when( datasourcePublishServiceSpy ).deleteXMI( any( ModelServerPublish.class ), anyString(),
      any() );
    doNothing().when( datasourcePublishServiceSpy ).publishDatabaseMeta( any( ModelServerPublish.class ),
      any( DatabaseMeta.class ), anyBoolean() );
    doNothing().when( datasourcePublishServiceSpy )
      .publishMetadataXmi( anyString(), any(), any( ModelServerPublish.class ),
        anyBoolean() );
    doThrow( new KettleException() ).when( datasourcePublishServiceSpy )
      .publishMondrianSchema( anyString(), any(), any(),
        any( ModelServerPublish.class ), anyBoolean() );

    model.setAccessType( "user" );
    model.setUserOrRole( "admin" );
    Result r3 = datasourcePublishSpy.execute( new Result( 0 ), 0 );
    assertTrue( !r3.getResult() ); // Success
    verify( datasourcePublishServiceSpy, times( 1 ) ).deleteDatabaseMeta( any( ModelServerPublish.class ),
      any( DatabaseMeta.class ) );
    verify( datasourcePublishServiceSpy, times( 1 ) ).deleteXMI( any( ModelServerPublish.class ), anyString(),
      any() );
  }

  @Test
  public void testExecuteDoubleSlash() throws Exception {
    DatasourcePublishService datasourcePublishServiceSpy = spy( publishService );
    JobEntryDatasourcePublish datasourcePublishSpy =
      spy( new JobEntryDatasourcePublish( datasourcePublishServiceSpy ) );

    when( datasourcePublishSpy.getParentJob() ).thenReturn( parentJob );
    when( parentJob.getJobMeta() ).thenReturn( jobMeta );
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

    doNothing().when( datasourcePublishServiceSpy ).publishDatabaseMeta( any( ModelServerPublish.class ),
      any( DatabaseMeta.class ), anyBoolean() );
    doNothing().when( datasourcePublishServiceSpy )
      .publishMetadataXmi( anyString(), anyString(), any( ModelServerPublish.class ),
        anyBoolean() );
    doNothing().when( datasourcePublishServiceSpy )
      .publishMondrianSchema( anyString(), anyString(), anyString(), any( ModelServerPublish.class ),
        anyBoolean() );

    assertTrue( biServerConnection.getUrl().endsWith( "//" ) );

    datasourcePublishSpy.execute( result, 0 );
    verify( modelServerPublish ).setAclModel( argThat( matchesUser( "suzy" ) ) );
    verify( datasourcePublishServiceSpy )
      .publishDatabaseMeta( any( ModelServerPublish.class ), any( DatabaseMeta.class ),
        anyBoolean() );
    assertFalse( biServerConnection.getUrl().endsWith( "//" ) );
  }

  private ArgumentMatcher<DataSourceAclModel> matchesUser( final String userName ) {
    return new ArgumentMatcher<DataSourceAclModel>() {
      @Override public boolean matches( final DataSourceAclModel item ) {
        DataSourceAclModel acl = (DataSourceAclModel) item;
        return acl.getUsers().size() == 1
          && acl.getUsers().get( 0 ).equals( userName )
          && acl.getRoles() == null;
      }
    };
  }

  @Test
  public void testExecuteError() throws Exception {

    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );

    when( datasourcePublishSpy.getParentJob() ).thenReturn( parentJob );
    when( parentJob.getJobMeta() ).thenReturn( jobMeta );

    // suppress log basic
    doNothing().when( datasourcePublishSpy ).logBasic( anyString() );

    // return mocks
    when( datasourcePublishSpy.getModelServerPublish() ).thenReturn( modelServerPublish );
    when( datasourcePublishSpy
      .getConnectionValidator( any( BiServerConnection.class ) ) ).thenReturn( connectionValidator );
    doReturn( databaseConnection ).when( modelServerPublish ).connectionNameExists( any() );

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
    when( parentJob.getJobMeta() ).thenReturn( jobMeta );

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
    verify( modelServerPublish, times( 0 ) ).connectionNameExists( any() ); // statement not reached
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
    DatasourcePublishService datasourcePublishServiceSpy = spy( publishService );
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
    jobEntryDatasourcePublish = new JobEntryDatasourcePublish( datasourcePublishServiceSpy );
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
    DatasourcePublishService datasourcePublishServiceSpy = spy( publishService );

    result = datasourcePublishServiceSpy.checkDswId( "test.xmi" );
    assertEquals( result, "test.xmi" );

    datasourcePublishServiceSpy.checkDswId( "test" );
    assertEquals( result, "test.xmi" );

    result = datasourcePublishServiceSpy.checkDswId( "test.XMI" );
    assertEquals( result, "test.xmi" );
  }

  @Test( expected = KettleException.class )
  public void testPublishDswXmi() throws Exception {

    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );
    DatasourcePublishService datasourcePublishServiceSpy = spy( publishService );

    String xmiString = "<xmi></xmi>";
    String modelName = "MyModel";
    ModelServerPublish modelServerPublish = mock( ModelServerPublish.class );

    // publish not called
    when( datasourcePublishSpy.getParentJob() ).thenReturn( parentJob );
    when( parentJob.getVariable( "JobEntryBuildModel.XMI.MyModel" ) ).thenReturn( null );
    datasourcePublishServiceSpy.publishDswXmi( modelName, null, modelServerPublish, true );
    verify( modelServerPublish, times( 0 ) ).publishDsw( any( InputStream.class ), anyString() );

    // publish, mock success
    when( parentJob.getVariable( "JobEntryBuildModel.XMI.MyModel" ) ).thenReturn( xmiString );
    when( datasourcePublishServiceSpy.checkDswId( modelName ) ).thenReturn( "MyModel.xmi" );
    when( modelServerPublish.publishDsw( any( InputStream.class ), anyString() ) )
      .thenReturn( ModelServerPublish.PUBLISH_SUCCESS );
    datasourcePublishServiceSpy.publishDswXmi( modelName, xmiString, modelServerPublish, true );
    verify( modelServerPublish, times( 1 ) ).publishDsw( any( InputStream.class ), anyString() );

    // publish, mock error, throws exception
    when( modelServerPublish.publishDsw( any( InputStream.class ), anyString() ) )
      .thenReturn( ModelServerPublish.PUBLISH_FAILED );
    datasourcePublishServiceSpy.publishDswXmi( modelName, xmiString, modelServerPublish, true );
    verify( modelServerPublish, times( 1 ) ).publishDsw( any( InputStream.class ), anyString() );
  }

  @Test( expected = KettleException.class )
  public void testPublishMondrianSchema() throws Exception {
    DatasourcePublishService datasourcePublishServiceSpy = spy( publishService );

    JobEntryDatasourcePublish datasourcePublishSpy = spy( jobEntryDatasourcePublish );

    String datasource = "ds";
    String mondrianSchema = "<xml></xml>";
    String modelName = "MyModel";
    ModelServerPublish modelServerPublish = mock( ModelServerPublish.class );

    // publish not called
    when( datasourcePublishSpy.getParentJob() ).thenReturn( parentJob );
    when( parentJob.getVariable( "JobEntryBuildModel.Mondrian.Schema.MyModel" ) ).thenReturn( null );
    when( parentJob.getVariable( "JobEntryBuildModel.Mondrian.Datasource.MyModel" ) ).thenReturn( null );
    datasourcePublishServiceSpy.publishMondrianSchema( modelName, null, null, modelServerPublish, true );
    verify( modelServerPublish, times( 0 ) )
      .publishMondrianSchema( any( InputStream.class ), anyString(), anyString(), anyBoolean() );

    // publish not called - missing datasource
    when( parentJob.getVariable( "JobEntryBuildModel.Mondrian.Schema.MyModel" ) ).thenReturn( modelName );
    when( parentJob.getVariable( "JobEntryBuildModel.Mondrian.Datasource.MyModel" ) ).thenReturn( null );
    datasourcePublishServiceSpy.publishMondrianSchema( modelName, null, null, modelServerPublish, true );
    verify( modelServerPublish, times( 0 ) )
      .publishMondrianSchema( any( InputStream.class ), anyString(), anyString(), anyBoolean() );

    // publish not called - missing model
    when( parentJob.getVariable( "JobEntryBuildModel.Mondrian.Schema.MyModel" ) ).thenReturn( null );
    when( parentJob.getVariable( "JobEntryBuildModel.Mondrian.Datasource.MyModel" ) ).thenReturn( datasource );
    datasourcePublishServiceSpy.publishMondrianSchema( modelName, null, null, modelServerPublish, true );
    verify( modelServerPublish, times( 0 ) )
      .publishMondrianSchema( any( InputStream.class ), anyString(), anyString(), anyBoolean() );


    // publish, mock success
    when( parentJob.getVariable( "JobEntryBuildModel.Mondrian.Schema.MyModel" ) ).thenReturn( mondrianSchema );
    when( parentJob.getVariable( "JobEntryBuildModel.Mondrian.Datasource.MyModel" ) ).thenReturn( datasource );
    when( modelServerPublish.publishMondrianSchema( any( InputStream.class ), anyString(), anyString(), anyBoolean() ) )
      .thenReturn( ModelServerPublish.PUBLISH_SUCCESS );
    datasourcePublishServiceSpy
      .publishMondrianSchema( modelName, mondrianSchema, datasource, modelServerPublish, true );
    verify( modelServerPublish, times( 1 ) )
      .publishMondrianSchema( any( InputStream.class ), anyString(), anyString(), anyBoolean() );


    // publish, mock error, throws exception
    when( modelServerPublish.publishMondrianSchema( any( InputStream.class ), anyString(), anyString(), anyBoolean() ) )
      .thenReturn( ModelServerPublish.PUBLISH_FAILED );
    datasourcePublishServiceSpy
      .publishMondrianSchema( modelName, mondrianSchema, datasource, modelServerPublish, true );
    verify( modelServerPublish, times( 1 ) )
      .publishMondrianSchema( any( InputStream.class ), anyString(), anyString(), anyBoolean() );
    datasourcePublishServiceSpy
      .publishMondrianSchema( modelName, mondrianSchema, datasource, modelServerPublish, true );
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
    verify( datasourcePublishSpy )
      .logError( contains( "Access Type 'other' not recognized" ), isA( KettleException.class ) );
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

    JobEntryDatasourcePublish datasourcePublish = new JobEntryDatasourcePublish( null );

    ObjectId id_jobentry = mock( ObjectId.class );
    Repository rep = mock( Repository.class );

    // ok if no exception
    datasourcePublish.saveRep( rep, null, id_jobentry );
  }

  private ArgumentMatcher<DataSourceAclModel> matchesEveryoneAcl() {
    return new ArgumentMatcher<DataSourceAclModel>() {
      @Override public boolean matches( final DataSourceAclModel item ) {
        DataSourceAclModel aclModel = (DataSourceAclModel) item;
        return aclModel.getRoles() == null && aclModel.getUsers() == null;
      }
    };
  }
}
