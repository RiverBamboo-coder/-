package com.bri.ui;

import java.awt.Color;
import java.util.List;

/**
 * Configuration for one business page. The page itself stays generic.
 */
public final class ModuleSpec {
    public final String key;
    public final String title;
    public final String subtitle;
    public final String badge;
    public final String icon;
    public final Color accent;
    public final String defenseSummary;
    public final List<String> defensePoints;
    public final List<QueryAction> actions;

    public ModuleSpec(String key, String title, String subtitle, String badge, String icon,
                      Color accent, String defenseSummary, List<String> defensePoints,
                      List<QueryAction> actions) {
        this.key = key;
        this.title = title;
        this.subtitle = subtitle;
        this.badge = badge;
        this.icon = icon;
        this.accent = accent;
        this.defenseSummary = defenseSummary;
        this.defensePoints = defensePoints;
        this.actions = actions;
    }
}
