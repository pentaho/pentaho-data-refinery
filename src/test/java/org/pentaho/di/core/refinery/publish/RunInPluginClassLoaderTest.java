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

import org.junit.Before;
import org.junit.Test;
import org.pentaho.di.core.plugins.PluginInterface;

import static org.mockito.Mockito.mock;

/**
 * @author Rowell Belen
 */
public class RunInPluginClassLoaderTest {

  private PluginInterface pluginInterface;

  @Before
  public void setup() {
    pluginInterface = mock( PluginInterface.class );
  }

  @Test
  public void testNull() {

    Runnable runnable = new Runnable() {
      @Override public void run() {
        // do something;
      }
    };

    new RunInPluginClassLoader();
    RunInPluginClassLoader.run( null, null );
    RunInPluginClassLoader.run( null, runnable );
  }

  @Test
  public void testMock() {

    Runnable runnable = new Runnable() {
      @Override public void run() {
        // do something;
      }
    };

    RunInPluginClassLoader.run( pluginInterface, runnable );
  }

  @Test( expected = RuntimeException.class )
  public void testException() {

    Runnable runnable = new Runnable() {
      @Override public void run() {
        throw new RuntimeException();
      }
    };

    RunInPluginClassLoader.run( pluginInterface, runnable );
  }
}
