package com.huawei.healthtracker

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.huawei.hmf.tasks.Task
import com.huawei.hms.hihealth.DataController
import com.huawei.hms.hihealth.HiHealthOptions
import com.huawei.hms.hihealth.HuaweiHiHealth
import com.huawei.hms.hihealth.data.DataCollector
import com.huawei.hms.hihealth.data.DataType
import com.huawei.hms.hihealth.data.Field
import com.huawei.hms.hihealth.data.SampleSet
import com.huawei.hms.hihealth.options.DeleteOptions
import com.huawei.hms.hihealth.options.ReadOptions
import com.huawei.hms.hihealth.options.UpdateOptions
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CalorieTrackerActivity : AppCompatActivity() {

    val TAG = "CalorieTrackerActivity"

    val SIX_DAY_MILLIS = 518_400_000
    val ONE_DAY_MILLIS = 86_400_000

    private lateinit var etConsumedCal: EditText
    private lateinit var etWeight: EditText

    private lateinit var tvChartHead: TextView
    private lateinit var barChart: BarChart

    private lateinit var btnAddConsumedCal: Button
    private lateinit var btnAddWeight: Button
    private lateinit var btnShowWeight: Button
    private lateinit var btnShowConsCal: Button

    private lateinit var dataController: DataController


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calorie_tracker)

        initViews()

        initDataController()

        readConsumedData()

    }

    private fun initDataController() {
        val hiHealthOptions = HiHealthOptions.builder()
            .addDataType(DataType.DT_CONTINUOUS_CALORIES_CONSUMED, HiHealthOptions.ACCESS_READ)
            .addDataType(DataType.DT_CONTINUOUS_CALORIES_CONSUMED, HiHealthOptions.ACCESS_WRITE)
            .addDataType(DataType.DT_INSTANTANEOUS_BODY_WEIGHT, HiHealthOptions.ACCESS_READ)
            .addDataType(DataType.DT_INSTANTANEOUS_BODY_WEIGHT, HiHealthOptions.ACCESS_WRITE)
            .build()

        val signInHuaweiId = HuaweiIdAuthManager.getExtendedAuthResult(hiHealthOptions)
        dataController = HuaweiHiHealth.getDataController(this, signInHuaweiId)
    }

    private fun initViews() {
        btnAddConsumedCal = findViewById(R.id.btnAddConsumedCal)
        btnAddWeight = findViewById(R.id.btnAddWeight)
        btnShowWeight = findViewById(R.id.btnShowWeight)
        btnShowConsCal = findViewById(R.id.btnShowConsCal)

        etWeight = findViewById(R.id.etBurntCal)
        etConsumedCal = findViewById(R.id.etConsumedCal)

        tvChartHead = findViewById(R.id.tvChartHead)
        barChart = findViewById(R.id.barchartWeeklyCal)

        btnAddConsumedCal.setOnClickListener {
            if (etConsumedCal.text.isNotEmpty()) {
                val calorie = etConsumedCal.text.toString()
                addConsumedCalorie(calorie.toFloat())
                etConsumedCal.text.clear()
            } else {
                Toast.makeText(
                    this, "Calorie must be greater than 0",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        btnAddWeight.setOnClickListener {
            if (etWeight.text.isNotEmpty()) {
                val weight = etWeight.text.toString()
                addWeightData(weight.toFloat())
                etWeight.text.clear()
            } else {
                Toast.makeText(
                    this, "Weight must be greater than 0",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        btnShowWeight.setOnClickListener {
            tvChartHead.text = "Weekly Weight History"
            checkHealthAppAuthorization()
            readWeightData()

        }

        btnShowConsCal.setOnClickListener {
            tvChartHead.text = "Weekly Consumed Calories"
            checkHealthAppAuthorization()
            readConsumedData()
        }
    }

    private fun getTodayStartInMillis(): Long {
        val todayStart = GregorianCalendar()
        todayStart.set(Calendar.HOUR_OF_DAY, 0)
        todayStart.set(Calendar.MINUTE, 0)
        todayStart.set(Calendar.SECOND, 0)
        todayStart.set(Calendar.MILLISECOND, 0)
        return todayStart.timeInMillis
    }

    private fun addConsumedCalorie(calorie: Float) {
        val dataCollector: DataCollector = DataCollector.Builder().setPackageName(this)
            .setDataType(DataType.DT_CONTINUOUS_CALORIES_CONSUMED)
            .setDataGenerateType(DataCollector.DATA_TYPE_RAW)
            .build()

        val sampleSet = SampleSet.create(dataCollector)

        val currentTime = System.currentTimeMillis()

        val samplePoint = sampleSet.createSamplePoint()
            .setTimeInterval(currentTime - 1, currentTime, TimeUnit.MILLISECONDS)
        samplePoint.getFieldValue(Field.FIELD_CALORIES).setFloatValue(calorie)
        sampleSet.addSample(samplePoint)

        val insertTask: Task<Void> = dataController.insert(sampleSet)

        insertTask.addOnSuccessListener {
            Toast.makeText(this, "Calorie added successfully", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, e.message.toString(), Toast.LENGTH_LONG).show()
        }
    }

    private fun addWeightData(weight: Float) {
        val dataCollector: DataCollector = DataCollector.Builder().setPackageName(this)
            .setDataType(DataType.DT_INSTANTANEOUS_BODY_WEIGHT)
            .setDataGenerateType(DataCollector.DATA_TYPE_RAW)
            .build()

        val sampleSet = SampleSet.create(dataCollector)

        val currentTime = System.currentTimeMillis()

        val samplePoint = sampleSet.createSamplePoint()
            .setTimeInterval(currentTime, currentTime, TimeUnit.MILLISECONDS)
        samplePoint.getFieldValue(Field.FIELD_BODY_WEIGHT).setFloatValue(weight)
        sampleSet.addSample(samplePoint)

        val insertTask: Task<Void> = dataController.insert(sampleSet)

        insertTask.addOnSuccessListener {
            Toast.makeText(this, "Weight added successfully", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, e.message.toString(), Toast.LENGTH_SHORT).show()
        }
    }


    private fun readConsumedData() {

        val caloriesMap = mutableMapOf<Long, Float>()

        val endDate = System.currentTimeMillis()
        val startDate = endDate - SIX_DAY_MILLIS

        val readOptions = ReadOptions.Builder()
            .read(DataType.DT_CONTINUOUS_CALORIES_CONSUMED)
            .setTimeRange(startDate, endDate, TimeUnit.MILLISECONDS).build()

        val readReplyTask = dataController.read(readOptions)

        readReplyTask.addOnSuccessListener { readReply ->
            for (sampleSet in readReply.sampleSets) {
                if (sampleSet.isEmpty.not()) {
                    sampleSet.samplePoints.forEach {
                        caloriesMap.put(
                            it.getStartTime(TimeUnit.MILLISECONDS),
                            it.getFieldValue(Field.FIELD_CALORIES).asFloatValue()
                        )
                    }
                } else {
                    Toast.makeText(this, "No data to show", Toast.LENGTH_SHORT).show()
                }
            }

            showCaloriesWeekly(caloriesMap)

        }.addOnFailureListener {
            Log.i(TAG, it.message.toString())
        }
    }


    private fun readWeightData() {

        val weightsMap = mutableMapOf<Long, Float>()

        val endDate = System.currentTimeMillis()
        val startDate = endDate - SIX_DAY_MILLIS

        val readOptions = ReadOptions.Builder().read(DataType.DT_INSTANTANEOUS_BODY_WEIGHT)
            .setTimeRange(startDate, endDate, TimeUnit.MILLISECONDS).build()

        val readReplyTask = dataController.read(readOptions)

        readReplyTask.addOnSuccessListener { readReply ->

            for (sampleSet in readReply.sampleSets) {
                if (sampleSet.isEmpty.not()) {
                    sampleSet.samplePoints.forEach {
                        weightsMap.put(
                            it.getStartTime(TimeUnit.MILLISECONDS),
                            it.getFieldValue(Field.FIELD_BODY_WEIGHT).asFloatValue()
                        )
                    }
                } else {
                    Toast.makeText(this, "No data to show", Toast.LENGTH_SHORT).show()
                }
            }

            showWeightsWeekly(weightsMap)

        }.addOnFailureListener {
            Log.i(TAG, it.message.toString())
        }
    }


    private fun showCaloriesWeekly(dataList: Map<Long, Float>) {

        val arrangedValuesAsMap = mutableMapOf<Long, Float>()
        val currentTimeMillis = System.currentTimeMillis()

        var firstDayCal = 0f
        var secondDayCal = 0f
        var thirdDayCal = 0f
        var fourthDayCal = 0f
        var fifthDayCal = 0f
        var sixthDayCal = 0f
        var seventhDayCal = 0f

        dataList.forEach { (time, value) ->
            when (time) {
                in getTodayStartInMillis()..currentTimeMillis -> {
                    seventhDayCal += value
                }
                in getTodayStartInMillis() - ONE_DAY_MILLIS until getTodayStartInMillis() -> {
                    sixthDayCal += value
                }
                in getTodayStartInMillis() - ONE_DAY_MILLIS * 2 until
                        getTodayStartInMillis() - ONE_DAY_MILLIS -> {
                    fifthDayCal += value
                }
                in getTodayStartInMillis() - ONE_DAY_MILLIS * 3 until
                        getTodayStartInMillis() - ONE_DAY_MILLIS * 2 -> {
                    fourthDayCal += value
                }
                in getTodayStartInMillis() - ONE_DAY_MILLIS * 4 until
                        getTodayStartInMillis() - ONE_DAY_MILLIS * 3 -> {
                    thirdDayCal += value
                }
                in getTodayStartInMillis() - ONE_DAY_MILLIS * 5 until
                        getTodayStartInMillis() - ONE_DAY_MILLIS * 4 -> {
                    secondDayCal += value
                }
                in getTodayStartInMillis() - ONE_DAY_MILLIS * 6 until
                        getTodayStartInMillis() - ONE_DAY_MILLIS * 5 -> {
                    firstDayCal += value
                }
            }
        }

        arrangedValuesAsMap.put(getTodayStartInMillis() - ONE_DAY_MILLIS * 6, firstDayCal)
        arrangedValuesAsMap.put(getTodayStartInMillis() - ONE_DAY_MILLIS * 5, secondDayCal)
        arrangedValuesAsMap.put(getTodayStartInMillis() - ONE_DAY_MILLIS * 4, thirdDayCal)
        arrangedValuesAsMap.put(getTodayStartInMillis() - ONE_DAY_MILLIS * 3, fourthDayCal)
        arrangedValuesAsMap.put(getTodayStartInMillis() - ONE_DAY_MILLIS * 2, fifthDayCal)
        arrangedValuesAsMap.put(getTodayStartInMillis() - ONE_DAY_MILLIS, sixthDayCal)
        arrangedValuesAsMap.put(getTodayStartInMillis(), seventhDayCal)

        initBarChart(arrangedValuesAsMap)

    }

    private fun showWeightsWeekly(dataList: Map<Long, Float>) {

        val arrangedValuesAsMap = mutableMapOf<Long, Float>()
        val currentTimeMillis = System.currentTimeMillis()

        var firstDayWeight = 0f
        var secondDayWeight = 0f
        var thirdDayWeight = 0f
        var fourthDayWeight = 0f
        var fifthDayWeight = 0f
        var sixthDayWeight = 0f
        var seventhDayWeight = 0f

        dataList.forEach { (time, value) ->
            when (time) {
                in getTodayStartInMillis()..currentTimeMillis -> {
                    seventhDayWeight = value
                }
                in getTodayStartInMillis() - ONE_DAY_MILLIS until getTodayStartInMillis() -> {
                    sixthDayWeight = value
                }
                in getTodayStartInMillis() - ONE_DAY_MILLIS * 2 until
                        getTodayStartInMillis() - ONE_DAY_MILLIS -> {
                    fifthDayWeight = value
                }
                in getTodayStartInMillis() - ONE_DAY_MILLIS * 3 until
                        getTodayStartInMillis() - ONE_DAY_MILLIS * 2 -> {
                    fourthDayWeight = value
                }
                in getTodayStartInMillis() - ONE_DAY_MILLIS * 4 until
                        getTodayStartInMillis() - ONE_DAY_MILLIS * 3 -> {
                    thirdDayWeight = value
                }
                in getTodayStartInMillis() - ONE_DAY_MILLIS * 5 until
                        getTodayStartInMillis() - ONE_DAY_MILLIS * 4 -> {
                    secondDayWeight = value
                }
                in getTodayStartInMillis() - ONE_DAY_MILLIS * 6 until
                        getTodayStartInMillis() - ONE_DAY_MILLIS * 5 -> {
                    firstDayWeight = value
                }
            }
        }

        arrangedValuesAsMap.put(getTodayStartInMillis() - ONE_DAY_MILLIS * 6, firstDayWeight)
        arrangedValuesAsMap.put(getTodayStartInMillis() - ONE_DAY_MILLIS * 5, secondDayWeight)
        arrangedValuesAsMap.put(getTodayStartInMillis() - ONE_DAY_MILLIS * 4, thirdDayWeight)
        arrangedValuesAsMap.put(getTodayStartInMillis() - ONE_DAY_MILLIS * 3, fourthDayWeight)
        arrangedValuesAsMap.put(getTodayStartInMillis() - ONE_DAY_MILLIS * 2, fifthDayWeight)
        arrangedValuesAsMap.put(getTodayStartInMillis() - ONE_DAY_MILLIS, sixthDayWeight)
        arrangedValuesAsMap.put(getTodayStartInMillis(), seventhDayWeight)

        initBarChart(arrangedValuesAsMap)

    }


    private fun initBarChart(values: MutableMap<Long, Float>) {

        var barIndex = 0f
        val labelWeekdayNames = arrayListOf<String>()
        val entries = ArrayList<BarEntry>()

        val simpleDateFormat = SimpleDateFormat("E", Locale.US)

        values.forEach { (time, value) ->
            labelWeekdayNames.add(simpleDateFormat.format(time))
            entries.add(BarEntry(barIndex, value))
            barIndex++
        }

        barChart.apply {
            setDrawBarShadow(false)
            setDrawValueAboveBar(false)
            description.isEnabled = false
            setDrawGridBackground(false)
            isDoubleTapToZoomEnabled = false
        }

        barChart.xAxis.apply {
            setDrawGridLines(false)
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawLabels(true)
            setDrawAxisLine(false)
            valueFormatter = IndexAxisValueFormatter(labelWeekdayNames)
            axisMaximum = labelWeekdayNames.size.toFloat()
        }

        barChart.axisRight.isEnabled = false

        val legend = barChart.legend
        legend.isEnabled = false

        val dataSets = arrayListOf<IBarDataSet>()
        val barDataSet = BarDataSet(entries, " ")
        barDataSet.color = Color.parseColor("#76C33A")
        barDataSet.setDrawValues(false)
        dataSets.add(barDataSet)

        val data = BarData(dataSets)
        barChart.data = data
        barChart.invalidate()
        barChart.animateY(1500)

    }

    fun checkHealthAppAuthorization() {
        // Check the user privacy authorization to Health Kit. If the authorization has not been granted, the user will be redirected to the authorization screen where they can authorize the HUAWEI Health app to open data to Health Kit.
        // Display the returned result on the phone screen.
        val hiHealthOptions = HiHealthOptions.builder().build()
        val signInHuaweiId = HuaweiIdAuthManager.getExtendedAuthResult(hiHealthOptions)
        this.let {
            HuaweiHiHealth.getSettingController(it, signInHuaweiId)!!
                .checkHealthAppAuthorization()
                .addOnFailureListener { e ->
                    Log.i(
                        TAG,
                        "checkHealthAppAuthorization failure : ${e.message}"
                    )
                }
                .addOnCompleteListener { task ->
                    val res = if (task.isSuccessful) "success" else "failed"
                    Log.i("TAG", "checkHealthAppAuthorization is $res")
                }

        }


    }

    private fun updateWeight(weight: Float, startTimeInMillis: Long, endTimeInMillis: Long) {

        val dataCollector: DataCollector = DataCollector.Builder().setPackageName(this)
            .setDataType(DataType.DT_INSTANTANEOUS_BODY_WEIGHT)
            .setDataGenerateType(DataCollector.DATA_TYPE_RAW)
            .build()

        val sampleSet = SampleSet.create(dataCollector)

        val samplePoint = sampleSet.createSamplePoint()
            .setTimeInterval(startTimeInMillis, endTimeInMillis, TimeUnit.MILLISECONDS)
        samplePoint.getFieldValue(Field.FIELD_BODY_WEIGHT).setFloatValue(weight)

        sampleSet.addSample(samplePoint)

        val updateOptions = UpdateOptions.Builder()
            .setTimeInterval(startTimeInMillis, endTimeInMillis, TimeUnit.MILLISECONDS)
            .setSampleSet(sampleSet)
            .build()

        dataController.update(updateOptions)
            .addOnSuccessListener {
                Toast.makeText(this, "Weight has been updated successfully", Toast.LENGTH_SHORT)
                    .show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message.toString(), Toast.LENGTH_SHORT).show()
            }

    }


    private fun deleteWeight(startTimeInMillis: Long, endTimeInMillis: Long) {

        val dataCollector: DataCollector = DataCollector.Builder().setPackageName(this)
            .setDataType(DataType.DT_INSTANTANEOUS_BODY_WEIGHT)
            .setDataGenerateType(DataCollector.DATA_TYPE_RAW)
            .build()

        val deleteOptions = DeleteOptions.Builder()
            .addDataCollector(dataCollector)
            .setTimeInterval(startTimeInMillis, endTimeInMillis, TimeUnit.MILLISECONDS)
            .build()

        dataController.delete(deleteOptions).addOnSuccessListener {
            Toast.makeText(this, "Weight has been deleted successfully", Toast.LENGTH_SHORT)
                .show()
        }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message.toString(), Toast.LENGTH_SHORT).show()
            }

    }


    private fun clearHealthData() {

        dataController.clearAll()
            .addOnSuccessListener {
                Toast.makeText(this, "All Health Kit data has been deleted.", Toast.LENGTH_SHORT)
                    .show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message.toString(), Toast.LENGTH_SHORT).show()
            }

    }

}