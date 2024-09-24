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

package org.pentaho.di.core.refinery.publish.model;

import java.io.Serializable;

import org.pentaho.di.core.refinery.publish.agilebi.BiServerConnection;

/**
 * @author Rowell Belen
 */
public class DataSourcePublishModel implements Serializable {

  private static final long serialVersionUID = 7797060550124837560L;
  public static final String ACCESS_TYPE_EVERYONE = "everyone";
  public static final String ACCESS_TYPE_USER = "user";
  public static final String ACCESS_TYPE_ROLE = "role";

  private String modelName = "";
  private boolean override;
  private String userOrRole;
  private String accessType = ACCESS_TYPE_EVERYONE;

  private BiServerConnection biServerConnection;

  public BiServerConnection getBiServerConnection() {
    return biServerConnection;
  }

  public void setBiServerConnection( BiServerConnection biServerConnection ) {
    this.biServerConnection = biServerConnection;
  }

  public String getModelName() {
    return modelName;
  }

  public void setModelName( String modelName ) {
    this.modelName = modelName;
  }

  public boolean isOverride() {
    return override;
  }

  public void setOverride( boolean override ) {
    this.override = override;
  }

  public String getUserOrRole() {
    return userOrRole;
  }

  public void setUserOrRole( String userOrRole ) {
    this.userOrRole = userOrRole;
  }

  public String getAccessType() {
    return accessType;
  }

  public void setAccessType( String accessType ) {
    this.accessType = accessType;
  }
}
