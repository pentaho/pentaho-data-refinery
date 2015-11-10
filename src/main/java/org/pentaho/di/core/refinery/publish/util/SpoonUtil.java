package org.pentaho.di.core.refinery.publish.util;

import org.pentaho.di.ui.spoon.Spoon;
import org.springframework.stereotype.Component;

/**
 * @author Rowell Belen
 */
@Component
public class SpoonUtil {

  public void runAsyncInSpoonThread( final Runnable runnable ) {
    if ( runnable == null ) {
      return;
    }
    Spoon.getInstance().getShell().getDisplay().asyncExec( runnable );
  }

}
