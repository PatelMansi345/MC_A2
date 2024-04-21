package com.example.a2

import android.os.Build
import okhttp3.Request
import org.json.JSONObject
import java.util.Calendar
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.room.Room
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate

// all the required import framework and libraries

// MainActivity is a class in which program getting started
class MainActivity : AppCompatActivity() {

    //we created lateinit variable for our button, text and view

    // this code take data from XML file and create button
    private lateinit var Calculate_temperature_button : Button

    // this code take data from XML file and create Edittext for date
    private lateinit var Insert_data_data: EditText

    // this code take data from XML file and create Edittext for date for month
    private lateinit var Insert_month_data: EditText

    // this code take data from XML file and create Edittext for date for year in YYYY
    private lateinit var Insert_year_data: EditText

    // // this code take data from XML file and create TextView for showing result
    private lateinit var Show_Temperature: TextView

    //this is created for database beacuse by API i fetched data of temperature
    private lateinit var Api_Data_store: AppDatabase


    // this function will start the app and taking some inputa
    // from the XML file and given to local variable

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        // super is used for to used global variable
        super.onCreate(savedInstanceState)

        // this method set the screen data into main activity data
        setContentView(R.layout.activity_main)

        // now we take inputs from the data and screen and calculate the temperature
        Calculate_temperature_button = findViewById(R.id.searchButton)

        // this is for showing the all results
        Show_Temperature = findViewById(R.id.resultTextView)

        // this all variable taking inputs from the edittext and store for
        // further process

        // first one is for day
        Insert_data_data = findViewById(R.id.dateEditText)

        // second one is for month
        Insert_month_data = findViewById(R.id.monthEditText)

        // third  one is for year
        Insert_year_data = findViewById(R.id.yearEditText)


        // room is a aPI which we used to fetch the data from API
        // for that we used below function
        Api_Data_store = Room.databaseBuilder(this, AppDatabase::class.java, "TemperatureData").build()

