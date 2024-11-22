package com.searchengine.core.validation;

import java.util.*;

public class ValidationResult {
    private final List<String> errors;
    private final List<String> warnings;
    private boolean valid;

    public ValidationResult() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.valid = true;
    }

    public void addError(String error) {
        errors.add(error);
        valid = false;
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }
    public boolean isValid() { return valid; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!errors.isEmpty()) {
            sb.append("Errors:\n");
            errors.forEach(error -> sb.append("- ").append(error).append("\n"));
        }
        if (!warnings.isEmpty()) {
            sb.append("Warnings:\n");
            warnings.forEach(warning -> sb.append("- ").append(warning).append("\n"));
        }
        if (valid && warnings.isEmpty()) {
            sb.append("Validation passed successfully");
        }
        return sb.toString();
    }
}
