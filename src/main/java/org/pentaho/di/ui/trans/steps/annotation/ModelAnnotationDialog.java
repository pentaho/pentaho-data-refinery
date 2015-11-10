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

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationManager;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.plugins.PluginInterface;
import org.pentaho.di.core.refinery.publish.util.ObjectUtils;
import org.pentaho.di.core.util.StringUtil;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.annotation.ModelAnnotationMeta;
import org.pentaho.di.ui.core.ConstUI;
import org.pentaho.di.ui.core.widget.ComboVar;
import org.pentaho.di.ui.trans.steps.common.LocalLinkedComposite;
import org.pentaho.di.ui.trans.steps.common.ModelAnnotationEvent;
import org.pentaho.di.ui.trans.steps.common.ModelAnnotationsTableComposite;
import org.pentaho.di.ui.util.SwtSvgImageUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Rowell Belen
 */
public class ModelAnnotationDialog extends BaseAnnotationStepDialog {

  private static final Class<?> PKG = ModelAnnotationMeta.class; // for i18n purposes, needed by Translator2!!

  private ModelAnnotationMeta input;
  private Button wHelp;
  private LocalLinkedComposite wLocalLinked;
  private ModelAnnotationsTableComposite tableComposite;
  private Button bLocal;
  private Button bLinked;
  private Listener lsLinked;
  private Listener lsLocal;

  public ModelAnnotationDialog( Shell parent, Object in, TransMeta transMeta, String stepname ) {
    super( parent, (StepMetaInterface) in, transMeta, stepname );
    input = (ModelAnnotationMeta) baseStepMeta;
  }

  @Override
  protected void initializeListeners() {

    super.initializeListeners();

    lsLinked = new Listener() {
      @Override
      public void handleEvent( final Event event ) {
        bLocal.setSelection( false );
        getWGroups().setEditable( true );
        setShareEnabled( true );
        if ( tableComposite.getTable().getItemCount() > 0 ) {
          getWGroups().setText( "" );
          getWGroups().getCComboWidget().setFocus();
          getWGroups().getCComboWidget().setSelection( new Point( 0, 0 ) );
        } else if ( getWGroups().getItemCount() > 0 ) {
          getWGroups().select( 0 );
          populateTableFromMetastore();
          onModelAnnotationDirty();
          wLocalLinked.setEnableAddCopyButtons( true ); // special case - override previous setting
        }
      }
    };

    lsLocal = new Listener() {
      @Override
      public void handleEvent( final Event event ) {
        bLocal.setSelection( false );
        Listener lsWarningYes = new Listener() {
          @Override
          public void handleEvent( final Event event ) {
            bLocal.setSelection( true );
            bLinked.setSelection( false );
            wLocalLinked.setEnabled( false );
            populateCategories( null, false );
            onModelAnnotationDirty();
            ( tableComposite.getData() ).setSharedDimension( false );
            setAnnotationsEditEnabled( true );
          }
        };
        Listener lsWarningNo = new Listener() {
          @Override
          public void handleEvent( final Event event ) {
            setShareEnabled( true );
          }
        };
        Map<String, Listener> listenerMap = new LinkedHashMap<String, Listener>();
        listenerMap.put( BaseMessages.getString( PKG, "System.Button.No" ), lsWarningNo );
        listenerMap.put( BaseMessages.getString( PKG, "ModelAnnotation.LocalWarning.Yes" ), lsWarningYes );
        WarningDialog warningDialog = new WarningDialog(
            shell,
            BaseMessages.getString( PKG, "ModelAnnotation.LocalWarning.Title" ),
            BaseMessages.getString( PKG, "ModelAnnotation.LocalWarning.Message" ),
            listenerMap );
        warningDialog.open();
      }
    };
  }

  private Control createLocalLinked( final Control topWidget ) {

    // Initialize Local/Linked composite
    wLocalLinked = new LocalLinkedComposite( shell, SWT.NULL );
    wLocalLinked.setLocalizationPkg( PKG );
    wLocalLinked.setLog( log );
    wLocalLinked.setVariables( transMeta );
    wLocalLinked.createWidgets();

    //TODO - Encapsulate this
    bLocal = wLocalLinked.getbLocal();
    bLinked = wLocalLinked.getbShared();

    bLinked.addListener( SWT.Selection, lsLinked );
    bLocal.addListener( SWT.Selection, lsLocal );

    wLocalLinked.setAddGroupListener( lsAddGroup );
    wLocalLinked.setCopyGroupListener( lsCopyGroup );
    getWGroups().getCComboWidget().addListener( SWT.Selection, lsGroups );
    getWGroups().getCComboWidget().addListener( SWT.FocusOut, lsCheckForVariables );
    getWGroups().getCComboWidget().addListener( SWT.KeyUp, new Listener() {
      @Override public void handleEvent( Event event ) {
        if ( event.keyCode == 13 ) {
          wDescription.setFocus(); //effectivley unfocuses wGroups
        }
      }
    } );
    getWGroups().getCComboWidget().addModifyListener( new ModifyListener() {
      @Override public void modifyText( ModifyEvent modifyEvent ) {
        if ( StringUtils.equals( getWGroups().getText(), getTableComposite().getData().getName() ) ) {
          wLocalLinked.setEnableAddCopyButtons( true );
        } else {
          wLocalLinked.setEnableAddCopyButtons( false );
        }
      }
    } );

    positionControl( wLocalLinked, topWidget );

    return wLocalLinked;
  }

