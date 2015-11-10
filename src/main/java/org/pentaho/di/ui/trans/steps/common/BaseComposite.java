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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.gui.SpoonFactory;
import org.pentaho.di.core.gui.SpoonInterface;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.ui.core.PropsUI;

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
    SpoonInterface spoon = getSpoon();
    spoon.messageBox( message, title, false, Const.ERROR );
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
