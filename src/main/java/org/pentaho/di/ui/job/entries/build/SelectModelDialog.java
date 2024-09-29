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


package org.pentaho.di.ui.job.entries.build;

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.refinery.model.ModelServerFetcher;
import org.pentaho.di.core.refinery.publish.agilebi.BiServerConnection;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entries.JobEntryDialogBoilerplate;
import org.pentaho.di.job.entries.build.JobEntryBuildModel;
import org.pentaho.di.job.entry.JobEntryDialogInterface;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.widget.ComboVar;
import org.pentaho.di.ui.job.entries.common.ConnectionValidator;
import org.pentaho.di.ui.job.entries.common.ServerConnectionGroupWrapper;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static org.pentaho.di.core.refinery.UIBuilder.*;

/**
 * @author Rowell Belen
 */
public class SelectModelDialog extends JobEntryDialogBoilerplate<JobEntryBuildModel> implements
    JobEntryDialogInterface {

  private static final Pattern varRegex = Pattern.compile( "\\$\\{[^}]*\\}" );
  private final int SHELL_MIN_WIDTH = 470;
  // private final int SHELL_MIN_HEIGHT = 455;
  private Label wlChooseModel;
  private ComboVar wChooseModel;

  private Button bCreateOnPublish;

  private ServerConnectionGroupWrapper serverConnectionGroupWrapper;

  public SelectModelDialog(
      Shell parent, JobEntryInterface jobEntry, Repository rep, JobMeta jobMeta ) {
    super( parent, jobEntry, rep, jobMeta );
    PKG = JobEntryBuildModel.class;
  }

  protected Group createServerConnectionGroup( final Composite main ) {
    serverConnectionGroupWrapper = new ServerConnectionGroupWrapper( main, props, this.jobMeta, this.jobEntry, PKG );
    serverConnectionGroupWrapper.addSubmitButtonListener( new Listener() {
      public void handleEvent( Event event ) {
        BiServerConnection biServerConnection = serverConnectionGroupWrapper.getBiServerConnection( true );
        // test connection
        ConnectionValidator connectionValidator = new ConnectionValidator();
        connectionValidator.setSuppressSuccessMessage( true );
        connectionValidator.setConnection( biServerConnection );
        if ( !connectionValidator.validateConnectionInDesignTime() ) {
          wChooseModel.removeAll();
          return; // exit if connection is not valid
        }

        ModelServerFetcher fetcher = new ModelServerFetcher( biServerConnection );
        try {
          List<String> datasourceList = fetcher.fetchDswList();
          datasourceList.addAll( fetcher.fetchAnalysisList() );
          Collections.sort( datasourceList );
          String prevSelection = wChooseModel.getText();
          wChooseModel.removeAll();
          for ( String datasource : datasourceList ) {
            wChooseModel.add( datasource );
          }
          // restore selection if compatible (either var or part of list)
          if ( datasourceList.contains( prevSelection ) ) {
            wChooseModel.select( datasourceList.indexOf( prevSelection ) );
          } else if ( varRegex.matcher( prevSelection ).find() ) {
            wChooseModel.setText( prevSelection );
            wChooseModel.getCComboWidget().setSelection( new Point( 0, 0 ) );
          } else {
            wChooseModel.select( 0 );
          }
        } catch ( Exception e ) {
          new ErrorDialog( shell, "Error", "Error retrieving data sources", e );
        }
      }
    } );

    return serverConnectionGroupWrapper.getGroup();
  }

  protected Composite createModelComposite( final Composite main ) {

    Composite composite = createFormComposite( main );
    props.setLook( composite );

    // Choose Model Label
    wlChooseModel = new Label( composite, SWT.RIGHT );
    wlChooseModel.setText( getMsg( "SelectModelDialog.ChooseModel.Label" ) );
    props.setLook( wlChooseModel );

    // Choose Model Combo Box
    wChooseModel = new ComboVar( jobMeta, composite, SWT.LEFT | SWT.BORDER );
    wChooseModel.setItems( new String[] { "" } );
    wChooseModel.setToolTipText( getMsg( "SelectModelDialog.ChooseModel.Label" ) );
    wChooseModel.addModifyListener( new ModifyListener() {
      @Override
      public void modifyText( ModifyEvent modifyEvent ) {
        if ( wChooseModel.getText() != null
            && ( wChooseModel.getText().endsWith( ".xmi" )
            || wChooseModel.getText().startsWith( "${" ) ) ) {
          bCreateOnPublish.setEnabled( true );
        } else {
          bCreateOnPublish.setEnabled( false );
          bCreateOnPublish.setSelection( false );
        }

      }
    } );
    wChooseModel.addSelectionListener( new SelectionAdapter() {
      @Override public void widgetSelected( SelectionEvent selectionEvent ) {
        super.widgetSelected( selectionEvent );
        wChooseModel.getCComboWidget().setSelection( new Point( 0, 0 ) );
      }
    } );
    props.setLook( wChooseModel );

    // Create on Publish Flag
    bCreateOnPublish = new Button( composite, SWT.CHECK );
    bCreateOnPublish.setText( getMsg( "SelectModelDialog.CreateOnPublish.Label" ) );
    bCreateOnPublish.setToolTipText( getMsg( "SelectModelDialog.CreateOnPublish.Label" ) );
    props.setLook( bCreateOnPublish );

    positionLabelInputPairBelow( wlChooseModel, wChooseModel, null, 0 );
    ( (FormData) wChooseModel.getLayoutData() ).width = DEFAULT_TEXT_SIZE_REGULAR;

    positionControlBelow( bCreateOnPublish, wChooseModel, DEFAULT_CONTROLS_TOP_MARGIN + 5 );

    ( (FormLayout) composite.getLayout() ).marginBottom = DEFAULT_COMPOSITE_BOTTOM_MARGIN;

    return composite;
  }

  @Override
  protected void createControls( final Shell shell, Control topControl, Control bottomControl, final Composite main ) {

    FormLayout layout = new FormLayout();
    layout.marginTop = 5;
    layout.marginBottom = 0;
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    main.setLayout( layout );

    final Control serverGroup = createServerConnectionGroup( main );
    positionControlBelow( serverGroup, null, DEFAULT_LABEL_INPUT_MARGIN );
    ( (FormData) serverGroup.getLayoutData() ).right = new FormAttachment( 100, -Const.MARGIN );

    final Control modelComposite = createModelComposite( main );
    positionControlBelow( modelComposite, serverGroup, DEFAULT_CONTROLS_TOP_MARGIN );

    // set min size of dialog
    final int height = getMinHeight( shell );
    shell.setMinimumSize( SHELL_MIN_WIDTH, height );
    shell.setSize( SHELL_MIN_WIDTH, height );
  }

  @Override
  protected boolean hasEntryNameHeader() {
    return false;
  }

  @Override
  protected void loadData( JobEntryBuildModel jobEntry ) {
    super.loadData( jobEntry );
    if ( jobEntry.getBiServerConnection() != null ) {
      serverConnectionGroupWrapper.setBiServerConnection( jobEntry.getBiServerConnection() );
    }
    if ( StringUtils.isNotBlank( jobEntry.getSelectedModel() ) ) {
      wChooseModel.setText( jobEntry.getSelectedModel() );
      wChooseModel.getCComboWidget().setSelection( new Point( 0, 0 ) );
    }
    bCreateOnPublish.setSelection( jobEntry.isCreateOnPublish() );
  }

  @Override
  protected void saveData( JobEntryBuildModel jobEntry ) {
    super.saveData( jobEntry );
    // save variables, not actual values
    BiServerConnection biServerConnection = serverConnectionGroupWrapper.getBiServerConnection( false );
    jobEntry.setBiServerConnection( biServerConnection );
    jobEntry.setSelectedModel( wChooseModel.getText() );
    jobEntry.setCreateOnPublish( bCreateOnPublish.getSelection() );
  }

  @Override protected String getTitle() {
    return getMsg( "SelectModelDialog.Title" );
  }

  protected BiServerConnection getBIServerConnection() {
    return serverConnectionGroupWrapper.getBiServerConnection( true );
  }

  @Override
  protected String getJobIcon() {
    return "ui/model_entry.png";
  }
}
