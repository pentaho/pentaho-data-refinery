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

package org.pentaho.di.ui.job.entries.common;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.gui.SpoonFactory;
import org.pentaho.di.core.gui.SpoonInterface;
import org.pentaho.di.core.refinery.publish.agilebi.BiServerConnection;
import org.pentaho.di.core.refinery.publish.util.PublishRestUtil;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.entries.publish.JobEntryDatasourcePublish;

import java.util.logging.Logger;

/**
 * @author Rowell Belen
 */
public class ConnectionValidator {

  private static final Class<JobEntryDatasourcePublish> PKG = JobEntryDatasourcePublish.class;
  private static final Logger logger = Logger.getLogger( PKG.getName() );

  private boolean suppressSuccessMessage;
  private PublishRestUtil publishRestUtil;
  private BiServerConnection connection;

  public ConnectionValidator() {
    this.publishRestUtil = new PublishRestUtil(); // default
  }

  public void validateConnectionInRuntime() throws KettleException {
    // check server
    if ( !this.isPentahoServer() ) {
      throw new KettleException( this.getMsg( "JobEntryDatasourcePublish.Test.InvalidPentahoServerMsgRuntime" ) );
    }

    // check login info
    if ( !this.isUserInfoProvided() ) {
      throw new KettleException( this.getMsg( "JobEntryDatasourcePublish.Test.MissingUserMsg" ) );
    }

    boolean isAuthenticated = !publishRestUtil.isUnauthenticatedUser( this.connection );
    if ( !isAuthenticated ) {
      throw new KettleException( this.getMsg( "JobEntryDatasourcePublish.Test.UsernamePasswordFailMsg" ) );
    }

    // test permissions
    if ( !this.canConnect() ) {
      if ( publishRestUtil.getLastHTTPStatus() == 404 ) {
        throw new KettleException( this.getMsg( "JobEntryDatasourcePublish.Test.BadURLMsg" ) );
      } else {
        throw new KettleException( this.getMsg( "JobEntryDatasourcePublish.Test.PermissionsErrorMsg" ) );
      }
    }
  }

  public boolean validateConnectionInDesignTime() {

    // check server
    if ( !this.isPentahoServer() ) {
      this.showError(
          getMsg( "JobEntryDatasourcePublish.Test.InvalidPentahoServer" ),
          getMsg( "JobEntryDatasourcePublish.Test.InvalidPentahoServerMsg" ) );

      return false; // exit
    }

    // check login info
    if ( !this.isUserInfoProvided() ) {
      this.showError(
          getMsg( "JobEntryDatasourcePublish.Test.MissingUser" ),
          getMsg( "JobEntryDatasourcePublish.Test.MissingUserMsg" ) );

      return false; // exit
    }

    boolean isAuthenticated = !publishRestUtil.isUnauthenticatedUser( this.connection );
    if ( !isAuthenticated ) {
      this.showError(
          getMsg( "JobEntryDatasourcePublish.Test.UsernamePasswordFail" ),
          getMsg( "JobEntryDatasourcePublish.Test.UsernamePasswordFailMsg" ) );

      return false; // exit
    }

    // test permissions
    if ( this.canConnect() ) {
      if ( !this.suppressSuccessMessage ) {
        this.showInfo(
            getMsg( "JobEntryDatasourcePublish.Test.Passed" ),
            getMsg( "JobEntryDatasourcePublish.Test.PassedMsg" ) );
      }

      return true; // valid

    } else {
      if ( publishRestUtil.getLastHTTPStatus() == 404 ) {
        this.showError(
            getMsg( "JobEntryDatasourcePublish.Test.BadURL" ),
            getMsg( "JobEntryDatasourcePublish.Test.BadURLMsg" ) );
      } else {
        this.showError(
            getMsg( "JobEntryDatasourcePublish.Test.PermissionsError" ),
            getMsg( "JobEntryDatasourcePublish.Test.PermissionsErrorMsg" ) );
      }
    }

    return false;
  }

  public boolean isBiServerConnectionProvided() {
    return this.connection != null;
  }

  public boolean isUserInfoProvided() {
    return publishRestUtil.isUserInfoProvided( this.connection );
  }

  public boolean isPentahoServer() {
    try {
      return isBiServerConnectionProvided() && publishRestUtil.isPentahoServer( this.connection );
    } catch ( Exception e ) {
      logger.warning( e.getLocalizedMessage() );
      return false;
    }
  }

  public boolean canConnect() {

    if ( !isPentahoServer() ) {
      return false;
    }

    boolean canPublish = publishRestUtil.canPublish( this.connection );
    if ( !canPublish ) {
      return false;
    }

    boolean canManageDatasources = publishRestUtil.canManageDatasources( this.connection );
    if ( !canManageDatasources ) {
      return false;
    }

    return true;
  }

  public void showInfo( String title, String message ) {
    SpoonInterface spoon = getSpoon();
    spoon.messageBox( message, title, false, Const.INFO );
  }

  public void showError( String title, String message ) {
    SpoonInterface spoon = getSpoon();
    spoon.messageBox( message, title, false, Const.ERROR );
  }

  public void setConnection( BiServerConnection connection ) {
    this.connection = connection;
  }

  public void setSuppressSuccessMessage( boolean suppressSuccessMessage ) {
    this.suppressSuccessMessage = suppressSuccessMessage;
  }

  public void setPublishRestUtil( PublishRestUtil publishRestUtil ) {
    this.publishRestUtil = publishRestUtil;
  }

  protected SpoonInterface getSpoon() {
    return SpoonFactory.getInstance();
  }

  protected String getMsg( String messageKey ) {
    return BaseMessages.getString( PKG, messageKey );
  }
}
