package me.shetj.player

/**
 * **@author：** shetj<br></br>
 * **@createTime：** 2019/6/20<br></br>
 * **@email：** 375105540@qq.com<br></br>
 * **@describe** 获取权限 <br></br>
 */
interface PermissionListener {
    /**
     * 缺少权限时回调该接口
     */
    fun needPermission()
}
