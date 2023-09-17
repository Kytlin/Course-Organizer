package com.example.a2enhanced.model

import com.example.a2enhanced.view.IView
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlin.math.sqrt

class Model {
    private val views = mutableListOf<IView>()
    class Course(val code : String, val term : String, val grade : Int, val wd : Boolean)

    // Variables from Course Admin Section
    var cur_code=""
    var cur_term=""
    var cur_grade=0
    var cur_wd=false

    // Stats
    var avg = 0.0
    var std = 0.0
    var termAvgs = mutableListOf<Double>()
    var stdAvgs = mutableListOf<Double>()

    // Note: The distinction of the two lists below are essential to handle logic and UI independently
    //  > allCourses - all courses stored when created i.e. list as a database
    //  > viewCourses - courses that are kept solely for the UI

    var allCourses = mutableListOf<Course>(
        Course("MATH 135", "F20", 89, false),
        Course("CS 371", "W22", 59, false),
        Course("STAT 231", "S21", 77, false),
        Course("CS 136", "W21", 97, false),
        Course("PSYCH 101", "W23", 100, false),
        Course("ENGL 710", "F22", 36, false),
        Course("MATH 247", "S21", -1, true),
        Course("MATH 245", "S21", 83, false),
    )
    var viewCourses = mutableListOf<Course>(
        Course("MATH 135", "F20", 89, false),
        Course("CS 371", "W22", 59, false),
        Course("STAT 231", "S21", 77, false),
        Course("CS 136", "W21", 97, false),
        Course("PSYCH 101", "W23", 100, false),
        Course("ENGL 710", "F22", 43, false),
        Course("MATH 247", "S21", -1, true),
        Course("MATH 245", "S21", 83, false),
    )

    val terms = listOf<String>("F20", "W21", "S21", "W22", "F22", "W23", "F23", "S23")
    var curTab = 0

    // Variables for "Progress towards Degree"
    var csCount = 0
    var mathCount = 0
    var electiveCount = 0
    var courseCount = 0

    // Variables for "Course Outcomes"
    var courseSelect = listOf<Course>()
    var includeWD = true

    var wdPie = false
    var curPie = ""

    // Variables for "Incremental Average"
    var coursesPerTerm = mutableListOf<List<Course>>()
    var statsPerTerm = mutableListOf<List<Double>>()

    fun addView(view : IView) {
        views.add(view)
        view.updateView()
    }

    private fun notifyObservers() {
        for (view in views) {
            println("Model: notify $view")
            view.updateView()
        }
    }

    // Setters
    fun setCode(code : String) {
        cur_code=code
    }
    fun setTerm(term: String) {
        cur_term=term
    }
    fun setGrade(grade : String) : Int {
        if (grade == "WD") {
            cur_grade = -1
            cur_wd = true
        } else if (grade.toInt() in 0..100) {
            cur_grade = grade.toInt()
            cur_wd = false
        } else {
            println("Grade ERROR - Invalid Input")
        }
        return cur_grade
    }

    // Course operations
    fun createCourse(curCode: String, curTerm: String, curGrade: String) {
        var curWD = false
        var numGrade = setGrade(curGrade)
        if (numGrade==-1) {
            curWD = true
        }
        var curCourse = Course(curCode, curTerm, numGrade, curWD)
        allCourses.add(curCourse)
        viewCourses.add(curCourse)
        notifyObservers()
    }
    fun deleteCourse(index : Int) {
        allCourses.removeAt(index)
        viewCourses.removeAt(index)
        printCourses()
        notifyObservers()
    }
    fun updateCourse(curCode : String, curTerm : String, curGrade : String, curWD : Boolean, index : Int) {
        var numGrade = setGrade(curGrade)
        viewCourses[index] = Course(curCode, curTerm, numGrade, curWD)
        allCourses[index] = Course(curCode, curTerm, numGrade, curWD)
        notifyObservers()
    }

    fun updTab(state : Int) {
        println("Updating tab: ${state}")
        curTab = state
    }

