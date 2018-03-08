package by.ntnk.msluschedule.db

import by.ntnk.msluschedule.di.PerApp
import javax.inject.Inject

@PerApp
class DatabaseRepository @Inject constructor(private val appDatabase: AppDatabase)
