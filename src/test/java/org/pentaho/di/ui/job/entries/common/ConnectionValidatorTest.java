/*! ******************************************************************************
 *
 * Pentaho Community Edition Project: data-refinery-pdi-plugin
 *
 * Copyright (C) 2002-2017 by Hitachi Vantara : http://www.pentaho.com
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

package org.pentaho.di.ui.job.entries.common;

import org.junit.Before;
import org.junit.Test;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.gui.SpoonInterface;
import org.pentaho.di.core.refinery.publish.agilebi.BiServerConnection;
import org.pentaho.di.core.refinery.publish.model.DataSourcePublishModel;
import org.pentaho.di.core.refinery.publish.util.PublishRestUtil;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @author Rowell Belen
 */
public class ConnectionValidatorTest {

  private DataSourcePublishModel model;
  private ConnectionValidator connectionValidator;
  private PublishRestUtil publishRestUtil;
  private SpoonInterface spoon;

  @Before
  public void setup() {
    spoon = mock( SpoonInterface.class );
    publishRestUtil = mock( PublishRestUtil.class );

    model = new DataSourcePublishModel();
    model.setBiServerConnection( mock( BiServerConnection.class ) );

    connectionValidator = new ConnectionValidator();
    connectionValidator.setPublishRestUtil( publishRestUtil );
    connectionValidator.setConnection( model.getBiServerConnection() );
  }

  @Test
  public void testCanConnectTrue() {
    ConnectionValidator spy = spy( connectionValidator );
    when( publishRestUtil.isPentahoServer( model.getBiServerConnection() ) ).thenReturn( true );
    when( publishRestUtil.canPublish( model.getBiServerConnection() ) ).thenReturn( true );
    when( publishRestUtil.canManageDatasources( model.getBiServerConnection() ) ).thenReturn( true );

    spy.canConnect();
    verify( publishRestUtil ).isPentahoServer( model.getBiServerConnection() );
    verify( publishRestUtil ).canPublish( model.getBiServerConnection() );
    verify( publishRestUtil ).canManageDatasources( model.getBiServerConnection() );
  }

  @Test
  public void testCanConnectFalse1() {
    ConnectionValidator spy = spy( connectionValidator );
    when( publishRestUtil.isPentahoServer( model.getBiServerConnection() ) ).thenReturn( false );

    spy.canConnect();
    verify( publishRestUtil ).isPentahoServer( model.getBiServerConnection() );
  }

  @Test
  public void testCanConnectFalse2() {
    ConnectionValidator spy = spy( connectionValidator );
    when( publishRestUtil.isPentahoServer( model.getBiServerConnection() ) ).thenReturn( true );

    spy.canConnect();
    verify( publishRestUtil ).isPentahoServer( model.getBiServerConnection() );
  }

  @Test
  public void testCanConnectFalse3() {
    ConnectionValidator spy = spy( connectionValidator );
    when( publishRestUtil.isPentahoServer( model.getBiServerConnection() ) ).thenReturn( true );
    when( publishRestUtil.canPublish( model.getBiServerConnection() ) ).thenReturn( false );

    spy.canConnect();
    verify( publishRestUtil ).isPentahoServer( model.getBiServerConnection() );
    verify( publishRestUtil ).canPublish( model.getBiServerConnection() );
  }

  @Test
  public void testCanConnectFalse4() {
    ConnectionValidator spy = spy( connectionValidator );
    when( publishRestUtil.isPentahoServer( model.getBiServerConnection() ) ).thenReturn( true );
    when( publishRestUtil.canPublish( model.getBiServerConnection() ) ).thenReturn( true );
    when( publishRestUtil.canManageDatasources( model.getBiServerConnection() ) ).thenReturn( false );

    spy.canConnect();
    verify( publishRestUtil ).isPentahoServer( model.getBiServerConnection() );
    verify( publishRestUtil ).canPublish( model.getBiServerConnection() );
    verify( publishRestUtil ).canManageDatasources( model.getBiServerConnection() );
  }

