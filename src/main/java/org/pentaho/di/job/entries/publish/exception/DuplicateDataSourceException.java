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


package org.pentaho.di.job.entries.publish.exception;

/**
 * Created by bmorrise on 9/26/16.
 */
public class DuplicateDataSourceException extends Exception {
  public DuplicateDataSourceException( String s ) {
    super( s );
  }

  public DuplicateDataSourceException() {

  }
}
