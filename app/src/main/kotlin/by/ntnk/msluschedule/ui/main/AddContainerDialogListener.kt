package by.ntnk.msluschedule.ui.main

import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.data.Teacher

interface AddContainerDialogListener {
    fun onPositiveButtonGroup(studyGroup: StudyGroup)
    fun onPositiveButtonTeacher(teacher: Teacher)
}
