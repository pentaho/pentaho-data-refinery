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
