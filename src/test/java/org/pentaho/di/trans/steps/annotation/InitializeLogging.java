/*! ******************************************************************************
 *
 * Pentaho Community Edition Project: data-refinery-pdi-plugin
 *
 * Copyright (C) 2002-2024 by Hitachi Vantara : http://www.pentaho.com
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

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.pentaho.di.core.logging.KettleLogStore;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.logging.LogChannelInterfaceFactory;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@RunWith( MockitoJUnitRunner.class )
public class InitializeLogging {

    @Mock
    protected LogChannelInterface mockLog;
    @Mock
    private LogChannelInterfaceFactory mockLogFactory;
    private LogChannelInterfaceFactory existingChannel;

    @Before
    public void setUpClass() throws Exception {
        existingChannel = KettleLogStore.getLogChannelInterfaceFactory();
        KettleLogStore.setLogChannelInterfaceFactory( mockLogFactory );
        when( mockLogFactory.create( any(), any() ) ).thenReturn( mockLog );
    }

    @After
    public void tearDownClass() {
        KettleLogStore.setLogChannelInterfaceFactory( existingChannel );
    }
}
