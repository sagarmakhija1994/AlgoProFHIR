package com.sagarmakhija1994.algoprofhir

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import androidx.work.Constraints
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.LocalChange
import com.google.android.fhir.db.impl.dao.LocalChangeToken

import com.google.android.fhir.search.Order
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.StringFilterModifier
import com.sagarmakhija1994.algoprofhir.model.PatientItem
import org.hl7.fhir.r4.model.Patient
import com.google.android.fhir.search.search
import com.google.android.fhir.sync.*
import com.sagarmakhija1994.algoprofhir.data.FhirPeriodicSyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.RiskAssessment
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

import kotlinx.coroutines.flow.shareIn
import java.util.concurrent.TimeUnit

class MainActivityViewModel2(application: Application): AndroidViewModel(application) {

    lateinit var fhirEngine:FhirEngine
    var patientItemList=MutableLiveData<List<PatientItem>>()
    var lastPatientSearchQuery = ""

    fun initFHIR(fhirEngine:FhirEngine){
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
//                filterCity(this)
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

    fun sync(){
        Sync.oneTimeSync<FhirSyncWorker>(getApplication())
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



    fun addPatientPatient(patient:Patient){
        viewModelScope.launch {
            savePatient(patient)
        }
    }

    private fun savePatient(patient:Patient) {
        viewModelScope.launch {
            patient.id = UUID.randomUUID().toString()
            fhirEngine.create(patient)
        }
    }


}