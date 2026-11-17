# 实体高度差 API

`com.thelongtravail.api.EntityHeightApi.heightDifference(Entity first, Entity second)` 返回两个实体原点的绝对 Y 坐标差，类型为 `double`，单位为格。

计算式为 `Math.abs(first.getY() - second.getY())`。直接读取传入实体的坐标，体型和碰撞箱不参与计算；体节使用自身坐标。参数须为非空实体，在所属逻辑线程调用。

比较投射物的攻击者与受击者时，先解析攻击者，再传入接口：

```java
boolean beyond = EntityHeightApi.heightDifference(attacker, target) > threshold;
```

维度筛选、阈值及效果由调用方设置。

[开发接入目录](README.md)
