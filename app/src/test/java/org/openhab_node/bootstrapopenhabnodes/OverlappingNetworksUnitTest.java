package org.openhab_node.bootstrapopenhabnodes;

import org.junit.Test;
import org.openhab_node.bootstrapopenhabnodes.adapter.OverlappingNetworksAdapter;
import org.openhab_node.bootstrapopenhabnodes.bootstrap.OverlappingNetworks;

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