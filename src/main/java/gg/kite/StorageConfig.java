package gg.kite;

public record StorageConfig(String nexoId, String menuTitle, int rows) {
    public StorageConfig {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("Rows must be between 1 and 6");
        }
    }
}