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
 *//*!
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
package org.pentaho.di.core.refinery.publish.util;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;
import com.sun.jersey.multipart.FormDataMultiPart;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

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
    verify( publishRestUtil ).getSimpleHttpClient( connection, true );
  }

  @Test
  public void testIsUnauthenticatedUserMockResponse() {

    BiServerConnection connection = getMockConnection();

    ResponseStatus mockResponse = mock( ResponseStatus.class );
    stub( mockResponse.getStatus() ).toReturn( 401 );

    doReturn( mockResponse ).when( publishRestUtil )
        .simpleHttpGet( connection, PublishRestUtil.CAN_PUBLISH_PATH, true );

    // unauthenticated
    assertTrue( publishRestUtil.isUnauthenticatedUser( connection ) );

    // authenticated
    stub( mockResponse.getStatus() ).toReturn( 200 );
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
    stub( mockResponse.getEntity( String.class ) ).toReturn( "true" );
    stub( mockResponse.getStatus() ).toReturn( 200 );

    doReturn( mockResponse ).when( publishRestUtil ).httpGet( connection, PublishRestUtil.CAN_PUBLISH_PATH, true );

    // can publish
    assertTrue( publishRestUtil.canPublish( connection ) );
    assertTrue( publishRestUtil.lastHTTPStatus == 200 );

    // cannot publish
    stub( mockResponse.getEntity( String.class ) ).toReturn( "false" );
    stub( mockResponse.getStatus() ).toReturn( 404 );
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
    stub( mockResponse.getEntity( String.class ) ).toReturn( "true" );
    stub( mockResponse.getStatus() ).toReturn( 200 );

    doReturn( mockResponse ).when( publishRestUtil )
        .httpGet( connection, PublishRestUtil.CAN_MANAGE_DATASOURCES, true );

    // can manage data sources
    assertTrue( publishRestUtil.canManageDatasources( connection ) );
    assertTrue( publishRestUtil.lastHTTPStatus == 200 );

    // cannot manage data sources
    stub( mockResponse.getEntity( String.class ) ).toReturn( "false" );
    stub( mockResponse.getStatus() ).toReturn( 404 );
    assertFalse( publishRestUtil.canManageDatasources( connection ) );
    assertTrue( publishRestUtil.lastHTTPStatus == 404 );

    stub( mockResponse.getEntity( String.class ) ).toReturn( "false" );

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
    stub( mockResponse.getEntity( String.class ) ).toReturn( "true" );

    doReturn( mockResponse ).when( publishRestUtil ).httpGet( connection, PublishRestUtil.CAN_CREATE_PATH, true );

    // can create
    assertTrue( publishRestUtil.canCreate( connection ) );

    // cannot create
    stub( mockResponse.getEntity( String.class ) ).toReturn( "false" );
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
    stub( mockResponse.getEntity( String.class ) ).toReturn( "true" );

    doReturn( mockResponse ).when( publishRestUtil ).httpGet( connection, PublishRestUtil.CAN_EXECUTE_PATH, true );

    // can execute
    assertTrue( publishRestUtil.canExecute( connection ) );

    // cannot execute
    stub( mockResponse.getEntity( String.class ) ).toReturn( "false" );
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
    stub( mockResponse.getEntity( String.class ) ).toReturn( PublishRestUtil.PENTAHO_WEBCONTEXT_MATCH );

    doReturn( mockResponse ).when( publishRestUtil )
        .httpGet( connection, PublishRestUtil.PENTAHO_WEBCONTEXT_PATH, false );

    assertFalse( publishRestUtil.isPentahoServer( connection ) ); // url is null

    // is pentaho server
    connection.setUrl( "http://localhost:8181/pentaho/" );
    assertTrue( publishRestUtil.isPentahoServer( connection ) );

    // is not pentaho server
    stub( mockResponse.getEntity( String.class ) ).toReturn( "false" );
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
    GetMethod getMethod = mock( GetMethod.class );
    when( getMethod.getResponseBodyAsString() ).thenReturn( "true" );
    doReturn( mockHttpClient ).when( publishRestUtil ).getSimpleHttpClient( connection, false );
    doReturn( getMethod ).when( publishRestUtil ).createGetMethod( anyString(), anyBoolean() );
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
