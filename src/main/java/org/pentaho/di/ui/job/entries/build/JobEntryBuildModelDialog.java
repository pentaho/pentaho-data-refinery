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

package org.pentaho.di.ui.job.entries.build;

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.exception.KettleException;
import static org.pentaho.di.core.refinery.UIBuilder.BUTTON_MIN_WIDTH;
import static org.pentaho.di.core.refinery.UIBuilder.DEFAULT_LABEL_INPUT_MARGIN;
import static org.pentaho.di.core.refinery.UIBuilder.DEFAULT_TEXT_SIZE_REGULAR;
import static org.pentaho.di.core.refinery.UIBuilder.SHELL_MIN_WIDTH;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entries.JobEntryDialogBoilerplate;
import org.pentaho.di.job.entries.build.JobEntryBuildModel;
import org.pentaho.di.job.entry.JobEntryDialogInterface;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.util.TransUtil;
import org.pentaho.di.ui.core.widget.ComboVar;
import org.pentaho.di.ui.core.widget.TextVar;

public class JobEntryBuildModelDialog extends JobEntryDialogBoilerplate<JobEntryBuildModel> implements
    JobEntryDialogInterface {

  private ComboVar wOutputStep;
  private Label wlOutputStep;
  private TextVar wModelName;
  private Label wlModelName;
  private Button wUseAutoModel;
  private Button wUseExistingModel;
  private Group gModelGroup;
  private Text wExistingModelName;
  private Label wlExistingModelExtra;
  private Button wSelectExistingModel;
  private Composite wExistingModel;

  private SelectModelDialog selectModelDialog;

  private JobEntryBuildModel jobEntry;

  public JobEntryBuildModelDialog( Shell parent, JobEntryInterface jobEntry, Repository rep, JobMeta jobMeta )
    throws KettleException {
    super( parent, jobEntry, rep, jobMeta );
    this.jobEntry = (JobEntryBuildModel) jobEntry;
    this.jobEntry.setRepository( jobMeta.getRepository() );
    TransUtil.resetParams( jobMeta, jobEntry.getLogChannel() );

    PKG = JobEntryBuildModel.class;
  }

  @Override
  protected String getTitle() {
    return getMsg( "BuildModelJob.Name" );
  }

  @Override
  protected void createControls( final Shell shell, Control topControl, Control bottomControl, final Composite main ) {

    FormLayout layout = new FormLayout();
    layout.marginTop = 0;
    layout.marginBottom = 0;
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    Control widgetAbove = main;

    // select output step
    wOutputStep = new ComboVar( jobMeta, main, SWT.LEFT | SWT.BORDER );
    final String[] stepNames = jobEntry.getOutputStepList( jobMeta );

    // empty list doesn't render well
    wOutputStep.setItems( stepNames.length > 0 ? stepNames : new String[] { "" } );
    wOutputStep.setToolTipText( getMsg( "BuildModelJob.OutputStep.Description" ) );
    wOutputStep.addSelectionListener( new SelectionAdapter() {
      @Override public void widgetSelected( SelectionEvent selectionEvent ) {
        super.widgetSelected( selectionEvent );
        wOutputStep.getCComboWidget().setSelection( new Point( 0, 0 ) );
      }
    } );

    wlOutputStep = new Label( main, SWT.RIGHT );
    wlOutputStep.setText( getMsg( "BuildModelJob.OutputStep.Label" ) );
    widgetAbove = addLabelInputPairBelow( wlOutputStep, wOutputStep, widgetAbove,
        DEFAULT_TEXT_SIZE_REGULAR + VAR_EXTRA_WIDTH );

    // Model Name text box
    wModelName = new TextVar( jobMeta, main, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    wModelName.setToolTipText( getMsg( "BuildModelJob.ModelName.Description" ) );
    wlModelName = new Label( main, SWT.RIGHT );
    wlModelName.setText( getMsg( "BuildModelJob.ModelName.Label" ) );
    widgetAbove = addLabelInputPairBelow( wlModelName, wModelName, widgetAbove,
        DEFAULT_TEXT_SIZE_REGULAR + VAR_EXTRA_WIDTH );

    addModelGroup( main, widgetAbove );

    // set a decent minimum
    final int minHeight = getMinHeight( shell );
    shell.setMinimumSize( SHELL_MIN_WIDTH, minHeight );
    shell.setSize( SHELL_MIN_WIDTH, minHeight );
  }

  /**
   * Modeling method group
   */
  private Group addModelGroup( final Composite parent, Control widgetAbove ) {

    gModelGroup = createGroup( parent, widgetAbove, getMsg( "BuildModelJob.ModelingMethod" ) );
    widgetAbove = gModelGroup;

    wUseAutoModel = new Button( gModelGroup, SWT.RADIO );
    wUseAutoModel.setText( getMsg( "BuildModelJob.AutoModel" ) );
    wUseAutoModel.setSelection( true );
    wUseAutoModel.addSelectionListener( modelMethodSelectionListener( false ) );
    widgetAbove = addControlBelow( wUseAutoModel, widgetAbove );
    ( (FormData) wUseAutoModel.getLayoutData() ).left = new FormAttachment( 0, 1 ); // override default

    wUseExistingModel = new Button( gModelGroup, SWT.RADIO );
    wUseExistingModel.setText( getMsg( "BuildModelJob.UseExistingModel" ) );
    wUseExistingModel.addSelectionListener( modelMethodSelectionListener( true ) );
    widgetAbove = addControlBelow( wUseExistingModel, widgetAbove );
    ( (FormData) wUseExistingModel.getLayoutData() ).left = new FormAttachment( 0, 1 ); // override default

    widgetAbove = addExistingModelControl( gModelGroup, widgetAbove );
    return gModelGroup;
  }

  private Control addExistingModelControl( Composite parent, Control widgetAbove ) {
    wExistingModel = new ExistingModelControl( parent, SWT.NULL );
    wExistingModel.setLayout( new FormLayout() );

    wSelectExistingModel = new Button( wExistingModel, SWT.BUTTON1 );
    wSelectExistingModel.setText( getMsg( "BuildModelJob.SelectExistingModel" ) );
    props.setLook( wSelectExistingModel );
    FormData fdSelectExisting = new FormData( BUTTON_MIN_WIDTH, SWT.DEFAULT );
    fdSelectExisting.right = new FormAttachment( 100 );
    wSelectExistingModel.setLayoutData( fdSelectExisting );
    wSelectExistingModel.addListener( SWT.Selection, new Listener() {
      @Override public void handleEvent( Event event ) {
        selectModelDialog = new SelectModelDialog( shell, jobEntry, rep, jobMeta ) {

          @Override protected String getJobIcon() {
            return null;
          }

          @Override
          protected void ok() {
            super.ok();
            updateExistingModel( jobEntry );
          }
        };
        selectModelDialog.open();
      }
    } );

    wExistingModelName = new Text( wExistingModel, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wExistingModelName );
    // 'magic' offset...
    final int leftIndent = 18;
    FormData fdExistingModelName = new FormData( DEFAULT_TEXT_SIZE_REGULAR, SWT.DEFAULT );
    fdExistingModelName.top = new FormAttachment( wUseExistingModel );
    fdExistingModelName.left = new FormAttachment( 0, leftIndent );
    fdExistingModelName.right = new FormAttachment( wSelectExistingModel, -DEFAULT_LABEL_INPUT_MARGIN );
    wExistingModelName.setLayoutData( fdExistingModelName );
    wExistingModelName.setEditable( false );
    wlExistingModelExtra = new Label( wExistingModel, SWT.LEFT | SWT.WRAP );
    FormData fdExistingModelExtra = new FormData();
    fdExistingModelExtra.top = new FormAttachment( wExistingModelName, DEFAULT_LABEL_INPUT_MARGIN );
    fdExistingModelExtra.left = new FormAttachment( 0, leftIndent );
    fdExistingModelExtra.bottom = new FormAttachment( 100 );
    fdExistingModelExtra.right = new FormAttachment( 100 );
    fdExistingModelExtra.height = 35;
    wlExistingModelExtra.setLayoutData( fdExistingModelExtra );
    wlExistingModelExtra.setEnabled( false );
    props.setLook( wlExistingModelExtra );
    wSelectExistingModel.pack();
    int width = leftIndent + DEFAULT_TEXT_SIZE_REGULAR + DEFAULT_LABEL_INPUT_MARGIN
        + Math.max( BUTTON_MIN_WIDTH, wSelectExistingModel.getSize().x ) + 20;
    return addControlBelow( wExistingModel, widgetAbove, width );
  }

  private class ExistingModelControl extends Composite {

    public ExistingModelControl( Composite parent, int flags ) {
      super( parent, flags );
    }

    @Override
    public void setEnabled( boolean enabled ) {
      wSelectExistingModel.setEnabled( enabled );
      wExistingModelName.setEnabled( enabled );
      wlExistingModelExtra.setEnabled( enabled );

      // Manually set "disabled" look due to OSX bug with labels inside group components
      if ( enabled ) {
        wlExistingModelExtra.setForeground( null ); // use default
      } else {
        wlExistingModelExtra.setForeground( getDisplay().getSystemColor( SWT.COLOR_GRAY ) ); // gray out
      }
    }
  }

  private void updateExistingModel( JobEntryBuildModel jobEntry ) {
    final String selectedModel = StringUtils.defaultString( jobEntry.getSelectedModel(), StringUtils.EMPTY );
    wExistingModelName.setText( selectedModel );
    if ( StringUtils.isBlank( selectedModel ) ) {
      wlExistingModelExtra.setText( getMsg( "BuildModelJob.SelectedModelInfo.NotSelected" ) );
    } else {
      if ( jobEntry.isCreateOnPublish() ) {
        wlExistingModelExtra.setText( getMsg( "BuildModelJob.SelectedModelInfo.CreateOnPublish" ) );
      } else {
        wlExistingModelExtra.setText( getMsg( "BuildModelJob.SelectedModelInfo.NotCreateOnPublish" ) );
      }
    }
  }

  private SelectionListener modelMethodSelectionListener( final boolean enabled ) {
    return new SelectionListener() {
      @Override public void widgetSelected( final SelectionEvent selectionEvent ) {
        wExistingModel.setEnabled( enabled );
      }

      @Override public void widgetDefaultSelected( final SelectionEvent selectionEvent ) {
      }
    };
  }

  @Override
  protected void loadData( JobEntryBuildModel jobEntry ) {
    super.loadData( jobEntry );
    if ( jobEntry.getOutputStep() != null ) {
      wOutputStep.setText( jobEntry.getOutputStep() );
      wOutputStep.getCComboWidget().setSelection( new Point( 0, 0 ) );
    }
    if ( jobEntry.getModelName() != null ) {
      wModelName.setText( jobEntry.getModelName() );
    }
    wUseExistingModel.setSelection( jobEntry.useExistingModel() );
    wUseAutoModel.setSelection( !jobEntry.useExistingModel() );
    wExistingModel.setEnabled( jobEntry.useExistingModel() );
    updateExistingModel( jobEntry );
  }

  @Override
  protected void saveData( JobEntryBuildModel jobEntry ) {
    super.saveData( jobEntry );
    jobEntry.setOutputStep( wOutputStep.getText() );
    jobEntry.setModelName( wModelName.getText() );
    jobEntry.setUseExistingModel( wUseExistingModel.getSelection() );
    jobEntry.setExistingModel( wExistingModelName.getText() );
  }

  @Override
  protected String getJobIcon() {
    return "model_entry.svg";
  }

  @Override protected boolean okToClose() {
    if ( super.okToClose() ) {
      if ( StringUtils.containsAny( wModelName.getText(), "/\\\t\r\n" ) ) {
        showError( getMsg( "System.StepJobEntryNameMissing.Title" ), getMsg( "BuildModelJob.Error.ModelName.InvalidCharacter" ) );
        return false;
      }
      return true;
    }
    return false;
  }
}
