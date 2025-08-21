package de.joancode.anticheat.util;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

// Deprecated: validation now occurs after join via payload. No-op.
public class BannedModsUpdater implements PreLaunchEntrypoint { @Override public void onPreLaunch() { } }
