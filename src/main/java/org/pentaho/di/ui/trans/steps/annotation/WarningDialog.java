/*!
 * PENTAHO CORPORATION PROPRIETARY AND CONFIDENTIAL
 *
 * Copyright 2002 - 2014 Pentaho Corporation (Pentaho). All rights reserved.
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
package org.pentaho.di.ui.trans.steps.annotation;


import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.Const;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.gui.GUIResource;

import java.util.Map;

public class WarningDialog extends Dialog {
  private final PropsUI props;
  private final String title;
  private final String message;
  private final Map<String, Listener> listenerMap;
  private Shell shell;

  public WarningDialog(
      final Shell parent, final String title, final String message, final Map<String, Listener> listenerMap ) {
    super( parent );
    this.title = title;
    this.message = message.trim();
    this.listenerMap = listenerMap;
    this.props = PropsUI.getInstance();

  }

  public void open() {
    Shell parent = getParent();
    Display display = parent.getDisplay();

    shell = new Shell( parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.SHEET | SWT.RESIZE );
    shell.setImage( GUIResource.getInstance().getImageSpoon() );
    shell.setLayout( new FormLayout() );
    shell.setText( title );
    props.setLook( shell );

    Label wlImage = new Label( shell, SWT.NONE );
    wlImage.setImage( GUIResource.getInstance().getImageWarning32() );
    FormData fdWarnImage = new FormData();
    fdWarnImage.left = new FormAttachment( 0, 15 );
    fdWarnImage.top = new FormAttachment( 0, 15 );
    fdWarnImage.height = 32;
    fdWarnImage.width = 32;
    wlImage.setLayoutData( fdWarnImage );
    props.setLook( wlImage );

    Label wlMessage = new Label( shell, SWT.FLAT  | SWT.WRAP );
    wlMessage.setText( message );
    FormData fdMessage = new FormData();
    fdMessage.left = new FormAttachment( wlImage, 15, SWT.RIGHT );
    fdMessage.right = new FormAttachment( 100, -15 );
    fdMessage.top = new FormAttachment( wlImage, 0, SWT.TOP );
    wlMessage.setLayoutData( fdMessage );
    props.setLook( wlMessage );


    Button spacer = new Button( shell, SWT.NONE );
    FormData fdSpacer = new FormData();
    fdSpacer.right = new FormAttachment( 100, 0 );
    fdSpacer.bottom = new FormAttachment( 100, -15 );
    fdSpacer.left = new FormAttachment( 100, -11 );
    fdSpacer.top = new FormAttachment( wlMessage, 15, SWT.BOTTOM );
    spacer.setLayoutData( fdSpacer );
    spacer.setVisible( false );
    props.setLook( spacer );

    Control attachTo = spacer;
    for ( String label : listenerMap.keySet() ) {
      Button wButton = new Button( shell, SWT.PUSH );
      wButton.setText( label );
      FormData fdButton = new FormData();
      fdButton.right = new FormAttachment( attachTo, -Const.MARGIN, SWT.LEFT );
      fdButton.bottom = new FormAttachment( attachTo, 0, SWT.BOTTOM );
      wButton.setLayoutData( fdButton );
      wButton.addListener( SWT.Selection, listenAndDispose( listenerMap.get( label ) ) );
      props.setLook( wButton );
      attachTo = wButton;
    }
    Point point = shell.computeSize( 436, SWT.DEFAULT );
    shell.setSize( point );
    shell.open();
    while ( !shell.isDisposed() ) {
      if ( !display.readAndDispatch() ) {
        display.sleep();
      }
    }
  }

  private Listener listenAndDispose( final Listener lsCancel ) {
    return new Listener() {
      @Override public void handleEvent( final Event event ) {
        lsCancel.handleEvent( event );
        shell.dispose();
      }
    };
  }
}