        // when we click the button at that time some events we
        // need to handle and that is selected when we click button
        Calculate_temperature_button.setOnClickListener {

            // all three variable take data from the inputs and convert int into string

            // first one is for date
            val tarikh = Insert_data_data.text.toString()

            // second one is for month
            val mahino = Insert_month_data.text.toString()

            // third one is for year
            val varsh = Insert_year_data.text.toString()


            // this condition will check all the entries which we collected from
            // all three edittext that must not be null

            // if that is null then raise null exception
            if (listOf(tarikh, mahino, varsh).all { !it.isNullOrBlank() }) {
                collect_data(mahino, tarikh, varsh)
            }
        }
    }

    // this function will collect month , date, year into string format
    @RequiresApi(Build.VERSION_CODES.O)
    private fun collect_data(month: String, date: String, year: String) {

        // now we collect all the data and match with calender for our API
        val this_varsh = Calendar.getInstance().get(Calendar.YEAR)
        val inserted_varsh = year.toInt()

        // take two local variable for stored minimum and maximum temperature
        var Maximum_mean = 0.0
        var Minimum_mean = 0.0

        // now as mentioned in assignment if our date is past then we need to just
        // fetch the temp from API and show it
        // but if date is in future then show temperature in term of average of
        // past 10 years temperature

        // this if for future data
        if (inserted_varsh > this_varsh) {

            // this will find the year in YYYY formate
            val fixed_type_varsh = String.format("%04d", inserted_varsh)

            val last_varsh = 10

            // belowe funciton store the year in local variable from calender
            val aa_varsh = LocalDate.now().year

            // it find the data of last 10 years
            val last_das_varsh = (aa_varsh - last_varsh + 1)..aa_varsh

            // now after collecting all the data we need to call our API
            GlobalScope.launch(Dispatchers.IO) {
               // for loop run for last 10 year data and take temperature for average
                for (last_year in last_das_varsh) {

                    // convert year in pattern of YYYY
                    val pattern_of_year = "%04d".format(last_year)

                    // this function will fetch the data and store in variable
                    val get_whether_API_data = Getting_data_of_city_from_API(month, date, pattern_of_year)

                    //if gethered data is not null then we process the result otherwise showing exception
                    if (get_whether_API_data != null) {
                        val (maximum_tapman, minimum_tapman) = get_whether_API_data

                        // store the summation of 10 year max temperature
                        Maximum_mean = Maximum_mean.plus(maximum_tapman)


                        // store the summation of 10 year min temperature
                        Minimum_mean = Minimum_mean.plus(minimum_tapman)

                    }
                }

                // this is for finding the average of last 10 year minimum temperature
                Minimum_mean = Minimum_mean/10
                // this is for finding the average of last 10 year maximum temperature
                Maximum_mean = Maximum_mean/10

                // this will store the data into particular format of API
                val temperatureData = TemperatureData(date = "$fixed_type_varsh-$month-$date", maxTemperature = Maximum_mean, minTemperature = Minimum_mean)

                // it will insert the data into storage
                Api_Data_store.TemperatureDataDao().insertTemperatureData(temperatureData)

                // this will show the result on the screen when we clicked find temprature
                withContext(Dispatchers.Main) {
                    Show_Temperature.text = "Maximum_Temperature: ${"%.2f".format(Maximum_mean)} °C\nMinimum_Temperature: ${"%.2f".format(Minimum_mean)} °C"
                }
            }
        }
        // this part is for past years
        else {
            GlobalScope.launch(Dispatchers.IO) {

                // gethered data from the API by dates
                val get_data_temp_from_API = Api_Data_store.TemperatureDataDao().getTemperatureDataByDate("$year-$month-$date")


                // if data strored in database then directly collect
                if (get_data_temp_from_API != null) {
                    // fetched data directly from database
                    withContext(Dispatchers.Main) {
                        Show_Temperature.text = "Maximum_Temperature: ${get_data_temp_from_API.maxTemperature} °C\nMinimum_Temperature: ${get_data_temp_from_API.minTemperature} °C"
                    }
                }

                // otherwise fetched by API and stored into the database
                else {
                    // fetched data from databases if found if not then store into database
                    val city_data = Getting_data_of_city_from_API(month, date, year)

                    // here city is hardcored so we don't need to think about it
                    if (city_data != null) {
                        val (more_temp, less_temp) = city_data
                        withContext(Dispatchers.Main) {
                            Show_Temperature.text = "Maximum_Temperature: $more_temp °C\nMinimum_Temperature: $less_temp °C"
                        }
                    }
                }
            }
        }
    }

    // this function which is main importance for fetching data from the API
    private fun Getting_data_of_city_from_API(mahino: String, divas: String, varsh: String): Pair<Double, Double>? {

        // it will initialise the all the 2 formate of date
        // this all make the formate of date , month and year
        val MM_mahino = "%02d".format(mahino.toInt())
        val DD_divas = "%02d".format(divas.toInt())
        val YYYY_varsh = "%04d".format(varsh.toInt())

        // main URL ot he API so from this we can call the endpoint and retrieve our data
        // MY key is not working so used my Friend'd API key
        val s1="https://archive-api.open-meteo.com/v1/archive?latitude=22&longitude=79"
        val s2="&start_date=${YYYY_varsh}-${MM_mahino}-${DD_divas}&end_date=${YYYY_varsh}-${MM_mahino}-${DD_divas}"
        val s3="&hourly=temperature_2m&daily=temperature_2m_max,temperature_2m_min"

        // it will collect all the links and make API
        val link_data = s1+s2+s3

        // by that URL it will fetched the data and stored into user this will
        // build the structure for tha retrieve data
        val req_by_user = Request.Builder().url(link_data).build()

        // finally after fetching data return result of the data
        // if there is any error occur then we can handle it by using
        // try and catch exception
        return try {

            // tihis will make the HTTP connection between client and server
            val App_user = OkHttpClient()

            // it will call the API
            val result_from_API = App_user.newCall(req_by_user).execute()

            // if we are getting any error in http connection then we can thrown
            // this error
            if (!result_from_API.isSuccessful) {
                throw RuntimeException("Encountered HTTP issue with code: ${result_from_API.code}")
            }

            // result stored in this data
            val result_data = result_from_API.body?.string()

            // finally we got the data into json file so we can store collect it
            // store into some particular format
            if (result_data != null) {

                // it will stored whole json object
                val result_in_json = JSONObject(result_data)

                // it will collect daily data of the temperature
                val all_daily_temp = result_in_json.getJSONObject("daily")

                // like above it store for minimum
                val vector_minimum_temperature = all_daily_temp.getJSONArray("temperature_2m_min")

                // in this vector we can store all the collected data of min and max
                val vector_maximim_temperature = all_daily_temp.getJSONArray("temperature_2m_max")

                // took two local variable for min and max temperature
                var city_maximum_temp: Double
                var city_minimum_temp: Double

                // now if we have some data in vector so we can fetch it otherwise it store 0
                city_maximum_temp = vector_maximim_temperature.takeIf { it.length() > 0 }?.getDouble(0) ?: 0.0
                city_minimum_temp = vector_minimum_temperature.takeIf { it.length() > 0 }?.getDouble(0) ?: 0.0

                // this below code can store the date , min temperature and max temperature
                // this will help us to store data in particular format
                val data_of_daily_temp = TemperatureData(date = "$YYYY_varsh-$MM_mahino-$DD_divas", maxTemperature = city_maximum_temp, minTemperature = city_minimum_temp)

                // finally we need to store our fetched data into some data structure
                CoroutineScope(Dispatchers.IO).launch { Api_Data_store.TemperatureDataDao().insertTemperatureData(data_of_daily_temp) }

                // so by pair we can store it and then return to above function so that
                // that will display the result
                city_maximum_temp to city_minimum_temp
            }
            // if we are getting some error or data not fetched then it show null in result
            else {
                return null
            }
        }
        // if we are getting some error of exception handling then we can manage it by
        // catching exceptions
        catch (e: Exception)
        {
            null
        }
    }
}
