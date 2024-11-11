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

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.util.StringUtil;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.steps.annotation.BaseAnnotationMeta;
import org.pentaho.di.trans.steps.annotation.SharedDimensionGroupValidation;
import org.pentaho.di.trans.util.TransUtil;
import org.pentaho.di.ui.core.widget.ComboVar;
import org.pentaho.metastore.api.IMetaStore;


public class SharedDimensionComposite extends BaseComposite {

  private ComboVar wOutputSteps;
  private GroupComposite groupComposite;

  public SharedDimensionComposite( Composite composite, int i ) {
    super( composite, i );
  }

  public void createWidgets() {

    setDefaultRowLayout();

    Label wlSharedDimension = new Label( this, SWT.LEFT );
    wlSharedDimension.setText(
        BaseMessages.getString( getLocalizationPkg(), "SharedDimension.Dialog.SharedDimension.Label" ) );
    setLook( wlSharedDimension );

    groupComposite = new GroupComposite( this, getVariables() );
    groupComposite.setLocalizationPkg( getLocalizationPkg() );
    groupComposite.setLog( getLog() );
    groupComposite.createWidgets();

    // Override default tooltips
    groupComposite.setAddGroupTooltip(
        BaseMessages.getString( getLocalizationPkg(), "SharedDimension.AddGroup.ToolTip" ) );
    groupComposite.setCopyGroupTooltip(
        BaseMessages.getString( getLocalizationPkg(), "SharedDimension.CopyGroup.ToolTip" ) );

    Label spacer = new Label( this, SWT.NONE );
    spacer.setLayoutData( new RowData( 0, 4 ) );

    Label wlOutputSteps = new Label( this, SWT.LEFT );
    wlOutputSteps.setText(
        BaseMessages.getString( getLocalizationPkg(), "SharedDimension.Dialog.DataProvider.Label" ) );
    setLook( wlOutputSteps );

    wOutputSteps = new ComboVar( getVariables(), this, SWT.BORDER );
    RowData rowData = new RowData();
    rowData.width = 300;
    wOutputSteps.setLayoutData( rowData );
    setLook( wOutputSteps );
  }

  public void setEnabled( boolean enabled ) {
    wOutputSteps.setEnabled( enabled );
    groupComposite.setEnabled( enabled );
  }

  public void populateOutputSteps( BaseAnnotationMeta input, TransMeta transMeta,
                                   Repository repository, IMetaStore metaStore ) {
    try {
      for ( String key : TransUtil.collectOutputStepInTrans( transMeta, repository, metaStore ).keySet() ) {
        wOutputSteps.add( key );
      }
      wOutputSteps.getCComboWidget().setEnabled( true );
      selectOutputStep( input );
    } catch ( KettleException e ) {
      logError( "error collecting output steps", e );
    }
  }

  private void selectOutputStep( BaseAnnotationMeta input ) {
    String[] items = wOutputSteps.getItems();
    for ( int i = 0; i < items.length; i++ ) {
      String item = items[i];
      if ( StringUtils.equals( item, input.getTargetOutputStep() ) ) {
        wOutputSteps.select( i );
        return;
      }
    }
    if ( !StringUtil.isEmpty( input.getTargetOutputStep() ) ) {
      wOutputSteps.setText( input.getTargetOutputStep() );
    }
  }

  public String getOutputStepName() {
    return wOutputSteps.getText();
  }

  @Override public boolean setFocus() {
    return wOutputSteps.setFocus();
  }

  public boolean validateAnnotationGroup( ModelAnnotationGroup mag ) {
    if ( mag != null ) {
      SharedDimensionGroupValidation validation = new SharedDimensionGroupValidation( mag, getLog() );
      if ( validation.hasErrors() ) {
        StringBuilder str = new StringBuilder();
        for ( String msg : validation.getErrorSummary() ) {
          str.append( msg );
          str.append( Const.CR );
        }
        showError(
            BaseMessages.getString( getLocalizationPkg(), "ModelAnnotation.SharedDimension.ValidationError.Title" ),
            str.toString() );
        return false;
      }
    }
    return true;
  }

  public ComboVar getGroupComboWidget() {
    return groupComposite.getGroupComboWidget();
  }

  public String getGroupName() {
    return groupComposite.getGroupComboWidget().getText();
  }

  public void setAddGroupListener( Listener listener ) {
    groupComposite.setAddGroupListener( listener );
  }

  public void setCopyGroupListener( Listener listener ) {
    groupComposite.setCopyGroupListener( listener );
  }

  public void setEnableAddCopyButtons( final boolean enable ) {
    groupComposite.setEnableAddCopyButtons( enable );
  }

  public ComboVar getOutputStepsWidget() {
    return wOutputSteps;
  }

}
