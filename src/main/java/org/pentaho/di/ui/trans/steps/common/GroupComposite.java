/*!
 * PENTAHO CORPORATION PROPRIETARY AND CONFIDENTIAL
 *
 * Copyright 2002 - 2015 Pentaho Corporation (Pentaho). All rights reserved.
 *
 * NOTICE: All information including source code contained herein is, and
 * remains the sole property of Pentaho and its licensors. The intellectual
 * and technical concepts contained herein are proprietary and confidential
 * to, and are trade secrets of Pentaho and may be covered by U.S. and foreign
 * patents, or patents in process, and are protected by trade secret and
 * copyright laws. The receipt or possession of this source code and/or related
 * information does not convey or imply any rights to reproduce, disclose or
 * distribute its contents, or to manufacture, use, or sell anything that it
 * may describe, in whole or in part. Any reproduction, modification, distribution,
 * or public display of this information without the express written authorization
 * from Pentaho is strictly prohibited and in violation of applicable laws and
 * international treaties. Access to the source code contained herein is strictly
 * prohibited to anyone except those individuals and entities who have executed
 * confidentiality and non-disclosure agreements or other agreements with Pentaho,
 * explicitly covering such access.
 */

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
