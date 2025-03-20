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


/*!
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
* Foundation.
*
* You should have received a copy of the GNU Lesser General Public License along with this
* program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
* or from the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*
* This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
* without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
* See the GNU Lesser General Public License for more details.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package org.pentaho.di.core.refinery.publish.agilebi;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.util.StringUtil;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;

/**
 * A bean defining a BI server connection
 * @author jamesdixon
 * @modified tyler band - removed publishPassword
 *
 */
@XmlRootElement
public class BiServerConnection implements Serializable {

  private static final long serialVersionUID = 938485134641425825L;

  private String url;

  private String userId;

  private String password;

  private String name;

  private String defaultFolder;

  private boolean defaultDatasourcePublish = false;

  public BiServerConnection() {
    super();
  }

  public String getDefaultFolder() {
    return defaultFolder;
  }

  public void setDefaultFolder( String defaultFolder ) {
    this.defaultFolder = defaultFolder;
  }

  public boolean getDefaultDatasourcePublish() {
    return defaultDatasourcePublish;
  }

  public void setDefaultDatasourcePublish( boolean defaultDatasourcePublish ) {
    this.defaultDatasourcePublish = defaultDatasourcePublish;
  }

  /**
   * Gets the URL for the BI server
   * In the form protocol:server:port/context, e.g. http://localhost:8080/pentaho
   * @return
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets the URL for the BI server
   * In the form protocol:server:port/context, e.g. http://localhost:8080/pentaho
   * @param url
   */
  public void setUrl( String url ) {
    this.url = url;
    if ( Const.isEmpty( url ) ) {
      return;
    }
    if ( ( this.url.charAt( this.url.length() - 1 ) != RepositoryFile.SEPARATOR.charAt( 0 ) )
        && !StringUtil.isVariable( this.url ) ) {
      this.url = this.url + RepositoryFile.SEPARATOR;
    }

  }

  /**
   * Gets the user id for the BI server connection
   * @return
   */
  public String getUserId() {
    return userId;
  }

  /**
   * Sets the user id for the BI server
   * @param userId
   */
  public void setUserId( String userId ) {
    this.userId = userId;
  }

  /**
   * Gets the password for the BI server connection
   * @return
   */
  public String getPassword() {
    return password;
  }

  /**
   * Sets the password for the BI server
   * @param password
   */
  public void setPassword( String password ) {
    this.password = password;
  }

  /**
   * Gets the name for the BI server connection
   * @return
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name for the BI server
   * @param name
   */
  public void setName( String name ) {
    this.name = name;
  }

}