  private void positionControl( Composite composite, Control topWidget ) {

    FormData fData = new FormData();
    fData.top = new FormAttachment( topWidget, 17, SWT.TOP );
    fData.left = new FormAttachment( 0, LEFT_MARGIN_OFFSET + 2 );
    fData.right = new FormAttachment( topWidget, 0, SWT.RIGHT );
    composite.setLayoutData( fData );

    props.setLook( composite );
  }

  private void createFieldsTable( Control topWidget ) {

    tableComposite = new ModelAnnotationsTableComposite( shell, topWidget, wBottomHorizontalSeparator ) {
      @Override
      protected void onDelete() {
        ModelAnnotationDialog.this.deleteAnnotation();
      }

      @Override
      protected void onEdit() {
        ModelAnnotationDialog.this.editAnnotation();
      }

      @Override
      protected void onGetFields() {
        ModelAnnotationDialog.this.getFields();
      }

      @Override
      protected void onItemSelect() {
        toggleDeleteEditButtons();
      }

      @Override
      protected void onItemDoubleClick() {
        ModelAnnotationDialog.this.editAnnotation();
      }

      @Override
      protected boolean supportsCalculatedMeasure() {
        return Boolean.TRUE;
      }

      @Override protected void onCreateCalculatedMeasure() {
        ModelAnnotationDialog.this.createCalculatedMeasure();
      }
    };
    // Inject other dependencies
    tableComposite.setLocalizationPkg( PKG );
    tableComposite.setLog( log );

    // initialize
    tableComposite.createWidgets();
  }

  public void createCalculatedMeasure() {
    final ModelAnnotationGroup groupCopy = ObjectUtils.deepClone( getTableComposite().getData() );
    getTableComposite().createCalculatedMeasure( shell, input, transMeta, stepname, getMetaStore(), new Listener() {
      @Override public void handleEvent( Event event ) {
        if ( event instanceof ModelAnnotationEvent ) {
          ModelAnnotationEvent e = (ModelAnnotationEvent) event;
          if ( !modelAnnotationDirty ) {
            setDirtyFlag( groupCopy, e.getModelAnnotations() );
          }
          populateTable( e.getModelAnnotations() ); // Update Table
        }
      }
    } );
  }

  @Override
  protected Button createHelpButton( final Shell shell, final StepMeta stepMeta, final PluginInterface plugin ) {
    wHelp = super.createHelpButton( shell, stepMeta, plugin ); // capture help button from base class
    FormData fdHelp = new FormData();
    fdHelp.bottom = new FormAttachment( 100, -10 );
    fdHelp.left = new FormAttachment( 0, LEFT_MARGIN_OFFSET );
    wHelp.setLayoutData( fdHelp );
    return wHelp;
  }

  @Override
  protected void createContent( final Control topWidget ) {
    Control top = topWidget;
    top = createLocalLinked( top );
    top = createDescription( top );
    createFieldsTable( top );
    resizeSummaryColumn( tableComposite );

  }

  @Override protected ModelAnnotationManager getModelAnnotationManger() {
    return new ModelAnnotationManager(); // use default namespace
  }

  @Override
  protected ModelAnnotationsTableComposite getTableComposite() {
    return tableComposite;
  }

  @Override
  protected ComboVar getWGroups() {
    return wLocalLinked.getwGroups();
  }

  @Override
  protected String getSelectedModelAnnotationGroupName() {
    return getWGroups().getText();
  }

  @Override
  protected void afterOpen() {
    populateCategories( envSub( input.getModelAnnotationCategory() ), false );
    if ( StringUtils.isBlank( envSub( input.getModelAnnotationCategory() ) ) ) {
      populateTable( input.getModelAnnotations() );
      wLocalLinked.setEnableAddCopyButtons( false );
    } else {
      populateTableFromMetastore();
      if ( StringUtil.isVariable( input.getModelAnnotationCategory() ) ) {
        setAnnotationsEditEnabled( false );
        wLocalLinked.setEnableAddCopyButtons( false ); // disable if variable
      } else {
        wLocalLinked.setEnableAddCopyButtons( true );
        getWGroups().setEditable( true ); // allow rename
      }
    }

    wApply.setEnabled( false ); // force disable
  }

