/*! ******************************************************************************
 *
 * Pentaho Community Edition Project: data-refinery-pdi-plugin
 *
 * Copyright (C) 2002-2018 by Hitachi Vantara : http://www.pentaho.com
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
package org.pentaho.di.trans.steps.annotation;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.pentaho.di.core.logging.KettleLogStore;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.logging.LogChannelInterfaceFactory;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InitializeLogging {
    protected static LogChannelInterface mockLog;
    private static LogChannelInterfaceFactory existingChannel;

    @BeforeClass
    public static void setUpClass() throws Exception {
        LogChannelInterfaceFactory interfaceFactory = mock( LogChannelInterfaceFactory.class );
        existingChannel = KettleLogStore.getLogChannelInterfaceFactory();
        KettleLogStore.setLogChannelInterfaceFactory( interfaceFactory );
        mockLog = mock( LogChannelInterface.class );
        when( interfaceFactory.create( any(), any() ) ).thenReturn( mockLog );
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        KettleLogStore.setLogChannelInterfaceFactory( existingChannel );
    }
}
