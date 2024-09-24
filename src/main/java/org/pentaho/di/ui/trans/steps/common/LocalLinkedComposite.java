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
