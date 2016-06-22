/*! ******************************************************************************
 *
 * Pentaho Community Edition Project: data-refinery-pdi-plugin
 *
 * Copyright (C) 2002-2016 by Pentaho : http://www.pentaho.com
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

package org.pentaho.di.ui.trans.steps.annotation;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.agilebi.modeler.ModelerException;
import org.pentaho.agilebi.modeler.models.annotations.CreateCalculatedMember;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.steps.annotation.BaseAnnotationMeta;
import org.pentaho.di.ui.trans.steps.common.CalculatedMeasureComposite;
import org.pentaho.metastore.api.IMetaStore;

/**
 * @author Rowell Belen
 */
public class ModelAnnotationActionCustomDialog extends ModelAnnotationActionPropertiesDialog {

  protected Composite mainPanel;
  protected StackLayout mainPanelLayout;
  protected CalculatedMeasureComposite calculatedMeasureComposite;

  public ModelAnnotationActionCustomDialog( Shell parent,
                                            BaseAnnotationMeta baseStepMeta, TransMeta transMeta,
                                            String stepname, IMetaStore metaStore ) {
    super( parent, baseStepMeta, transMeta, stepname, metaStore );
  }

  protected void createMainPanel() {

    // create the composite that the pages will share
    mainPanel = new Composite( shell, SWT.NONE );
    FormData fd = new FormData();
    fd.top = new FormAttachment( 0, 0 );
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( 100, 0 );
    fd.bottom = new FormAttachment( wBottomHorizontalSeparator, 0 );
    mainPanel.setLayoutData( fd );

    mainPanelLayout = new StackLayout();
    mainPanel.setLayout( mainPanelLayout );
  }

  protected void showTopControl( Control control ) {
    mainPanelLayout.topControl = control;
    mainPanel.layout();
  }

  @Override
  protected Composite getMainComposite() {
    return mainPanel;
  }

  protected void createCalculatedMeasureComposite() {
    calculatedMeasureComposite = new CalculatedMeasureComposite( mainPanel );
    calculatedMeasureComposite.setLocalizationPkg( PKG );
    calculatedMeasureComposite.createWidgets();

    // take up the entire space of the main panel
    FormLayout layout = new FormLayout();
    layout.marginLeft = 0;
    layout.marginRight = 0;
    layout.marginBottom = 0;
    layout.marginTop = 5; // minor adjustment
    layout.marginHeight = 0;
    calculatedMeasureComposite.setLayout( layout );
  }

  @Override
  public void createWidgets() {

    // create main panel
    createMainPanel();

    // create contentPanel
    super.createWidgets(); // initializes contentPanel

    // create calculated measure panel
    createCalculatedMeasureComposite();

    showTopControl( contentPanel ); // default
  }

  @Override
  public void populateDialog() {

    if ( getModelAnnotation() == null || getModelAnnotation().getAnnotation() == null ) {
      return;
    }

    if ( getModelAnnotation().getAnnotation().getType() == ModelAnnotation.Type.CREATE_CALCULATED_MEMBER ) {
      calculatedMeasureComposite.load( getModelAnnotation() );
      showTopControl( calculatedMeasureComposite );
    } else {
      super.populateDialog();
      showTopControl( contentPanel );
    }
  }

  @Override
  protected void persistAnnotationProperties() throws ModelerException {
    if ( getModelAnnotation() == null || getModelAnnotation().getAnnotation() == null ) {
      return;
    }

    if ( getModelAnnotation().getAnnotation().getType() == ModelAnnotation.Type.CREATE_CALCULATED_MEMBER ) {

      CreateCalculatedMember annotationType = new CreateCalculatedMember();
      annotationType.setPdiContext( true );
      calculatedMeasureComposite.save( annotationType );
      if ( annotationType != null ) {
        annotationType.validate(); // may throw ModelerException
      }

      // it's valid, so save it
      getModelAnnotation().setAnnotation( annotationType ); // always override existing type

    } else {
      super.persistAnnotationProperties();
    }
  }
}
