/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 - 2026 by Pentaho Canada Inc. : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2030-06-15
 ******************************************************************************/



package org.pentaho.di.core.refinery.publish.model;

/**
 * @author Rowell Belen
 */
public class ResponseStatus {

  private int status;
  private String message;

  public int getStatus() {
    return status;
  }

  public void setStatus( int status ) {
    this.status = status;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage( String message ) {
    this.message = message;
  }
}
