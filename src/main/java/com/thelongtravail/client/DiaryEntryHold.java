package com.thelongtravail.client;

public final class DiaryEntryHold {
    public enum Entry {
        DIARY,
        CATALOG
    }

    private final DiaryHoldProgress progress = new DiaryHoldProgress();
    private Entry owner;

    public Entry advance(double elapsed, boolean diaryDown, boolean catalogDown, boolean valid) {
        if (owner == null && valid && elapsed > 0) {
            if (diaryDown) owner = Entry.DIARY;
            else if (catalogDown) owner = Entry.CATALOG;
        }
        if (progress.advance(elapsed, valid && owner != null && (diaryDown || catalogDown))) return owner;
        if (progress.fraction() == 0) owner = null;
        return null;
    }

    public double fraction() {
        return progress.fraction();
    }

    public void reset() {
        progress.reset();
        owner = null;
    }
}
