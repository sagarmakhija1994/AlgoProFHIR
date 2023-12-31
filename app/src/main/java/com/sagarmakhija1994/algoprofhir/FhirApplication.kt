/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sagarmakhija1994.algoprofhir

import android.app.Application
import android.content.Context
import com.google.android.fhir.DatabaseErrorStrategy.RECREATE_AT_OPEN
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineConfiguration
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.ServerConfiguration
import com.google.android.fhir.datacapture.DataCaptureConfig
import com.google.android.fhir.sync.Sync
import com.sagarmakhija1994.algoprofhir.utils.DemoDataStore
import com.sagarmakhija1994.algoprofhir.worker.FhirSyncWorker


class FhirApplication : Application(), DataCaptureConfig.Provider {
  // Only initiate the FhirEngine when used for the first time, not when the app is created.
  private val fhirEngine: FhirEngine by lazy { constructFhirEngine() }

  private var dataCaptureConfig: DataCaptureConfig? = null

  private val dataStore by lazy { DemoDataStore(this) }

  override fun onCreate() {
    super.onCreate()
    /*if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }*/
    FhirEngineProvider.init(
      FhirEngineConfiguration(
        enableEncryptionIfSupported = true,
        RECREATE_AT_OPEN,
        ServerConfiguration("https://hapi.fhir.org/baseR4/")
      )
    )
    Sync.oneTimeSync<FhirSyncWorker>(this)
  }

  private fun constructFhirEngine(): FhirEngine {
    return FhirEngineProvider.getInstance(this)
  }

  companion object {
    fun fhirEngine(context: Context) = (context.applicationContext as FhirApplication).fhirEngine

    fun dataStore(context: Context) = (context.applicationContext as FhirApplication).dataStore
  }
  override fun getDataCaptureConfig(): DataCaptureConfig = dataCaptureConfig ?: DataCaptureConfig()
}
