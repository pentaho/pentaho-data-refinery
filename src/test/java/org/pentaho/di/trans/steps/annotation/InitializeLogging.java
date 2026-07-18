/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 - 2026 by Pentaho Canada Inc. : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2030-06-15
 ******************************************************************************/


package org.pentaho.di.trans.steps.annotation;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
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
