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
