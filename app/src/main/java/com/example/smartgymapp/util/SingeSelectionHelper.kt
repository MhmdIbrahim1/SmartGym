package com.example.smartgymapp.util

import android.app.Activity
import android.app.Dialog
import android.view.LayoutInflater
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import com.example.smartgymapp.R
import com.example.smartgymapp.databinding.BottomSelectionDialogBinding
import com.example.smartgymapp.util.CommonActivity.dismissSafe

object SingeSelectionHelper {

    fun Activity?.showDialog(
        binding: BottomSelectionDialogBinding,
        dialog: Dialog,
        items: List<String>,
        selectedIndex: List<Int>,
        name: String,
        showApply: Boolean,
        isMultiSelect: Boolean,
        callback: (List<Int>) -> Unit,
        dismissCallback: () -> Unit,
        itemLayout: Int = R.layout.sort_bottom_single_choice
    ) {
        if (this == null) return

        val realShowApply = showApply || isMultiSelect
        val listView = binding.listview1
        val textView = binding.text1
        val applyButton = binding.applyBtt
        val cancelButton = binding.cancelBtt
        val applyHolder =
            binding.applyBttHolder

        applyHolder.isVisible = realShowApply
        if (!realShowApply) {
            val params = listView.layoutParams as LinearLayout.LayoutParams
            params.setMargins(listView.marginLeft, listView.marginTop, listView.marginRight, 0)
            listView.layoutParams = params
        }

        textView.text = name
        textView.isGone = name.isBlank()

        val arrayAdapter = ArrayAdapter<String>(this, itemLayout)
        arrayAdapter.addAll(items)

        listView.adapter = arrayAdapter
        if (isMultiSelect) {
            listView.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE
        } else {
            listView.choiceMode = AbsListView.CHOICE_MODE_SINGLE
        }

        for (select in selectedIndex) {
            listView.setItemChecked(select, true)
        }

        selectedIndex.minOrNull()?.let {
            listView.setSelection(it)
        }

        //  var lastSelectedIndex = if(selectedIndex.isNotEmpty()) selectedIndex.first() else -1

        dialog.setOnDismissListener {
            dismissCallback.invoke()
        }

        listView.setOnItemClickListener { _, _, which, _ ->
            //  lastSelectedIndex = which
            if (realShowApply) {
                if (!isMultiSelect) {
                    listView.setItemChecked(which, true)
                }
            } else {
                callback.invoke(listOf(which))
                dialog.dismissSafe(this)
            }
        }
        if (realShowApply) {
            applyButton.setOnClickListener {
                val list = ArrayList<Int>()
                for (index in 0 until listView.count) {
                    if (listView.checkedItemPositions[index])
                        list.add(index)
                }
                callback.invoke(list)
                dialog.dismissSafe(this)
            }
            cancelButton.setOnClickListener {
                dialog.dismissSafe(this)
            }
        }
    }

    fun Activity?.showDialog(
        items: List<String>,
        selectedIndex: Int,
        name: String,
        showApply: Boolean,
        dismissCallback: () -> Unit,
        callback: (Int) -> Unit,
    ) {
        if (this == null) return

        val binding: BottomSelectionDialogBinding = BottomSelectionDialogBinding.inflate(
            LayoutInflater.from(this)
        )
        val builder =
            AlertDialog.Builder(this, R.style.AlertDialogCustom)
                .setView(binding.root)

        val dialog = builder.create()
        dialog.show()


        showDialog(
            binding,
            dialog,
            items,
            listOf(selectedIndex),
            name,
            showApply,
            false,
            { if (it.isNotEmpty()) callback.invoke(it.first()) },
            dismissCallback
        )
    }

}