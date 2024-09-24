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
import org.apache.commons.lang.builder.EqualsBuilder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationManager;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.gui.SpoonFactory;
import org.pentaho.di.core.gui.SpoonInterface;
import org.pentaho.di.core.plugins.PluginInterface;
import org.pentaho.di.core.refinery.publish.util.ObjectUtils;
import org.pentaho.di.core.util.StringUtil;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.annotation.BaseAnnotationMeta;
import org.pentaho.di.trans.steps.annotation.ModelAnnotationMeta;
import org.pentaho.di.trans.util.TransUtil;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.widget.ComboVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;
import org.pentaho.di.ui.trans.steps.common.ModelAnnotationEvent;
import org.pentaho.di.ui.trans.steps.common.ModelAnnotationsTableComposite;
import org.pentaho.metastore.api.exceptions.MetaStoreException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * @author Rowell Belen
 */
public abstract class BaseAnnotationStepDialog extends BaseStepDialog implements StepDialogInterface {

  protected static Class<?> PKG = ModelAnnotationMeta.class; // for i18n purposes, needed by Translator2!!
  protected final int margin = Const.MARGIN;
  protected final int LEFT_MARGIN_OFFSET = 10;
  protected final int RIGHT_MARGIN_OFFSET = -10;
  protected final int SHELL_MIN_WIDTH = 900;
  protected final int SHELL_MIN_HEIGHT = 670;

  protected StyledText wDescription;
  protected Button wApply;
  protected Button wHelp;
  protected Label wIcon;
  protected Label wBottomHorizontalSeparator;

  protected BaseAnnotationMeta input;
  protected ModifyListener lsMod;

  protected Listener lsApply;
  protected Listener lsGroups;
  protected Listener lsAddGroup;
  protected Listener lsCopyGroup;
  protected Listener lsCheckForVariables;

  protected final Stack<String> markedForDeletion = new Stack<String>();

  protected boolean modelAnnotationDirty;
  protected boolean preventNewOrRename;

  public BaseAnnotationStepDialog( Shell parent, Object baseStepMeta,
      TransMeta transMeta, String stepname ) {
    super( parent, (StepMetaInterface) baseStepMeta, transMeta, stepname );
    input = (BaseAnnotationMeta) baseStepMeta;
    TransUtil.resetParams( transMeta, transMeta.getLogChannel() );
  }

  @Override
  public String open() {

    configureShell();
    initializeListeners();
    createOkCancelButtons();

    Control topWidget = createStepNameWidget();
    createContent( topWidget );

    afterOpen();

    // Open the dialog
    shell.open();

    // Wait for close
    while ( !shell.isDisposed() ) {
      if ( !getParent().getDisplay().readAndDispatch() ) {
        getParent().getDisplay().sleep();
      }
    }
    return stepname;
  }

  protected void configureShell() {
    shell = new Shell( getParent(), SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN );
    FormLayout shellLayout = new FormLayout();
    shellLayout.marginWidth = Const.FORM_MARGIN;
    shellLayout.marginHeight = Const.FORM_MARGIN;
    shell.setLayout( shellLayout );
    props.setLook( shell );
    setShellImage( shell, input );
    shell.setText( getDialogTitle() );

    // set min size of dialog
    shell.setMinimumSize( SHELL_MIN_WIDTH, SHELL_MIN_HEIGHT );
    shell.setSize( SHELL_MIN_WIDTH, SHELL_MIN_HEIGHT );
  }

