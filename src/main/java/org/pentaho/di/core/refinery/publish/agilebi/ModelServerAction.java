/*! ******************************************************************************
 *
 * Pentaho Community Edition Project: data-refinery-pdi-plugin
 *
 * Copyright (C) 2002-2021 by Hitachi Vantara : http://www.pentaho.com
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
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import org.pentaho.database.IDatabaseDialect;
import org.pentaho.database.model.IDatabaseType;
import org.pentaho.database.service.DatabaseDialectService;
import org.pentaho.database.util.DatabaseTypeHelper;
import org.pentaho.di.core.database.DatabaseInterface;
import org.pentaho.di.core.database.DatabaseMeta;

import javax.ws.rs.ext.Providers;

public class ModelServerAction {

  protected BiServerConnection biServerConnection;
  private Client _client = null;
  protected DatabaseMeta databaseMeta;

  public ModelServerAction() {
    super();
  }

  public ModelServerAction( BiServerConnection serverConnection ) {
    setBiServerConnection( serverConnection );
  }

  protected Client getClient() {

    // initialize
    if ( this._client == null ) {
      /*
      MessageBodyReader and MessageBodyWriter interfaces from JAX-RS-1.1 api are loaded by the main classloader
      (due to configuration in custom.properties), as well as Jersey-1.19 classes,
      whereas Jackson-2 is loaded by bundle's classloader. As a result, Jackson's providers are not visible to Jersey.

      Moving Jackson to be also loaded by the main classloader is not working.
      Cause Jackson-2 implements JAX-RS 2.0, and we would also have to replace JAX-RS-1.1 api
      (the one implemented by Jersey-1.19) by JAX-RS-2.0.
      While it's looking fine on the shallow (2.0 is compatible with 1.1),
      it leads to similar errors when starting Jersey-2 required by other bundles, where Jersey-2 is instantiated
      using JAX-RS api - api classes do not see an implementation.

      Thus, we've come to the solution to load Jackson classes in this particular case by the main classloader.
       */
      ClassLoader orig = Thread.currentThread().getContextClassLoader();
      try {
        Thread.currentThread().setContextClassLoader( Providers.class.getClassLoader() );

        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put( JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE );
        this._client = Client.create( clientConfig );
      } finally {
        Thread.currentThread().setContextClassLoader( orig );
      }
    }

    return this._client;
  }

  protected IDatabaseType getDatabaseType( DatabaseInterface databaseInterface ) {
    ClassLoader orig = Thread.currentThread().getContextClassLoader();
    IDatabaseType dbType = null;
    try {
      Thread.currentThread().setContextClassLoader( IDatabaseDialect.class.getClassLoader() );

      DatabaseDialectService dds = new DatabaseDialectService( false );
      DatabaseTypeHelper dth = new DatabaseTypeHelper( dds.getDatabaseTypes() );
      dbType = dth.getDatabaseTypeByShortName( databaseInterface.getPluginId() );
    } finally {
      Thread.currentThread().setContextClassLoader( orig );
      return dbType;
    }
  }

  protected ClientResponse httpPut( final Builder builder ) {
    return builder.put( ClientResponse.class );
  }

  protected ClientResponse httpPost( final Builder builder ) {
    return builder.post( ClientResponse.class );
  }

  protected ClientResponse httpGet( final Builder builder ) {
    return builder.get( ClientResponse.class );
  }

  protected ClientResponse httpDelete( final Builder builder ) {
    return builder.delete( ClientResponse.class );
  }

  protected String getUrl( final String path ) {
    return biServerConnection.getUrl() + path;
  }

  protected WebResource getResource( final String path ) {
    return getClient().resource( getUrl( path ) );
  }

  /**
   * Sets the current BI server connection
   *
   * @param biServerConnection
   */
  public void setBiServerConnection( BiServerConnection biServerConnection ) {
    this.biServerConnection = biServerConnection;
    setupClient( getClient(), biServerConnection );
  }

  protected void setupClient( Client client, BiServerConnection biServerConnection ) {
    client
      .addFilter( new HTTPBasicAuthFilter( biServerConnection.getUserId(), biServerConnection.getPassword() ) );
  }

  public DatabaseMeta getDatabaseMeta() {
    return databaseMeta;
  }

  public void setDatabaseMeta( DatabaseMeta databaseMeta ) {
    this.databaseMeta = databaseMeta;
  }

  /**
   * 2xx
   */
  public boolean isSuccess( ClientResponse response ) {
    return response.getStatus() >= 200 && response.getStatus() < 300;
  }

}
