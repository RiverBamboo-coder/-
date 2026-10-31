package com.bri.ui;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A selectable query exposed by one module page.
 */
public final class QueryAction {
    public final String label;
    public final String description;
    public final String inputLabel;
    public final String inputDefault;
    public final String[] inputOptions;
    private final Function<String, TableData> loader;

    public QueryAction(String label, String description, Function<String, TableData> loader) {
        this(label, description, null, "", null, loader);
    }

    public QueryAction(String label, String description, Supplier<TableData> loader) {
        this(label, description, null, "", null, input -> loader.get());
    }

    public QueryAction(String label, String description, String inputLabel,
                       String inputDefault, String[] inputOptions,
                       Function<String, TableData> loader) {
        this.label = label;
        this.description = description;
        this.inputLabel = inputLabel;
        this.inputDefault = inputDefault == null ? "" : inputDefault;
        this.inputOptions = inputOptions;
        this.loader = loader;
    }

    public boolean needsInput() {
        return inputLabel != null && !inputLabel.isBlank();
    }

    public boolean hasOptions() {
        return inputOptions != null && inputOptions.length > 0;
    }

    public TableData load(String input) {
        return loader.apply(input == null ? "" : input.trim());
    }

    @Override
    public String toString() {
        return label;
    }
}
