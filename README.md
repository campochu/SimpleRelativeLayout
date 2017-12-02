## 简介
功能上：
`AsyncRelativeLayout`覆盖`RelativeLayout`除alignBaseLine外所有布局功能，并增加了水平或垂直的中线对齐，支持同步和异步两种模式进行measure；
开启异步模式后，AsyncRelativeLayout所有子节点均在异步线程中measure
性能上：
同步模式较RelativeLayout快20% ～ 50%
异步模式几乎不消耗主线程measure时间
## 使用
### 属性说明
| Layout 属性 |  |
| :--- | :--- |
| layout_above | 同RelativeLayout |
| layout_below | 同RelativeLayout |
| layout_alignTop | 同RelativeLayout |
| layout_alignBottom | 同RelativeLayout |
| layout_toLeftOf | 同RelativeLayout |
| layout_toRightOf | 同RelativeLayout |
| layout_alignLeft | 同RelativeLayout |
| layout_alignRight | 同RelativeLayout |
| layout_alignParentTop | 同RelativeLayout |
| layout_alignParentBottom | 同RelativeLayout |
| layout_alignParentLeft | 同RelativeLayout |
| layout_alignParentRight | 同RelativeLayout |
| layout_centerVertical | 同RelativeLayout |
| layout_centerHorizontal | 同RelativeLayout |
| layout_centerInParent | 同RelativeLayout |
| layout_alignVerticalCenter="@id/xxx" | 与xxx纵轴中线对齐，xxx可以是parentId |
| layout_alignHorizontalCenter="@id/xxx" | 与xxx横轴中线对齐，xxx可以是parentId |
| 自身属性 |  |
| gravity | 同RelativeLayout |
| async="true/false" | 是否开启异步模式 |

### 使用
1.在xml跟节点下引入布局属性
```xml
xmlns:app="http://schemas.android.com/apk/res/hit.campochu.ui"
```


2.以上所列属性使用 **app **作为namespace，如：app:async="true"
3.其它均同RelativeLayout

注：ARelativeLayout作为xml根节点时建议使用async=true，作为子节点时使用async=false；