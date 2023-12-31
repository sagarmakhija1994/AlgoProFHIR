package com.sagarmakhija1994.algoprofhir


import android.app.Application
import android.content.Context
import android.text.format.DateFormat
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.search
import com.sagarmakhija1994.algoprofhir.worker.FhirSyncWorker
import com.google.android.fhir.sync.PeriodicSyncConfiguration
import com.google.android.fhir.sync.RepeatInterval
import com.google.android.fhir.sync.Sync
//import com.google.android.fhir.sync.SyncJobStatus
import com.sagarmakhija1994.algoprofhir.model.PatientItem
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import okhttp3.internal.notifyAll
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.RiskAssessment
import java.time.LocalDate
import java.util.*

@OptIn(InternalCoroutinesApi::class)
class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    init {
        viewModelScope.launch {
            Sync.periodicSync<FhirSyncWorker>(
                application.applicationContext,
                PeriodicSyncConfiguration(
                    syncConstraints = Constraints.Builder().build(),
                    repeat = RepeatInterval(interval = 15, timeUnit = TimeUnit.MINUTES)
                )
            )
                //.shareIn(this, SharingStarted.Eagerly, 10)
                //.collect { /*_pollState.emit(it)*/ }
        }
    }

    lateinit var fhirEngine:FhirEngine
    var patientItemList=MutableLiveData<List<PatientItem>>()
    var lastPatientSearchQuery = ""

    fun initFHIR(fhirEngine: FhirEngine){
        this.fhirEngine = fhirEngine
    }

    fun searchPatient(nameQuery: String = ""){
        viewModelScope.launch {
            lastPatientSearchQuery = nameQuery
            patientItemList.value=getSearchResults(nameQuery)
        }
    }


    private suspend fun getSearchResults(nameQuery: String = ""): List<PatientItem> {
        val patients: MutableList<PatientItem> = mutableListOf()
        fhirEngine
            .search<Patient> {
                if (nameQuery.isNotEmpty()) {
                    filter(
                        Patient.NAME,
                        {
                            modifier = StringFilterModifier.CONTAINS
                            value = nameQuery
                        }
                    )
                }
                filterCity(this)
                sort(Patient.GIVEN, Order.ASCENDING)
                count = 100000
                from = 0
            }
            .mapIndexed { index, fhirPatient -> fhirPatient.toPatientItem(index + 1) }
            .let {
                patients.addAll(it)
            }
        val risks = getRiskAssessments()
        patients.forEach { patient ->    risks["Patient/${patient.resourceId}"]?.let {
            patient.risk = it.prediction?.first()?.qualitativeRisk?.coding?.first()?.code
        }
        }
        return patients
    }

    private fun filterCity(search: Search) {
        search.filter(Patient.ADDRESS_CITY, { value = "MUMBAI" })
    }

    fun sync(context: Context){
        viewModelScope.launch {
            val syncObj = Sync.periodicSync<FhirSyncWorker>(
                context,
                PeriodicSyncConfiguration(
                    syncConstraints = Constraints.Builder().build(),
                    repeat = RepeatInterval(interval = 15, timeUnit = TimeUnit.MINUTES)
                )
            )
            syncObj.let {
                Log.e("Sync Status:","let")
                it.runCatching {
                    Log.e("Sync Status:","run catching")
                }
                it.run {
                    Log.e("Sync Status:","run")
                }
                it.apply {
                    Log.e("Sync Status:","apply")
                }
                synchronized(it){
                    it.notifyAll()
                }
            }



            //.shareIn(this, SharingStarted.Eagerly, 10)
            //.collect { /*_pollState.emit(it)*/ }
        }

        //Sync.oneTimeSync<com.google.android.fhir.sync.FhirSyncWorker>(getApplication())
        //viewModelScope.launch {
        //Sync.basicSyncJob(getApplication())
        //val downloadManager: DownloadWorkManager
        //val resolver: ConflictResolver
        //Sync.oneTimeSync(getApplication(), fhirEngine, downloadManager, resolver)
        //Sync.oneTimeSync<FhirSyncWorker>(getApplication())
        //}
    }

    private fun Patient.toPatientItem(position: Int): PatientItem {
        // Show nothing if no values available for gender and date of birth.
        val patientId = if (hasIdElement()) idElement.idPart else ""
        val name = if (hasName()) name[0].nameAsSingleString else ""
        val gender = if (hasGenderElement()) genderElement.valueAsString else ""
        val dob =
            if (hasBirthDateElement())
                LocalDate.parse(birthDateElement.valueAsString, DateTimeFormatter.ISO_DATE)
            else null
        val phone = if (hasTelecom()) telecom[0].value else ""
        val city = if (hasAddress()) address[0].city else ""
        val country = if (hasAddress()) address[0].country else ""
        val isActive = active
        val html: String = if (hasText()) text.div.valueAsString else ""
        return PatientItem(
            id = position.toString(),
            resourceId = patientId,
            name = name,
            gender = gender ?: "",
            dob = dob,
            phone = phone ?: "",
            city = city ?: "",
            country = country ?: "",
            isActive = isActive,
            html = html
        )
    }

    private suspend fun getRiskAssessments(): Map<String, RiskAssessment?> {
        return fhirEngine.search<RiskAssessment> {}.groupBy { it.subject.reference }.mapValues { entry
            ->    entry
            .value
            .filter { it.hasOccurrence() }
            .sortedByDescending { it.occurrenceDateTimeType.value }
            .firstOrNull()
        }
    }



    fun addPatientPatient(patient: Patient){
        viewModelScope.launch {
            savePatient(patient)
        }
    }

    private fun savePatient(patient: Patient) {
        viewModelScope.launch {
            patient.id = UUID.randomUUID().toString()
            fhirEngine.create(patient)
        }
    }
}