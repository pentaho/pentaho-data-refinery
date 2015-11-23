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
