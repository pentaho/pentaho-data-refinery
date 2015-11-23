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

package org.pentaho.di.ui.job.entries.publish;

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.refinery.publish.agilebi.BiServerConnection;
import org.pentaho.di.core.refinery.publish.model.DataSourcePublishModel;
import org.pentaho.di.core.refinery.publish.util.ObjectUtils;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entries.JobEntryDialogBoilerplate;
import org.pentaho.di.job.entries.publish.JobEntryDatasourcePublish;
import org.pentaho.di.job.entry.JobEntryDialogInterface;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.util.TransUtil;
import org.pentaho.di.ui.core.widget.ComboVar;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.job.entries.common.ConnectionValidator;
import org.pentaho.di.ui.job.entries.common.ServerConnectionGroupWrapper;

import static org.pentaho.di.core.refinery.UIBuilder.*;

/**
 * @author Rowell Belen
 */
public class JobEntryDatasourcePublishDialog extends JobEntryDialogBoilerplate<JobEntryDatasourcePublish>
    implements JobEntryDialogInterface {

  private final int SHELL_MIN_WIDTH = 435;
  // private final int SHELL_MIN_HEIGHT = 600;
  private JobMeta jobMeta;
  private JobEntryDatasourcePublish jobEntry;
  private DataSourcePublishModel model;
  private Button bForceOverwrite;
  private ServerConnectionGroupWrapper serverConnectionGroupWrapper;
  private ComboVar wAccessType;
  private TextVar wUserOrRoleAcl;

  public JobEntryDatasourcePublishDialog( Shell parent, JobEntryInterface jobEntry,
      Repository rep, JobMeta jobMeta ) throws KettleException {
    super( parent, jobEntry, rep, jobMeta );
    init( jobEntry, jobMeta );
    TransUtil.resetParams( jobMeta, jobEntry.getLogChannel() );
  }

  protected void init( final JobEntryInterface jobEntry, final JobMeta jobMeta ) throws KettleException {
    PKG = JobEntryDatasourcePublish.class;
    this.jobMeta = jobMeta;
    this.jobEntry = (JobEntryDatasourcePublish) jobEntry;
    this.model = this.jobEntry.getDataSourcePublishModel();
    if ( this.model == null ) {
      this.model = new DataSourcePublishModel();
    }
  }

  protected Composite createModelComposite( Composite main ) {

    Composite composite = createFormComposite( main );
    props.setLook( composite );

    // Overwrite Flag
    bForceOverwrite = new Button( composite, SWT.CHECK );
    bForceOverwrite.setToolTipText( getMsg( "JobEntryDatasourcePublish.Overwrite.Tooltip" ) );
    bForceOverwrite.setText( getMsg( "JobEntryDatasourcePublish.Overwrite.Label" ) );
    props.setLook( bForceOverwrite );

    positionControlBelow( bForceOverwrite, null, 0 );

    return composite;
  }

  protected Group createServerConnectionGroup( final Composite main ) {
    serverConnectionGroupWrapper = new ServerConnectionGroupWrapper( main, props, this.jobMeta, this.jobEntry, PKG );
    serverConnectionGroupWrapper.addSubmitButtonListener( new Listener() {
      public void handleEvent( Event event ) {
        ConnectionValidator connectionValidator = new ConnectionValidator();
        connectionValidator.setConnection( getDataSourcePublishModel( true ).getBiServerConnection() );
        connectionValidator.validateConnectionInDesignTime();
      }
    } );

    return serverConnectionGroupWrapper.getGroup();
  }

  private Group createAclDefinitionGroup( final Composite main ) {

    Group group = createFormGroup( main );
    group.setText( getMsg( "AclDefinition.Group.Label" ) );
    props.setLook( group );

    //Access Type drop down
    Label wlAccessType = new Label( group, SWT.LEFT );
    wlAccessType.setText( getMsg( "AclDefinition.AccessType.Label" ) );
    wlAccessType.setToolTipText( getMsg( "AclDefinition.AccessType.Tooltip" ) );
    wAccessType = new ComboVar( jobMeta, group, SWT.BORDER );
    wAccessType.addModifyListener( new ModifyListener() {

      public void modifyText( ModifyEvent e ) {
        jobEntry.setChanged();
        boolean textFieldEnabled = !wAccessType.getText().equals( getMsg( "AclDefinition.AccessType.Everyone" ) );
        wUserOrRoleAcl.setEnabled( textFieldEnabled );
        if ( !textFieldEnabled ) {
          wUserOrRoleAcl.setText( StringUtils.EMPTY );
          wUserOrRoleAcl.setForeground( getParent().getDisplay().getSystemColor( SWT.COLOR_GRAY ) ); // gray out
        } else {
          wUserOrRoleAcl.setForeground( null ); // use default
        }
      }
    } );
    wAccessType.setToolTipText( getMsg( "AclDefinition.AccessType.Tooltip" ) );
    wAccessType.add( getMsg( "AclDefinition.AccessType.Everyone" ) );
    wAccessType.add( getMsg( "AclDefinition.AccessType.User" ) );
    wAccessType.add( getMsg( "AclDefinition.AccessType.Role" ) );
    props.setLook( wlAccessType );
    props.setLook( wAccessType );

    //User or Role text box
    Label wlUserOrRoleAcl = new Label( group, SWT.LEFT );
    wlUserOrRoleAcl.setText( getMsg( "AclDefinition.Role.Label" ) );
    wlUserOrRoleAcl.setToolTipText( getMsg( "AclDefinition.Role.Tooltip" ) );
    wUserOrRoleAcl = new TextVar( jobMeta, group, SWT.BORDER );
    wUserOrRoleAcl.addModifyListener( new ModifyListener() {
      public void modifyText( ModifyEvent e ) {
        jobEntry.setChanged();
      }
    } );
    wUserOrRoleAcl.setToolTipText( getMsg( "AclDefinition.Role.Tooltip" ) );
    props.setLook( wlUserOrRoleAcl );
    props.setLook( wUserOrRoleAcl );

    positionLabelInputPairBelow( wlAccessType, wAccessType, null, DEFAULT_NO_MARGIN, LEFT_MARGIN_OFFSET );
    ( (FormData) wAccessType.getLayoutData() ).width = DEFAULT_COMBO_SIZE;

    positionLabelInputPairBelow( wlUserOrRoleAcl, wUserOrRoleAcl, wAccessType, DEFAULT_CONTROLS_TOP_MARGIN,
        LEFT_MARGIN_OFFSET );
    ( (FormData) wUserOrRoleAcl.getLayoutData() ).width = DEFAULT_TEXT_SIZE_REGULAR;

    return group;
  }

  @Override
  protected void createControls( final Shell shell, Control topControl, Control bottomControl, final Composite main ) {

    final Control modelComposite = createModelComposite( main );
    positionControlBelow( modelComposite, null, DEFAULT_COMPOSITE_TOP_MARGIN );

    final Control serverGroup = createServerConnectionGroup( main );
    positionControlBelow( serverGroup, modelComposite, DEFAULT_COMPOSITE_TOP_MARGIN );

    final Control aclGroup = createAclDefinitionGroup( main );
    positionControlBelow( aclGroup, serverGroup, DEFAULT_COMPOSITE_TOP_MARGIN );

    // set min size of dialog
    final int minHeight = getMinHeight( shell );
    shell.setMinimumSize( SHELL_MIN_WIDTH, minHeight );
    shell.setSize( SHELL_MIN_WIDTH, minHeight );
  }

  @Override protected String getTitle() {
    return getMsg( "JobEntryDatasourcePublish.JobName" );
  }

  @Override
  protected void loadData( JobEntryDatasourcePublish jobEntry ) {
    super.loadData( jobEntry );
    this.model = this.jobEntry.getDataSourcePublishModel();
    if ( this.model == null ) {
      this.model = new DataSourcePublishModel();
    }

    this.bForceOverwrite.setSelection( this.model.isOverride() );
    if ( this.model.getUserOrRole() != null ) {
      this.wUserOrRoleAcl.setText( this.model.getUserOrRole() );
    }
    if ( this.model.getAccessType() != null ) {
      if ( this.model.getAccessType().equals( DataSourcePublishModel.ACCESS_TYPE_EVERYONE ) ) {
        this.wAccessType.select( 0 );
      } else if ( this.model.getAccessType().equals( DataSourcePublishModel.ACCESS_TYPE_USER ) ) {
        this.wAccessType.select( 1 );
      } else if ( this.model.getAccessType().equals( DataSourcePublishModel.ACCESS_TYPE_ROLE ) ) {
        this.wAccessType.select( 2 );
      } else {
        this.wAccessType.setText( this.model.getAccessType() );
      }
    } else {
      this.wAccessType.select( 0 );
    }

    if ( this.model.getBiServerConnection() != null ) {
      BiServerConnection biServerModel = this.model.getBiServerConnection();
      this.serverConnectionGroupWrapper.setBiServerConnection( biServerModel );
    }
  }

  @Override
  protected void saveData( JobEntryDatasourcePublish jobEntry ) {
    super.saveData( jobEntry );

    this.model = getDataSourcePublishModel( false ); // save variables, not actual values

    DataSourcePublishModel copy = ObjectUtils.deepClone( this.model );
    copy.setModelName( "" );

    jobEntry.setDataSourcePublishModel( copy );
  }

  protected DataSourcePublishModel getDataSourcePublishModel( boolean resolveVariables ) {

    DataSourcePublishModel dataSourcePublishModel = new DataSourcePublishModel();

    dataSourcePublishModel.setOverride( bForceOverwrite.getSelection() );

    if ( resolveVariables ) {
      dataSourcePublishModel.setAccessType( getAccessTypeCodeFromDescription(
              this.jobMeta.environmentSubstitute( wAccessType.getText() ) ) );
      dataSourcePublishModel.setUserOrRole( this.jobMeta.environmentSubstitute( wUserOrRoleAcl.getText() ) );
    } else {
      dataSourcePublishModel.setAccessType( getAccessTypeCodeFromDescription( wAccessType.getText() ) );
      dataSourcePublishModel.setUserOrRole( wUserOrRoleAcl.getText() );

    }

    BiServerConnection biServerModel = serverConnectionGroupWrapper.getBiServerConnection( resolveVariables );
    dataSourcePublishModel.setBiServerConnection( biServerModel );

    return dataSourcePublishModel;
  }


  private String getAccessTypeCodeFromDescription( String accessTypeDescription ) {
    if ( getMsg( "AclDefinition.AccessType.Everyone" ).equals( accessTypeDescription ) ) {
      return DataSourcePublishModel.ACCESS_TYPE_EVERYONE;
    }
    if ( getMsg( "AclDefinition.AccessType.User" ).equals( accessTypeDescription ) ) {
      return DataSourcePublishModel.ACCESS_TYPE_USER;
    }
    if ( getMsg( "AclDefinition.AccessType.Role" ).equals( accessTypeDescription ) ) {
      return DataSourcePublishModel.ACCESS_TYPE_ROLE;
    }
    return accessTypeDescription;
  }

  @Override
  protected String getJobIcon() {
    return "publish.svg";
  }
}
