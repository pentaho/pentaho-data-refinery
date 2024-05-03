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

package org.pentaho.di.core.refinery.publish.util;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;
import com.sun.jersey.multipart.FormDataMultiPart;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.pentaho.di.core.refinery.publish.agilebi.BiServerConnection;
import org.pentaho.di.core.refinery.publish.model.ResponseStatus;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Rowell Belen
 */
public class PublishRestUtilTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Captor ArgumentCaptor<BiServerConnection> connectionCaptor;
  @Captor ArgumentCaptor<String> restUrlCaptor;
  @Captor ArgumentCaptor<Client> clientCaptor;
  private PublishRestUtil publishRestUtil;
  private WebResource mockWebResource;
  private WebResource.Builder mockBuilder;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks( this );
    publishRestUtil = spy( new PublishRestUtil() );

    mockWebResource = mock( WebResource.class );
    doReturn( mockWebResource ).when( publishRestUtil )
        .getWebResource( any( BiServerConnection.class ), anyString(), any( Client.class ) );

    mockBuilder = mock( WebResource.Builder.class );
    doReturn( mockBuilder ).when( publishRestUtil )
        .getDefaultWebResourceBuilder( any( WebResource.class ) );
  }

  private BiServerConnection getMockConnection() {
    BiServerConnection model = new BiServerConnection();
    model.setUrl( "http://localhost:8181/pentaho/" );
    model.setUserId( "admin" );
    model.setPassword( "password" );
    return model;
  }

  @Test
  public void testGetAnonymousClient() {
    Client client = publishRestUtil.getAnonymousClient();
    assertTrue( client.getHeadHandler() instanceof URLConnectionClientHandler );
  }

  @Test
  public void testGetAuthenticatedClient() {
    Client client = publishRestUtil.getAuthenticatedClient( getMockConnection() );
    assertTrue( client.getHeadHandler() instanceof HTTPBasicAuthFilter );
  }

  @Test
  public void testGetResource() {

    BiServerConnection connection = getMockConnection();

    doCallRealMethod().when( publishRestUtil )
        .getWebResource( any( BiServerConnection.class ), anyString(), any( Client.class ) );

    String fullUrl = connection.getUrl() + PublishRestUtil.CAN_PUBLISH_PATH;
    WebResource resource = publishRestUtil.getWebResource( connection,
        PublishRestUtil.CAN_PUBLISH_PATH, publishRestUtil.getAuthenticatedClient( connection ) );
    assertEquals( resource.getURI().toString(), fullUrl );

    // test missing slash in url
    connection.setUrl( "http://localhost:8181/pentaho" );
    resource = publishRestUtil.getWebResource( connection,
        PublishRestUtil.CAN_PUBLISH_PATH, publishRestUtil.getAuthenticatedClient( connection ) );
    assertEquals( resource.getURI().toString(), fullUrl );
  }

  @Test
  public void testCreateFileUploadRequest() throws Exception {

    File bundle = File.createTempFile( "PROJECT_ID", ".ivb" );
    bundle.deleteOnExit();

    String repoPath = "/home/admin";

    FormDataMultiPart part = publishRestUtil.createFileUploadRequest( bundle, repoPath );
    assertEquals( part.getField( "fileUpload" ).getContentDisposition().getFileName(), bundle.getName() );
    assertTrue( part.getField( "importPath" ).getEntity().toString().contains( repoPath ) );
  }

  @Test
  public void testHttpGetAnonymous() {

    BiServerConnection connection = getMockConnection();

    publishRestUtil.httpGet( connection, PublishRestUtil.CAN_PUBLISH_PATH, false );

    verify( publishRestUtil ).getAnonymousClient();
    verify( mockBuilder ).get( ClientResponse.class );
  }

  @Test
  public void testHttpGetAuthenticated() {

    BiServerConnection connection = getMockConnection();

    doCallRealMethod().when( publishRestUtil )
        .getWebResource( any( BiServerConnection.class ), anyString(), any( Client.class ) );

    publishRestUtil.httpGet( connection, PublishRestUtil.CAN_PUBLISH_PATH, true );

    verify( publishRestUtil ).getAuthenticatedClient( connection );
    verify( mockBuilder ).get( ClientResponse.class );
  }

  @Test
  public void testIsUnauthenticatedUser() {

    BiServerConnection connection = getMockConnection();

    publishRestUtil.isUnauthenticatedUser( connection );

    verify( publishRestUtil ).simpleHttpGet( connection, PublishRestUtil.CAN_PUBLISH_PATH, true );
    verify( publishRestUtil ).getAuthenticateHttpClient( connection );
  }

  @Test
  public void testIsUnauthenticatedUserMockResponse() {

    BiServerConnection connection = getMockConnection();

    ResponseStatus mockResponse = mock( ResponseStatus.class );
    when( mockResponse.getStatus() ).thenReturn( 401 );

    doReturn( mockResponse ).when( publishRestUtil )
        .simpleHttpGet( connection, PublishRestUtil.CAN_PUBLISH_PATH, true );

    // unauthenticated
    assertTrue( publishRestUtil.isUnauthenticatedUser( connection ) );

    // authenticated
    when( mockResponse.getStatus() ).thenReturn( 200 );
    assertFalse( publishRestUtil.isUnauthenticatedUser( connection ) );

    // unknown response
    doReturn( null ).when( publishRestUtil ).httpGet( connection, PublishRestUtil.CAN_PUBLISH_PATH, true );
    assertFalse( publishRestUtil.isUnauthenticatedUser( connection ) );

    // null response
    doReturn( null ).when( publishRestUtil ).simpleHttpGet( connection, PublishRestUtil.CAN_PUBLISH_PATH, true );
    assertFalse( publishRestUtil.isUnauthenticatedUser( connection ) );
  }

  @Test
  public void testCanPublish() {

    BiServerConnection connection = getMockConnection();
    ClientResponse response = mock( ClientResponse.class );

    when( publishRestUtil.httpGet( connection, PublishRestUtil.CAN_PUBLISH_PATH, true ) )
      .thenReturn( response, response, null );
    when( response.getStatus() ).thenReturn( 200, 200 );
    when( response.getEntity( String.class ) ).thenReturn( "true", "false" );
    assertTrue( publishRestUtil.canPublish( connection ) );
    assertEquals( 200, publishRestUtil.getLastHTTPStatus() );
    assertFalse( publishRestUtil.canPublish( connection ) );
    assertEquals( 200, publishRestUtil.getLastHTTPStatus() );
    assertFalse( publishRestUtil.canPublish( connection ) );
    assertEquals( -1, publishRestUtil.getLastHTTPStatus() );

    verify( mockBuilder, times( 3 ) ).get( ClientResponse.class );
  }

  @Test
  public void testCanPublishMockResponse() {

    BiServerConnection connection = getMockConnection();

    ClientResponse mockResponse = mock( ClientResponse.class );
    when( mockResponse.getEntity( String.class ) ).thenReturn( "true" );
    when( mockResponse.getStatus() ).thenReturn( 200 );

    doReturn( mockResponse ).when( publishRestUtil ).httpGet( connection, PublishRestUtil.CAN_PUBLISH_PATH, true );

    // can publish
    assertTrue( publishRestUtil.canPublish( connection ) );
    assertTrue( publishRestUtil.lastHTTPStatus == 200 );

    // cannot publish
    when( mockResponse.getEntity( String.class ) ).thenReturn( "false" );
    when( mockResponse.getStatus() ).thenReturn( 404 );
    assertFalse( publishRestUtil.canPublish( connection ) );
    assertTrue( publishRestUtil.lastHTTPStatus == 404 );

    // unknown response
    doReturn( null ).when( publishRestUtil ).httpGet( connection, PublishRestUtil.CAN_PUBLISH_PATH, true );
    assertFalse( publishRestUtil.canPublish( connection ) );
  }

  @Test
  public void testCanManageDatasources() {

    BiServerConnection connection = getMockConnection();
    ClientResponse response = mock( ClientResponse.class );

    when( publishRestUtil.httpGet( connection, PublishRestUtil.CAN_MANAGE_DATASOURCES, true ) )
      .thenReturn( response, response, null );
    when( response.getStatus() ).thenReturn( 200, 200 );
    when( response.getEntity( String.class ) ).thenReturn( "true", "false" );
    assertTrue( publishRestUtil.canManageDatasources( connection ) );
    assertEquals( 200, publishRestUtil.getLastHTTPStatus() );
    assertFalse( publishRestUtil.canManageDatasources( connection ) );
    assertEquals( 200, publishRestUtil.getLastHTTPStatus() );
    assertFalse( publishRestUtil.canManageDatasources( connection ) );
    assertEquals( -1, publishRestUtil.getLastHTTPStatus() );

    verify( mockBuilder, times( 3 ) ).get( ClientResponse.class );
  }

  @Test
  public void testCanManageDatasourcesMockResponse() {

    BiServerConnection connection = getMockConnection();

    ClientResponse mockResponse = mock( ClientResponse.class );
    when( mockResponse.getEntity( String.class ) ).thenReturn( "true" );
    when( mockResponse.getStatus() ).thenReturn( 200 );

    doReturn( mockResponse ).when( publishRestUtil )
        .httpGet( connection, PublishRestUtil.CAN_MANAGE_DATASOURCES, true );

    // can manage data sources
    assertTrue( publishRestUtil.canManageDatasources( connection ) );
    assertTrue( publishRestUtil.lastHTTPStatus == 200 );

    // cannot manage data sources
    when( mockResponse.getEntity( String.class ) ).thenReturn( "false" );
    when( mockResponse.getStatus() ).thenReturn( 404 );
    assertFalse( publishRestUtil.canManageDatasources( connection ) );
    assertTrue( publishRestUtil.lastHTTPStatus == 404 );

    when( mockResponse.getEntity( String.class ) ).thenReturn( "false" );

    // unknown response
    doReturn( null ).when( publishRestUtil ).httpGet( connection, PublishRestUtil.CAN_MANAGE_DATASOURCES, true );
    assertFalse( publishRestUtil.canManageDatasources( connection ) );
  }

  @Test
  public void testCanCreate() {

    BiServerConnection connection = getMockConnection();

    publishRestUtil.canCreate( connection );

    verify( publishRestUtil ).httpGet( connection, PublishRestUtil.CAN_CREATE_PATH, true );
    verify( mockBuilder ).get( ClientResponse.class );
  }

  @Test
  public void testCanCreateMockResponse() {

    BiServerConnection connection = getMockConnection();

    ClientResponse mockResponse = mock( ClientResponse.class );
    when( mockResponse.getEntity( String.class ) ).thenReturn( "true" );

    doReturn( mockResponse ).when( publishRestUtil ).httpGet( connection, PublishRestUtil.CAN_CREATE_PATH, true );

    // can create
    assertTrue( publishRestUtil.canCreate( connection ) );

    // cannot create
    when( mockResponse.getEntity( String.class ) ).thenReturn( "false" );
    assertFalse( publishRestUtil.canCreate( connection ) );

    // unknown response
    doReturn( null ).when( publishRestUtil ).httpGet( connection, PublishRestUtil.CAN_CREATE_PATH, true );
    assertFalse( publishRestUtil.canCreate( connection ) );
  }

  @Test
  public void testCanExecute() {

    BiServerConnection connection = getMockConnection();

    publishRestUtil.canExecute( connection );

    verify( publishRestUtil ).httpGet( connection, PublishRestUtil.CAN_EXECUTE_PATH, true );
  }

  @Test
  public void testCanExecuteMockResponse() {

    BiServerConnection connection = getMockConnection();

    ClientResponse mockResponse = mock( ClientResponse.class );
    when( mockResponse.getEntity( String.class ) ).thenReturn( "true" );

    doReturn( mockResponse ).when( publishRestUtil ).httpGet( connection, PublishRestUtil.CAN_EXECUTE_PATH, true );

    // can execute
    assertTrue( publishRestUtil.canExecute( connection ) );

    // cannot execute
    when( mockResponse.getEntity( String.class ) ).thenReturn( "false" );
    assertFalse( publishRestUtil.canExecute( connection ) );

    // unknown response
    doReturn( null ).when( publishRestUtil ).httpGet( connection, PublishRestUtil.CAN_EXECUTE_PATH, true );
    assertFalse( publishRestUtil.canExecute( connection ) );
  }

  @Test
  public void testIsPentahoServer() {

    BiServerConnection connection = getMockConnection();

    publishRestUtil.isPentahoServer( connection );

    verify( publishRestUtil ).httpGet( connection, PublishRestUtil.PENTAHO_WEBCONTEXT_PATH, false );
    verify( mockBuilder ).get( ClientResponse.class );
  }

  @Test
  public void testIsPentahoServerMockResponse() {

    BiServerConnection connection = getMockConnection();
    connection.setUrl( null );

    ClientResponse mockResponse = mock( ClientResponse.class );
    when( mockResponse.getEntity( String.class ) ).thenReturn( PublishRestUtil.PENTAHO_WEBCONTEXT_MATCH );

    doReturn( mockResponse ).when( publishRestUtil )
        .httpGet( connection, PublishRestUtil.PENTAHO_WEBCONTEXT_PATH, false );

    assertFalse( publishRestUtil.isPentahoServer( connection ) ); // url is null

    // is pentaho server
    connection.setUrl( "http://localhost:8181/pentaho/" );
    assertTrue( publishRestUtil.isPentahoServer( connection ) );

    // is not pentaho server
    when( mockResponse.getEntity( String.class ) ).thenReturn( "false" );
    assertFalse( publishRestUtil.isPentahoServer( connection ) );

    // unknown response
    doReturn( null ).when( publishRestUtil ).httpGet( connection, PublishRestUtil.PENTAHO_WEBCONTEXT_PATH, false );
    assertFalse( publishRestUtil.isPentahoServer( connection ) );
  }

  @Test
  public void testHttpPutAuthenticated() throws Exception {

    BiServerConnection connection = getMockConnection();

    publishRestUtil.httpPut( connection, "", true, null );

    verify( publishRestUtil ).getAuthenticatedClient( connection );
    verify( mockBuilder ).put( ClientResponse.class, null );
  }

  @Test
  public void testHttpPut() throws Exception {

    publishRestUtil.httpPut( getMockConnection(), "", false, null );

    verify( publishRestUtil ).getAnonymousClient();
    verify( mockBuilder ).put( ClientResponse.class, null );
  }

  @Test
  public void testSimpleHttpGet() throws Exception {

    BiServerConnection connection = getMockConnection();
    connection.setUrl( "http://localhost:8181/pentaho" );

    publishRestUtil.simpleHttpGet( connection, "", false );

    HttpClient mockHttpClient = mock( HttpClient.class );
    HttpGet getMethod = mock( HttpGet.class );
    doReturn( "true" ).when( publishRestUtil ).getResponseString( any(HttpResponse.class) );
    doReturn( mockHttpClient ).when( publishRestUtil ).getSimpleHttpClient();
    doReturn( getMethod ).when( publishRestUtil ).createGetMethod( anyString() );
    publishRestUtil.simpleHttpGet( connection, "", false );
  }

  @Test
  public void testIsUserInfoProvided() throws Exception {

    assertFalse( publishRestUtil.isUserInfoProvided( null ) );

    BiServerConnection connection = new BiServerConnection();
    assertFalse( publishRestUtil.isUserInfoProvided( connection ) );

    connection.setUserId( "" );
    connection.setPassword( "" );
    assertFalse( publishRestUtil.isUserInfoProvided( connection ) );

    connection.setUserId( "admin" );
    assertFalse( publishRestUtil.isUserInfoProvided( connection ) );

    connection.setPassword( "password" );
    assertTrue( publishRestUtil.isUserInfoProvided( connection ) );
  }

  @Test
  public void testIsBiServerUrlProvided() throws Exception {

    assertFalse( publishRestUtil.isBiServerUrlProvided( null ) );

    BiServerConnection connection = new BiServerConnection();
    assertFalse( publishRestUtil.isBiServerUrlProvided( connection ) );

    connection.setUrl( null );
    assertFalse( publishRestUtil.isBiServerUrlProvided( connection ) );

    connection.setUrl( "http://localhost:8181/pentaho" );
    assertTrue( publishRestUtil.isBiServerUrlProvided( connection ) );
  }
}
