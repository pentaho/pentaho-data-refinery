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


package org.pentaho.di.core.refinery.publish;

import org.pentaho.di.core.plugins.PluginInterface;
import org.pentaho.di.core.plugins.PluginRegistry;

/**
 * @author Rowell Belen
 */
public class RunInPluginClassLoader {

  public static void run( final PluginInterface plugin, final Runnable runnable ) {

    if ( runnable == null ) {
      return; // nothing to run
    }

    if ( plugin == null ) {
      runnable.run();
      return;
    }

    ClassLoader origClassLoader = null;
    try {
      origClassLoader = Thread.currentThread().getContextClassLoader();
      ClassLoader pluginClassLoader = PluginRegistry.getInstance().getClassLoader( plugin );

      // Use the plugin class loader
      Thread.currentThread().setContextClassLoader( pluginClassLoader );

      if ( runnable != null ) {
        runnable.run();
      }
    } catch ( Exception e ) {
      throw new RuntimeException( e );
    } finally {
      if ( origClassLoader != null ) {
        Thread.currentThread().setContextClassLoader( origClassLoader );
      }
    }
  }

}
