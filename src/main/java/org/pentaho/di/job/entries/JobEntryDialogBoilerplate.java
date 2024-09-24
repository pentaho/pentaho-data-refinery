/*! ******************************************************************************
 *
 * Pentaho Community Edition Project: data-refinery-pdi-plugin
 *
 * Copyright (C) 2002-2018 by Hitachi Vantara : http://www.pentaho.com
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

package org.pentaho.di.job.entries;

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entry.JobEntryDialogInterface;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.ui.core.ConstUI;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.gui.WindowProperty;
import org.pentaho.di.ui.job.dialog.JobDialog;
import org.pentaho.di.ui.job.entry.JobEntryDialog;
import org.pentaho.di.ui.util.SwtSvgImageUtil;
import static org.pentaho.di.core.refinery.UIBuilder.DEFAULT_CONTROLS_TOP_MARGIN;
import static org.pentaho.di.core.refinery.UIBuilder.SHELL_MARGIN_WIDTH;
import static org.pentaho.di.core.refinery.UIBuilder.DEFAULT_LABEL_INPUT_MARGIN;
import static org.pentaho.di.core.refinery.UIBuilder.SHELL_MIN_WIDTH;
import static org.pentaho.di.core.refinery.UIBuilder.SHELL_MARGIN_HEIGHT;
import static org.pentaho.di.core.refinery.UIBuilder.GROUP_MARGIN_WIDTH;
import static org.pentaho.di.core.refinery.UIBuilder.GROUP_MARGIN_HEIGHT;
import static org.pentaho.di.core.refinery.UIBuilder.DEFAULT_TEXT_SIZE_REGULAR;
import static org.pentaho.di.core.refinery.UIBuilder.createFormComposite;

/**
 * boilerplate goes here.
 * @param <T> Job entry type
 */
