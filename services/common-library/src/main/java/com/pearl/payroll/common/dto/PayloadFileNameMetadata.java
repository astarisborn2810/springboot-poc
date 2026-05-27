package com.pearl.payroll.common.dto;

public record PayloadFileNameMetadata(
        String batchId,
        String vendorName,
        String plan) {

    public static PayloadFileNameMetadata from(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName is required.");
        }

        String normalized = lastPathSegment(fileName.trim());
        String baseName = stripExtension(normalized);
        String[] parts = baseName.split("_", -1);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            throw new IllegalArgumentException("fileName must use batchId_vendorName_plan format.");
        }

        return new PayloadFileNameMetadata(parts[0], parts[1], parts[2]);
    }

    public static boolean isValid(String fileName) {
        try {
            from(fileName);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static String lastPathSegment(String value) {
        int slashIndex = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        return slashIndex >= 0 ? value.substring(slashIndex + 1) : value;
    }

    private static String stripExtension(String value) {
        int extensionIndex = value.lastIndexOf('.');
        return extensionIndex > 0 ? value.substring(0, extensionIndex) : value;
    }
}
