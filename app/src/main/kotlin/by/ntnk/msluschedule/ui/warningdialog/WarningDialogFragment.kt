package by.ntnk.msluschedule.ui.warningdialog

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.widget.Button
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.utils.SchedulerProvider
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import java.util.Locale
import java.util.concurrent.TimeUnit

class WarningDialogFragment : DialogFragment() {
    private lateinit var disposable: Disposable
    private lateinit var listener: OnPositiveButtonClickListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            listener = parentFragment as OnPositiveButtonClickListener
        } catch (e: ClassCastException) {
            throw ClassCastException(parentFragment.toString() + " must implement OnPositiveButtonClickListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = initMaterialDialog()
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            initPositiveButtonCountdown(positiveButton)
        }
        return dialog
    }

    private fun initMaterialDialog(): AlertDialog {
        return AlertDialog.Builder(activity!!, R.style.MsluTheme_Dialog_Alert)
                .setTitle(R.string.dialog_delete_container_title)
                .setMessage(R.string.dialog_delete_container_message)
                .setPositiveButton(R.string.button_delete) { _, _ ->
                    listener.onPositiveButtonClick()
                }
                .setNegativeButton(R.string.button_cancel) { _, _ -> dismiss() }
                .create()
    }

    private fun initPositiveButtonCountdown(button: Button) {
        button.isEnabled = false
        val timeout = 5L
        disposable = Observable
                .intervalRange(1, timeout, 0, 1, TimeUnit.SECONDS)
                .map { timePassed -> timeout - timePassed }
                .map { timeLeft ->
                    String.format(
                            Locale.getDefault(),
                            "%s (%d)",
                            resources.getString(R.string.button_delete),
                            timeLeft)
                }
                .observeOn(SchedulerProvider.ui())
                .subscribeBy(
                        onNext = { button.text = it },
                        onComplete = {
                            button.setTextColor(ContextCompat.getColor(context!!, R.color.warning))
                            button.isEnabled = true
                            button.text = resources.getString(R.string.button_delete)
                        }
                )
    }

    override fun dismiss() {
        disposable.dispose()
        super.dismiss()
    }

    interface OnPositiveButtonClickListener {
        fun onPositiveButtonClick()
    }
}
