package com.thelongtravail.boundless;

// 仅标记调度容器内容变化，不介入原版任务排序。
public interface TickContainerRevision {
    long travail$tickRevision();
}
