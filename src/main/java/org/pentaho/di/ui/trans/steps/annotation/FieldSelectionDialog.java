/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/


package org.pentaho.di.ui.trans.steps.annotation;


import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.agilebi.modeler.models.annotations.BlankAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.annotation.ModelAnnotationMeta;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

import java.util.Iterator;


public class FieldSelectionDialog extends BaseStepDialog implements StepDialogInterface {
  private static Class<?> PKG = ModelAnnotationMeta.class; // for i18n purposes, needed by Translator2!!

  private Label wlAvailable;
  private Label wlSelected;
  private List wAvailable;
  private ListViewer wSelected;
  private Button wAdd;
  private Button wAddAll;
  private Button wRemove;
  private Button wRemoveAll;
  private RowMetaInterface prevStepFields;
  private final ModelAnnotationGroup modelAnnotations;
  private Label wBottomHorizontalSeparator;
  private Listener lsClose;

  public FieldSelectionDialog( final Shell parent, final StepMetaInterface baseStepMeta,
                               final TransMeta transMeta, final String stepname,
                               final ModelAnnotationGroup modelAnnotations ) {
    super( parent, baseStepMeta, transMeta, stepname );
    this.modelAnnotations = modelAnnotations;
    try {
      prevStepFields = transMeta.getPrevStepFields( stepname );
    } catch ( KettleStepException e ) {
      e.printStackTrace();
    }
  }


  @Override
  public String open() {
    Shell parent = getParent();
    Display display = parent.getDisplay();

    shell = new Shell( parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.SHEET | SWT.RESIZE );
    shell.setSize( 600, 500 );
    props.setLook( shell );
    shell.setImage( GUIResource.getInstance().getImageSpoon() );

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;

    shell.setLayout( formLayout );
    shell.setText( BaseMessages.getString( PKG, "ModelAnnotation.FieldSelectionDialog.Title" ) );

    initializeWidgets();
    initializeListeners();
    layoutWidgets();
    populateLists();


    shell.open();
    while ( !shell.isDisposed() ) {
      if ( !display.readAndDispatch() ) {
        display.sleep();
      }
    }

    return stepname;
  }

  private void initializeListeners() {
    wAdd.addListener( SWT.Selection, new Listener() {
      @Override public void handleEvent( final Event event ) {
        addField();
      }
    } );

    wAddAll.addListener( SWT.Selection, new Listener() {
      @Override public void handleEvent( final Event event ) {
        for ( String field : wAvailable.getItems() ) {
          BlankAnnotation annotation = new BlankAnnotation();
          annotation.setField( field );
          ModelAnnotation modelAnnotation = new ModelAnnotation( annotation );
          ( (ModelAnnotationGroup) wSelected.getInput() ).add( modelAnnotation );
          wSelected.refresh();
        }
      }
    } );

    wRemove.addListener( SWT.Selection, new Listener() {
      @Override public void handleEvent( final Event event ) {
        removeField();
      }
    } );

    wRemoveAll.addListener( SWT.Selection, new Listener() {
      @Override public void handleEvent( final Event event ) {
        ModelAnnotationGroup toSelect = new ModelAnnotationGroup();
        for ( ModelAnnotation modelAnnotation : (ModelAnnotationGroup) wSelected.getInput() ) {
          if ( modelAnnotation.getType() != ModelAnnotation.Type.CREATE_CALCULATED_MEMBER ) {
            toSelect.add( modelAnnotation );
          }
        }
        wSelected.setSelection( new StructuredSelection( toSelect ), false );
        removeField();
      }
    } );

    wOK.addListener( SWT.Selection, new Listener() {
      @Override public void handleEvent( final Event event ) {
        modelAnnotations.clear();
        modelAnnotations.addAll( (ModelAnnotationGroup) wSelected.getInput() );
        dispose();
      }
    } );

    wCancel.addListener( SWT.Selection, new Listener() {
      @Override public void handleEvent( final Event event ) {
        dispose();
      }
    } );

    wSelected.getControl().addFocusListener( new FocusListener() {
      @Override public void focusGained( final FocusEvent focusEvent ) {
        wAvailable.deselectAll();
      }
      @Override public void focusLost( final FocusEvent focusEvent ) {
      }
    }  );

    wAvailable.addFocusListener( new FocusListener() {
      @Override public void focusGained( final FocusEvent focusEvent ) {
        ( (List) wSelected.getControl() ).deselectAll();
      }
      @Override public void focusLost( final FocusEvent focusEvent ) {
      }
    } );

    wAvailable.addListener( SWT.MouseDoubleClick, new Listener() {
      @Override public void handleEvent( final Event event ) {
        addField();
      }
    } );

    wSelected.getList().addKeyListener( new KeyListener() {
      @Override public void keyPressed( final KeyEvent keyEvent ) {
        if ( keyEvent.character == SWT.DEL ) {
          removeField();
        }
      }

      @Override public void keyReleased( final KeyEvent keyEvent ) {
      }
    } );

    // Detect [X] or ALT-F4 or something that kills this window...
    shell.addShellListener( new ShellAdapter() {
      public void shellClosed( ShellEvent e ) {
        if ( lsClose != null ) {
          lsClose.handleEvent( null ); // Delegate to provided listener
        }
      }
    } );

    // Detect dispose
    shell.addDisposeListener( new DisposeListener() {
      @Override
      public void widgetDisposed( DisposeEvent de ) {
        if ( lsClose != null ) {
          lsClose.handleEvent( null ); // Delegate to provided listener
        }
      }
    } );
  }

