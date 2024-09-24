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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.agilebi.modeler.ModelerException;
import org.pentaho.agilebi.modeler.models.annotations.AnnotationType;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.gui.SpoonFactory;
import org.pentaho.di.core.gui.SpoonInterface;
import org.pentaho.di.core.plugins.PluginInterface;
import org.pentaho.di.core.refinery.publish.util.ObjectUtils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.annotation.BaseAnnotationMeta;
import org.pentaho.di.trans.steps.annotation.ModelAnnotationMeta;
import org.pentaho.di.ui.trans.step.BaseStepDialog;
import org.pentaho.di.ui.trans.steps.common.ModelAnnotationEvent;
import org.pentaho.metastore.api.IMetaStore;

/**
 * @author Rowell Belen
 */
public abstract class ModelAnnotationActionDialog extends BaseStepDialog implements StepDialogInterface {

  protected static Class<?> PKG = ModelAnnotationMeta.class; // for i18n purposes, needed by Translator2!!
  protected final int margin = Const.MARGIN;
  protected final int LEFT_MARGIN_OFFSET = 10;
  protected final int RIGHT_MARGIN_OFFSET = -10;
  protected final int SHELL_MIN_WIDTH = 500;
  protected final int SHELL_MIN_HEIGHT = 400;

  protected ModifyListener lsMod;
  protected Label wBottomHorizontalSeparator;

  private Button wPrevious;
  private Button wNext;
  private Button wHelp;
  private Listener lsPrevious;
  private Listener lsNext;
  private Listener lsClose;

  private ModelAnnotation<AnnotationType> modelAnnotation;
  private BaseAnnotationMeta modelAnnotationMeta;
  private ModelAnnotationGroup modelAnnotations;
  private ModelAnnotationGroup modelAnnotationsOrigCopy;
  private int selectionIndex;
  private boolean saved;

  public ModelAnnotationActionDialog( final Shell parent, final BaseAnnotationMeta baseStepMeta,
      final TransMeta transMeta, final String stepname, final IMetaStore metaStore ) {
    super( parent, (StepMetaInterface) baseStepMeta, transMeta, stepname );
    modelAnnotationMeta = baseStepMeta;
    setMetaStore( metaStore );
  }

