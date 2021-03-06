/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *  @author Victor Belov
 *  @since 1.4.0
 *
 */

package org.openhab.detectServers;

public interface DiscoverListener {
    void onOpenHABDiscovered(OpenHABServer server);

    void onOpenHABDiscoveryStarted();

    void onOpenHABDiscoveryFinished(FinishedState state);
    enum FinishedState {
        Success,
        Stopped
    }
}