  public void onClose( Listener listener ) {
    this.lsClose = listener;
  }

  private void removeField() {
    Iterator iterator = ( (IStructuredSelection) wSelected.getSelection() ).iterator();
    ModelAnnotationGroup toRemove = new ModelAnnotationGroup();
    while ( iterator.hasNext() ) {
      toRemove.add( (ModelAnnotation) iterator.next() );
    }
    ( (ModelAnnotationGroup) wSelected.getInput() ).removeAll( toRemove );
    wSelected.refresh();
  }

  private void addField() {
    for ( String field : wAvailable.getSelection() ) {
      BlankAnnotation annotation = new BlankAnnotation();
      annotation.setField( field );
      ModelAnnotation modelAnnotation = new ModelAnnotation( annotation );
      ( (ModelAnnotationGroup) wSelected.getInput() ).add( modelAnnotation );
      wSelected.refresh();
    }
  }

  private void populateLists() {
    for ( ValueMetaInterface valueMetaInterface : prevStepFields.getValueMetaList() ) {
      if ( StringUtils.isNotBlank( valueMetaInterface.getName() ) ) {
        wAvailable.add( valueMetaInterface.getName() );
      }
    }

    wSelected.setContentProvider( new ArrayContentProvider() );
    ModelAnnotationGroup input = new ModelAnnotationGroup();
    input.addAll( modelAnnotations );
    wSelected.setInput( input );
    wSelected.setLabelProvider( new LabelProvider() {
      @Override public String getText( final Object element ) {
        ModelAnnotation modelAnnotation = (ModelAnnotation) element;
        // Backwards compatibility. Will look for field in annotation type, If not found, fallback to generic
        return modelAnnotation.getField();
      }
    } );
    wSelected.setFilters( new ViewerFilter[] { new ViewerFilter() {
      @Override public boolean select( final Viewer viewer, final Object providerData, final Object item ) {
        return ( (ModelAnnotation) item ).getType() != ModelAnnotation.Type.CREATE_CALCULATED_MEMBER;
      }
    } } );
  }

  private void initializeWidgets() {
    wlAvailable = new Label( shell, SWT.NONE );
    wlAvailable.setText( BaseMessages.getString( PKG, "ModelAnnotation.FieldSelectionDialog.AvailableFields" ) );
    props.setLook( wlAvailable );

    wlSelected = new Label( shell, SWT.NONE );
    wlSelected.setText( BaseMessages.getString( PKG, "ModelAnnotation.FieldSelectionDialog.SelectedFields" ) );
    props.setLook( wlSelected );

    wSelected = new ListViewer( shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER );
    props.setLook( wSelected.getList() );

    wAvailable = new List( shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER );
    props.setLook( wAvailable );

    wAdd = new Button( shell, SWT.NONE );
    wAdd.setImage( GUIResource.getInstance().getImageAddSingle() );
    wAdd.setToolTipText( BaseMessages.getString( PKG, "ModelAnnotation.FieldSelectionDialog.Add" ) );
    props.setLook( wAdd );

    wAddAll = new Button( shell, SWT.NONE );
    wAddAll.setImage( GUIResource.getInstance().getImageAddAll() );
    wAddAll.setToolTipText( BaseMessages.getString( PKG, "ModelAnnotation.FieldSelectionDialog.AddAll" ) );
    props.setLook( wAddAll );

    wRemove = new Button( shell, SWT.NONE );
    wRemove.setImage( GUIResource.getInstance().getImageRemoveSingle() );
    wRemove.setToolTipText( BaseMessages.getString( PKG, "ModelAnnotation.FieldSelectionDialog.Remove" ) );
    props.setLook( wRemove );

    wRemoveAll = new Button( shell, SWT.NONE );
    wRemoveAll.setImage( GUIResource.getInstance().getImageRemoveAll() );
    wRemoveAll.setToolTipText( BaseMessages.getString( PKG, "ModelAnnotation.FieldSelectionDialog.RemoveAll" ) );
    props.setLook( wRemoveAll );

    wOK = new Button( shell, SWT.PUSH );
    wOK.setText( BaseMessages.getString( PKG, "System.Button.OK" ) );
    props.setLook( wOK );

    wCancel = new Button( shell, SWT.PUSH );
    wCancel.setText( BaseMessages.getString( PKG, "System.Button.Cancel" ) );
    props.setLook( wCancel );

    wBottomHorizontalSeparator = new Label( shell, SWT.HORIZONTAL | SWT.SEPARATOR );

  }

