package by.ntnk.msluschedule.ui.addteacher

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.databinding.FragmentAddTeacherBinding
import by.ntnk.msluschedule.mvp.views.MvpDialogFragment
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.ui.adapters.ScheduleFilterAdapter
import by.ntnk.msluschedule.utils.EMPTY_STRING
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.Lazy
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class AddTeacherFragment : MvpDialogFragment<AddTeacherPresenter, AddTeacherView>(), AddTeacherView {

    private var fragmentBinding: FragmentAddTeacherBinding? = null
    private val binding get() = fragmentBinding!!
    private lateinit var listener: DialogListener

    override val view: AddTeacherView
        get() = this

    @Inject
    lateinit var injectedPresenter: Lazy<AddTeacherPresenter>

    override fun onCreatePresenter(): AddTeacherPresenter = injectedPresenter.get()

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
        try {
            listener = context as DialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement DialogListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        fragmentBinding = FragmentAddTeacherBinding.inflate(LayoutInflater.from(context))
        setupViews()
        val dialog = createDialog()
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.setOnShowListener { dialogInterface ->
            dialogInterface as AlertDialog
            val isTeacherValid = presenter.isValidTeacher(binding.autocompletetextTeacher.text.toString())
            dialogInterface.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = isTeacherValid
        }
        return dialog
    }

    private fun setupViews() {
        binding.autocompletetextTeacher.setEnabledFocusable(false)
        binding.autocompletetextTeacher.setOnItemClickListener { _, _, _, id ->
            presenter.setTeacherId(id.toInt())
        }
        binding.autocompletetextTeacher.doOnTextChanged { text, _, _, _ ->
            var isEnabled = presenter.isValidTeacher(text.toString())
            if (presenter.isTeacherStored(text.toString())) {
                binding.textinputlayoutTeacher.error = resources.getString(R.string.teacher_already_added)
                isEnabled = false
            } else {
                binding.textinputlayoutTeacher.error = EMPTY_STRING
            }
            (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = isEnabled
        }
    }

    private fun createDialog(): AlertDialog {
        return MaterialAlertDialogBuilder(requireActivity(), R.style.MsluTheme_Dialog_Alert)
            .setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.rounded_corners_rect))
            .setTitle((R.string.add_teacher_title))
            .setView(binding.root)
            .setPositiveButton(R.string.button_add) { _, _ ->
                listener.onNewTeacher(presenter.createSelectedTeacherObject())
            }
            .setNegativeButton(R.string.button_cancel) { _, _ ->
                dismiss()
            }
            .create()
    }

    override fun onStart() {
        super.onStart()
        if (presenter.isTeachersNotEmpty()) {
            presenter.populateTeachersAdapter()
        } else {
            binding.progressindicatorTeacher.visibility = View.VISIBLE
            presenter.getTeachersScheduleFilter()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentBinding = null
    }

    override fun populateTeachersView(data: ScheduleFilter) {
        binding.progressindicatorTeacher.visibility = View.GONE
        val adapter = createAdapter(data)
        with (binding.autocompletetextTeacher) {
            setEnabledFocusable(true)
            setAdapter(adapter)
            requestFocus()
        }
    }

    private fun createAdapter(data: ScheduleFilter): ScheduleFilterAdapter {
        return ScheduleFilterAdapter(
            requireActivity(),
            android.R.layout.simple_spinner_dropdown_item,
            data,
            isFilteringEnabled = true
        )
    }

    override fun showError(t: Throwable) {
        dismiss()
        listener.onError(t)
    }

    override fun dismiss() {
        super.dismiss()
        presenter.clearDisposables()
    }

    interface DialogListener {
        fun onNewTeacher(teacher: Teacher)
        fun onError(t: Throwable)
    }
}
