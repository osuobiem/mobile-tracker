package com.osuobiem.mobiletracker.controls;

public class ControlDevice {
    private boolean lock;
    private boolean ring;
    private boolean wipe;

    public ControlDevice() {
    }

    public ControlDevice(boolean lock, boolean ring, boolean wipe) {
        this.lock = lock;
        this.ring = ring;
        this.wipe = wipe;
    }

    public boolean getLock() {
        return lock;
    }

    public boolean getRing() {
        return ring;
    }

    public boolean getWipe() {
        return wipe;
    }
}
