package com.oreyield.loot;

/** Describes a completed destruction path without guessing from player names. */
public record BreakContext(boolean automated, boolean explosion) {
    public static final BreakContext AUTOMATED = new BreakContext(true, false);
    public static final BreakContext EXPLOSION = new BreakContext(false, true);
    public static final BreakContext PLAYER = new BreakContext(false, false);

    public static BreakContext player(boolean fakePlayer) {
        return fakePlayer ? AUTOMATED : PLAYER;
    }

    public static BreakContext fromPlayer(boolean playerPresent, boolean fakePlayer) {
        return playerPresent && !fakePlayer ? PLAYER : AUTOMATED;
    }
}