  protected void initializeListeners() {
    lsMod = new ModifyListener() {
      public void modifyText( ModifyEvent e ) {
        input.setChanged();
      }
    };

    lsApply = new Listener() {
      @Override
      public void handleEvent( final Event event ) {
        if ( applyChanges() ) {
          modelAnnotationDirty = false;
          wApply.setEnabled( false );
        }
      }
    };

    lsOK = new Listener() {
      public void handleEvent( Event e ) {
        okay();
      }
    };

    lsCancel = new Listener() {
      public void handleEvent( Event e ) {
        cancel();
      }
    };

    lsCheckForVariables = new Listener() {
      @Override
      public void handleEvent( final Event event ) {
        if ( event.keyCode != 13 && event.keyCode != 0 ) {
          return;
        }

        if ( StringUtils.equals( getWGroups().getText(), getTableComposite().getData().getName() )
            || Const.isEmpty( getWGroups().getText() ) ) {
          return; // nothing changed or you are likely in the variable pop-up screen
        }

        if ( StringUtil.isVariable( getWGroups().getText() ) ) {
          populateTableFromMetastore( false );
          int idx = getWGroups().getCComboWidget().indexOf( envSub( getWGroups().getText() ) );
          if ( idx >= 0 ) {
            String varName = getWGroups().getText();
            getWGroups().getCComboWidget().select( idx );
            getWGroups().setText( varName );
          }
          setAnnotationsEditEnabled( false );
          modelAnnotationDirty = false;
        } else {
          handleNewOrRenameGroups();
        }
      }
    };

    lsGroups = new Listener() {
      @Override
      public void handleEvent( final Event event ) {
        // need to detect if existing model changed, show warning if it does
        if ( event == null || checkDirtyGroupDiscard() ) {
          populateTableFromMetastore();
        }
        getWGroups().setEditable( false );
      }
    };

    lsAddGroup = new Listener() {
      @Override
      public void handleEvent( final Event event ) {
        if ( checkDirtyGroupDiscard() ) {
          getWGroups().setText( "" );
          getWGroups().setEditable( true );
          populateTable( createNewAnnotationGroup() );
        }
      }
    };

    lsCopyGroup = new Listener() {
      @Override
      public void handleEvent( final Event event ) {
        if ( checkDirtyGroupDiscard() ) {
          String originalName = getWGroups().getText();
          ModelAnnotationGroup currentGroup = getTableComposite().getData();
          if ( StringUtils.isNotBlank( originalName ) && currentGroup != null ) {
            getWGroups().setText( BaseMessages.getString( PKG, "ModelAnnotation.NewAnnotation.CopyOf", originalName ) );
            getWGroups().setEditable( true );
            ModelAnnotationGroup modelAnnotations = ObjectUtils.deepClone( currentGroup );
            modelAnnotations.setName( getWGroups().getText() );
            populateTable( modelAnnotations );
          }
        }
      }
    };
  }

  protected ModelAnnotationGroup createNewAnnotationGroup() {
    return new ModelAnnotationGroup();
  }

  protected void handleNewOrRenameGroups() {

    if ( preventNewOrRename ) {
      return;
    }

    final String currentGroupName = getTableComposite().getData().getName();

    if ( groupNameExists() ) {
      Map<String, Listener> listenerMap = new LinkedHashMap<String, Listener>();
      listenerMap.put( BaseMessages.getString( PKG, "System.Button.No" ), new Listener() {
        @Override public void handleEvent( Event event ) {
          getWGroups().setText( currentGroupName == null ? "" : currentGroupName ); // revert to the current
          getWGroups().getCComboWidget().setFocus();
          getWGroups().getCComboWidget().setEditable( true );
        }
      } );
      listenerMap.put( BaseMessages.getString( PKG, "System.Button.Yes" ), new Listener() {
        @Override public void handleEvent( Event event ) {
          onModelAnnotationDirty();
          lsGroups.handleEvent( null ); //load the group
        }
      } );
      WarningDialog warningDialog = new WarningDialog(
          shell,
          BaseMessages.getString( PKG, "ModelAnnotation.UnableToRename.Title" ),
          BaseMessages.getString( PKG, "ModelAnnotation.UnableToRename.Message", getWGroups().getCComboWidget().getText() ),
          listenerMap );
      warningDialog.open();
    } else {
      if ( StringUtils.isBlank( currentGroupName ) ) {
        logDebug( "New Group..." );
      } else {
        logDebug( "Renamed " + currentGroupName + " to " + getWGroups().getText() );
        markedForDeletion.push( currentGroupName );
      }
      onModelAnnotationDirty();
    }

    setAnnotationsEditEnabled( true );
  }

