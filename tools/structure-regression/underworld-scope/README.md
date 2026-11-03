# 亡灵视角隔离回归

该工具独立检查一千年后的同步攻击附魔视角、嵌套附魔/生物作用域与异常恢复，以及剑与灯当前目标的加成。成功命中检查使用真实伤害与附魔回调；女仆组使用原生饰品栏、真实EntityMaid和夹具登记的在线主人。

从项目根目录运行，需要项目构建依赖及已接受的Minecraft服务端EULA文件：

```powershell
.\gradlew.bat --offline -I tools/structure-regression/underworld-scope.init.gradle -PscopeEula=run/eula.txt runServer
.\gradlew.bat --offline -I tools/structure-regression/underworld-scope.init.gradle -PscopeMaid -PscopeApotheosis -PscopeDeps=build/apotheosis-tales/deps -PscopeEula=run/eula.txt runServer
```

`scopeMaid`加载项目固定的女仆依赖，也可使用项目已有`maidApiJar`参数指定本地包。`scopeApotheosis`从`scopeDeps`加载神化及其完整服务端依赖；仅放入本测试所需JAR。对照依赖为神化7.4.8、Placebo8.6.3、ApothicAttributes1.3.7、Patchouli85与女仆1.5.3。JAR不随工具提交。

工具只监听127.0.0.1:25751，在build下建立独立世界，结束后自动退出；结果写入underworld-scope-result.txt与underworld-scope-details.txt。异常或断言失败使Gradle任务失败。女仆未加载时跳过女仆组，普通生物成功命中仍检查。

节肢杀手效果检查不同时附加亡灵杀手，避免神化对亡灵杀手增加的缓慢效果混入判断。在线主人由服务端索引登记，不建立真实网络连接。类型视角的接口范围见开发者文档“一千年后与冥府共用入口”。


逐日外部伤害修正沿用已有逐日测试组，可叠加以下测试初始化脚本：

```powershell
.\gradlew.bat --offline -I tools/sun-blade.init.gradle -I tools/structure-regression/external-damage.init.gradle -PscopeDeps=build/apotheosis-tales/deps runServer
```

该脚本只为测试源集加载外部依赖和伤害控制器，将默认5%随机暴击设为0，保留显式0%/100%暴击用例；不进入正式模组包。逐日测试使用run/eula.txt，目录和端口遵循其原有初始化脚本。

逐日精确伤害夹具固定追加比例2.5%、弹射物增伤30%及燃烧目标增伤20%，只用于隔离测试输入，不代表生产默认值。
