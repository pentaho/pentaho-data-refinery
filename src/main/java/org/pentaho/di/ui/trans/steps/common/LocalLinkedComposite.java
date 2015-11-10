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
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.ui.core.widget.ComboVar;

public class LocalLinkedComposite extends BaseComposite {
  private Button bLocal;
  private Button bShared;
  private GroupComposite groupComposite;

  public LocalLinkedComposite( Composite composite, int i ) {
    super( composite, i );
    init();
  }

  protected void init() {
    setDefaultRowLayout();
    ( (RowLayout) getLayout() ).spacing = 10;
  }

  public void createWidgets() {

    bLocal = new Button( this, SWT.RADIO );
    bLocal.setSelection( true );

    bLocal.setText( BaseMessages.getString( getLocalizationPkg(), "ModelAnnotation.Local.Label" ) );
    setLook( bLocal );

    Composite wLinkedComposite = new Composite( this, SWT.NULL );
    RowLayout linkedLayout = new RowLayout( SWT.HORIZONTAL );
    linkedLayout.marginLeft = 0;
    linkedLayout.marginRight = 0;
    linkedLayout.marginBottom = 0;
    linkedLayout.marginTop = 0;
    wLinkedComposite.setLayout( linkedLayout );
    setLook( wLinkedComposite );
    bShared = new Button( wLinkedComposite, SWT.RADIO );
    bShared.setText( BaseMessages.getString( getLocalizationPkg(), "ModelAnnotation.Shared.Label" ) );
    setLook( bShared );

    groupComposite = new GroupComposite( wLinkedComposite, getVariables() );
    groupComposite.setLocalizationPkg( getLocalizationPkg() );
    groupComposite.setLog( getLog() );
    groupComposite.createWidgets();
  }

  public Button getbLocal() {
    return bLocal;
  }

  public ComboVar getwGroups() {
    return groupComposite.getGroupComboWidget();
  }

  public Button getbShared() {
    return bShared;
  }

  public void setAddGroupListener( Listener listener ) {
    groupComposite.setAddGroupListener( listener );
  }

  public void setCopyGroupListener( Listener listener ) {
    groupComposite.setCopyGroupListener( listener );
  }

  @Override
  public void setEnabled( final boolean enabled ) {
    groupComposite.setEnabled( enabled );
  }

  public void setEnableAddCopyButtons( final boolean enable ) {
    groupComposite.setEnableAddCopyButtons( enable );
  }
}
