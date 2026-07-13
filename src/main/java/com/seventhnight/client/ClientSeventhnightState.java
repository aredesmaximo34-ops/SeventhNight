package com.seventhnight.client;

public class ClientSeventhnightState {
    private static boolean active = false;
    private static float intensity = 0.0f;
    private static final float FADE_SPEED = 0.02f;

    public static boolean isActive() { return active; }
    public static void setActive(boolean newActive) { active = newActive; }
    public static float getIntensity() { return intensity; }

    public static void tick() {
        if (active && intensity < 1.0f) {
            intensity = Math.min(1.0f, intensity + FADE_SPEED);
        } else if (!active && intensity > 0.0f) {
            intensity = Math.max(0.0f, intensity - FADE_SPEED);
        }
    }
}