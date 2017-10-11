/*! ******************************************************************************
 *
 * Pentaho Community Edition Project: data-refinery-pdi-plugin
 *
 * Copyright (C) 2002-2017 by Hitachi Vantara : http://www.pentaho.com
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

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationManager;
import org.pentaho.di.core.refinery.publish.util.ObjectUtils;
import org.pentaho.di.core.util.StringUtil;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.ui.core.ConstUI;
import org.pentaho.di.ui.core.widget.ComboVar;
import org.pentaho.di.ui.trans.steps.common.ModelAnnotationsTableComposite;
import org.pentaho.di.ui.trans.steps.common.SharedDimensionComposite;
import org.pentaho.di.ui.util.SwtSvgImageUtil;

public class SharedDimensionDialog extends BaseAnnotationStepDialog {

  private SharedDimensionComposite sharedDimensionComposite;
  private ModelAnnotationsTableComposite tableComposite;

  public SharedDimensionDialog( Shell parent, Object baseStepMeta, TransMeta transMeta, String stepname ) {
    super( parent, baseStepMeta, transMeta, stepname );
    input.setSharedDimension( true );
  }

  @Override
  protected String getDialogTitle() {
    return BaseMessages.getString( PKG, "SharedDimension.Dialog.Title" );
  }

  @Override
  protected String getStepName() {
    return BaseMessages.getString( PKG, "SharedDimension.StepName" );
  }

  @Override
  protected Image getImageIcon() {
    return SwtSvgImageUtil
        .getUniversalImage( shell.getDisplay(), getClass().getClassLoader(), "SharedDimensions.svg" )
        .getAsBitmapForSize( shell.getDisplay(), ConstUI.ICON_SIZE, ConstUI.ICON_SIZE );
  }

  @Override
  protected void createContent( final Control topWidget ) {
    Control top = topWidget;
    top = createSharedDimensionComposite( top );
    top = createDescription( top );
    createFieldsTable( top );
    resizeSummaryColumn( tableComposite );
    sharedDimensionComposite.getOutputStepsWidget().addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent sel ) {
        if ( !StringUtils.equals( input.getTargetOutputStep(), sharedDimensionComposite.getOutputStepName() ) ) {
          onModelAnnotationDirty();
        }
      }
    } );
    sharedDimensionComposite.getOutputStepsWidget().addModifyListener( new ModifyListener() {
      public void modifyText( ModifyEvent modifyEvent ) {
        if ( !StringUtils.equals( input.getTargetOutputStep(), sharedDimensionComposite.getOutputStepName() ) ) {
          onModelAnnotationDirty();
        }
      }
    } );
  }

  @Override protected ModelAnnotationManager getModelAnnotationManger() {
    return new ModelAnnotationManager( true ); // use a separate namespace
  }

  @Override
  protected ModelAnnotationsTableComposite getTableComposite() {
    return tableComposite;
  }

  @Override
  protected String getSelectedModelAnnotationGroupName() {
    return sharedDimensionComposite.getGroupName();
  }

  @Override
  protected void initializeListeners() {
    super.initializeListeners();
  }

  @Override
  protected void afterOpen() {
    populateOutputSteps();
    populateCategories( envSub( input.getModelAnnotationCategory() ), true );
    if ( StringUtils.isBlank( envSub( input.getModelAnnotationCategory() ) ) ) {
      input.getModelAnnotations().setSharedDimension( true ); // default model should be shared dimension
      populateTable( input.getModelAnnotations() );
      sharedDimensionComposite.setEnableAddCopyButtons( false );
    } else {
      populateTableFromMetastore();
      if ( StringUtil.isVariable( input.getModelAnnotationCategory() ) ) {
        setAnnotationsEditEnabled( false );
        sharedDimensionComposite.setEnableAddCopyButtons( false ); // disable if variable
      } else {
        sharedDimensionComposite.setEnableAddCopyButtons( true );
      }
    }

    wApply.setEnabled( false ); // force disable
    getWGroups().setEditable( true ); // allow rename
  }

  @Override protected void populateTable( final ModelAnnotationGroup modelAnnotations ) {
    super.populateTable( modelAnnotations );
  }

  private void populateOutputSteps() {
    sharedDimensionComposite.populateOutputSteps( input, transMeta, getRepository(), getMetaStore() );
  }

  private Control createSharedDimensionComposite( Control topWidget ) {
    // Initialize Shared Dimension Composite
    sharedDimensionComposite = new SharedDimensionComposite( shell, SWT.NULL );
    sharedDimensionComposite.setVariables( transMeta );
    sharedDimensionComposite.setLocalizationPkg( PKG );
    sharedDimensionComposite.setLog( log );
    sharedDimensionComposite.createWidgets();

    sharedDimensionComposite.setAddGroupListener( lsAddGroup );
    sharedDimensionComposite.setCopyGroupListener( lsCopyGroup );

    getWGroups().getCComboWidget().addListener( SWT.Selection, lsGroups );
    getWGroups().getCComboWidget().addListener( SWT.FocusOut, lsCheckForVariables );
    getWGroups().getCComboWidget().addListener( SWT.KeyUp, new Listener() {
      @Override public void handleEvent( Event event ) {
        if ( event.keyCode == 13 ) {
          sharedDimensionComposite.setFocus();  //unfocuses wGroups
        }
      }
    }
    );
    getWGroups().getCComboWidget().addModifyListener( new ModifyListener() {
      @Override public void modifyText( ModifyEvent modifyEvent ) {
        if ( StringUtils.equals( getWGroups().getText(), getTableComposite().getData().getName() ) ) {
          sharedDimensionComposite.setEnableAddCopyButtons( true );
        } else {
          sharedDimensionComposite.setEnableAddCopyButtons( false );
        }
      }
    } );

    positionControl( sharedDimensionComposite, topWidget );

    return sharedDimensionComposite;
  }

  private void positionControl( Composite composite, Control topWidget ) {

    FormData fData = new FormData();
    fData.top = new FormAttachment( topWidget, 15, SWT.TOP );
    fData.left = new FormAttachment( 0, LEFT_MARGIN_OFFSET + 2 );
    fData.right = new FormAttachment( topWidget, 0, SWT.RIGHT );
    composite.setLayoutData( fData );

    props.setLook( composite );
  }

  private void createFieldsTable( Control topWidget ) {

    tableComposite = new ModelAnnotationsTableComposite( shell, topWidget, wBottomHorizontalSeparator ) {
      @Override
      protected void onDelete() {
        SharedDimensionDialog.this.deleteAnnotation();
      }

      @Override
      protected void onEdit() {
        SharedDimensionDialog.this.editAnnotation();
      }

      @Override
      protected void onGetFields() {
        SharedDimensionDialog.this.getFields();
      }

      @Override
      protected void onItemSelect() {
        toggleDeleteEditButtons();
      }

      @Override
      protected void onItemDoubleClick() {
        SharedDimensionDialog.this.editAnnotation();
      }

      @Override
      protected boolean supportsCalculatedMeasure() {
        return Boolean.FALSE;
      }

      @Override protected void onCreateCalculatedMeasure() {
        throw new UnsupportedOperationException();
      }
    };

    // Inject other dependencies
    tableComposite.setLocalizationPkg( PKG );
    tableComposite.setLog( log );

    // initialize
    tableComposite.createWidgets();
  }

  @Override
  protected void onModelAnnotationDirty() {
    super.onModelAnnotationDirty();
    sharedDimensionComposite.setEnableAddCopyButtons( false );

    if ( StringUtils.equals( getWGroups().getText(), input.getModelAnnotationCategory() ) ) {
      getWGroups().setEditable( false ); // prevent rename, unless triggered by rename
    }
  }

  @Override
  protected boolean applyChanges() {

    try {
      input.checkValidName( sharedDimensionComposite.getGroupName() );
    } catch ( Exception e ) {
      showError( BaseMessages.getString( PKG, "ModelAnnotation.MetaStoreSaveError.Title" ), e.getLocalizedMessage() );
      return false;
    }

    if ( !sharedDimensionComposite.validateAnnotationGroup( tableComposite.getData() ) ) {
      return false;
    }

    // TODO refactor this with ModelAnnotationDialog
    // we're losing the ability to cancel here
    boolean prevChanged = input.hasChanged();
    String prevStepName = wStepname.getText();
    ModelAnnotationGroup prevAnnotations = ObjectUtils.deepClone( input.getModelAnnotations() );
    String prevOutStep = input.getTargetOutputStep();
    String prevCategory = input.getModelAnnotationCategory();

    input.setChanged(
        prevChanged
            || !stepname.equals( wStepname.getText() )
            || !input.getModelAnnotations().equals( tableComposite.getData() )
            || !StringUtils.equals( input.getModelAnnotations().getDescription(), wDescription.getText() )
            || !StringUtils.equals( prevOutStep, sharedDimensionComposite.getOutputStepName() ) );
    stepname = wStepname.getText(); // return value
    input.setModelAnnotations( tableComposite.getData() );
    input.getModelAnnotations().setName( sharedDimensionComposite.getGroupName() );
    input.getModelAnnotations().setDescription( wDescription.getText() );
    input.getModelAnnotations().setSharedDimension( true );
    input.setSharedDimension( true ); // always true in this dialog
    input.setTargetOutputStep( sharedDimensionComposite.getOutputStepName() );

    input.setModelAnnotationCategory( getWGroups().getText() );

    if ( !StringUtil.isVariable( getWGroups().getText() ) ) {
      // vars are only referenced
      try {
        input.saveToMetaStore( getMetaStore() );
      } catch ( Exception e ) {
        showError( BaseMessages.getString( PKG, "ModelAnnotation.MetaStoreSaveError.Title" ),
            BaseMessages.getString( PKG, "ModelAnnotation.MetaStoreSaveError.Message", e.getLocalizedMessage() ) );

        // hasty undo
        input.setChanged( prevChanged );
        stepname = prevStepName;
        input.setModelAnnotations( prevAnnotations );
        input.setTargetOutputStep( prevOutStep );
        input.setModelAnnotationCategory( prevCategory );

        return false;
      }

      // Delete groups that need to be deleted - i.e. renamed groups
      deleteStaleGroups( true );

      sharedDimensionComposite.setEnableAddCopyButtons( true ); // force enable after apply
      getWGroups().setEditable( true );
    }

    return true;
  }

  @Override protected ComboVar getWGroups() {
    return sharedDimensionComposite.getGroupComboWidget();
  }

  @Override
  protected ModelAnnotationGroup createNewAnnotationGroup() {
    ModelAnnotationGroup newGroup = new ModelAnnotationGroup();
    newGroup.setSharedDimension( true );
    return newGroup;
  }

  @Override
  protected String missingMessageKey() {
    return "ModelAnnotation.MissingSharedDimension.Message";
  }

  @Override
  protected String missingTitleKey() {
    return "ModelAnnotation.MissingSharedDimension.Title";
  }
}
