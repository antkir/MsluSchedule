package by.ntnk.msluschedule.ui.warningdialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.utils.SchedulerProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import timber.log.Timber
import java.util.concurrent.TimeUnit

class WarningDialogFragment : DialogFragment() {
    private lateinit var disposable: Disposable
    private lateinit var listener: DialogListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = parentFragment as DialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$parentFragment must implement DialogListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = initMaterialDialog()
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            initPositiveButtonCountdown(positiveButton)
        }
        return dialog
    }

    private fun initMaterialDialog(): AlertDialog {
        return MaterialAlertDialogBuilder(requireActivity(), R.style.MsluTheme_Dialog_Alert)
                .setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.rounded_corners_rect))
                .setTitle(R.string.dialog_delete_container_title)
                .setMessage(R.string.dialog_delete_container_message)
                .setPositiveButton(R.string.button_delete) { _, _ ->
                    listener.onDeleteScheduleContainerClick()
                }
                .setNegativeButton(R.string.button_cancel) { _, _ -> dismiss() }
                .create()
    }

    private fun initPositiveButtonCountdown(button: Button) {
        context ?: return
        button.isEnabled = false
        val buttonDeleteString = resources.getString(R.string.button_delete)
        val buttonDeleteColor = ContextCompat.getColor(requireContext(), R.color.warning)
        val timeout = 5L
        disposable = Observable
                .intervalRange(1, timeout, 0, 1, TimeUnit.SECONDS)
                .map { timePassed -> timeout - timePassed }
                .map { timeLeft -> "$buttonDeleteString ($timeLeft)" }
                .observeOn(SchedulerProvider.ui())
                .subscribeBy(
                        onNext = { button.text = it },
                        onComplete = {
                            button.setTextColor(buttonDeleteColor)
                            button.isEnabled = true
                            button.text = buttonDeleteString
                        },
                        onError = { throwable -> Timber.e(throwable) }
                )
    }

    override fun dismiss() {
        super.dismiss()
        disposable.dispose()
    }

    interface DialogListener {
        fun onDeleteScheduleContainerClick()
    }
}
