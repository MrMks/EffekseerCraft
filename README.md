# EffekseerCraft 使用手册

EffekseerCraft 由 客户端 和 服务端 两部分组成，两侧分别维护一份特效注册表。

## 服务端

### 指令
使用特效的唯一方式是通过一组指令:  
* /effek play \<effect> \<emitter> \<entity>
* /effek play \<effect> \<emitter> \<world/dim> \<x> \<y> \<z> \[yaw] \[pitch]
* /effek stop \<effect> \<emitter> \<entity>
* /effek stop \<effect> \<emitter> \<world/dim> \<x> \<y> \<z>
* /effek clear \<player>
* /effek reload
* /effek version

前四个指令用于在目标实体上播放或停止特效，或在目标坐标上播放或停止特效。  
其中:
* effect 指向服务端特效注册表中的注册名，是客户端与服务端间识别特效动画的两个识别码之一
* emitter 是客户端和服务端间识别特效动画的另一个识别码

例如，在 play 中使用 example abc 这一组参数播放了一组特效动画，  
若想主动结束这一段动画，则在 stop 中亦使用同样一组参数。  
特殊的，stop 指令中若使用 "*" 作为 emitter，则会停止 effect 所指的全部特效动画。

第五个指令则是停止指定玩家客户端上播放的全部特效动画。

最后两个指令分别是 重载服务端注册表 和 显示当前服务端实现版本。

### 注册表
服务端注册表写在一个名为 effects.json 的文件中，在不同服务端上，这个文件放置在不同的位置下。
* Spigot及其下游: ./plugins/EffekseerCraft/effects.json
* Forge独立服务端: ./config/efscraft/effects.json
* Forge内置服务端: ./saves/\<save_name>/efscraft/effects.json

下面是一个例子: 
```json
{
  "example": {
    "effect": "Laser03",
    "lifespan": 200
  }
}
```

如此，便创建了一个名为 example 的注册项，这个例子表示:  
使用客户端注册名为 Laser03 的特效，播放200帧后停止。

服务端注册项的可配置项见下表，标注*的为必须项。  

| key               |      type |  default | desc                            |
|:------------------|----------:|---------:|:--------------------------------|
| extendsFrom       |    String |  \<null> | 继承父级注册项，包括父级中的必须项               |
| effect*           |    String |        \ | 要使用的客户端特效注册名                    |
| lifespan*         |       int |        \ | 这一特效要播放多少帧，effekseer 的 fps 为 60 |
| skipFrame         |       int |        0 | 自特效的第多少帧开始播放                    |
| scale             | float\[3] | \[1,1,1] | 特效缩放                            |
| rotateLocal       | float\[2] |   \[0,0] | 特效旋转，使用 yaw 与 pitch             |
| rotateModel       | float\[2] |   \[0,0] | 特效旋转，使用 yaw 与 pitch             |
| translateLocal    | float\[3] | \[0,0,0] | 特效平移                            |
| translateModel    | float\[3] | \[0,0,0] | 特效平移                            |
| overwriteConflict |   boolean |    false | 识别码冲突时后者是否覆盖前者                  |
| followX           |   boolean |    false | 是否跟随 x 轴平移                      |
| followY           |   boolean |    false | 是否跟随 y 轴平移                      |
| followZ           |   boolean |    false | 是否跟随 z 轴平移                      |
| followYaw         |   boolean |    false | 是否跟随 yaw 转向                     |
| followPitch       |   boolean |    false | 是否跟随 pitch 转向                   |
| useHead           |   boolean |    false | 跟随旋转时是否使用头部的方向                  |
| useRender         |   boolean |    false | 跟随旋转时是否使用模型渲染方向                 |
| inheritYaw        |   boolean |     true | 特效生成时是否基于目标方向旋转                 |
| inheritPitch      |   boolean |     true | 特效生成时是否基于目标方向旋转                 |

## 客户端

### 注册表
客户端的注册表从资源包中获取。

满足 assets/efscraft/effects/\<name>/\<name>.efkefc 这一路径特效文件的将被自动注册在客户端注册表中。

这一特效所需要的资源文件将在 assets/efscraft/effects/\<name>/ 中按相对路径加载。

\<name>即为这一特效的客户端注册名。