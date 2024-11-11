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


package org.pentaho.di.ui.trans.steps.common;

import org.eclipse.swt.widgets.Event;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;

public class ModelAnnotationEvent extends Event {

  private ModelAnnotationGroup modelAnnotations;
  private boolean actionCancelled;

  public ModelAnnotationEvent( ModelAnnotationGroup modelAnnotations ) {
    this.modelAnnotations = modelAnnotations;
  }

  public ModelAnnotationEvent( ModelAnnotationGroup modelAnnotations, boolean actionCancelled ) {
    this.modelAnnotations = modelAnnotations;
    this.actionCancelled = actionCancelled;
  }

  public ModelAnnotationGroup getModelAnnotations() {
    return modelAnnotations;
  }

  public boolean isActionCancelled() {
    return actionCancelled;
  }
}
