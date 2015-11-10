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
import org.pentaho.di.trans.steps.annotation.ModelAnnotationMeta;
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
      ModelAnnotationMeta baseStepMeta, TransMeta transMeta,
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
