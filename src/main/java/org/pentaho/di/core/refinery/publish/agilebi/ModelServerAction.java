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


package org.pentaho.di.core.refinery.publish.agilebi;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.pentaho.database.IDatabaseDialect;
import org.pentaho.database.model.IDatabaseType;
import org.pentaho.database.service.DatabaseDialectService;
import org.pentaho.database.util.DatabaseTypeHelper;
import org.pentaho.di.core.database.DatabaseInterface;
import org.pentaho.di.core.database.DatabaseMeta;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
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
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.property("jersey.config.jsonFeature", "JacksonFeature");
        clientConfig.property( ClientProperties.CONNECT_TIMEOUT, 2000 );
        clientConfig.property( ClientProperties.READ_TIMEOUT, 2000 );
        this._client = ClientBuilder.newClient( clientConfig );
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

  protected <T> Response httpPut( final Invocation.Builder builder, final Entity<T> entity ) {
    return builder.put( entity, Response.class );
  }

  protected <T> Response httpPost( final Invocation.Builder builder, final Entity<T> entity ) {
    return builder.post( entity, Response.class );
  }

  protected Response httpGet( final Invocation.Builder builder ) {
    return builder.get( Response.class );
  }

  protected Response httpDelete( final Invocation.Builder builder ) {
    return builder.delete( Response.class );
  }

  protected String getUrl( final String path ) {
    return biServerConnection.getUrl() + path;
  }

  protected WebTarget getResource( final String path ) {
    return getClient().target( getUrl( path ) );
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
      .register( HttpAuthenticationFeature.basic( biServerConnection.getUserId(), biServerConnection.getPassword() ) );
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
  public boolean isSuccess( Response response ) {
    return response.getStatus() >= 200 && response.getStatus() < 300;
  }

}
