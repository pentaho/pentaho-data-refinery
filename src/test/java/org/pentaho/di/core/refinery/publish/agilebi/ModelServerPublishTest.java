/*! ******************************************************************************
 *
 * Pentaho Community Edition Project: data-refinery-pdi-plugin
 *
 * Copyright (C) 2002-2024 by Hitachi Vantara : http://www.pentaho.com
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

package org.pentaho.di.core.refinery.publish.agilebi;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.pentaho.database.model.DatabaseAccessType;
import org.pentaho.database.model.DatabaseConnection;
import org.pentaho.database.model.IDatabaseType;
import org.pentaho.database.util.DatabaseTypeHelper;
import org.pentaho.di.core.database.DatabaseInterface;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.refinery.publish.model.DataSourceAclModel;
import org.pentaho.di.core.refinery.publish.util.JAXBUtils;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Rowell Belen
 */
public class ModelServerPublishTest {

  private ModelServerPublish modelServerPublish;
  private ModelServerPublish modelServerPublishSpy;
  private DatabaseMeta databaseMeta;
  private DatabaseInterface databaseInterface;
  private IDatabaseType databaseType;
  private DatabaseTypeHelper databaseTypeHelper;
  private DatabaseConnection databaseConnection;
  private BiServerConnection connection;
  private Properties attributes;
  private Client client;
  private ClientResponse clientResponse;
  private boolean overwrite;
  private LogChannelInterface logChannel;

  @Before
  public void setup() {

    overwrite = false;
    databaseMeta = mock( DatabaseMeta.class );
    connection = new BiServerConnection();
    connection.setUserId( "admin" );
    connection.setPassword( "password" );
    connection.setUrl( "http://localhost:8080/pentaho" );

    client = mock( Client.class );
    databaseInterface = mock( DatabaseInterface.class );
    databaseType = mock( IDatabaseType.class );
    databaseTypeHelper = mock( DatabaseTypeHelper.class );
    databaseConnection = mock( DatabaseConnection.class );
    attributes = mock( Properties.class );
    clientResponse = mock( ClientResponse.class );
    logChannel = mock( LogChannelInterface.class );

    modelServerPublish = new ModelServerPublish( logChannel );
    modelServerPublishSpy = spy( modelServerPublish );

    // mock responses
    doReturn( client ).when( modelServerPublishSpy ).getClient();

    // inject dependencies
    modelServerPublishSpy.setForceOverwrite( overwrite );
    modelServerPublishSpy.setBiServerConnection( connection );
    modelServerPublishSpy.setDatabaseMeta( databaseMeta );
  }

  @Test
  public void testConnectionnameExists() throws Exception {
    doCallRealMethod().when( modelServerPublishSpy ).getClient();

    DatabaseConnection dbConnection1 = new DatabaseConnection();
    dbConnection1.setName( "test" );
    String json = JAXBUtils.marshallToJson( dbConnection1 );

    // check null connection name
    assertNull( modelServerPublishSpy.connectionNameExists( null ) );

    // check null response
    doReturn( null ).when( modelServerPublishSpy ).httpGet( any( WebResource.Builder.class ) );
    assertNull( modelServerPublishSpy.connectionNameExists( "test" ) );

    // check invalid clientResponse
    doReturn( clientResponse ).when( modelServerPublishSpy ).httpGet( any( WebResource.Builder.class ) );
    assertNull( modelServerPublishSpy.connectionNameExists( "test" ) );

    // check invalid status
    when( clientResponse.getStatus() ).thenReturn( 404 );
    assertNull( modelServerPublishSpy.connectionNameExists( "test" ) );

    // check invalid payload
    when( clientResponse.getStatus() ).thenReturn( 200 );
    assertNull( modelServerPublishSpy.connectionNameExists( "test" ) );

    // valid
    when( clientResponse.getStatus() ).thenReturn( 200 );
    when( clientResponse.getEntity( String.class ) ).thenReturn( json );
    assertNotNull( modelServerPublishSpy.connectionNameExists( "test" ) );

    // valid
    String testStr = "クイズ";
    modelServerPublishSpy.connectionNameExists( testStr );
    verify( modelServerPublishSpy ).constructAbsoluteUrl( URLEncoder.encode( testStr, "UTF-8" ) );
  }

