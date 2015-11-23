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

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.pentaho.agilebi.modeler.models.annotations.CreateCalculatedMember;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.di.core.refinery.publish.util.ObjectUtils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.annotation.ModelAnnotationMeta;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.trans.steps.annotation.FieldSelectionDialog;
import org.pentaho.di.ui.trans.steps.annotation.ModelAnnotationActionCustomDialog;
import org.pentaho.di.ui.trans.steps.annotation.ModelAnnotationActionDialog;
import org.pentaho.metastore.api.IMetaStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class ModelAnnotationsTableComposite extends BaseComposite {

  private Composite parent;
  private Label wEdit;
  private Label wDelete;
  private Control topWidget;
  private Control bottomWidget;
  private TableViewer wFields;
  private Button wGetFields;
  private Button wAddCalcMeasure;
  private PropsUI props;
  private Color notInStreamColor;

  public ModelAnnotationsTableComposite( Composite parent, Control topWidget, Control bottomWidget ) {
    super( parent, SWT.NONE );
    this.parent = parent;
    this.topWidget = topWidget;
    this.bottomWidget = bottomWidget;
    init();
  }

  public void init() {
    this.props = PropsUI.getInstance();
    notInStreamColor = new Color( parent.getDisplay(), 160, 160, 160 );
  }

  public void createWidgets() {

    Control localTopWidget = createFieldsTableHeader( topWidget );

    // Select Fields Button
    wGetFields = new Button( parent, SWT.PUSH );
    wGetFields.setText( BaseMessages.getString( getLocalizationPkg(), "ModelAnnotation.GetFields.Button" ) );
    FormData fdGetFields = new FormData();
    fdGetFields.right = new FormAttachment( 100, RIGHT_MARGIN_OFFSET );
    fdGetFields.bottom = new FormAttachment( bottomWidget, -15 );
    wGetFields.setLayoutData( fdGetFields );
    wGetFields.addListener( SWT.Selection, new Listener() {
      @Override public void handleEvent( Event event ) {
        onGetFields();
      }
    } );

    if ( supportsCalculatedMeasure() ) {
      // Add Calculated Measure
      wAddCalcMeasure = new Button( parent, SWT.PUSH );
      wAddCalcMeasure
          .setText( BaseMessages.getString( getLocalizationPkg(), "ModelAnnotation.AddCalcMeasure.Button" ) );
      FormData fdAddCalcMeasure = new FormData();
      fdAddCalcMeasure.right = new FormAttachment( wGetFields, -margin );
      fdAddCalcMeasure.bottom = new FormAttachment( bottomWidget, -15 );
      wAddCalcMeasure.setLayoutData( fdAddCalcMeasure );
      wAddCalcMeasure.addListener( SWT.Selection, new Listener() {
        @Override public void handleEvent( Event event ) {
          onCreateCalculatedMeasure();
        }
      } );
    }

    // Annotations Table
    wFields =
        new TableViewer( parent, SWT.BORDER
            | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL );
    wFields.getControl().addListener( SWT.Selection, new Listener() {
      @Override public void handleEvent( final Event event ) {
        onItemSelect();
      }
    } );
    FormData fdFields = new FormData();
    fdFields.top = new FormAttachment( localTopWidget, 5 );
    fdFields.left = new FormAttachment( 0, LEFT_MARGIN_OFFSET );
    fdFields.right = new FormAttachment( 100, RIGHT_MARGIN_OFFSET );
    fdFields.bottom = new FormAttachment( wGetFields, -10 );
    wFields.getTable().setLayoutData( fdFields );

    wFields.getTable().setHeaderVisible( true );
    TableViewerColumn fieldColumn = new TableViewerColumn( wFields, SWT.NONE );
    fieldColumn.getColumn()
        .setText( BaseMessages.getString( getLocalizationPkg(), "ModelAnnotation.ColumnInfo.Field" ) );
    fieldColumn.getColumn().setWidth( 195 );
    fieldColumn.setLabelProvider( new ColumnLabelProvider() {
      @Override public String getText( final Object element ) {
        ModelAnnotation modelAnnotation = (ModelAnnotation) element;
        if ( modelAnnotation.getType() == ModelAnnotation.Type.CREATE_CALCULATED_MEMBER ) {
          return BaseMessages.getString( getLocalizationPkg(), "ModelAnnotation.Fields.CalculatedMeasure" );
        }
        // Backwards compatibility. Will look for field in annotation type, If not found, fallback to generic
        return modelAnnotation.getField();
      }
    } );

    TableViewerColumn actionColumn = new TableViewerColumn( wFields, SWT.NONE );
    actionColumn.getColumn()
        .setText( BaseMessages.getString( getLocalizationPkg(), "ModelAnnotation.ColumnInfo.ModelAction" ) );
    actionColumn.getColumn().setWidth( 150 );
    actionColumn.setLabelProvider( new ColumnLabelProvider() {
      @Override public String getText( final Object element ) {
        ModelAnnotation modelAnnotation = (ModelAnnotation) element;
        if ( modelAnnotation.getType() != null ) {
          return modelAnnotation.getType().description();
        }
        return "";
      }
    } );

    TableViewerColumn summaryColumn = new TableViewerColumn( wFields, SWT.NONE );
    summaryColumn.getColumn()
        .setText( BaseMessages.getString( getLocalizationPkg(), "ModelAnnotation.ColumnInfo.Summary" ) );
    summaryColumn.getColumn().setWidth( 0 );
    summaryColumn.setLabelProvider( new ColumnLabelProvider() {
      @Override public String getText( final Object element ) {
        ModelAnnotation modelAnnotation = (ModelAnnotation) element;
        if ( modelAnnotation.getAnnotation() != null ) {
          return modelAnnotation.getAnnotation().getSummary();
        }
        return BaseMessages.getString( getLocalizationPkg(), "ModelAnnotation.Dialog.NoAnnotation" );
      }
    } );

    // Add listeners
    wFields.addDoubleClickListener( new IDoubleClickListener() {
      @Override public void doubleClick( DoubleClickEvent doubleClickEvent ) {
        onItemDoubleClick();
      }
    } );

    // Try to set initial size of the columns
    wFields.getTable().setSize( SHELL_MIN_WIDTH, SHELL_MIN_HEIGHT );
  }

  private Control createFieldsTableHeader( Control topWidget ) {

    // Table Label
    Label wFieldsTableHeader = new Label( parent, SWT.LEFT );
    wFieldsTableHeader
        .setText( BaseMessages.getString( getLocalizationPkg(), "ModelAnnotation.FieldsTable.Header.Label" ) );
    props.setLook( wFieldsTableHeader );
    FormData fdFieldsTableHeader = new FormData();
    fdFieldsTableHeader.top = new FormAttachment( topWidget, 10 );
    fdFieldsTableHeader.left = new FormAttachment( 0, LEFT_MARGIN_OFFSET );
    wFieldsTableHeader.setLayoutData( fdFieldsTableHeader );

    // Delete Button
    wDelete = new Label( parent, SWT.FLAT );
    wDelete.setImage( GUIResource.getInstance().getImageDelete() );
    wDelete
        .setToolTipText( BaseMessages.getString( getLocalizationPkg(), "ModelAnnotation.FieldsTable.Header.Delete" ) );
    wDelete.setEnabled( false );
    props.setLook( wDelete );
    FormData fdDelete = new FormData();
    fdDelete.top = new FormAttachment( topWidget, 10 );
    fdDelete.right = new FormAttachment( 100, RIGHT_MARGIN_OFFSET );
    wDelete.setLayoutData( fdDelete );

    // Edit button
    wEdit = new Label( parent, SWT.FLAT );
    wEdit.setImage( GUIResource.getInstance().getImageEdit() );
    wEdit.setToolTipText( BaseMessages.getString( getLocalizationPkg(), "ModelAnnotation.FieldsTable.Header.Edit" ) );
    wEdit.setEnabled( false );
    props.setLook( wEdit );
    FormData fdEdit = new FormData();
    fdEdit.top = new FormAttachment( topWidget, 10 );
    fdEdit.right = new FormAttachment( wDelete, -margin, SWT.LEFT );
    wEdit.setLayoutData( fdEdit );

    // Add listeners
    wDelete.addListener( SWT.MouseUp, new Listener() {
      @Override public void handleEvent( Event event ) {
        onDelete();
      }
    } );
    wEdit.addListener( SWT.MouseUp, new Listener() {
      @Override public void handleEvent( Event event ) {
        onEdit();
      }
    } );

    return wFieldsTableHeader;
  }

  public void populateTable( final ModelAnnotationGroup modelAnnotations, final TransMeta transMeta,
      String stepName, Shell shell ) {
    String[] fieldNames = new String[] {};
    try {
      fieldNames = transMeta.getPrevStepFields( stepName ).getFieldNames();
    } catch ( Exception e ) {
      logError( "could not determine stream fields", e );
    }

    ModelAnnotationGroup tableModelAnnotations = ObjectUtils.deepClone( modelAnnotations ); // need to retain all info
    this.setData( tableModelAnnotations );
    this.adjustFont( this.getTable().getItems(), shell, fieldNames );
    this.toggleDeleteEditButtons();
  }

  public void toggleDeleteEditButtons() {
    int selectionCount = wFields.getTable().getSelectionCount();
    boolean validSelection = isAnnotationEditValid();
    wEdit.setEnabled( validSelection && selectionCount == 1 );
    wEdit.setImage( new Image( parent.getParent().getDisplay(), GUIResource.getInstance().getImageEdit(),
        selectionCount == 1 && validSelection ? SWT.NONE : SWT.IMAGE_DISABLE ) );
    wDelete.setEnabled( selectionCount > 0 );
    wDelete.setImage( new Image( parent.getParent().getDisplay(), GUIResource.getInstance().getImageDelete(),
        selectionCount > 0 ? SWT.NONE : SWT.IMAGE_DISABLE ) );
  }

  private boolean isAnnotationEditValid() {
    boolean validSelection = true;
    TableItem[] sel = wFields.getTable().getSelection();
    if ( sel != null && sel.length > 0 ) {
      if ( sel[0].getForeground().equals( notInStreamColor ) ) {
        validSelection = false;
      }
    }
    return validSelection;
  }

  public void deleteAnnotation() {
    int[] selectionIndices = this.getTable().getSelectionIndices();
    ModelAnnotationGroup toRemove = new ModelAnnotationGroup();
    ModelAnnotationGroup tableInput = this.getData();
    for ( final int selectionIndice : selectionIndices ) {
      toRemove.add( tableInput.get( selectionIndice ) );
    }
    tableInput.removeAll( toRemove );
    this.refresh();
    this.toggleDeleteEditButtons();
  }

  public void editAnnotation( Shell shell, ModelAnnotationMeta input, TransMeta transMeta,
      String stepName, IMetaStore metaStore, Listener onClose ) {
    if ( this.getTable().getSelectionIndex() < 0 ) {
      return;
    }
    if ( !isAnnotationEditValid() ) {
      return;
    }
    final ModelAnnotationActionDialog actionDialog
      = new ModelAnnotationActionCustomDialog( shell, input, transMeta, stepName, metaStore );
    actionDialog.setSelectionIndex( this.getTable().getSelectionIndex() );
    actionDialog.setModelAnnotations( this.getData() ); // we should load the table data
    actionDialog.onClose( onClose );
    actionDialog.open();
  }

  public void createCalculatedMeasure( Shell shell, ModelAnnotationMeta input, TransMeta transMeta,
      String stepName, IMetaStore metaStore, final Listener onClose ) {

    // Create a placeholder calculatedMember annotation to simulate adding a field
    final CreateCalculatedMember calculatedMember = new CreateCalculatedMember();
    final ModelAnnotation<CreateCalculatedMember> calculatedMemberAnnot = new ModelAnnotation<>( calculatedMember );
    final ModelAnnotationGroup modelAnnotations = ObjectUtils.deepClone( this.getData() );
    modelAnnotations.add( calculatedMemberAnnot );

    final ModelAnnotationActionDialog actionDialog
      = new ModelAnnotationActionCustomDialog( shell, input, transMeta, stepName, metaStore );
    actionDialog.setSelectionIndex( modelAnnotations.size() - 1 );
    actionDialog.setModelAnnotations( modelAnnotations ); // we should load the table data
    actionDialog.onClose( new Listener() { // intercept the onClose listener
      @Override public void handleEvent( Event event ) {

        try {
          ModelAnnotationEvent e = (ModelAnnotationEvent) event;
          // if the user cancels, remove the placeholder annotation
          if ( e.isActionCancelled() ) {
            removePlaceholderCalculatedMember( e, calculatedMemberAnnot );
          }
        } catch ( Exception e ) {
          logError( e.getMessage(), e );
        }

        onClose.handleEvent( event ); // call the original onClose method
      }
    } );
    actionDialog.open();
  }

  private void removePlaceholderCalculatedMember( ModelAnnotationEvent e, ModelAnnotation annotation ) {

    if ( e == null || e.getModelAnnotations() == null || annotation == null ) {
      return;
    }

    ModelAnnotation toBeRemoved = null;
    for ( ModelAnnotation modelAnnotation : e.getModelAnnotations() ) {
      if ( modelAnnotation.getType() != null
          && modelAnnotation.getType() == ModelAnnotation.Type.CREATE_CALCULATED_MEMBER ) {
        if ( modelAnnotation.getName().equals( annotation.getName() ) ) { // compare UUID
          toBeRemoved = modelAnnotation;
          break;
        }
      }
    }

    if ( toBeRemoved != null ) {
      e.getModelAnnotations().remove( toBeRemoved );
    }
  }

  public void getFields( final Shell parent, final StepMetaInterface input,
      final TransMeta transMeta, final String stepName, Listener onClose ) {
    final ModelAnnotationGroup modelAnnotations = ObjectUtils.deepClone( this.getData() );
    final FieldSelectionDialog fieldSelectionDialog =
        new FieldSelectionDialog( parent, input, transMeta, stepName, modelAnnotations );
    fieldSelectionDialog.open();
    populateTable( modelAnnotations, transMeta, stepName, parent );
    onClose.handleEvent( new ModelAnnotationEvent( modelAnnotations ) ); // manually trigger after loaded to the table
  }

  public void adjustFont( final TableItem[] tableItems, final Shell shell, String[] fieldNamesArray ) {
    try {
      final Table table = this.getTable();
      removeListener( table, SWT.MouseMove );
      removeListener( table, SWT.MouseEnter );
      final ArrayList<TableItem> missingItems = new ArrayList<TableItem>();
      final List<String> fieldNames = Arrays.asList( fieldNamesArray );
      markMissingItems( tableItems, shell, fieldNames, missingItems );
      final Listener mouseListener = new Listener() {
        @Override public void handleEvent( final Event event ) {
          boolean overMissing = false;
          boolean needRefresh = false;
          for ( TableItem missingItem : missingItems ) {
            if ( missingItem.isDisposed() || !isMarkedMissing( missingItem ) ) {
              // out of sync after a delete
              needRefresh = true;
            } else if ( missingItem.getBounds( 0 ).contains( event.x, event.y )
                || missingItem.getBounds( 1 ).contains( event.x, event.y )
                || missingItem.getBounds( 2 ).contains( event.x, event.y ) ) {
              // set tooltip if hovering over a missing element
              table.setToolTipText( BaseMessages.getString( getLocalizationPkg(),
                  "ModelAnnotation.FieldsTable.Missing.ToolTip" ) );
              overMissing = true;
            }
          }
          if ( needRefresh ) {
            missingItems.clear();
            markMissingItems( table.getItems(), shell, fieldNames, missingItems );
          }
          if ( !overMissing ) {
            table.setToolTipText( null );
          }
        }
      };
      table.addListener( SWT.MouseMove, mouseListener );
      table.addListener( SWT.MouseEnter, mouseListener );
    } catch ( Exception e ) {
      logError( "could not determine stream fields", e );
    }
  }

  private boolean isMarkedMissing( TableItem item ) {
    return item.getForeground().equals( notInStreamColor );
  }

  private void markMissingItems( final TableItem[] tableItems, Shell shell, final List<String> fieldNames,
      final ArrayList<TableItem> missingItems ) {
    for ( final TableItem tableItem : tableItems ) {

      ModelAnnotation annotation = (ModelAnnotation) tableItem.getData();
      if ( annotation.getType() == ModelAnnotation.Type.CREATE_CALCULATED_MEMBER ) {
        continue; // Skip calculated member annotations. They should be editable all the time
      }

      if ( !fieldNames.contains( tableItem.getText() ) ) {
        tableItem.setForeground( notInStreamColor );
        FontData fontData = tableItem.getFont().getFontData()[0];
        Font font =
            new Font( shell.getDisplay(), new FontData( fontData.getName(), fontData.getHeight(), SWT.ITALIC ) );
        tableItem.setFont( font );
        missingItems.add( tableItem );
      }
    }
  }

  private void removeListener( final Control control, final int listenerType ) {
    Listener[] listeners = control.getListeners( listenerType );
    if ( listeners.length > 0 ) {
      control.removeListener( listenerType, listeners[0] );
    }
  }

  public void setData( ModelAnnotationGroup group ) {
    wFields.setContentProvider( new ArrayContentProvider() );
    wFields.setInput( group );
  }

  public ModelAnnotationGroup getData() {
    return (ModelAnnotationGroup) ( wFields.getInput() == null ? new ModelAnnotationGroup() : wFields.getInput() );
  }

  public void setEnabled( boolean enabled ) {
    wFields.getControl().setEnabled( enabled );
    wGetFields.setEnabled( enabled );
  }

  public void refresh() {
    wFields.refresh();
  }

  public Table getTable() {
    return wFields.getTable();
  }

  protected abstract void onDelete();

  protected abstract void onEdit();

  protected abstract void onGetFields();

  protected abstract void onItemSelect();

  protected abstract void onItemDoubleClick();

  protected abstract boolean supportsCalculatedMeasure();

  protected abstract void onCreateCalculatedMeasure();
}
