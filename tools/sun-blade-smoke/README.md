# 逐日与籠釣瓶隔离回归

在项目根目录使用现有 Java、Gradle/Forge、Curios 依赖和 run/eula.txt 运行：

    gradlew.bat -I tools/sun-blade.init.gradle runServer --offline --console=plain

测试位于独立 sourceSet，不进入正式 JAR；服务端绑定 localhost:25743，数据在 build/sun-blade-server，结果为 sun-blade-result.txt。夹具会创建目标、改变测试玩家装备和位置，只用于独立测试实例。源码集依赖主工程编译类路径；首次准备依赖需联网，缓存齐全后可用 --offline。

女仆回归使用 tools/structure-regression/round-05.init.gradle，运行时加入项目既有 maidApiDependency，结果在 build/structure-round-05-<structurePhase>。整包定向模式仅用于另行准备的测试副本，不读取正式存档。
