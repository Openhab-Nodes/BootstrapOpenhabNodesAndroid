package org.libBSTbootstrap;

import android.os.Bundle;
import android.support.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Implement this interface and register your class to {@see org.libBSTbootstrap.BootstrapService#addDataIntentExtractor}.
 * As soon as addBSData is called, your class will be asked for additional bootstrap data by the given input intent.
 */
public interface BootstrapDataIntentExtractor {
    Map<String, String> getData(@Nullable Bundle extra);
}
