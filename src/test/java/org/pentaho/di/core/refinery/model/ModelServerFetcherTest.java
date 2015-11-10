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

package org.pentaho.di.core.refinery.model;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.pentaho.di.core.refinery.model.ModelServerFetcher.AuthorizationException;
import org.pentaho.di.core.refinery.model.ModelServerFetcher.ServerException;
import org.pentaho.metadata.model.Domain;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import static org.mockito.Mockito.*;

public class ModelServerFetcherTest {

  /* mocks */
  Client client;
  WebResource webResource;
  WebResource.Builder builder;


  @Before
  public void init() {
    builder = mock( WebResource.Builder.class );
    when( builder.type( any( MediaType.class ) ) ).thenReturn( builder );
    webResource = mock( WebResource.class );
    when( webResource.type( any( MediaType.class ) ) ).thenReturn( builder );
    when( webResource.type( any( String.class ) ) ).thenReturn( builder );
    client = mock( Client.class );
    when( client.resource( any( String.class ) ) ).thenReturn( webResource );
  }

  @Test
  public void testFetchDswList() throws Exception {
    final String okPayload =
        "<List>\n"
            + "<Item xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xsi:type=\"xs:string\">One.xmi</Item>\n"
            + "<Item xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xsi:type=\"xs:string\">Two.xmi</Item>\n"
            + "<Item xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xsi:type=\"xs:string\">Three.xmi</Item>\n"
            + "</List>";
    mockResponse( 200, okPayload );
    ModelServerFetcher fetcher = createModelServerFetcher();
    List<String> dswList = fetcher.fetchDswList();
    verify( client, times( 1 ) ).resource( "http://server:8081/webapp/plugin/data-access/api/datasource/dsw/ids" );
    assertEquals( 3, dswList.size() );
    assertTrue( dswList.contains( "One.xmi" ) );
    assertTrue( dswList.contains( "Two.xmi" ) );
    assertTrue( dswList.contains( "Three.xmi" ) );
  }

  @Test
  public void testFetchEmptyDswList() throws Exception {
    final String okPayload =
        "<List>\n"
        + "</List>";
    mockResponse( 200, okPayload );
    ModelServerFetcher fetcher = createModelServerFetcher();
    List<String> dswList = fetcher.fetchDswList();
    assertEquals( 0, dswList.size() );
  }

  @Test
  public void testFetchDswListServerError() throws Exception {
    mockResponse( 500, "oops" );
    ModelServerFetcher fetcher = createModelServerFetcher();
    try {
      fetcher.fetchDswList();
      fail( "no exception" );
    } catch ( ServerException e ) {
      // improve
    } catch ( Exception e ) {
      fail( "unexpected exception" );
    }
  }

  @Test
  public void testFetchAnalysisList() throws Exception {
    final String okPayload =
        "<List>\n"
            + "<Item xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xsi:type=\"xs:string\">SomeSchema</Item>\n"
            + "</List>";
    mockResponse( 200, okPayload );
    ModelServerFetcher fetcher = createModelServerFetcher();
    List<String> dswList = fetcher.fetchAnalysisList();
    verify( client, times( 1 ) ).resource( "http://server:8081/webapp/plugin/data-access/api/datasource/analysis/ids" );
    assertTrue( dswList.get( 0 ).equals( "SomeSchema" ) );
  }

  @Test
  public void testDownloadAnalysisFile() throws Exception {
    InputStream in = getClass().getResourceAsStream( "/SteelWheels.mondrian.xml" );
    try {
      ModelServerFetcher fetcher = createModelServerFetcher();
      mockResponse( 200, in, "xml" );
      String mondrianFile = fetcher.downloadAnalysisFile( "Steel Wheels" );
      verify( client, times( 1 ) ).resource( "http://server:8081/webapp/plugin/data-access/api/datasource/analysis/Steel%20Wheels/download" );
      assertTrue( mondrianFile.contains( "Cube name=\"SteelWheelsSales\"" ) );
    } finally {
      IOUtils.closeQuietly( in );
    }
  }

  @Test
  public void testDownloadZippedAnalysisFile() throws Exception {
    InputStream in = getClass().getResourceAsStream( "/sample.zip" );
    try {
      ModelServerFetcher fetcher = createModelServerFetcher();
      mockResponse( 200, in, "zip" );
      String mondrianFile = fetcher.downloadAnalysisFile( "Steel Wheels" );
      verify( client, times( 1 ) ).resource(
          "http://server:8081/webapp/plugin/data-access/api/datasource/analysis/Steel%20Wheels/download" );
      assertTrue( mondrianFile.contains( "Cube name=\"SteelWheelsSales\"" ) );
    } finally {
      IOUtils.closeQuietly( in );
    }
  }

  @Test
  public void testDownloadAnalysisFileNoAuth() throws Exception {
    ModelServerFetcher fetcher = createModelServerFetcher();
    mockResponse( 401, "" );
    try {
      fetcher.downloadAnalysisFile( "SteelWheels" );
      fail( "no exception" );
    } catch ( AuthorizationException ke ) {
      //
    } catch ( Exception e ) {
      fail( "wrong exception" );
    }
  }

  @Test
  public void testDownloadDswFile() throws Exception {
    InputStream in = getClass().getResourceAsStream( "/Dsw Test.zip" );
    try {
      ModelServerFetcher fetcher = createModelServerFetcher();
      mockResponse( 200, in, "xml" );
      Domain dsw = fetcher.downloadDswFile( "Dsw Test.xmi" );
      verify( client, times( 1 ) ).resource( "http://server:8081/webapp/plugin/data-access/api/datasource/dsw/Dsw%20Test.xmi/download" );
      assertEquals( "DswTest", dsw.getLogicalModels().get( 1 ).getProperty( "MondrianCatalogRef" ) );
    } finally {
      IOUtils.closeQuietly( in );
    }
  }


  private ModelServerFetcher createModelServerFetcher() {
    return new MockFriendlyServerFetcher();
  }

  private class MockFriendlyServerFetcher extends ModelServerFetcher {
    static final String BOGUS_SERVER = "http://server:8081/webapp/";
    public MockFriendlyServerFetcher() {
      super();
    }

    @Override
    protected Client getClient() {
      return client;
    }
    @Override
    protected String getUrl( String path ) {
      return BOGUS_SERVER + path;
    }
  }

  private ClientResponse mockResponse( final int status, final String entity ) throws Exception {
    return mockResponse( status, IOUtils.toInputStream( entity, "UTF-8" ), "xml" );
  }

  private ClientResponse mockResponse( final int status, final InputStream entity, final String fileType ) throws Exception {
    ClientResponse resp = mock( ClientResponse.class );
    when( resp.getStatus() ).thenReturn( status );
    when( resp.getEntity( InputStream.class ) ).thenReturn( entity );
    when( resp.getEntity( String.class ) ).thenAnswer( new Answer<String>() {
      public String answer( InvocationOnMock invocation ) throws Throwable {
        return IOUtils.toString( entity, "UTF-8" );
      }
    } );
    when( resp.getType() ).thenReturn( new MediaType( "application", fileType ) );
    when( builder.get( ClientResponse.class ) ).thenReturn( resp );
    when( webResource.get( ClientResponse.class ) ).thenReturn( resp );
    return resp;
  }


}