  @Test
  public void testConstructAbsoluteUrl() throws Exception {
    String connectionName = "local pentaho";
    String actual = modelServerPublishSpy.constructAbsoluteUrl( connectionName );
    String expected
      = "http://localhost:8080/pentaho/plugin/data-access/api/connection/getresponse?name=local%20pentaho";
    assertEquals( expected, actual );
  }

  @Test
  public void testPublishDataSource() throws Exception {

    doReturn( databaseInterface ).when( databaseMeta ).getDatabaseInterface();

    final HashMap<String, String> extraOptions = new HashMap<String, String>();
    final String username = "username";
    final String password = "password";
    final String dbName = "dbName";
    final String dbPort = "dbPort";
    final String hostname = "hostname";

    doReturn( extraOptions ).when( databaseMeta ).getExtraOptions();
    doReturn( username ).when( databaseMeta ).getUsername();
    doReturn( username ).when( databaseMeta ).environmentSubstitute( username );
    doReturn( password ).when( databaseMeta ).getPassword();
    doReturn( password ).when( databaseMeta ).environmentSubstitute( password );
    doReturn( dbName ).when( databaseInterface ).getDatabaseName();
    doReturn( dbName ).when( databaseMeta ).environmentSubstitute( dbName );
    doReturn( dbPort ).when( databaseMeta ).environmentSubstitute( dbPort );
    doReturn( hostname ).when( databaseMeta ).getHostname();
    doReturn( hostname ).when( databaseMeta ).environmentSubstitute( hostname );

    doReturn( attributes ).when( databaseInterface ).getAttributes();
    doReturn( dbPort ).when( attributes ).getProperty( "PORT_NUMBER" );
    doReturn( "Y" ).when( attributes ).getProperty( "FORCE_IDENTIFIERS_TO_LOWERCASE" );
    doReturn( "Y" ).when( attributes ).getProperty( "QUOTE_ALL_FIELDS" );
    doReturn( databaseType ).when( modelServerPublishSpy ).getDatabaseType( databaseInterface );

    try {
      modelServerPublishSpy.publishDataSource( true, "id" );
    } catch ( KettleException e ) {
      // Will hit this block
    }
    verify( modelServerPublishSpy ).updateConnection( argThat( new ArgumentMatcher<DatabaseConnection>() {
      @Override public boolean matches( Object o ) {
        DatabaseConnection db = (DatabaseConnection) o;
        return db.getUsername().equals( username )
            && db.getPassword().equals( password )
            && db.getDatabaseName().equals( dbName )
            && db.getDatabasePort().equals( dbPort )
            && db.getHostname().equals( hostname )
            && db.isForcingIdentifiersToLowerCase()
            && db.isQuoteAllFields()
            && db.getAccessType().equals( DatabaseAccessType.NATIVE )
            && db.getExtraOptions().equals( databaseMeta.getExtraOptions() )
            && db.getDatabaseType().equals( databaseType );
      }
    } ), anyBoolean() );

    doReturn( "N" ).when( attributes ).get( anyString() );
    try {
      modelServerPublishSpy.publishDataSource( false, "id" );
    } catch ( KettleException e ) {
      // Will hit this block
    }
  }

