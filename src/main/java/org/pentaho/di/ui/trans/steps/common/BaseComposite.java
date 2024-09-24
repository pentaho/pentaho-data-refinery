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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.gui.SpoonFactory;
import org.pentaho.di.core.gui.SpoonInterface;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.trans.steps.annotation.WarningDialog;

public abstract class BaseComposite extends Composite {

  private static final Class<?> PKG = BaseComposite.class; // for i18n purposes, needed by Translator2!!

  private LogChannel log;
  private Class<?> localizationPkg = PKG;
  private VariableSpace variables;
  private PropsUI props;

  protected final int margin = Const.MARGIN;
  protected final int LEFT_MARGIN_OFFSET = 10;
  protected final int RIGHT_MARGIN_OFFSET = -10;
  protected final int SHELL_MIN_WIDTH = 900;
  protected final int SHELL_MIN_HEIGHT = 570;

  public BaseComposite( Composite composite, int i ) {
    super( composite, i );
    this.props = PropsUI.getInstance();
    setLook( this );
  }

  protected void logError( String message, Exception e ) {
    if ( this.log != null ) {
      this.log.logError( message, e );
    }
  }

  public LogChannel getLog() {
    return log;
  }

  public void setLog( LogChannel log ) {
    this.log = log;
  }

  public Class<?> getLocalizationPkg() {
    return localizationPkg;
  }

  public void setLocalizationPkg( Class<?> localizationPkg ) {
    this.localizationPkg = localizationPkg;
  }

  public VariableSpace getVariables() {
    return variables;
  }

  public void setVariables( VariableSpace variables ) {
    this.variables = variables;
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
    new WarningDialog( getShell(), title, message, listenerMap ).open();
  }

  protected SpoonInterface getSpoon() {
    return SpoonFactory.getInstance();
  }

  protected void setDefaultRowLayout() {
    RowLayout layout = new RowLayout( SWT.VERTICAL );
    layout.marginLeft = 0;
    layout.marginRight = 0;
    layout.marginTop = 0;
    layout.marginBottom = 0;
    this.setLayout( layout );
  }

  protected void setLook( Control control ) {
    this.props.setLook( control );
  }
}