  private void configureShell() {
    shell = new Shell( getParent(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE | SWT.MAX | SWT.MIN );
    FormLayout shellLayout = new FormLayout();
    shellLayout.marginWidth = Const.FORM_MARGIN;
    shellLayout.marginHeight = Const.FORM_MARGIN;
    shell.setLayout( shellLayout );
    props.setLook( shell );
    setShellImage( shell, modelAnnotationMeta );

    shell.setText( BaseMessages.getString( PKG, "ModelAnnotation.ModelAnnotationActionDialog.Title" ) );

    // set min size of dialog
    shell.setMinimumSize( SHELL_MIN_WIDTH, SHELL_MIN_HEIGHT );
    shell.setSize( SHELL_MIN_WIDTH, SHELL_MIN_HEIGHT );

    // Detect [X] or ALT-F4 or something that kills this window...
    shell.addShellListener( new ShellAdapter() {
      public void shellClosed( ShellEvent e ) {
        if ( lsClose != null ) {
          // Delegate to provided listener
          lsClose.handleEvent( new ModelAnnotationEvent( getModelAnnotations(), !saved ) );
        }
      }
    } );

    // Detect dispose
    shell.addDisposeListener( new DisposeListener() {
      @Override
      public void widgetDisposed( DisposeEvent de ) {
        if ( lsClose != null ) {
          // Delegate to provided listener
          lsClose.handleEvent( new ModelAnnotationEvent( getModelAnnotations(), !saved ) );
        }
      }
    } );
  }

  protected void initializeListeners() {
    lsMod = new ModifyListener() {
      public void modifyText( ModifyEvent e ) {
        modelAnnotationMeta.setChanged();
      }
    };

    lsOK = new Listener() {
      public void handleEvent( Event e ) {
        ok();
      }
    };

    lsCancel = new Listener() {
      public void handleEvent( Event e ) {
        cancel();
      }
    };

    lsPrevious = new Listener() {
      public void handleEvent( Event e ) {
        previous();
      }
    };

    lsNext = new Listener() {
      public void handleEvent( Event e ) {
        next();
      }
    };
  }

  private void initializeModel() {
    try {

      // Save original copy
      this.modelAnnotationsOrigCopy = this.modelAnnotations;

      // We need to work with a copy, in case the user cancels the dialog.
      this.modelAnnotations = ObjectUtils.deepClone( this.modelAnnotations );
      if ( this.modelAnnotations == null ) {
        this.modelAnnotations = new ModelAnnotationGroup();
      }

      this.modelAnnotation = this.modelAnnotations.get( getSelectionIndex() );
      if ( this.modelAnnotation == null ) {
        throw new Exception( "Model Annotation does not exist" );
      }
    } catch ( Exception e ) {
      // auto-create new
      this.modelAnnotation = new ModelAnnotation<AnnotationType>();
      this.modelAnnotations.add( this.modelAnnotation );
      this.setSelectionIndex( this.modelAnnotations.size() );
    }
  }

  public String open() {

    initializeModel();
    configureShell();
    initializeListeners();
    createOkCancelButtons();

    createWidgets();
    populateDialog();

    // Open the dialog
    shell.open();

    modelAnnotationMeta.setChanged( changed );

    // Wait for close
    while ( !shell.isDisposed() ) {
      if ( !getParent().getDisplay().readAndDispatch() ) {
        getParent().getDisplay().sleep();
      }
    }
    return stepname;
  }

  private void previous() {
    try {
      persistAnnotationProperties(); // may throw validation error

      if ( getSelectionIndex() <= 0 ) {
        setSelectionIndex( this.modelAnnotations.size() ); // return to the end
      }
      this.modelAnnotation = this.modelAnnotations.get( --selectionIndex );
      populateDialog();
    } catch ( ModelerException me ) {
      showError( BaseMessages.getString( PKG, "ModelAnnotation.ModelAnnotationActionDialog.ValidationError.Title" ),
          me.getMessage() );
    }
  }

  private void next() {
    try {
      persistAnnotationProperties(); // may throw validation error

      if ( getSelectionIndex() >= ( modelAnnotations.size() - 1 ) ) {
        setSelectionIndex( -1 ); // return to the start
      }
      this.modelAnnotation = this.modelAnnotations.get( ++selectionIndex );
      populateDialog();
    } catch ( ModelerException me ) {
      showError( BaseMessages.getString( PKG, "ModelAnnotation.ModelAnnotationActionDialog.ValidationError.Title" ),
          me.getMessage() );
    }
  }

  private void cancel() {
    saved = false;
    dispose();
  }

  private void ok() {
    try {
      persistAnnotationProperties(); // may throw validation error

      saved = true;
      dispose();
    } catch ( ModelerException me ) {
      showError( BaseMessages.getString( PKG, "ModelAnnotation.ModelAnnotationActionDialog.ValidationError.Title" ),
          me.getMessage() );
    }
  }

  private void createOkCancelButtons() {

    // Create a horizontal separator
    wBottomHorizontalSeparator = new Label( shell, SWT.HORIZONTAL | SWT.SEPARATOR );
    FormData fdSepator = new FormData();
    fdSepator.left = new FormAttachment( 0, 8 ); // match the left offset of the help button
    fdSepator.right = new FormAttachment( 100, RIGHT_MARGIN_OFFSET );
    fdSepator.bottom = new FormAttachment( wHelp, -10 ); // above help button
    wBottomHorizontalSeparator.setLayoutData( fdSepator );

    // Previous Button
    wPrevious = new Button( shell, SWT.PUSH );
    wPrevious.setText( BaseMessages.getString( PKG, "ModelAnnotation.ModelAnnotationActionDialog.Button.Previous" ) );
    FormData fdPrevious = new FormData();
    fdPrevious.top = new FormAttachment( wBottomHorizontalSeparator, 10 );
    fdPrevious.right = new FormAttachment( 50, -2 );
    wPrevious.setLayoutData( fdPrevious );

    // Next Button
    wNext = new Button( shell, SWT.PUSH );
    wNext.setText( BaseMessages.getString( PKG, "ModelAnnotation.ModelAnnotationActionDialog.Button.Next" ) );
    FormData fdNext = new FormData();
    fdNext.top = new FormAttachment( wBottomHorizontalSeparator, 10 );
    fdNext.left = new FormAttachment( 50, 2 );
    wNext.setLayoutData( fdNext );

    // Cancel Button
    wCancel = new Button( shell, SWT.PUSH );
    wCancel.setText( BaseMessages.getString( PKG, "System.Button.Cancel" ) );
    wCancel.setAlignment( SWT.CENTER );
    FormData fdCancel = new FormData();
    fdCancel.top = new FormAttachment( wBottomHorizontalSeparator, 10 );
    fdCancel.right = new FormAttachment( 100, RIGHT_MARGIN_OFFSET );
    wCancel.setLayoutData( fdCancel );

    // OK Button
    wOK = new Button( shell, SWT.PUSH );
    wOK.setText( BaseMessages.getString( PKG, "System.Button.OK" ) );
    wOK.setAlignment( SWT.CENTER );
    FormData fdOK = new FormData();
    fdOK.top = new FormAttachment( wBottomHorizontalSeparator, 10 );
    fdOK.right = new FormAttachment( wCancel, -4 );
    wOK.setLayoutData( fdOK );

    // Add listeners
    wOK.addListener( SWT.Selection, lsOK );
    wCancel.addListener( SWT.Selection, lsCancel );
    wPrevious.addListener( SWT.Selection, lsPrevious );
    wNext.addListener( SWT.Selection, lsNext );
  }

  @Override
  protected Button createHelpButton( final Shell shell, final StepMeta stepMeta, final PluginInterface plugin ) {
    wHelp = super.createHelpButton( shell, stepMeta, plugin ); // capture help button from base class
    return wHelp;
  }

  public int getSelectionIndex() {
    return selectionIndex;
  }

  public void setSelectionIndex( int selectionIndex ) {
    this.selectionIndex = selectionIndex;
  }

  public ModelAnnotationGroup getModelAnnotations() {
    if ( saved ) {
      return this.modelAnnotations;
    } else {
      return this.modelAnnotationsOrigCopy;
    }
  }

  public ModelAnnotationGroup getModifiedModelAnnotations() {
    return this.modelAnnotations;
  }

  public void setModelAnnotations( ModelAnnotationGroup modelAnnotations ) {
    this.modelAnnotations = modelAnnotations;
  }

  public void onClose( Listener listener ) {
    this.lsClose = listener;
  }

  public void showInfo( String title, String message ) {
    SpoonInterface spoon = getSpoon();
    spoon.messageBox( message, title, false, Const.INFO );
  }

  public void showError( String title, String message ) {
    SpoonInterface spoon = getSpoon();
    spoon.messageBox( message, title, false, Const.ERROR );
  }

  protected SpoonInterface getSpoon() {
    return SpoonFactory.getInstance();
  }

  protected abstract void createWidgets();

  protected abstract void populateDialog();

  protected abstract void persistAnnotationProperties() throws ModelerException;

  protected ModelAnnotation<AnnotationType> getModelAnnotation() {
    return modelAnnotation;
  }

  protected void setModelAnnotation(
      ModelAnnotation<AnnotationType> modelAnnotation ) {
    this.modelAnnotation = modelAnnotation;
  }
}
