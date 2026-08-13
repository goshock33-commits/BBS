package com.bbs.bbs.web.model;

public enum ContentStatus {
    NORMAL,
    HIDDEN,
    DELETED;

    public boolean canTransitionTo(ContentStatus target) {
        if (target == null || target == this) {
            return false;
        }
        return switch (this) {
            case NORMAL -> target == HIDDEN || target == DELETED;
            case HIDDEN -> target == NORMAL || target == DELETED;
            case DELETED -> false;
        };
    }
}
