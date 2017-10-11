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
