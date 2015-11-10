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

package org.pentaho.di.core.refinery.publish.model;

import org.pentaho.di.core.refinery.publish.agilebi.BiServerConnection;

/**
 * @author Rowell Belen
 */
public class DataSourcePublishModel {

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
