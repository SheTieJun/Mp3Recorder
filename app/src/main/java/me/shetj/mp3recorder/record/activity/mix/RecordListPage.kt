/*
 * MIT License
 *
 * Copyright (c) 2019 SheTieJun
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package me.shetj.mp3recorder.record.activity.mix

import android.app.Activity
import android.transition.Scene
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import me.shetj.base.tools.app.ArmsUtils
import me.shetj.mp3recorder.R
import me.shetj.mp3recorder.record.adapter.RecordAdapter
import me.shetj.mp3recorder.record.bean.Record
import me.shetj.mp3recorder.record.bean.RecordDbUtils
import me.shetj.mp3recorder.record.utils.EventCallback
import me.shetj.mp3recorder.record.view.RecordBottomSheetDialog
import java.util.*

/**
 */
class RecordListPage(
    private val context: Activity,
    mRoot: ViewGroup,
    private var callback: EventCallback
) {

    private var root: RelativeLayout? = null
    val scene: Scene
    private var mRecyclerView: RecyclerView? = null
    private var mIvRecordState: ImageView? = null
    private lateinit var recordAdapter: RecordAdapter
    private var mRlRecordView: FrameLayout? = null


    /**
     * 得到当前选中的record
     */
    val curRecord: Record?
        get() = if (recordAdapter.curPosition != -1) {
            recordAdapter.getItem(recordAdapter.curPosition)
        } else null


    init {
        root = LayoutInflater.from(context).inflate(R.layout.page_my_record, null) as RelativeLayout
        scene = Scene(mRoot, root as View)
        initView(root)
        initData()
    }

    private fun initData() {
        RecordDbUtils.getInstance().allRecord
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                recordAdapter.setNewInstance(it.toMutableList())
                checkShow(it)
            }
            .subscribe()
    }

    /**
     * 判断是不是存在录音
     */
    private fun checkShow(allRecord: List<Record>) {
        root?.let {
            TransitionManager.beginDelayedTransition(root)
        }
        if (allRecord.isNotEmpty()) {
            mRlRecordView!!.visibility = View.VISIBLE
        } else {
            mRlRecordView!!.visibility = View.GONE
        }
    }

    private fun initView(view: View?) {
        //绑定view
        mRecyclerView = view!!.findViewById(R.id.recycler_view)
        mIvRecordState = view.findViewById(R.id.iv_record_state)
        mRlRecordView = view.findViewById(R.id.rl_record_view)
        //设置界面
        recordAdapter = RecordAdapter(ArrayList())
        mRecyclerView?.adapter = recordAdapter
        //设置点击
        recordAdapter.setOnItemClickListener { _, _, position ->
            recordAdapter.setPlayPosition(
                position
            )
        }
        recordAdapter.setOnItemChildClickListener { adapter, view1, position ->
            when (view1.id) {
                R.id.tv_more -> {
                    val dialog = showBottomDialog(position)
                    dialog.showBottomSheet()
                }
            }
        }
        recordAdapter.addChildClickViewIds(R.id.tv_more)
        recordAdapter.setOnItemLongClickListener { adapter, _, position ->
            val dialog = showBottomDialog(position)
            dialog.showBottomSheet()
            true
        }

        //设置空界面
        val emptyView = LayoutInflater.from(context).inflate(R.layout.empty_view, null)
        recordAdapter.setEmptyView(emptyView)
        //空界面点击开启
        emptyView.findViewById<View>(R.id.cd_start_record).setOnClickListener {
            callback.onEvent(0)
        }


        //添加一个head
        val headView = View(context)
        headView.layoutParams =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ArmsUtils.dp2px(35f))
        recordAdapter.addHeaderView(headView)
        //去录音界面
        mIvRecordState!!.setOnClickListener {
            recordAdapter.setPlayPosition(-1)
            callback.onEvent(0)
        }

    }

    private fun showBottomDialog(position: Int): RecordBottomSheetDialog {
        recordAdapter.onPause()
        return RecordBottomSheetDialog(
            context, position, recordAdapter.getItem(position),
            callback
        )
    }


    fun onDestroy() {
        recordAdapter.onDestroy()
        root = null
    }

    fun onPause() {
        recordAdapter.onPause()
    }
}
