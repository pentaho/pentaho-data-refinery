/*! ******************************************************************************
 *
 * Pentaho Community Edition Project: data-refinery-pdi-plugin
 *
 * Copyright (C) 2002-2015 by Pentaho : http://www.pentaho.com
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