  @Test
  public void testPublishDataSourceEnvironmentSubstitute() throws Exception {

    doReturn( databaseInterface ).when( databaseMeta ).getDatabaseInterface();
    doReturn( "${USER_NAME}" ).when( databaseMeta ).getUsername();
    doReturn( "${USER_PASSWORD}" ).when( databaseMeta ).getPassword();
    doReturn( "${HOST_NAME}" ).when( databaseMeta ).getHostname();
    doReturn( "SubstitutedUser" ).when( databaseMeta ).environmentSubstitute( "${USER_NAME}" );
    doReturn( "SubstitutedHostName" ).when( databaseMeta ).environmentSubstitute( "${HOST_NAME}" );
    doReturn( "SubstitutedPassword" ).when( databaseMeta ).environmentSubstitute( "${USER_PASSWORD}" );
    doReturn( attributes ).when( databaseInterface ).getAttributes();
    doReturn( "${DB_PORT}" ).when( attributes ).getProperty( anyString() );
    doReturn( "8080" ).when( databaseMeta ).environmentSubstitute( "${DB_PORT}" );
    doReturn( databaseType ).when( modelServerPublishSpy ).getDatabaseType( databaseInterface );

    try {
      modelServerPublishSpy.publishDataSource( true, "id" );
    } catch ( KettleException e ) {
      // Will hit this block
    }
    verify( modelServerPublishSpy ).updateConnection( argThat( new ArgumentMatcher<DatabaseConnection>( ) {
        @Override
        public boolean matches( Object o ) {
          DatabaseConnection db = (DatabaseConnection) o;
          return db.getUsername( ).equals( "SubstitutedUser" )
                  && db.getHostname( ).equals( "SubstitutedHostName" )
                  && db.getPassword( ).equals( "SubstitutedPassword" )
                  && db.getDatabasePort( ).equals( "8080" );
        }
      } ), anyBoolean() );

  }



  @Test
  public void testGetDatabaseType() throws Exception {
    doReturn( "" ).when( databaseInterface ).getPluginId();
    assertNull( modelServerPublishSpy.getDatabaseType( databaseInterface ) );
  }

  @Test
  public void testCanPublishDatabaseTypesThatAreNotAvailableInThisClassloader() throws Exception {
    doReturn( "ORACLE" ).when( databaseInterface ).getPluginId();  //Oracle driver shouldn't be available for unit tests
    assertEquals( "Oracle", modelServerPublishSpy.getDatabaseType( databaseInterface ).getName() );
  }

  @Test
  public void testGetClient() throws Exception {

    doCallRealMethod().when( modelServerPublishSpy ).getClient();

    Client client1 = modelServerPublishSpy.getClient();
    Client client2 = modelServerPublishSpy.getClient();
    assertEquals( client1, client2 ); // assert same instance
  }

  @Test
  public void testHttpPost() throws Exception {
    modelServerPublishSpy.httpPost( mock( WebResource.Builder.class ) );
  }

  @Test
  public void testUpdateConnection() throws Exception {

    doCallRealMethod().when( modelServerPublishSpy ).getClient();

    // check null response
    doReturn( null ).when( modelServerPublishSpy ).httpPost( any( WebResource.Builder.class ) );
    boolean success = modelServerPublishSpy.updateConnection( databaseConnection, false );
    assertFalse( success );

    // check invalid clientResponse
    doReturn( clientResponse ).when( modelServerPublishSpy ).httpPost( any( WebResource.Builder.class ) );
    success = modelServerPublishSpy.updateConnection( databaseConnection, false );
    assertFalse( success );

    // check invalid status
    when( clientResponse.getStatus() ).thenReturn( 404 );
    success = modelServerPublishSpy.updateConnection( databaseConnection, false );
    assertFalse( success );

    // valid
    when( clientResponse.getStatus() ).thenReturn( 200 );
    success = modelServerPublishSpy.updateConnection( databaseConnection, false );
    assertTrue( success );
  }

  @Test
  public void testDeleteConnectionEncodesName() throws Exception {
    modelServerPublishSpy.deleteConnection( "some name" );
    verify( client ).resource( "http://localhost:8080/pentaho/plugin/data-access/api/connection/deletebyname?name=some+name" );
  }