  @Test
  public void testCanConnectNull() {
    ConnectionValidator spy = spy( connectionValidator );

    spy.setConnection( null );
    assertFalse( spy.canConnect() );

    spy.setConnection( new BiServerConnection() );
    spy.canConnect();
    assertFalse( spy.canConnect() );
  }

  @Test
  public void testShowInfo() {
    ConnectionValidator spy = spy( connectionValidator );
    when( spy.getSpoon() ).thenReturn( spoon );
    spy.showInfo( "title", "message" );
    verify( spoon ).messageBox( "message", "title", false, Const.INFO );
  }

  @Test
  public void testShowError() {
    ConnectionValidator spy = spy( connectionValidator );
    when( spy.getSpoon() ).thenReturn( spoon );
    spy.showError( "title", "message" );
    verify( spoon ).messageBox( "message", "title", false, Const.ERROR );
  }

  @Test
  public void testGetMsg() {
    ConnectionValidator spy = spy( connectionValidator );
    spy.getMsg( "key" );
  }

  @Test
  public void testTestConnectionSuccess() throws KettleException {
    ConnectionValidator spy = spy( connectionValidator );
    when( spy.getSpoon() ).thenReturn( spoon );
    doReturn( true ).when( spy ).isPentahoServer();
    doReturn( true ).when( spy ).isUserInfoProvided();
    doReturn( true ).when( spy ).canConnect();

    spy.validateConnectionInDesignTime();
    verify( spy ).showInfo( anyString(), anyString() );

    spy.validateConnectionInRuntime();
  }

  @Test
  public void testTestConnection404() throws KettleException {
    ConnectionValidator spy = spy( connectionValidator );
    when( spy.getSpoon() ).thenReturn( spoon );
    doReturn( true ).when( spy ).isPentahoServer();
    doReturn( true ).when( spy ).isUserInfoProvided();
    doReturn( false ).when( spy ).canConnect();
    when( publishRestUtil.getLastHTTPStatus() ).thenReturn( 404 );

    spy.validateConnectionInDesignTime();
    verify( spy ).getMsg( "JobEntryDatasourcePublish.Test.BadURL" );
    verify( spy ).showError( anyString(), anyString() );
    try {
      spy.validateConnectionInRuntime();
    } catch ( KettleException ex ) {
      assertTrue( ex.getMessage().indexOf( "to be getting connections to the sever" ) > 0 );
    }
  }

  @Test( expected = Exception.class )
  public void testTestConnectionFailPentahoServer() throws KettleException {
    ConnectionValidator spy = spy( connectionValidator );
    when( spy.getSpoon() ).thenReturn( spoon );
    doReturn( false ).when( spy ).isPentahoServer();

    spy.validateConnectionInDesignTime();
    verify( spy ).showError( anyString(), anyString() );

    spy.validateConnectionInRuntime();
  }

  @Test( expected = Exception.class )
  public void testTestConnectionFailPentahoServerDueToException() throws KettleException {
    ConnectionValidator spy = spy( connectionValidator );
    when( spy.getSpoon() ).thenReturn( spoon );
    doThrow( new Exception(  ) ).when( spy ).isBiServerConnectionProvided();

    spy.validateConnectionInDesignTime();
    verify( spy ).showError( anyString(), anyString() );

    spy.validateConnectionInRuntime();
  }

  @Test( expected = Exception.class )
  public void testTestConnectionFailUserInfoProvided() throws KettleException {
    ConnectionValidator spy = spy( connectionValidator );
    when( spy.getSpoon() ).thenReturn( spoon );
    doReturn( true ).when( spy ).isPentahoServer();
    doReturn( false ).when( spy ).isUserInfoProvided();

    spy.validateConnectionInDesignTime();
    verify( spy ).showError( anyString(), anyString() );

    spy.validateConnectionInRuntime();
  }

  @Test( expected = Exception.class )
  public void testTestConnectionException() throws KettleException {
    ConnectionValidator spy = spy( connectionValidator );
    when( spy.getSpoon() ).thenReturn( spoon );
    doThrow( new RuntimeException() ).when( spy ).isPentahoServer();

    spy.validateConnectionInDesignTime();
    verify( spy ).showError( anyString(), anyString() );

    spy.validateConnectionInRuntime();
  }
}
