package org.openhab.bootstrap;

import org.junit.Test;
import org.libBSTbootstrap.OverlappingNetworks;

import static org.junit.Assert.*;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class OverlappingNetworksUnitTest {
    OverlappingNetworks networks = new OverlappingNetworks();
    @Test
    public void overlap_test() throws Exception {
        //networks.addDevice();
        assertEquals(4, 2 + 2);
    }
}