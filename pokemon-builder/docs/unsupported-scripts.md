# unsupported-scripts

> 由 `builder.bat audit` 自动生成(阶段 6),请勿手工编辑。

- 工程路径: `E:\仓库\范例\929`
- 生成时间: 2026-10-09 13:22:26 UTC
- Builder 版本: 0.1.0

本表列出所有**无法被自动识别**的内容;硬性规则是不允许静默忽略,每一条都必须带位置。

## 未解析标识符


无。所有事件脚本标识符都能在 `Scripts.rxdata`、Ruby / RMXP 运行时或事件全局中找到。

## 无法解压的 Scripts.rxdata 段


无。

## 无法解析的事件数据


无。`MapXXX.rxdata` / `CommonEvents.rxdata` 全部可解析,355 / 655 全部可合并。

## 无法读取的 .rxdata


无。

## 解释器实例状态(移植时需实现的字段)


| 实例变量 | 出现 block 数 |
| --- | --- |
| `@ch_cmd` | 41 |
| `@event_id` | 8 |
| `@ch_ret` | 6 |

这些是事件解释器(Game_Interpreter)的实例变量,不是 API 调用;Java 运行时需要对应字段。