  private void layoutWidgets() {
    FormLayout layout = new FormLayout();
    layout.marginHeight = 5;
    layout.marginWidth = 5;
    shell.setLayout( layout );

    FormData fdCancel = new FormData();
    fdCancel.right = new FormAttachment( 100, -Const.MARGIN );
    fdCancel.bottom = new FormAttachment( 100, -Const.MARGIN );
    wCancel.setLayoutData( fdCancel );

    FormData fdOk = new FormData();
    fdOk.right = new FormAttachment( wCancel, -Const.MARGIN, SWT.LEFT );
    fdOk.top = new FormAttachment( wCancel, 0, SWT.TOP );
    wOK.setLayoutData( fdOk );

    FormData fdBottomSeparator = new FormData();
    fdBottomSeparator.left = new FormAttachment( 0,  Const.MARGIN );
    fdBottomSeparator.right = new FormAttachment( 100, -Const.MARGIN );
    fdBottomSeparator.bottom = new FormAttachment( wOK, -10, SWT.TOP );
    wBottomHorizontalSeparator.setLayoutData( fdBottomSeparator );

    FormData fdAvailableLabel = new FormData();
    fdAvailableLabel.left = new FormAttachment( 0, Const.MARGIN );
    fdAvailableLabel.top = new FormAttachment( 0, 0 );
    fdAvailableLabel.right = new FormAttachment( 44 );
    wlAvailable.setLayoutData( fdAvailableLabel );

    FormData fdSelectedLabel = new FormData();
    fdSelectedLabel.left = new FormAttachment( 56, 0  );
    fdSelectedLabel.top = new FormAttachment( 0, 0 );
    fdSelectedLabel.right = new FormAttachment( 100, -Const.MARGIN );
    wlSelected.setLayoutData( fdSelectedLabel );

    FormData fdAvailableList = new FormData();
    fdAvailableList.top = new FormAttachment( wlAvailable,  Const.MARGIN, SWT.BOTTOM );
    fdAvailableList.left = new FormAttachment( wlAvailable,  0, SWT.LEFT );
    fdAvailableList.right = new FormAttachment( wlAvailable, 0, SWT.RIGHT );
    fdAvailableList.bottom = new FormAttachment( wBottomHorizontalSeparator, -10, SWT.TOP );
    wAvailable.setLayoutData( fdAvailableList );

    FormData fdAdd = new FormData();
    fdAdd.top = new FormAttachment( 30 );
    fdAdd.left = new FormAttachment( wAvailable, Const.MARGIN, SWT.RIGHT );
    fdAdd.right = new FormAttachment( 56, -Const.MARGIN );
    wAdd.setLayoutData( fdAdd );
    wAdd.setAlignment( SWT.CENTER );

    FormData fdAddAll = new FormData();
    fdAddAll.top = new FormAttachment( wAdd, Const.MARGIN, SWT.BOTTOM );
    fdAddAll.left = new FormAttachment( wAdd, 0, SWT.LEFT );
    fdAddAll.right = new FormAttachment( wAdd, 0, SWT.RIGHT );
    wAddAll.setLayoutData( fdAddAll );
    wAddAll.setAlignment( SWT.CENTER );

    FormData fdRemove = new FormData();
    fdRemove.top = new FormAttachment( wAddAll, Const.MARGIN, SWT.BOTTOM );
    fdRemove.left = new FormAttachment( wAddAll, 0, SWT.LEFT );
    fdRemove.right = new FormAttachment( wAddAll, 0, SWT.RIGHT );
    wRemove.setLayoutData( fdRemove );
    wRemove.setAlignment( SWT.CENTER );

    FormData fdRemoveAll = new FormData();
    fdRemoveAll.top = new FormAttachment( wRemove, Const.MARGIN, SWT.BOTTOM );
    fdRemoveAll.left = new FormAttachment( wRemove, 0, SWT.LEFT );
    fdRemoveAll.right = new FormAttachment( wRemove, 0, SWT.RIGHT );
    wRemoveAll.setLayoutData( fdRemoveAll );
    wRemoveAll.setAlignment( SWT.CENTER );

    FormData fdSelectedList = new FormData();
    fdSelectedList.top = new FormAttachment( wlSelected, Const.MARGIN, SWT.BOTTOM );
    fdSelectedList.left = new FormAttachment( wlSelected, 0, SWT.LEFT );
    fdSelectedList.right = new FormAttachment( wlSelected, 0, SWT.RIGHT );
    fdSelectedList.bottom = new FormAttachment( wAvailable, 0, SWT.BOTTOM );
    wSelected.getList().setLayoutData( fdSelectedList );
  }
}
