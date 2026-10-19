package com.thelongtravail.valley;

public interface ExecutionBody {
    ExecutionLoot.State travail$executionLoot();

    void travail$executionLoot(ExecutionLoot.State value);

    ExecutionLease travail$executionLease();

    void travail$executionLease(ExecutionLease value);

    boolean travail$rawCleanupRequired();

    void travail$rawCleanupRequired(boolean value);

    boolean travail$cleanupPending();

    void travail$cleanupPending(boolean value);

    boolean travail$executionTerminated();

    void travail$executionTerminated(boolean value);

    boolean travail$executionLocked();

    void travail$executionLocked(boolean value);
}
