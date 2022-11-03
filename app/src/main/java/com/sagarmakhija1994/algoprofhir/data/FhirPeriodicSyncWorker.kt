package com.sagarmakhija1994.algoprofhir.data

import android.content.Context
import androidx.work.WorkerParameters
import com.google.android.fhir.demo.data.DownloadWorkManagerImpl
import com.google.android.fhir.sync.AcceptLocalConflictResolver
import com.google.android.fhir.sync.DownloadWorkManager
import com.google.android.fhir.sync.FhirSyncWorker
import com.sagarmakhija1994.algoprofhir.FhirApplication

class FhirPeriodicSyncWorker(appContext: Context, workerParams: WorkerParameters) :
  FhirSyncWorker(appContext, workerParams) {

  override fun getDownloadWorkManager(): DownloadWorkManager {
    return DownloadWorkManagerImpl()
  }

  override fun getConflictResolver() = AcceptLocalConflictResolver

  override fun getFhirEngine() = FhirApplication.fhirEngine(applicationContext)
}