  @Override
  protected void populateCategories( final String selectedCategory, final boolean sharedDimensions ) {
    boolean isShared = selectedCategory != null;
    bLocal.setSelection( !isShared );
    bLinked.setSelection( isShared );
    setShareEnabled( isShared );
    super.populateCategories( selectedCategory, sharedDimensions );
  }

  @Override
  protected void onModelAnnotationDirty() {
    super.onModelAnnotationDirty();
    wLocalLinked.setEnableAddCopyButtons( false );

    if ( StringUtils.equals( getWGroups().getText(), input.getModelAnnotationCategory() ) ) {
      getWGroups().setEditable( false ); // prevent rename, unless triggered by rename
    }
  }

  @Override
  protected boolean applyChanges() {
    if ( Const.isEmpty( wStepname.getText() ) ) {
      return false;
    }

    if ( bLinked.getSelection() && StringUtils.isBlank( getWGroups().getText() ) ) {
      showError( BaseMessages.getString( PKG, "ModelAnnotation.ModelAnnotationGroupMissingName.Title" ),
          BaseMessages.getString( PKG, "ModelAnnotation.ModelAnnotationGroupMissingName.Message" ) );
      return false;
    }

    try {
      if ( bLinked.getSelection() ) { // only validate when linked/shared option is selected
        input.checkValidName( getWGroups().getText() );
      }
    } catch ( Exception e ) {
      showError( BaseMessages.getString( PKG, "ModelAnnotation.MetaStoreSaveError.Title" ), e.getLocalizedMessage() );
      return false;
    }

    // TODO we need to refactor this with SharedDimensionDialog
    // we're losing the ability to cancel here
    boolean prevChanged = input.hasChanged();
    String prevStepName = wStepname.getText();
    ModelAnnotationGroup prevAnnotations = ObjectUtils.deepClone( input.getModelAnnotations() );

    input.setChanged(
        input.hasChanged()
            || changed
            || !stepname.equals( wStepname.getText() )
            || !input.getModelAnnotations().equals( tableComposite.getData() )
            || !StringUtils.equals( input.getModelAnnotations().getDescription(), wDescription.getText() ) );
    stepname = wStepname.getText(); // return value
    input.setModelAnnotations( tableComposite.getData() );
    input.getModelAnnotations().setName( getWGroups().getText() );
    input.getModelAnnotations().setDescription( wDescription.getText() );
    if ( bLinked.getSelection() ) {
      String prevCategory = input.getModelAnnotationCategory();
      input.setModelAnnotationCategory( getWGroups().getText() );
      if ( !StringUtil.isVariable( getWGroups().getText() ) ) {
        // vars are only referenced
        try {
          input.saveToMetaStore( getMetaStore() );
        } catch ( Exception e ) {
          showError( BaseMessages.getString( PKG, "ModelAnnotation.MetaStoreSaveError.Title" ),
              BaseMessages.getString( PKG, "ModelAnnotation.MetaStoreSaveError.Message", e.getLocalizedMessage() ) );

          // attempt undo
          input.setChanged( prevChanged );
          stepname = prevStepName;
          input.setModelAnnotations( prevAnnotations );
          input.setModelAnnotationCategory( prevCategory );

          return false;
        }

        wLocalLinked.setEnableAddCopyButtons( true ); // force enable after apply
        getWGroups().setEditable( true );
      }

      // Delete groups that need to be deleted - i.e. renamed groups
      deleteStaleGroups( false );

    } else {
      input.setModelAnnotationCategory( null );
    }
    return true;
  }

  public void setShareEnabled( final boolean enabled ) {
    wLocalLinked.setEnabled( enabled );
    bLinked.setSelection( enabled );
    bLocal.setSelection( !enabled );
  }

  @Override
  protected void onLinkAnnotations() {
    onModelAnnotationDirty();
    setShareEnabled( true );
  }

  @Override
  protected void onDiscardAll() {
    onModelAnnotationDirty();
    setShareEnabled( true );
  }

  @Override
  protected String getDialogTitle() {
    return BaseMessages.getString( PKG, "ModelAnnotation.Dialog.Title" );
  }

  @Override
  protected String getStepName() {
    return BaseMessages.getString( PKG, "ModelAnnotation.StepName" );
  }

  @Override
  protected Image getImageIcon() {
    return SwtSvgImageUtil.getImage( shell.getDisplay(), getClass().getClassLoader(), "ModelAnnotation.svg",
        ConstUI.ICON_SIZE, ConstUI.ICON_SIZE );
  }

  @Override
  protected String missingMessageKey() {
    return "ModelAnnotation.MissingAnnotationGroup.Message";
  }

  @Override
  protected String missingTitleKey() {
    return "ModelAnnotation.MissingAnnotationGroup.Title";
  }
}