  @Test
  public void testPublishMondrianSchema() throws Exception {

    InputStream mondrianFile = mock( InputStream.class );
    String catalogName = "Catalog";
    String datasourceInfo = "Test";
    WebResource.Builder builder = Mockito.mock( WebResource.Builder.class );

    doCallRealMethod().when( modelServerPublishSpy ).getClient();

    // check null response
    doReturn( null ).when( modelServerPublishSpy ).httpPost( any( WebResource.Builder.class ) );
    int status = modelServerPublishSpy.publishMondrianSchema( mondrianFile, catalogName, datasourceInfo, true );
    assertEquals( ModelServerPublish.PUBLISH_FAILED, status );

    // check invalid clientResponse
    doReturn( builder ).when( modelServerPublishSpy )
        .resourceBuilder(
          argThat( matchResource( "http://localhost:8080/pentaho/plugin/data-access/api/mondrian/postAnalysis" ) ),
          argThat( matchPart(
            "Datasource=Test;retainInlineAnnotations=true", mondrianFile, "Catalog", "true", "true" ) ) );
    doReturn( clientResponse ).when( modelServerPublishSpy ).httpPost( builder );
    status = modelServerPublishSpy.publishMondrianSchema( mondrianFile, catalogName, datasourceInfo, true );
    assertEquals( ModelServerPublish.PUBLISH_FAILED, status );

    // check invalid status
    when( clientResponse.getStatus() ).thenReturn( 404 );
    status = modelServerPublishSpy.publishMondrianSchema( mondrianFile, catalogName, datasourceInfo, true );
    assertEquals( ModelServerPublish.PUBLISH_FAILED, status );

    // valid status, invalid payload
    when( clientResponse.getStatus() ).thenReturn( 200 );
    when( clientResponse.getEntity( String.class ) ).thenReturn( "" );
    status = modelServerPublishSpy.publishMondrianSchema( mondrianFile, catalogName, datasourceInfo, true );
    assertEquals( ModelServerPublish.PUBLISH_FAILED, status );

    // valid status, catalog exists
    when( clientResponse.getEntity( String.class ) ).thenReturn( ModelServerPublish.PUBLISH_CATALOG_EXISTS + "" );
    status = modelServerPublishSpy.publishMondrianSchema( mondrianFile, catalogName, datasourceInfo, true );
    assertEquals( ModelServerPublish.PUBLISH_CATALOG_EXISTS, status );

    // success
    when( clientResponse.getEntity( String.class ) ).thenReturn( ModelServerPublish.PUBLISH_SUCCESS + "" );
    status = modelServerPublishSpy.publishMondrianSchema( mondrianFile, catalogName, datasourceInfo, true );
    assertEquals( ModelServerPublish.PUBLISH_SUCCESS, status );
  }

  private Matcher<FormDataMultiPart> matchPart(
      final String parameters, final Object inputStream, final String catalog,
      final String overwrite, final String xmlaEnabled ) {
    return new BaseMatcher<FormDataMultiPart>() {
      @Override public boolean matches( final Object item ) {
        FormDataMultiPart part = (FormDataMultiPart) item;
        List<BodyPart> bodyParts = part.getBodyParts();
        return bodyParts.size() == 5
          && bodyParts.get( 0 ).getEntity().equals( parameters )
          && bodyParts.get( 1 ).getEntity().equals( inputStream )
          && bodyParts.get( 2 ).getEntity().equals( catalog )
          && bodyParts.get( 3 ).getEntity().equals( overwrite )
          && bodyParts.get( 4 ).getEntity().equals( xmlaEnabled );
      }

      @Override public void describeTo( final Description description ) {

      }
    };
  }

  private Matcher<WebResource> matchResource( final String expectedUri ) {
    return new BaseMatcher<WebResource>() {
      @Override public boolean matches( final Object item ) {
        WebResource resource = (WebResource) item;
        return resource.getURI().toString().equals( expectedUri );
      }

      @Override public void describeTo( final Description description ) {

      }
    };
  }

