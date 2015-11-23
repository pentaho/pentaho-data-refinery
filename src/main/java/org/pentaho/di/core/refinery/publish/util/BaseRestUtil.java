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

package org.pentaho.di.core.refinery.publish.util;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataMultiPart;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.pentaho.di.core.refinery.publish.agilebi.BiServerConnection;
import org.pentaho.di.core.refinery.publish.model.ResponseStatus;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLEncoder;

/**
 * @author Rowell Belen
 */
public abstract class BaseRestUtil {

  protected Client getAnonymousClient() {
    ClientConfig clientConfig = new DefaultClientConfig();
    clientConfig.getFeatures().put( JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE );
    clientConfig.getProperties().put(
        ClientConfig.PROPERTY_READ_TIMEOUT, 2000 ); // 2 sec. timeout
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

  protected HttpClient getSimpleHttpClient( final BiServerConnection connection, boolean authenticate ) {

    HttpClient client = new HttpClient();
    if ( authenticate ) {
      client.getState().setCredentials(
          new AuthScope( AuthScope.ANY_HOST, AuthScope.ANY_PORT ),
          new UsernamePasswordCredentials( connection.getUserId(), connection.getPassword() ) );
    }

    return client;
  }

  protected GetMethod createGetMethod( final String url, final boolean authenticate ) {
    GetMethod get = new GetMethod( url );
    get.setDoAuthentication( authenticate );
    return get;
  }

  protected ResponseStatus simpleHttpGet( final BiServerConnection connection, final String restUrl,
      boolean authenticate ) {

    ResponseStatus responseStatus = new ResponseStatus();

    HttpClient client = getSimpleHttpClient( connection, authenticate );

    final String url =
        connection.getUrl().endsWith( "/" ) ? ( connection.getUrl() + restUrl )
            : ( connection.getUrl() + "/" + restUrl );

    GetMethod get = createGetMethod( url, authenticate );

    try {
      // execute the GET
      client.executeMethod( get );

      responseStatus.setStatus( get.getStatusCode() );
      responseStatus.setMessage( get.getResponseBodyAsString() );

    } catch ( Exception e ) {

      responseStatus.setStatus( -1 );
      responseStatus.setMessage( e.getMessage() );

    } finally {
      // release any connection resources used by the method
      get.releaseConnection();
    }

    return responseStatus;
  }

  protected WebResource.Builder getDefaultWebResourceBuilder( WebResource webResource ) {
    return webResource
        .type( MediaType.APPLICATION_JSON )
        .type( MediaType.APPLICATION_XML )
        .accept( MediaType.WILDCARD );
  }
}
