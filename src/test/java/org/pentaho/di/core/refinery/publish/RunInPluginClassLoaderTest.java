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
