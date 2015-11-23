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

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.pentaho.agilebi.modeler.models.annotations.AnnotationType;
import org.pentaho.agilebi.modeler.models.annotations.CreateCalculatedMember;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation;
import org.pentaho.di.core.refinery.UIBuilder;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.ui.trans.steps.annotation.OptionsResolver;

/**
 * @author Rowell Belen
 */
public class CalculatedMeasureComposite extends BaseComposite {

  private static String DEFAULT_DIMENSION = "Measures";

  private Label wlMeasureAction;
  private Label wMeasureAction;

  private Label wlMeasureName;
  private Text wMeasureName;

  private Label wlFormat;
  private CCombo wFormat;

  private Label wlFormula;
  private Text wFormula;

  private Button wCalculateSubtotals;

  private OptionsResolver optionsResolver;

  public CalculatedMeasureComposite( Composite composite ) {
    super( composite, SWT.NONE );
    optionsResolver = new OptionsResolver();
  }

  public void createWidgets() {

    wlMeasureAction = new Label( this, SWT.LEFT );
    wlMeasureAction
        .setText( BaseMessages.getString( getLocalizationPkg(), "CalculatedMeasure.Composite.ModelAction.Label" ) );
    setLook( wlMeasureAction );
    UIBuilder.positionControlBelow( wlMeasureAction, null, 10, 10 );

    wMeasureAction = new Label( this, SWT.LEFT );
    wMeasureAction
        .setText( BaseMessages.getString( getLocalizationPkg(), "CalculatedMeasure.Composite.ModelAction.Text" ) );
    setLook( wMeasureAction );
    UIBuilder.positionControlBelow( wMeasureAction, wlMeasureAction, 5, 10 );

    wlMeasureName = new Label( this, SWT.LEFT );
    wlMeasureName.setText( BaseMessages.getString( getLocalizationPkg(), "CalculatedMeasure.Composite.Name.Label" ) );
    setLook( wlMeasureName );
    UIBuilder.positionControlBelow( wlMeasureName, wMeasureAction, 10, 10 );

    wMeasureName = new Text( this, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    setLook( wMeasureName );
    UIBuilder.positionControlBelow( wMeasureName, wlMeasureName, 5, 10 );
    setLength( wMeasureName, 250 );

    wlFormat = new Label( this, SWT.LEFT );
    wlFormat.setText( BaseMessages.getString( getLocalizationPkg(), "CalculatedMeasure.Composite.Format.Label" ) );
    setLook( wlFormat );
    UIBuilder.positionControlBelow( wlFormat, wMeasureName, 10, 10 );

    wFormat = new CCombo( this, SWT.LEFT | SWT.BORDER );
    wFormat.setItems( optionsResolver.resolveMeasureFormatOptions() );
    setLook( wFormat );
    UIBuilder.positionControlBelow( wFormat, wlFormat, 5, 10 );
    setLength( wFormat, 125 );

    wlFormula = new Label( this, SWT.LEFT );
    wlFormula.setText( BaseMessages.getString( getLocalizationPkg(), "CalculatedMeasure.Composite.Formula.Label" ) );
    setLook( wlFormula );
    UIBuilder.positionControlBelow( wlFormula, wFormat, 10, 10 );

    wCalculateSubtotals = new Button( this, SWT.CHECK );
    wCalculateSubtotals.setText(
        BaseMessages.getString( getLocalizationPkg(), "CalculatedMeasure.Composite.Subtotal.Label" ) );
    setLook( wCalculateSubtotals );
    FormData fdCalculatedSubtotals = new FormData();
    fdCalculatedSubtotals.left = new FormAttachment( 0, 10 );
    fdCalculatedSubtotals.right = new FormAttachment( 100, -10 );
    fdCalculatedSubtotals.bottom = new FormAttachment( 100, -10 );
    wCalculateSubtotals.setLayoutData( fdCalculatedSubtotals );

    wFormula = new Text( this, SWT.MULTI | SWT.BORDER | SWT.WRAP );
    setLook( wFormula );
    FormData formData = new FormData();
    formData.top = new FormAttachment( wlFormula, 5 );
    formData.left = new FormAttachment( 0, 10 );
    formData.right = new FormAttachment( 100, -10 );
    formData.bottom = new FormAttachment( wCalculateSubtotals, -10 );
    wFormula.setLayoutData( formData );
  }

  public void load( ModelAnnotation<?> modelAnnotation ) {

    if ( modelAnnotation == null || modelAnnotation.getAnnotation() == null || !( modelAnnotation
        .getType() == ModelAnnotation.Type.CREATE_CALCULATED_MEMBER ) ) {
      return;
    }

    CreateCalculatedMember calculatedMember = (CreateCalculatedMember) modelAnnotation.getAnnotation();
    wMeasureName.setText( StringUtils.defaultIfBlank( calculatedMember.getName(), "" ) );
    wFormat.setText( StringUtils.defaultIfBlank( calculatedMember.getFormatString(), "" ) );
    wFormula.setText( StringUtils.defaultIfBlank( calculatedMember.getFormula(), "" ) );
    wCalculateSubtotals.setSelection( calculatedMember.isCalculateSubtotals() );
  }

  public void save( AnnotationType annotationType ) {

    if ( annotationType == null || !( annotationType.getType() == ModelAnnotation.Type.CREATE_CALCULATED_MEMBER ) ) {
      return;
    }

    CreateCalculatedMember calculatedMember = (CreateCalculatedMember) annotationType;

    calculatedMember.setName( StringUtils.defaultIfBlank( wMeasureName.getText(), null ) );
    calculatedMember.setFormatString( StringUtils.defaultIfBlank( wFormat.getText(), null ) );
    calculatedMember.setFormula( StringUtils.defaultIfBlank( wFormula.getText(), null ) );
    calculatedMember.setDimension( DEFAULT_DIMENSION );
    calculatedMember.setCalculateSubtotals( wCalculateSubtotals.getSelection() );
  }

  public ModelAnnotation<?> get() {
    return null;
  }

  private void setLength( Control control, int width ) {
    ( (FormData) control.getLayoutData() ).width = width;
  }
}
