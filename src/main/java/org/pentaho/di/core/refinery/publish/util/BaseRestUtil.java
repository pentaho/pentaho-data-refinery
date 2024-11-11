/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/


package org.pentaho.di.core.refinery.publish.util;

import com.google.common.annotations.VisibleForTesting;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataMultiPart;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.refinery.publish.agilebi.BiServerConnection;
import org.pentaho.di.core.refinery.publish.model.ResponseStatus;
import org.pentaho.di.core.util.EnvUtil;
import org.pentaho.di.core.util.HttpClientManager;
import org.pentaho.di.core.util.HttpClientUtil;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

/**
 * @author Rowell Belen
 */
public abstract class BaseRestUtil {

  protected static final String KETTLE_DATA_REFINERY_HTTP_CLIENT_TIMEOUT = "KETTLE_DATA_REFINERY_HTTP_CLIENT_TIMEOUT";

  protected Client getAnonymousClient() {
    ClientConfig clientConfig = new DefaultClientConfig();
    clientConfig.getFeatures().put( JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE );
    clientConfig.getProperties().put(
        ClientConfig.PROPERTY_READ_TIMEOUT,
            Const.toInt( EnvUtil.getSystemProperty( KETTLE_DATA_REFINERY_HTTP_CLIENT_TIMEOUT ), 2000 ) );  // 2 sec. timeout
    return Client.create( clientConfig );
  }

  protected Client getAuthenticatedClient( final BiServerConnection connection ) {
    Client client = getAnonymousClient();
    client.addFilter( new HTTPBasicAuthFilter( connection.getUserId(), connection.getPassword() ) );
    return client;
  }

  protected WebResource getWebResource( final BiServerConnection connection, final String restUrl,
      final Client client ) {
    final String url =
        connection.getUrl().endsWith( "/" ) ? ( connection.getUrl() + restUrl )
            : ( connection.getUrl() + "/" + restUrl );

    return client.resource( url );
  }

  protected ClientResponse httpGet( final BiServerConnection connection, final String restUrl, boolean authenticate ) {

    Client client = getAnonymousClient();
    if ( authenticate ) {
      client = getAuthenticatedClient( connection );
    }

    WebResource resource = getWebResource( connection, restUrl, client );
    WebResource.Builder builder = getDefaultWebResourceBuilder( resource );
    return builder.get( ClientResponse.class );
  }

  protected ClientResponse httpPut( final BiServerConnection connection, final String restUrl,
      final boolean authenticate, final Object requestEntity ) {

    Client client = getAnonymousClient();
    if ( authenticate ) {
      client = getAuthenticatedClient( connection );
    }

    WebResource resource = getWebResource( connection, restUrl, client );
    WebResource.Builder builder = getDefaultWebResourceBuilder( resource );
    return builder.put( ClientResponse.class, requestEntity );
  }

  protected FormDataMultiPart createFileUploadRequest( final File file, final String repositoryPath ) throws Exception {

    InputStream in = new FileInputStream( file );

    FormDataMultiPart part = new FormDataMultiPart();
    part.field( "importPath", repositoryPath, MediaType.MULTIPART_FORM_DATA_TYPE )
        .field( "fileUpload", in, MediaType.MULTIPART_FORM_DATA_TYPE )
        .field( "overwriteFile", String.valueOf( true ), MediaType.MULTIPART_FORM_DATA_TYPE );

    part.getField( "fileUpload" ).setContentDisposition(
        FormDataContentDisposition.name( "fileUpload" )
            .fileName( URLEncoder.encode( file.getName(), "UTF-8" ) ).build() );

    return part;
  }

  protected HttpGet createGetMethod( final String url ) {
    HttpGet get = new HttpGet( url );
    return get;
  }

  protected ResponseStatus simpleHttpGet( final BiServerConnection connection, final String restUrl,
      boolean authenticate ) {

    ResponseStatus responseStatus = new ResponseStatus();

    HttpClientContext authContext = null;
    HttpClient client = null;

    if ( authenticate ) {
      //authContext = createAuthContext( connection );
      client = getAuthenticateHttpClient( connection );
    } else {
      client = getSimpleHttpClient();
    }

    final String url =
        connection.getUrl().endsWith( "/" ) ? ( connection.getUrl() + restUrl )
            : ( connection.getUrl() + "/" + restUrl );

    HttpGet get = createGetMethod( url );

    try {
      // execute the GET
      HttpResponse response = ( authContext == null ) ? client.execute( get ) : client.execute( get, authContext );

      responseStatus.setStatus( response.getStatusLine().getStatusCode() );
      responseStatus.setMessage( getResponseString( response ) );

    } catch ( Exception e ) {
      responseStatus.setStatus( -1 );
      responseStatus.setMessage( e.getMessage() );
    }

    return responseStatus;
  }

  @VisibleForTesting
  HttpClient getSimpleHttpClient() {
    return HttpClientManager.getInstance().createDefaultClient();
  }

  @VisibleForTesting
  HttpClient getAuthenticateHttpClient( BiServerConnection connection ) {
    HttpClientManager.HttpClientBuilderFacade clientBuilder = HttpClientManager.getInstance().createBuilder();
    clientBuilder.setCredentials( connection.getUserId(), connection.getPassword() );
    return clientBuilder.build();
  }

  @VisibleForTesting
  String getResponseString( HttpResponse response ) throws IOException {
    return HttpClientUtil.responseToString( response );
  }

  protected WebResource.Builder getDefaultWebResourceBuilder( WebResource webResource ) {
    return webResource
        .type( MediaType.APPLICATION_JSON )
        .type( MediaType.APPLICATION_XML )
        .accept( MediaType.WILDCARD );
  }

  private HttpClientContext createAuthContext( BiServerConnection connection ) {
    HttpClientContext context = HttpClientContext.create();
    CredentialsProvider provider = new BasicCredentialsProvider();
    UsernamePasswordCredentials credentials =
        new UsernamePasswordCredentials( connection.getUserId(), connection.getPassword() );
    provider.setCredentials( AuthScope.ANY, credentials );
    context.setCredentialsProvider( provider );
    return context;
  }
}