public abstract class JobEntryDialogBoilerplate<T extends JobEntryInterface>
    extends JobEntryDialog
    implements JobEntryDialogInterface {

  protected T jobEntry;

  protected Label wlName;
  protected Text wName;
  protected Label wIcon;
  private Label wTopSeparator;

  protected Button wOK, wCancel;
  protected Listener lsOK, lsCancel;

  /**
   * Sets entry changed.
   */
  protected ModifyListener lsMod;
  protected SelectionAdapter lsDef;

  protected boolean changed;

  protected Class<?> PKG;

  // width of the icon in a varfield
  protected static final int VAR_EXTRA_WIDTH = GUIResource.getInstance().getImageVariable().getBounds().width;

  /**
   * it's the contructor.
   * @param parent
   * @param jobEntry
   * @param rep
   * @param jobMeta
   */
  @SuppressWarnings( "unchecked" )
  public JobEntryDialogBoilerplate( Shell parent, JobEntryInterface jobEntry, Repository rep, JobMeta jobMeta ) {
    super( parent, jobEntry, rep, jobMeta );
    this.jobEntry = (T) jobEntry;
    PKG = getClass();
  }

  /**
   * open a shell.
   */
  public JobEntryInterface open() {
    Shell parent = getParent();
    Display display = parent.getDisplay();

    shell = new Shell( parent, SWT.DIALOG_TRIM | SWT.RESIZE );

    props.setLook( shell );
    JobDialog.setShellImage( shell, jobEntry );

    changed = jobEntry.hasChanged();

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = SHELL_MARGIN_WIDTH;
    formLayout.marginHeight = SHELL_MARGIN_HEIGHT;
    formLayout.marginBottom = 5; // Adjust
    shell.setLayout( formLayout );

    shell.setText( getTitle() );

    Control topControl = hasEntryNameHeader() ? createJobNameHeader() : shell;
    Control bottomControl = createOkCancelButtons();
    Composite main = createMainComposite( topControl, bottomControl );

    createControls( shell, topControl, bottomControl, main );

    createStandardListeners();

    loadData( jobEntry );

    // Set the shell size
    // BaseStepDialog.setSize( shell, SHELL_MIN_WIDTH, minHeight, true );

    shell.open();
    while ( !shell.isDisposed() ) {
      if ( !display.readAndDispatch() ) {
        display.sleep();
      }
    }
    return jobEntry;
  }

  protected boolean hasEntryNameHeader() {
    return true;
  }

  protected int getMinHeight( Composite shell ) {
    shell.pack();
    int minHeight = shell.computeSize( SHELL_MIN_WIDTH, SWT.DEFAULT ).y;
    return minHeight;
  }

  protected Composite createMainComposite( Control widgetAbove, Control widgetBelow ) {
    Composite composite = createFormComposite( shell );
    props.setLook( composite );
    FormData fdComposite = new FormData();
    fdComposite.top = new FormAttachment( widgetAbove, 0 );
    fdComposite.bottom = new FormAttachment( widgetBelow, 0 );
    fdComposite.left = new FormAttachment( 0 );
    fdComposite.right = new FormAttachment( 100 );
    composite.setLayoutData( fdComposite );
    FormLayout layout = new FormLayout();
    layout.marginWidth = 0;
    layout.marginHeight = 15;
    composite.setLayout( layout );
    return composite;
  }

  /**
   * Define UI.<br/>
   * Starts with a name label/field on top and ok/cancel buttons on bottom
   * unless {@link #open()} is overridden.
   *
   * @param shell
   */
  protected abstract void createControls( Shell shell, Control topControl, Control bottomControl, Composite main );

  protected Control createOkCancelButtons() {

    // Cancel Button
    wCancel = new Button( shell, SWT.PUSH );
    wCancel.setAlignment( SWT.CENTER );
    wCancel.setText( BaseMessages.getString( PKG, "System.Button.Cancel" ) + "  " ); // add spaces to push left
    FormData fdCancel = new FormData();
    fdCancel.right = new FormAttachment( 100 );
    fdCancel.bottom = new FormAttachment( 100 );
    wCancel.setLayoutData( fdCancel );

    // OK Button
    wOK = new Button( shell, SWT.PUSH );
    wOK.setAlignment( SWT.CENTER );
    wOK.setText( BaseMessages.getString( PKG, "System.Button.OK" ) );
    FormData fdOK = new FormData();
    fdOK.top = new FormAttachment( wCancel, 0, SWT.TOP );
    fdOK.right = new FormAttachment( wCancel, -Const.MARGIN, SWT.LEFT );
    wOK.setLayoutData( fdOK );

    // Create a horizontal separator
    Label wBottomHorizontalSeparator = new Label( shell, SWT.HORIZONTAL | SWT.SEPARATOR );
    FormData fdSeparator = new FormData();
    fdSeparator.left = new FormAttachment( 0 ); // match the left offset of the help button
    fdSeparator.right = new FormAttachment( 100 );
    fdSeparator.bottom = new FormAttachment( wCancel, -15 ); // above cancel button
    wBottomHorizontalSeparator.setLayoutData( fdSeparator );

    return wBottomHorizontalSeparator;
  }

  protected Composite createJobNameHeader() {

    Composite composite = createFormComposite( shell );
    props.setLook( composite );

    // Create Step Name label
    wlName = new Label( composite, SWT.LEFT );
    wlName.setText( BaseMessages.getString( PKG, "System.JobName.Label" ) );
    props.setLook( wlName );

    // Create Step Name Text
    wName = new Text( composite, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wName );

    addLabelInputPairBelow( wlName, wName, composite, DEFAULT_TEXT_SIZE_REGULAR );

    // Create a horizontal separator
    wTopSeparator = new Label( composite, SWT.HORIZONTAL | SWT.SEPARATOR );
    FormData fdSeparator = new FormData();
    fdSeparator.top = new FormAttachment( wName, 15 );
    fdSeparator.left = new FormAttachment( 0 );
    fdSeparator.right = new FormAttachment( 100 );
    wTopSeparator.setLayoutData( fdSeparator );

    createIconLabel( composite );
    return composite;
  }

  protected void createIconLabel( Composite parent ) {
    if ( StringUtils.isNotBlank( getJobIcon() ) ) {
      wIcon = new Label( parent, SWT.CENTER );
      Image img = SwtSvgImageUtil.getImage( shell.getDisplay(), getClass().getClassLoader(),
          getJobIcon(), ConstUI.LARGE_ICON_SIZE, ConstUI.LARGE_ICON_SIZE );
      wIcon.setImage( img );
      FormData fdIcon = new FormData();
      fdIcon.top = new FormAttachment( 0, 0 );
      fdIcon.right = new FormAttachment( 100 );
      wIcon.setLayoutData( fdIcon );
      PropsUI.getInstance().setLook( wIcon );
    }
  }

  protected abstract String getJobIcon();

  /**
   * get localized message from this entry's bundle<br>
   * @see BaseMessages#getString(String, String)
   */
  protected String getMsg( String messageKey ) {
    return BaseMessages.getString( PKG, messageKey );
  }

  private void createStandardListeners() {
    // Add listeners
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

    lsMod = new ModifyListener() {
      public void modifyText( ModifyEvent event ) {
        jobEntry.setChanged();
      }
    };

    lsDef = new SelectionAdapter() {
      public void widgetDefaultSelected( SelectionEvent e ) {
        ok();
      }
    };

    // window close
    shell.addShellListener( new ShellAdapter() {

      public void shellClosed( ShellEvent e ) {
        cancel();
      }
    } );
    wOK.addListener( SWT.Selection, lsOK );
    wCancel.addListener( SWT.Selection, lsCancel );

    if ( hasEntryNameHeader() ) {
      wName.addSelectionListener( lsDef );
      wName.addModifyListener( lsMod );
    }
  }

  /**
   * Add a label/input pair below a given widget with standard attachments.
   * @param label
   * @param input
   * @param widgetAbove
   * @return
   */
  protected Control addLabelInputPairBelow( Control label, Control input, Control widgetAbove ) {
    addLabelBelow( label, widgetAbove );
    return addControlBelow( input, label );
  }
  protected Control addLabelInputPairBelow( Control label, Control input, Control widgetAbove, int fieldWidth ) {
    addLabelBelow( label, widgetAbove );
    return addControlBelow( input, label, fieldWidth );
  }

  protected Control addLabelBelow( Control label, Control widgetAbove ) {
    props.setLook( label );
    FormData fData = new FormData();
    int topMargin = label.getParent().equals( widgetAbove ) ? 0 : DEFAULT_CONTROLS_TOP_MARGIN;
    fData.top = new FormAttachment( widgetAbove, topMargin );
    fData.left = new FormAttachment( 0 );
    label.setLayoutData( fData );
    return label;
  }

  protected Control addControlBelow( Control control, Control label ) {
    return addControlBelow( control, label, SWT.DEFAULT );
  }

  protected Control addControlBelow( Control control, Control above, int controlWidth ) {
    props.setLook( control );
    FormData fData = new FormData();
    int topMargin = control.getParent().equals( above ) ? 0 : DEFAULT_LABEL_INPUT_MARGIN;
    fData.top = new FormAttachment( above, topMargin );
    fData.left = new FormAttachment( 0 );
    if ( controlWidth > 0 ) {
      fData.width = controlWidth;
      fData.right = new FormAttachment( 100, -getControlOffset( control, controlWidth ) );
    }
    control.setLayoutData( fData );
    return control;
  }

  private int getControlOffset( Control control, int controlWidth ) {
    // remaining space for min size match
    return SHELL_MIN_WIDTH - getMarginWidths( control ) - controlWidth;
  }

  private int getMarginWidths( Control control ) {
    // get the width added by container margins and (wm-specific) decorations
    int extraWidth = 0;
    for ( Composite parent = control.getParent(); !parent.equals( getParent() ); parent = parent.getParent() ) {
      extraWidth += parent.computeTrim( 0, 0, 0, 0 ).width;
      if ( parent.getLayout() instanceof FormLayout ) {
        extraWidth += 2 * ( (FormLayout) parent.getLayout() ).marginWidth;
      }
    }
    return extraWidth;
  }

  protected Group createGroup( final Composite parent, Control widgetAbove, String groupText ) {
    Group group = new Group( parent, SWT.SHADOW_ETCHED_IN );
    group.setText( groupText );
    FormLayout groupLayout = new FormLayout();
    groupLayout.marginTop = -5; // Adjust. UX Recommendation for Top Margin of Group: 15
    groupLayout.marginBottom = -5; // Adjust. UX Recommendation for Bottom Margin of Group: 15
    groupLayout.marginWidth = GROUP_MARGIN_WIDTH;
    groupLayout.marginHeight = GROUP_MARGIN_HEIGHT;
    group.setLayout( groupLayout );
    props.setLook( group );
    FormData fData = new FormData();
    int topMargin = group.getParent().equals( widgetAbove ) ? 0 : DEFAULT_CONTROLS_TOP_MARGIN;
    fData.top = new FormAttachment( widgetAbove, topMargin );
    fData.left = new FormAttachment( 0 );
    fData.right = new FormAttachment( 100 );
    group.setLayoutData( fData );
    return group;
  }

  /**
   * Adds a single control in the middle of the window, with its 'natural' size, below another control.
   */
  protected Control addLoneControlBelow( Control control, Control widgetAbove ) {
    props.setLook( control );
    FormData fData = new FormData();
    int topMargin = control.getParent().equals( widgetAbove ) ? 0 : DEFAULT_CONTROLS_TOP_MARGIN;
    fData.top = new FormAttachment( widgetAbove, topMargin );
    fData.left = new FormAttachment( 0 );
    control.setLayoutData( fData );
    return control;
  }

  protected Control addLoneControlBelow( Control control, Control widgetAbove, int width ) {
    props.setLook( control );
    FormData fData = new FormData();
    fData.top = new FormAttachment( widgetAbove, Const.MARGIN );
    fData.width = width;
    control.pack( false );
    control.setLayoutData( fData );
    return control;
  }

  protected void setControlWidth( Control control, int width ) {
    Point size = control.getSize();
    size.x = width;
    control.setSize( size );
  }

  /**
   * When OK button is pressed.
   * @see #okToClose()
   */
  protected void ok() {
    if ( !okToClose() ) {
      return;
    }
    saveData( jobEntry );
    dispose();
  }

  /**
   * If can close dialog and save info.<br>
   * Checks if there is a name defined.
   */
  protected boolean okToClose() {
    if ( hasEntryNameHeader() && Const.isEmpty( wName.getText() ) ) {
      showError( getMsg( "System.StepJobEntryNameMissing.Title" ), getMsg( "System.JobEntryNameMissing.Msg" ) );
      return false;
    }
    return true;
  }

  /**
   * Show basic error dialog.
   */
  protected void showError( final String title, final String msg ) {
    MessageBox mb = new MessageBox( shell, SWT.OK | SWT.ICON_ERROR );
    mb.setText( title );
    mb.setMessage( msg );
    mb.open();
  }

  protected void cancel() {
    jobEntry.setChanged( changed );
    jobEntry = null;
    dispose();
  }

  public void dispose() {
    WindowProperty winprop = new WindowProperty( shell );
    props.setScreen( winprop );
    shell.dispose();
  }

  protected abstract String getTitle();

  /**
   * Load config into UI
   * 
   * @param jobEntry
   */
  protected void loadData( T jobEntry ) {
    if ( hasEntryNameHeader() && jobEntry.getName() != null ) {
      wName.setText( jobEntry.getName() );
    }
  }

  /**
   * Save UI values into config
   * 
   * @param jobEntry
   */
  protected void saveData( T jobEntry ) {
    // save config
    if ( hasEntryNameHeader() ) {
      jobEntry.setName( wName.getText() );
    }
  }
}