  protected boolean groupNameExists() {

    String name = StringUtils.trim( getWGroups().getText() );
    String[] groupNames = getWGroups().getCComboWidget().getItems();

    if ( groupNames != null ) {
      for ( String groupName : groupNames ) {
        if ( StringUtils.equalsIgnoreCase(
            StringUtils.trim( groupName ), StringUtils.trim( name ) ) ) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * @return continue
   */
  protected boolean checkDirtyGroupDiscard() {
    if ( modelAnnotationDirty ) {

      preventNewOrRename = true; // suppress additional warnings

      Listener lsWarningYes = new Listener() {
        @Override
        public void handleEvent( final Event event ) {
          try {
            input.saveToMetaStore( getMetaStore(), getTableComposite().getData() );
            modelAnnotationDirty = false;
            onModelAnnotationDirty();
            preventNewOrRename = false; // reset
          } catch ( Exception e ) {
            showError( BaseMessages.getString( PKG, "ModelAnnotation.MetaStoreSaveError.Title" ),
                BaseMessages.getString( PKG, "ModelAnnotation.MetaStoreSaveError.Message", e.getLocalizedMessage() ) );
          }
        }
      };
      Listener lsWarningNo = new Listener() {
        @Override
        public void handleEvent( final Event event ) {
          String groupName = ( getTableComposite().getData() ).getName();
          if ( groupName != null ) {
            removeItem( getWGroups(), groupName );
          }
          modelAnnotationDirty = false;
          onModelAnnotationDirty();
          preventNewOrRename = false; // reset
        }
      };
      Listener lsWarningCancel = new Listener() {
        @Override
        public void handleEvent( final Event event ) {
          String name = ( getTableComposite().getData() ).getName();
          if ( name != null ) {
            getWGroups().setText( ( getTableComposite().getData() ).getName() ); // reset
          }
          preventNewOrRename = false; // reset
        }
      };
      Map<String, Listener> listenerMap = new LinkedHashMap<String, Listener>();
      listenerMap.put( BaseMessages.getString( PKG, "System.Button.Cancel" ), lsWarningCancel );
      listenerMap.put( BaseMessages.getString( PKG, "System.Button.No" ), lsWarningNo );
      listenerMap.put( BaseMessages.getString( PKG, "ModelAnnotation.UnsavedChangesWarning.Yes" ), lsWarningYes );
      WarningDialog warningDialog = new WarningDialog(
          shell,
          BaseMessages.getString( PKG, "ModelAnnotation.UnsavedChangesWarning.Title" ),
          BaseMessages.getString( PKG, "ModelAnnotation.UnsavedChangesWarning.Message",
              ( getTableComposite().getData() ).getName() ),
          listenerMap );
      warningDialog.open();
    } else {
      setAnnotationsEditEnabled( true );
      modelAnnotationDirty = false;
      onModelAnnotationDirty();
    }
    return !modelAnnotationDirty;
  }

  protected void okay() {
    if ( applyChanges() ) {
      dispose();
    }
  }

  protected void cancel() {
    stepname = null;
    dispose();
  }

  @Override
  public void dispose() {
    super.dispose();
  }

  protected void createOkCancelButtons() {

    // Create a horizontal separator
    wBottomHorizontalSeparator = new Label( shell, SWT.HORIZONTAL | SWT.SEPARATOR );
    FormData fdSepator = new FormData();
    fdSepator.left = new FormAttachment( 0, LEFT_MARGIN_OFFSET ); // match the left offset of the help button
    fdSepator.right = new FormAttachment( 100, RIGHT_MARGIN_OFFSET );
    fdSepator.bottom = new FormAttachment( wHelp, -15 ); // above help button
    wBottomHorizontalSeparator.setLayoutData( fdSepator );

    // Cancel Button
    wCancel = new Button( shell, SWT.PUSH );
    wCancel.setAlignment( SWT.CENTER );
    wCancel.setText( BaseMessages.getString( PKG, "System.Button.Cancel" ) + "  " ); // add spaces to push left
    FormData fdCancel = new FormData();
    fdCancel.top = new FormAttachment( wHelp, 0, SWT.TOP );
    fdCancel.right = new FormAttachment( 100, RIGHT_MARGIN_OFFSET );
    wCancel.setLayoutData( fdCancel );

    // OK Button
    wOK = new Button( shell, SWT.PUSH );
    wOK.setAlignment( SWT.CENTER );
    wOK.setText( BaseMessages.getString( PKG, "System.Button.OK" ) );
    FormData fdOK = new FormData();
    fdOK.top = new FormAttachment( wCancel, 0, SWT.TOP );
    fdOK.right = new FormAttachment( wCancel, -margin, SWT.LEFT );
    wOK.setLayoutData( fdOK );

    FormData fdApplyCont = new FormData();
    fdApplyCont.top = new FormAttachment( wOK, 0, SWT.TOP );
    fdApplyCont.right = new FormAttachment( wOK, -margin, SWT.LEFT );
    fdApplyCont.left = new FormAttachment( wHelp, margin, SWT.RIGHT );
    Composite comp = new Composite( shell, SWT.NONE );
    comp.setLayoutData( fdApplyCont );

    RowLayout rl = new RowLayout();
    rl.pack = true;
    rl.justify = true;
    rl.marginTop = 0;

    comp.setLayout( rl );
    props.setLook( comp );

    // Apply Button
    wApply = new Button( comp, SWT.PUSH );
    wApply.setAlignment( SWT.CENTER );
    wApply.setEnabled( false );
    wApply.setText( BaseMessages.getString( PKG, "System.Button.Apply" ) );

    RowData fdApply = new RowData();
    wApply.setLayoutData( fdApply );

    // Add listeners
    wOK.addListener( SWT.Selection, lsOK );
    wCancel.addListener( SWT.Selection, lsCancel );
    wApply.addListener( SWT.Selection, lsApply );
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

  protected Control createStepNameWidget() {

    // Create Step Name label
    wlStepname = new Label( shell, SWT.LEFT );
    wlStepname.setText( getStepName() );
    props.setLook( wlStepname );

    fdlStepname = new FormData();
    fdlStepname.left = new FormAttachment( 0, LEFT_MARGIN_OFFSET );
    fdlStepname.top = new FormAttachment( 0, 10 );
    wlStepname.setLayoutData( fdlStepname );

    // Create Step Name Text
    wStepname = new Text( shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    wStepname.setText( stepname );
    wStepname.addModifyListener( lsMod );
    props.setLook( wStepname );

    fdStepname = new FormData();
    fdStepname.top = new FormAttachment( wlStepname, 5 );
    fdStepname.left = new FormAttachment( 0, LEFT_MARGIN_OFFSET + 2 );
    fdStepname.right = new FormAttachment( 0, LEFT_MARGIN_OFFSET + 252 );
    wStepname.setLayoutData( fdStepname );

    // Create a horizontal separator
    Label wSeparator = new Label( shell, SWT.HORIZONTAL | SWT.SEPARATOR );
    FormData fdSepator = new FormData();
    fdSepator.top = new FormAttachment( wStepname, 15, SWT.BOTTOM );
    fdSepator.left = new FormAttachment( 0, LEFT_MARGIN_OFFSET + 2 );
    fdSepator.right = new FormAttachment( 100, RIGHT_MARGIN_OFFSET );
    wSeparator.setLayoutData( fdSepator );

    wIcon = new Label( shell, SWT.CENTER );
    wIcon.setImage( getImageIcon() );
    FormData fdIcon = new FormData();
    fdIcon.top = new FormAttachment( 0, 15 );
    fdIcon.right = new FormAttachment( 100, RIGHT_MARGIN_OFFSET );
    wIcon.setLayoutData( fdIcon );
    props.setLook( wIcon );

    return wSeparator;
  }

  protected Control createDescription( Control topWidget ) {
    Label wlDescription = new Label( shell, SWT.LEFT );
    wlDescription.setText(
        BaseMessages.getString( PKG, "SharedDimension.Dialog.Description.Label" ) );
    props.setLook( wlDescription );

    FormData fdlDescription = new FormData();
    fdlDescription.top = new FormAttachment( topWidget, 10 );
    fdlDescription.left = new FormAttachment( 0, LEFT_MARGIN_OFFSET );
    fdlDescription.right = new FormAttachment( 100, RIGHT_MARGIN_OFFSET );
    wlDescription.setLayoutData( fdlDescription );

    wDescription = new StyledText( shell, SWT.MULTI | SWT.BORDER | SWT.WRAP );
    props.setLook( wDescription );

    FormData fdDescription = new FormData();
    fdDescription.height = 50;
    fdDescription.top = new FormAttachment( wlDescription, 5 );
    fdDescription.left = new FormAttachment( 0, LEFT_MARGIN_OFFSET );
    fdDescription.right = new FormAttachment( 100, RIGHT_MARGIN_OFFSET );
    wDescription.setLayoutData( fdDescription );

    // Add Listener
    wDescription.addListener( SWT.FocusOut, new Listener() {
      @Override public void handleEvent( Event event ) {
        if ( input.getModelAnnotations() != null
            && textChanged( wDescription,
                StringUtils.defaultIfBlank( input.getModelAnnotations().getDescription(), "" ) ) ) {
          modelAnnotationDirty = true;
          onModelAnnotationDirty();
        }
      }
    } );

    return wDescription;
  }

  protected void afterOpen() {
    // Do nothing. Optional
  }

  protected String envSub( String var ) {
    return this.transMeta.environmentSubstitute( var );
  }

  protected void setDirtyFlag( final ModelAnnotationGroup g1, final ModelAnnotationGroup g2 ) {
    // compare if model annotations changed
    if ( groupChanged( g1, g2 ) ) {
      modelAnnotationDirty = true;
      onModelAnnotationDirty();
    } else {
      modelAnnotationDirty = false;
    }
  }

  private boolean groupChanged( final ModelAnnotationGroup g1, final ModelAnnotationGroup g2 ) {

    try {
      if ( !EqualsBuilder.reflectionEquals( g1, g2 ) ) {
        return true;
      }

      // check model annotations
      for ( int i = 0; i < g1.size(); i++ ) {

        // check fields except for annotation field
        if ( !EqualsBuilder.reflectionEquals( g1.get( i ), g2.get( i ), new String[] { "annotation" } ) ) {
          return true;
        }

        // manually check annotation properties
        Map<String, Serializable> g1Properties = g1.get( i ).describeAnnotation();
        Map<String, Serializable> g2Properties = g2.get( i ).describeAnnotation();

        if ( !EqualsBuilder.reflectionEquals( g1Properties, g2Properties ) ) {
          return true; // check mainly if one is null and the other isn't
        }

        if ( g1Properties != null && g2Properties != null ) {
          if ( !g1Properties.equals( g2Properties ) ) {
            return true; // deep check
          }
        }
      }
      return false; // no change
    } catch ( Exception e ) {
      return true;
    }
  }

  protected void getFields() {
    final ModelAnnotationGroup groupCopy = ObjectUtils.deepClone( getTableComposite().getData() );
    getTableComposite().getFields( getParent(), input, transMeta, stepname, new Listener() {
      @Override public void handleEvent( Event event ) {
        if ( event instanceof ModelAnnotationEvent ) {
          ModelAnnotationEvent e = (ModelAnnotationEvent) event;
          setDirtyFlag( groupCopy, e.getModelAnnotations() );
        }
      }
    } );
  }

  protected void editAnnotation() {
    final ModelAnnotationGroup groupCopy = ObjectUtils.deepClone( getTableComposite().getData() );
    getTableComposite().editAnnotation( shell, input, transMeta, stepname, getMetaStore(), new Listener() {
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

  protected void deleteAnnotation() {
    getTableComposite().deleteAnnotation();
    modelAnnotationDirty = true;
    onModelAnnotationDirty();
  }

  protected void populateTableFromMetastore() {
    populateTableFromMetastore( !StringUtil.isVariable( getSelectedModelAnnotationGroupName() ) );
  }

  protected void populateTableFromMetastore( boolean warnNotFound ) {
    try {
      String groupName = envSub( getSelectedModelAnnotationGroupName() );
      ModelAnnotationGroup modelAnnotations = null;
      if ( !StringUtil.isEmpty( groupName ) ) {
        modelAnnotations = getModelAnnotationManger().readGroup( groupName, getMetaStore() );
      }
      if ( modelAnnotations == null ) {
        if ( warnNotFound ) {
          Map<String, Listener> listenerMap = new LinkedHashMap<String, Listener>();
          listenerMap.put( BaseMessages.getString( PKG, "System.Button.OK" ), new Listener() {
            @Override
            public void handleEvent( final Event event ) {
            }
          } );
          WarningDialog warningDialog =
              new WarningDialog( shell, BaseMessages.getString( PKG, missingTitleKey() ),
                  BaseMessages.getString( PKG, missingMessageKey(),
                      getSelectedModelAnnotationGroupName() ),
                  listenerMap );
          warningDialog.open();
        }
        modelAnnotations = createNewAnnotationGroup();
      }
      populateTable( modelAnnotations );
    } catch ( MetaStoreException e ) {
      log.logError( e.getLocalizedMessage(), e );
      populateTable( new ModelAnnotationGroup() );
    }
  }

  protected abstract String missingMessageKey();

  protected abstract String missingTitleKey();

  protected void populateCategories( final String selectedCategory, final boolean sharedDimensions ) {
    boolean isShared = selectedCategory != null;
    if ( getWGroups().getItemCount() > 0 ) {
      getWGroups().removeAll();
    }
    try {
      final List<String> groupNames = getGroupNames( sharedDimensions );
      boolean foundCategory = false;
      for ( int i = 0; i < groupNames.size(); i++ ) {
        final String groupName = groupNames.get( i );
        getWGroups().add( groupName );
        if ( isShared && input.getModelAnnotationCategory().equals( groupName ) ) {
          getWGroups().select( i );
          foundCategory = true;
        }
      }
      if ( ( isShared && !foundCategory ) || ( StringUtils.isNotBlank( input.getModelAnnotationCategory() )
          && StringUtils
          .equals( selectedCategory, envSub( input.getModelAnnotationCategory() ) ) ) ) { // display var name
        getWGroups().setText( input.getModelAnnotationCategory() );
      }
    } catch ( Exception e ) {
      log.logError( e.getLocalizedMessage(), e );
    }
  }

  protected void setAnnotationsEditEnabled( boolean enabled ) {
    getTableComposite().setEnabled( enabled );
    wDescription.setEnabled( enabled );
    if ( enabled ) {
      props.setLook( wDescription );
    } else {
      wDescription.setBackground( GUIResource.getInstance().getColorDemoGray() );
    }
  }

  protected void populateTable( final ModelAnnotationGroup modelAnnotations ) {
    getTableComposite().populateTable( modelAnnotations, transMeta, stepname, shell );
    if ( wDescription != null && modelAnnotations != null ) {
      wDescription.setText( StringUtils.defaultIfBlank( modelAnnotations.getDescription(), "" ) );
    }
  }

  protected boolean textChanged( StyledText textWidget, String text ) {
    if ( textWidget != null && !StringUtils.equals( textWidget.getText(), text ) ) {
      return true;
    }

    return false;
  }

  protected List<String> getGroupNames( boolean sharedDimension ) {
    List<String> filteredGroupNames = new ArrayList<String>();
    try {
      final List<ModelAnnotationGroup> groupNames = getModelAnnotationManger().listGroups( getMetaStore() );
      if ( groupNames != null ) {
        for ( ModelAnnotationGroup modelAnnotationGroup : groupNames ) {
          if ( sharedDimension ) {
            if ( modelAnnotationGroup.isSharedDimension() ) {
              filteredGroupNames.add( modelAnnotationGroup.getName() );
            }
          } else {
            if ( !modelAnnotationGroup.isSharedDimension() ) {
              filteredGroupNames.add( modelAnnotationGroup.getName() );
            }
          }
        }
      }
    } catch ( Exception e ) {
      log.logError( e.getLocalizedMessage(), e );
    }

    return filteredGroupNames;
  }

  protected boolean removeItem( ComboVar combo, String elementName ) {
    int idx = combo.getCComboWidget().indexOf( elementName );
    if ( idx >= 0 ) {
      combo.getCComboWidget().remove( idx );
      return true;
    }
    return false;
  }

  public void linkAnnotations( final ComboVar combo, final String name, final String description, final Event parent ) {
    final String groupName = envSub( name );
    if ( !checkValidName( name ) ) {
      parent.doit = false;
      return;
    }
    try {
      // check if already exists
      ModelAnnotationGroup modelAnnotations = getModelAnnotationManger().readGroup( groupName, getMetaStore() );
      if ( modelAnnotations != null ) {
        Listener lsWarningYes = new Listener() {
          @Override
          public void handleEvent( final Event event ) {
            linkAnnotations( combo, groupName, description );
          }
        };
        Listener lsWarningNo = new Listener() {
          @Override
          public void handleEvent( final Event event ) {
            // stop parent action
            parent.doit = false;
          }
        };
        Map<String, Listener> listenerMap = new LinkedHashMap<String, Listener>();
        listenerMap.put( BaseMessages.getString( PKG, "System.Button.No" ), lsWarningNo );
        listenerMap.put( BaseMessages.getString( PKG, "ModelAnnotation.AnnotationGroupExists.Yes" ), lsWarningYes );
        WarningDialog warningDialog = new WarningDialog(
            shell,
            BaseMessages.getString( PKG, "ModelAnnotation.AnnotationGroupExists.Title" ),
            BaseMessages.getString( PKG, "ModelAnnotation.AnnotationGroupExists.Message", groupName ),
            listenerMap );
        warningDialog.open();
      } else {
        linkAnnotations( combo, groupName, description );
      }
    } catch ( MetaStoreException e ) {
      log.logError( e.getLocalizedMessage(), e );
    }
  }

  protected void linkAnnotations( final ComboVar combo, final String groupName, final String description ) {
    removeItem( combo, groupName );
    combo.add( groupName );
    combo.select( combo.getItemCount() - 1 );
    if ( wDescription != null ) {
      wDescription.setText( StringUtils.defaultIfBlank( description, "" ) );
    }
    ( getTableComposite().getData() ).setDescription( description );
    ( getTableComposite().getData() ).setName( groupName );
    modelAnnotationDirty = true;
    onLinkAnnotations();
  }

  protected boolean checkValidName( final String name ) {
    try {
      input.checkValidName( name );
      return true;
    } catch ( KettleException e ) {
      showError( BaseMessages.getString( PKG, "ModelAnnotation.MetaStoreInvalidName.Title" ), e.getLocalizedMessage() );
      return false;
    }
  }

  protected void discardAll( final ComboVar combo, final String selectItem ) {
    ( getTableComposite().getData() ).clear();
    getTableComposite().refresh();
    String[] items = combo.getItems();
    for ( int i = 0; i < items.length; i++ ) {
      if ( items[i].equals( selectItem ) ) {
        combo.select( i );
      }
    }
    populateTableFromMetastore();
    onDiscardAll();
  }

  protected void deleteStaleGroups( final boolean sharedDimensions ) {

    // Delete groups that need to be deleted - i.e. renamed groups
    while ( !markedForDeletion.empty() ) {
      try {
        String groupName = markedForDeletion.pop();
        if ( getModelAnnotationManger().containsGroup( groupName, getMetaStore() ) ) {
          getModelAnnotationManger().deleteGroup( groupName, getMetaStore() );
        }
      } catch ( MetaStoreException e ) {
        logError( e.getLocalizedMessage(), e );
      }
    }
    populateCategories( getWGroups().getText(), sharedDimensions );

  }

  public void showInfo( String title, String message ) {
    SpoonInterface spoon = getSpoon();
    spoon.messageBox( message, title, false, Const.INFO );
  }

  public void showError( String title, String message ) {
    Map<String, Listener> listenerMap = new HashMap<String, Listener>();
    listenerMap.put( BaseMessages.getString( PKG, "System.Button.OK" ), new Listener() {
      @Override
      public void handleEvent( final Event event ) {
      }
    } );
    new WarningDialog( shell, title, message, listenerMap ).open();
  }

  protected void resizeSummaryColumn( final ModelAnnotationsTableComposite tableComposite ) {
    // Resize certain widgets when dialog is resized
    shell.addControlListener( new ControlAdapter() {
      public void controlResized( ControlEvent e ) {

        if ( tableComposite != null ) {
          // calculate widths of first 3 columns of table
          int width = 0;
          for ( int i = 0; i < 3; i++ ) {
            width += tableComposite.getTable().getColumn( i ).getWidth();
          }

          // expand summary column to take up the rest of the space in the table
          tableComposite.getTable().getColumn( 2 ).setWidth( tableComposite.getTable().getSize().x - width );
        }
      }
    } );
  }

  protected SpoonInterface getSpoon() {
    return SpoonFactory.getInstance();
  }

  protected void onLinkAnnotations() {
    onModelAnnotationDirty();
  }

  protected void onDiscardAll() {
    onModelAnnotationDirty();
  }

  protected void onModelAnnotationDirty() {
    wApply.setEnabled( true );
  }

  protected abstract String getDialogTitle();

  protected abstract String getStepName();

  protected abstract Image getImageIcon();

  protected abstract void createContent( Control topWidget );

  protected abstract ModelAnnotationManager getModelAnnotationManger();

  protected abstract ModelAnnotationsTableComposite getTableComposite();

  protected abstract String getSelectedModelAnnotationGroupName();

  protected abstract boolean applyChanges();

  protected abstract ComboVar getWGroups();
}
