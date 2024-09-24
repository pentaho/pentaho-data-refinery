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

package org.pentaho.di.ui.trans.steps.common;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.widget.ComboVar;

/**
 * @author Rowell Belen
 */
public class GroupComposite extends BaseComposite {

  private ComboVar wGroups;
  private Label bAddGroup;
  private Label bCopyGroup;

  public GroupComposite( Composite parent, VariableSpace variables ) {
    super( parent, SWT.NONE );
    setVariables( variables );
    init();
  }

  protected void init() {
    setDefaultRowLayout();
  }

  public void createWidgets() {

    Composite wLinkedComposite = new Composite( this, SWT.NULL );
    RowLayout linkedLayout = new RowLayout();
    linkedLayout.marginLeft = 0;
    linkedLayout.marginRight = 0;
    linkedLayout.marginBottom = 0;
    linkedLayout.marginTop = 0;
    wLinkedComposite.setLayout( linkedLayout );
    setLook( wLinkedComposite );

    wGroups = new ComboVar( getVariables(), wLinkedComposite, SWT.BORDER );
    RowData rowData = new RowData();
    rowData.width = 300;
    wGroups.setLayoutData( rowData );
    setLook( wGroups );

    Label spacer1 = new Label( wLinkedComposite, SWT.FLAT );
    spacer1.setLayoutData( new RowData( 5, 0 ) );

    bAddGroup = new Label( wLinkedComposite, SWT.FLAT );
    bAddGroup.setImage( GUIResource.getInstance().getImageAdd() );
    bAddGroup.setToolTipText( BaseMessages.getString( getLocalizationPkg(), "ModelAnnotation.AddGroup.ToolTip" ) );
    setLook( bAddGroup );

    Label spacer2 = new Label( wLinkedComposite, SWT.FLAT );
    spacer2.setLayoutData( new RowData( 5, 0 ) );

    bCopyGroup = new Label( wLinkedComposite, SWT.FLAT );
    bCopyGroup.setImage( GUIResource.getInstance().getImageCopyHop() );
    bCopyGroup.setToolTipText( BaseMessages.getString( getLocalizationPkg(), "ModelAnnotation.CopyGroup.ToolTip" ) );
    setLook( bCopyGroup );
  }

  public void setEnableAddCopyButtons( final boolean enable ) {
    bAddGroup.setEnabled( enable );
    bAddGroup.setImage( new Image( getParent().getDisplay(), GUIResource.getInstance().getImageAdd(),
        enable ? SWT.NONE : SWT.IMAGE_DISABLE ) );
    bCopyGroup.setEnabled( enable );
    bCopyGroup.setImage( new Image( getParent().getDisplay(), GUIResource.getInstance().getImageCopyHop(),
        enable ? SWT.NONE : SWT.IMAGE_DISABLE ) );
  }

  public ComboVar getGroupComboWidget() {
    return wGroups;
  }

  public void setAddGroupListener( Listener listener ) {
    bAddGroup.addListener( SWT.MouseUp, listener );
  }

  public void setCopyGroupListener( Listener listener ) {
    bCopyGroup.addListener( SWT.MouseUp, listener );
  }

  public void setAddGroupTooltip( String message ) {
    bAddGroup.setToolTipText( message );
  }

  public void setCopyGroupTooltip( String message ) {
    bCopyGroup.setToolTipText( message );
  }

  @Override
  public void setEnabled( final boolean enabled ) {
    wGroups.setEnabled( enabled );
    setEnableAddCopyButtons( enabled );
  }
}