  @Test
  public void testPublishMetaDataFile() throws Exception {

    InputStream metadataFile = mock( InputStream.class );
    String domainId = "Test";

    doCallRealMethod().when( modelServerPublishSpy ).getClient();

    // check null response
    doReturn( null ).when( modelServerPublishSpy ).httpPut( any( WebResource.Builder.class ) );
    int status = modelServerPublishSpy.publishMetaDataFile( metadataFile, domainId );
    assertEquals( ModelServerPublish.PUBLISH_FAILED, status );

    // check invalid clientResponse
    doReturn( clientResponse ).when( modelServerPublishSpy ).httpPut( any( WebResource.Builder.class ) );
    status = modelServerPublishSpy.publishMetaDataFile( metadataFile, domainId );
    assertEquals( ModelServerPublish.PUBLISH_FAILED, status );

    // check invalid status
    when( clientResponse.getStatus() ).thenReturn( 404 );
    status = modelServerPublishSpy.publishMetaDataFile( metadataFile, domainId );
    assertEquals( ModelServerPublish.PUBLISH_FAILED, status );

    modelServerPublishSpy.setForceOverwrite( true );

    // valid status, invalid payload
    when( clientResponse.getStatus() ).thenReturn( 200 );
    when( clientResponse.getEntity( String.class ) ).thenReturn( "" );
    status = modelServerPublishSpy.publishMetaDataFile( metadataFile, domainId );
    assertEquals( ModelServerPublish.PUBLISH_FAILED, status );

    // success
    when( clientResponse.getEntity( String.class ) ).thenReturn( ModelServerPublish.PUBLISH_SUCCESS + "" );
    status = modelServerPublishSpy.publishMetaDataFile( metadataFile, domainId );
    assertEquals( ModelServerPublish.PUBLISH_SUCCESS, status );

    // valid status, but throw error
    when( clientResponse.getEntity( String.class ) ).thenThrow( new RuntimeException() );
    status = modelServerPublishSpy.publishMetaDataFile( metadataFile, domainId );
    assertEquals( ModelServerPublish.PUBLISH_FAILED, status );
  }


  @Test
  public void testPublishMetaDataFileWithAcl() throws Exception {
    InputStream metadataFile = mock( InputStream.class );
    String domainId = "Test";

    doCallRealMethod().when( modelServerPublishSpy ).getClient();

    DataSourceAclModel aclModel = new DataSourceAclModel();
    aclModel.addUser( "testUser" );
    modelServerPublishSpy.setAclModel( aclModel );

    doReturn( clientResponse ).when( modelServerPublishSpy ).httpPut( any( WebResource.Builder.class ) );
    when( clientResponse.getStatus() ).thenReturn( 200 );
    when( clientResponse.getEntity( String.class ) ).thenReturn( ModelServerPublish.PUBLISH_SUCCESS + "" );
    int status = modelServerPublishSpy.publishMetaDataFile( metadataFile, domainId );
    assertEquals( ModelServerPublish.PUBLISH_SUCCESS, status );
  }


  @Test( expected = IllegalArgumentException.class )
  public void testPublishDsw() throws Exception {

    InputStream metadataFile = mock( InputStream.class );
    String domainId = "Test.xmi";

    doCallRealMethod().when( modelServerPublishSpy ).getClient();

    // check null response
    doReturn( null ).when( modelServerPublishSpy ).httpPut( any( WebResource.Builder.class ) );
    int status = modelServerPublishSpy.publishDsw( metadataFile, domainId );
    assertEquals( ModelServerPublish.PUBLISH_FAILED, status );

    // check invalid clientResponse
    doReturn( clientResponse ).when( modelServerPublishSpy ).httpPut( any( WebResource.Builder.class ) );
    status = modelServerPublishSpy.publishDsw( metadataFile, domainId );
    assertEquals( ModelServerPublish.PUBLISH_FAILED, status );

    // check invalid status
    when( clientResponse.getStatus() ).thenReturn( 404 );
    status = modelServerPublishSpy.publishDsw( metadataFile, domainId );
    assertEquals( ModelServerPublish.PUBLISH_FAILED, status );

    modelServerPublishSpy.setForceOverwrite( true );

    // valid status - 200
    when( clientResponse.getStatus() ).thenReturn( 200 );
    when( clientResponse.getEntity( String.class ) ).thenReturn( ModelServerPublish.PUBLISH_SUCCESS + "" );
    status = modelServerPublishSpy.publishDsw( metadataFile, domainId );
    assertEquals( ModelServerPublish.PUBLISH_SUCCESS, status );

    // valid status - 201
    when( clientResponse.getStatus() ).thenReturn( 201 );
    status = modelServerPublishSpy.publishDsw( metadataFile, domainId );
    assertEquals( ModelServerPublish.PUBLISH_SUCCESS, status );

    // throw exception
    domainId = "Test";
    modelServerPublishSpy.publishDsw( metadataFile, domainId );
  }
}
