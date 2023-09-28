package com.sagarmakhija1994.algoprofhir

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import com.google.android.fhir.FhirEngine
import org.hl7.fhir.r4.model.*


class MainActivity : AppCompatActivity() {
    lateinit var fhirEngine:FhirEngine
    private val viewModel:MainActivityViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
        viewModel.patientItemList.observe(this){ it ->
            var data = ""
            it.forEach {
                data += it.name+"\n"
                Log.e("Data", it.name)
            }
            runOnUiThread {
                try{
                    val textview = findViewById<TextView>(R.id.txtLogs)
                    textview.text = data
                }catch (e:Exception){}
            }
        }
    }

    private fun init(){
        fhirEngine = FhirApplication.fhirEngine(this)
        viewModel.initFHIR(fhirEngine)
        viewModel.searchPatient()
        //addPatient()
    }

    fun btnGetPatientList(view: View){
        viewModel.searchPatient()
    }
    fun btnAddPatient(view: View){
        addPatient()
    }
    fun btnSync(view: View){
        viewModel.sync(this)
    }

    var c = 2
    private fun addPatient(){
        c += 1
        val patient = Patient()
        val humanName = HumanName()
        humanName.text = "Sagar_$c"
        humanName.family = "Sagar$c"

        val contactPointPhone = ContactPoint()
        contactPointPhone.system = ContactPoint.ContactPointSystem.PHONE
        contactPointPhone.value = "+919322935000"

        val contactPointEmail = ContactPoint()
        contactPointEmail.system = ContactPoint.ContactPointSystem.EMAIL
        contactPointEmail.value = "sagarmakhija1994@gmail.com"

        val address = Address()
        address.text = "502-B, Satya Jeevan Co-Op Housing"
        address.city = "MUMBAI"
        address.district = "Maharashtra"
        address.country = "India"
        address.postalCode = "421004"


        patient.name.add(humanName)
        patient.telecom.add(contactPointPhone)
        patient.telecom.add(contactPointEmail)
        patient.address.add(address)
        patient.gender = Enumerations.AdministrativeGender.MALE

        viewModel.addPatientPatient(patient)

    }
}