    fun copyCourses() {
        viewCourses.clear()
        allCourses.forEach {
            viewCourses.add(it)
        }
    }
    fun printCourses() {
        allCourses.forEach{ println("All Courses - " + it.code) }
        println()
    }

    // Handles an update on View
    fun updateCourseData(courses: List<Course>) {
        courseStats(courses)
        computeAvgTerm()
        subjectCount(courses)
    }

    fun clearCourseData() {
        csCount=0
        mathCount=0
        electiveCount=0
        courseCount=0
    }

    // Toolbar and Course Row logic functions
    fun courseStats(courses : List<Course>) : List<Double> {
        if (courses.isEmpty()) {
            avg = 0.0
            std = 0.0
        } else {
            avg = courses.filter { !it.wd }.fold(0.0) { acc, it -> acc + it.grade } / courses.filter { !it.wd }.size
            std = sqrt(courses.filter { !it.wd }.fold(0.0) { acc, it -> acc + (it.grade - avg) * (it.grade - avg) } / courses.filter { !it.wd }.size)
        }
        return listOf(avg, std)
    }

    // Visualization functions
    fun computeAvgTerm() {
        termAvgs.clear()
        stdAvgs.clear()
        coursesPerTerm.clear()
        (0..terms.size-1).forEach {
            var c = terms[it]
            println(c)
            coursesPerTerm.add(viewCourses.filter { it.term == c })
            println(coursesPerTerm.size)
            var stats = courseStats(coursesPerTerm[it])
            termAvgs.add(stats[0])
            stdAvgs.add(stats[1])
        }
    }

    fun addMissingPie(state : Boolean) {
        wdPie = state
        notifyObservers()
    }

    fun updcurPie(status : String) {
        curPie = status
        notifyObservers()
    }

    fun courseRatio(status : String, wdPie : Boolean) : Double {
        when (status) {
            "WD'd" -> courseSelect = viewCourses.filter { it.grade==-1 && it.wd }
            "failed" -> courseSelect = viewCourses.filter { it.grade in 0 until 50 }
            "low" -> courseSelect = viewCourses.filter { it.grade in 50 until 60 }
            "good" -> courseSelect = viewCourses.filter { it.grade in 60 .. 90 }
            "great" -> courseSelect = viewCourses.filter { it.grade in 91 until 96 }
            "excellent" -> courseSelect = viewCourses.filter { it.grade in 96 .. 100 }
        }
        if (wdPie && status != "missing") {
            return courseSelect.size / 40.0
        }
        if (wdPie && status == "missing") {
            return (40 - viewCourses.size) / 40.0
        }
        return courseSelect.size / viewCourses.size.toDouble()
    }

    fun setPieData(status : String, wdPie : Boolean) {
        updcurPie(status)
        courseRatio(status, wdPie)
        notifyObservers()
    }

    // Incremental Averages functions

    fun courseRangeTerm(start : Int, end : Int) : List<Course> {
        var incCourses = mutableListOf<Course>()
        (start..end).forEach {
            var c = terms[it]
            viewCourses.filter { it.term == c && !it.wd }.map {i ->
                incCourses.add(i)
            }
        }
        return incCourses
    }

    fun subjectCount(courses: List<Course>) {
        courses.forEach() {
            if (it.code.split(" ")[0]=="CS") {
                csCount+=1
            }
            else if (it.code.split(" ")[0]=="MATH" ||
                it.code.split(" ")[0]=="STAT" ||
                it.code.split(" ")[0]=="CO") {
                mathCount+=1
            }
            else {
                electiveCount+=1
            }
            courseCount+=1
        }
    }

    fun courseColour(grade: Int, wd: Boolean): Paint? {
        if (wd) {
            return Color.DARKSLATEGRAY
        } else if (grade in 0 until 50) {
            return Color.LIGHTCORAL
        } else if (grade in 50 until 60) {
            return Color.LIGHTBLUE
        } else if (grade in 60 until 91) {
            return Color.LIGHTGREEN
        } else if (grade in 91 until 96) {
            return Color.SILVER
        } else if (grade in 96..100) {
            return Color.GOLD
        }
        return Color.WHITE
    }
}
