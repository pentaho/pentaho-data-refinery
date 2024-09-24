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

package org.pentaho.di.ui.trans.steps.common;

import org.junit.Test;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;

import static org.junit.Assert.*;

public class ModelAnnotationEventTest {
  @Test
  public void testGetAndSet() throws Exception {
    ModelAnnotationGroup group = new ModelAnnotationGroup();
    ModelAnnotationEvent event = new ModelAnnotationEvent( group, true );
    assertSame( group, event.getModelAnnotations() );
    assertTrue( event.isActionCancelled() );
  }
}
