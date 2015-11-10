/*!
 * PENTAHO CORPORATION PROPRIETARY AND CONFIDENTIAL
 *
 * Copyright 2002 - 2015 Pentaho Corporation (Pentaho). All rights reserved.
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
