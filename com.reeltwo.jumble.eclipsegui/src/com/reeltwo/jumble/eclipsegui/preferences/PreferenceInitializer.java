package com.reeltwo.jumble.eclipsegui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.reeltwo.jumble.eclipsegui.Activator;

/**
 * Class used to initialize default preference values.
 * 
 * @author Tin Pavlinic
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {
  @Override
  public void initializeDefaultPreferences() {
    IPreferenceStore store = Activator.getDefault().getPreferenceStore();
    store.setDefault(PreferenceConstants.P_ARGS, "");
    store.setDefault(PreferenceConstants.P_VERBOSE , false);
    store.setDefault(PreferenceConstants.P_RETURNS, true);
    store.setDefault(PreferenceConstants.P_INCREMENTS, true);
    store.setDefault(PreferenceConstants.P_INLINE_CONSTANTS, true);
    store.setDefault(PreferenceConstants.P_CONSTANT_POOL_CONSTANTS, true);
    store.setDefault(PreferenceConstants.P_STRING_CONSTANTS, true);
    store.setDefault(PreferenceConstants.P_SWITCH, true);
    store.setDefault(PreferenceConstants.P_STORES, false);
  }

}
