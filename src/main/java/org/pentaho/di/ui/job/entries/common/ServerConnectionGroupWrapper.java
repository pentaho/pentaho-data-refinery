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


package org.pentaho.di.ui.job.entries.common;

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.pentaho.di.core.refinery.publish.agilebi.BiServerConnection;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.widget.TextVar;

import static org.pentaho.di.core.refinery.UIBuilder.*;

/**
 * @author Rowell Belen
 */
public class ServerConnectionGroupWrapper {

  private Class<?> msgClass;

  private Composite main;
  private TextVar wPublishUrl;
  private Label wlPublishUrl;
  private TextVar wPublishUser;
  private Label wlPublishUser;
  private TextVar wPublishPassword;
  private Label wlPublishPassword;
  private Button bSubmit;
  private PropsUI props;
  private JobMeta jobMeta;
  private JobEntryInterface jobEntry;
  private ModifyListener modifyListener;

  private Group group;

  public ServerConnectionGroupWrapper( Composite main, PropsUI props, JobMeta jobMeta, JobEntryInterface jobEntry,
      Class<?> msgClass ) {
    this.msgClass = msgClass;
    this.main = main;
    this.props = props;
    this.jobMeta = jobMeta;
    this.jobEntry = jobEntry;
    init();
  }

  private void init() {
    modifyListener = new ModifyListener() {
      public void modifyText( ModifyEvent e ) {
        jobEntry.setChanged();
      }
    };

    group = createServerGroup();
  }

  private Group createServerGroup() {

    Group group = createFormGroup( this.main );
    group.setText( getMsg( "ServerConnection.Group.Label" ) );
    props.setLook( group );

    // Publish URL text box
    wlPublishUrl = new Label( group, SWT.LEFT );
    wlPublishUrl.setText( getMsg( "ServerConnection.Url.Label" ) );
    wlPublishUrl.setToolTipText( getMsg( "ServerConnection.Url.Tooltip" ) );
    wPublishUrl = new TextVar( jobMeta, group, SWT.BORDER );
    wPublishUrl.addModifyListener( modifyListener );
    wPublishUrl.setToolTipText( getMsg( "ServerConnection.Url.Tooltip" ) );
    props.setLook( wlPublishUrl );
    props.setLook( wPublishUrl );

    // Publish User text box
    wlPublishUser = new Label( group, SWT.LEFT );
    wlPublishUser.setText( getMsg( "ServerConnection.User.Label" ) );
    wlPublishUser.setToolTipText( getMsg( "ServerConnection.User.Tooltip" ) );
    wPublishUser = new TextVar( jobMeta, group, SWT.BORDER );
    wPublishUser.addModifyListener( modifyListener );
    wPublishUser.setToolTipText( getMsg( "ServerConnection.User.Tooltip" ) );
    props.setLook( wlPublishUser );
    props.setLook( wPublishUser );

    // Publish Password text box
    wlPublishPassword = new Label( group, SWT.LEFT );
    wlPublishPassword.setText( getMsg( "ServerConnection.Password.Label" ) );
    wPublishPassword = new TextVar( jobMeta, group, SWT.PASSWORD | SWT.BORDER );
    wPublishPassword.addModifyListener( modifyListener );
    props.setLook( wlPublishPassword );
    props.setLook( wPublishPassword );

    // Submit Button
    bSubmit = new Button( group, SWT.PUSH | SWT.CENTER );
    bSubmit.setText( " " + getMsg( "ServerConnection.Submit.Label" ) + " " ); // padding not supported, use space hack
    bSubmit.setToolTipText( getMsg( "ServerConnection.Submit.Description" ) );
    props.setLook( bSubmit );

    positionServerGroup( group );

    return group;
  }

  public Group getGroup() {
    return group;
  }

  public void setSubmitButtonText( String txt ) {
    bSubmit.setText( txt );
  }

  public void setSubmitButtonTooltip( String txt ) {
    bSubmit.setToolTipText( txt );
  }

  public void addSubmitButtonListener( Listener listener ) {
    if ( listener != null ) {
      bSubmit.addListener( SWT.Selection, listener );
    }
  }

  public void setBiServerConnection( BiServerConnection biserverConnection ) {
    wPublishUrl.setText( biserverConnection.getUrl() );
    wPublishUser.setText( biserverConnection.getUserId() );
    wPublishPassword.setText( biserverConnection.getPassword() );
  }

  public BiServerConnection getBiServerConnection( boolean resolveVariables ) {

    BiServerConnection biServerModel = new BiServerConnection();

    if ( resolveVariables ) {
      String url = this.jobMeta.environmentSubstitute( wPublishUrl.getText() );
      if ( StringUtils.endsWith( url, "//" ) ) {
        url = StringUtils.chop( url );
      }
      biServerModel.setUrl( url );
      biServerModel.setUserId( this.jobMeta.environmentSubstitute( wPublishUser.getText() ) );
      biServerModel.setPassword( this.jobMeta.environmentSubstitute( wPublishPassword.getText() ) );
    } else {
      biServerModel.setUrl( wPublishUrl.getText() );
      biServerModel.setUserId( wPublishUser.getText() );
      biServerModel.setPassword( wPublishPassword.getText() );
    }

    return biServerModel;
  }

  private void positionServerGroup( final Group group ) {

    positionLabelInputPairBelow( wlPublishUrl, wPublishUrl, null, DEFAULT_NO_MARGIN, LEFT_MARGIN_OFFSET );
    ( (FormData) wPublishUrl.getLayoutData() ).width = DEFAULT_TEXT_SIZE_LONG;

    positionLabelInputPairBelow( wlPublishUser, wPublishUser, wPublishUrl, DEFAULT_CONTROLS_TOP_MARGIN,
        LEFT_MARGIN_OFFSET );
    ( (FormData) wPublishUser.getLayoutData() ).width = DEFAULT_TEXT_SIZE_REGULAR;

    positionLabelInputPairBelow( wlPublishPassword, wPublishPassword, wPublishUser, DEFAULT_CONTROLS_TOP_MARGIN,
        LEFT_MARGIN_OFFSET );
    ( (FormData) wPublishPassword.getLayoutData() ).width = DEFAULT_TEXT_SIZE_REGULAR;

    positionControlBelow( bSubmit, wPublishPassword, DEFAULT_COMPOSITE_TOP_MARGIN, LEFT_MARGIN_OFFSET );
  }

  /**
   * get localized message from this entry's bundle<br>
   *
   * @see org.pentaho.di.i18n.BaseMessages#getString(String, String)
   */
  protected String getMsg( String messageKey ) {
    return BaseMessages.getString( this.msgClass, messageKey );
  }
}
