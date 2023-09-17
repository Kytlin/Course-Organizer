package com.example.a2enhanced.view

import com.example.a2enhanced.model.Model
import com.example.a2enhanced.view.IView
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.canvas.Canvas
import javafx.scene.control.*
import javafx.scene.input.MouseEvent
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Arc
import javafx.scene.shape.ArcType
import javafx.scene.shape.Rectangle
import javafx.scene.transform.Transform

internal class View(
    private val model: Model
) : VBox(), IView {

    override fun updateView() {
        model.clearCourseData()
        model.updateCourseData(model.allCourses)

        // Reconstruct Layout
        if (children.size > 1) {
            children.remove(0, children.size)
        }

        val root = VBox().apply {
            (0..9).forEach() { children.add(Button("Button # $it").apply {
                prefWidth = 150.0 + it * 10 % 40
                prefHeight = 40.0
                maxHeight = Double.MAX_VALUE
                VBox.setVgrow(this, Priority.ALWAYS)
            })}
        }

        val courseDisplay = VBox()
        val scroll = ScrollPane(courseDisplay).apply {
            hbarPolicy = ScrollPane.ScrollBarPolicy.ALWAYS
            isFitToWidth = true
        }

        val courseView = VBox(scroll).apply { prefWidth = 400.0 }
        model.viewCourses.forEachIndexed { index, it ->
            courseDisplay.children.add(addCourseRow(it, index))
        }

        // Create y-label object in Course Outcome tab, as it changes after each hover on pie chart
        val yLabel3 = VBox().apply {
            prefWidth = 75.0
            padding = Insets(10.0,0.0,0.0,0.0)
        }

        model.courseSelect.forEach {
            var courseLabelByGrade = Label("${it.code}")
            setMargin(courseLabelByGrade, Insets(0.0, 0.0, 0.0, 10.0))
            yLabel3.children.add(courseLabelByGrade)
        }
        setMargin(yLabel3, Insets(15.0, 10.0, 10.0, 50.0))

        val adminSection = VBox(courseAdder(), courseView)

        val courseViz = visualization(yLabel3, model.wdPie)
        val coursePane = HBox(adminSection, courseViz)

        children.add(coursePane)
        children.add(statusBar())
    }

    init {
        this.alignment = Pos.CENTER
        this.minHeight = 100.0

        model.addView(this)
    }

    fun courseAdder() : VBox {
        var curCode = ""
        var curTerm = ""
        var curGrade = ""

        val toolbar = ToolBar().apply {
            background = Background(BackgroundFill(Color.LIGHTGRAY, CornerRadii(5.0), null))
            padding = Insets(10.0)
            isFillWidth = true
        }

        val codeInput = TextField().apply {
            prefWidth = 80.0
            textProperty().addListener() {_, _, newValue ->
                curCode = newValue
                model.setCode(curCode)
            }
        }
        setMargin(codeInput, Insets(10.0))

        val termMenu = ChoiceBox<String>().apply {
            items.addAll("F20", "W21", "S21", "F21", "W22", "F22", "S22", "W23", "F23", "S23")
            selectionModel.selectedItemProperty().addListener{ _, _, newValue ->
                curTerm = newValue
                model.setTerm(curTerm)
            }
        }
        setMargin(termMenu, Insets(10.0))

        val gradeInput = TextField().apply {
            prefWidth = 50.0
            textProperty().addListener() {_, _, newValue ->
                curGrade=newValue
                model.setGrade(curGrade)
            }
        }
        setMargin(gradeInput, Insets(10.0))

        val createButton = Button("Create")
        createButton.setOnMouseClicked {
            println("Processing ${model.cur_code} Course in View")
            model.createCourse(curCode,curTerm,curGrade)
        }

        toolbar.items.addAll(codeInput, termMenu, gradeInput, createButton)
        val adderBox = VBox(toolbar)
        setMargin(adderBox, Insets(5.0))
        return adderBox
    }

    fun addCourseRow(course : Model.Course, index : Int) : VBox {
        var curCode = course.code
        var curTerm = course.term
        var curGrade = course.grade.toString()
        var curWD = course.wd

        if (course.grade == -1) {
            curGrade = "WD"
        }

        val toolbar = ToolBar().apply {
            background = Background(BackgroundFill(
                model.courseColour(course.grade, course.wd),
                CornerRadii(5.0),
                null)
            )
            padding = Insets(10.0)
            isFillWidth = true
        }

        val codeInput = TextField(course.code).apply {
            prefWidth = 80.0
        }
        codeInput.setEditable(false)
        setMargin(codeInput, Insets(10.0))

        val updButton = Button("Update")
        updButton.setOnMouseClicked {
            println("Updating ${course.code} Course from View")
            model.computeAvgTerm()
            model.updateCourse(curCode, curTerm, curGrade, curWD, index)
        }
        updButton.setDisable(true)
        setMargin(updButton, Insets(10.0))

        val termMenu = ChoiceBox<String>().apply {
            items.addAll("F20", "W21", "S21", "W22", "F22", "W23", "F23", "S23") //  mutable necessary here
            value=course.term
            selectionModel.selectedItemProperty().addListener { _, _, newValue ->
                curTerm=newValue
                updButton.setDisable(false)
            }
        }
        setMargin(termMenu, Insets(10.0))

        val grade = TextField(curGrade).apply {
            prefWidth = 50.0
            textProperty().addListener { _, _, newValue ->
                curGrade = newValue
                updButton.setDisable(false)
            }
        }
        setMargin(grade, Insets(10.0))

        val delButton = Button("Delete")
        delButton.setOnMouseClicked {
            model.deleteCourse(index)
        }
        setMargin(delButton, Insets(10.0))

        toolbar.items.addAll(codeInput, termMenu, grade, updButton, delButton)
        val rowBox = VBox(toolbar)
        setMargin(rowBox, Insets(5.0, 2.5, 5.0, 2.5))

        return rowBox
    }

    fun visualization(yLabel3 : VBox, wdPie : Boolean) : TabPane {
        println(model.curTab)
        var courseViz = TabPane().apply {
            selectionModel.select(model.curTab)
            tabs.addAll(
                Tab("Average by Term", VBox(HBox(yLabels1(), Group(courseAvgGraph())), xLabels1())),
                Tab("Progress towards Degree", VBox(HBox(yLabels2(), Group(progressDeg())), xLabels2())),
                Tab("Course Outcome", VBox(HBox(yLabel3,
                    Group(courseOutcomes(wdPie)), Group(canvasLegend()), legendLabel()), xLabels3())),
                Tab("Incremental Average", VBox(HBox(yLabels1(), Group(incrementalAvgGraph())), xLabels1()))
            )
        }
        return courseViz
    }

    fun yLabels1() : VBox {
        var gradeLabel = VBox().apply {
            (0..10).forEach {
                children.add(Label("    ${100 - (it * 10)}").apply {
                    prefWidth=32.5
                    prefHeight=30.0
                })
            }
        }
        setMargin(gradeLabel, Insets(5.0))
        return gradeLabel
    }

    fun yLabels2() : VBox {
        var gradeLabel = VBox().apply {
            listOf("", "CS", "MATH", "Other", "Total").forEach {
                children.add(Label("    ${it}").apply {
                    prefWidth=60.0
                    prefHeight=60.0
                })
            }
        }
        setMargin(gradeLabel, Insets(5.0))
        return gradeLabel
    }

    fun xLabels1() : HBox {
        var yLabel3 = HBox().apply {
            model.terms.forEach {
                children.add(Label(it).apply {
                    prefWidth=65.0
                })
            }
        }
        setMargin(yLabel3, Insets(5.0, 0.0, 0.0, 50.0))
        return yLabel3
    }

    fun xLabels2() : HBox {
        var yLabel3 = HBox().apply {
//            println("Printing y-axis labels")
            (0..6).forEach {
                children.add(Label("${it*5.0}").apply {
                    prefWidth=75.0
                })
            }
        }
        setMargin(yLabel3, Insets(5.0, 0.0, 0.0, 50.0))
        return yLabel3
    }

    fun xLabels3() : HBox {
        var checkPie = CheckBox("Include missing courses").apply {
            padding = Insets(10.0)
            isSelected = model.wdPie
            selectedProperty().addListener { _, _, newValue ->
                model.addMissingPie(newValue)
            }
        }
        return HBox(checkPie).apply {
            prefWidth=450.0
            alignment=Pos.CENTER
        }
    }

    fun courseAvgGraph() : Canvas {
        println("COURSE AVG GRAPH")

        var canvas = Canvas(475.0, 350.0)
        canvas.graphicsContext2D.apply {
            // data points
            fill = Color.LIGHTGREEN
            var offset = 0

            // We have to scale down the average score data points w.r.t the number of lines on graph
            model.termAvgs.forEach() {
                fillOval( (offset * 60.0) + 25.0, (310-((it/10) * 30.0)) - 5.0, 10.0, 10.0)
                offset+=1
            }

            // Enhancement #1
            var gap = 1
            offset = 1
            stroke = Color.BLUE
            (1..model.terms.size-1).forEach {
                if (model.termAvgs[it]==0.0) {
                    gap += 1
                }
                else {
                    strokeLine(
                        ((offset - gap) * 60.0) + 30.0,
                        (310 - ((model.termAvgs[it - gap] / 10.0) * 30.0)),
                        (offset * 60.0) + 30.0,
                        (310 - ((model.termAvgs[it] / 10.0) * 30.0))
                    )
                    gap = 1
                }
                offset+=1
            }

            // grid strokes
            stroke = Color.LIGHTGRAY
            lineWidth = 0.5
            (0..9).forEach() {
                strokeLine(10.0, (it * 30.0) + 10, 700.0, (it * 30.0) + 10)
            }
            stroke = Color.BLACK
            lineWidth = 1.0
            strokeLine(10.0, 310.0, 700.0, 310.0) // x-axis
            strokeLine(10.0, 0.0, 10.0, 310.0) // y-axis
        }
        canvas.setOnMouseEntered { evt -> if (model.curTab == 0) { model.updTab(0) } }
        return canvas
    }

    fun progressDeg() : Canvas {
        var canvas = Canvas(475.0, 350.0)
        println("PROGRESS BAR")

        canvas.graphicsContext2D.apply {
            // grid strokes
            stroke = Color.BLACK
            lineWidth = 1.0
            (0..10).forEach() {
                strokeLine((it * 75.0), 30.0, (it * 75.0), 370.0)
            }

            fill = Color.LIGHTYELLOW
            fillRect(0.0, 80.0, (11*15.0), 30.0)
            fill = Color.YELLOW
            fillRect(0.0, 80.0, ((model.csCount*15.0)/2), 30.0)

            fill = Color.PINK
            fillRect(0.0, 140.0, (4*15.0), 30.0)
            fill = Color.HOTPINK
            fillRect(0.0, 140.0, ((model.mathCount*15.0)/2), 30.0)

            fill = Color.LIGHTGRAY
            fillRect(0.0, 200.0, (5*15.0), 30.0)
            fill = Color.GRAY
            fillRect(0.0, 200.0, ((model.electiveCount*15.0)/2), 30.0)

            fill = Color.LIGHTGREEN
            fillRect(0.0, 260.0, (20*15.0), 30.0)
            fill = Color.DARKGREEN
            fillRect(0.0, 260.0, ((model.courseCount*15.0)/2), 30.0)
        }
        canvas.setOnMouseEntered { evt -> if (model.curTab!=1) { model.updTab(1) } }
        return canvas
    }

    fun courseOutcomes(wdPie: Boolean) : List<Arc> {
        println("COURSE OUTCOMES")

        val myArcs = mutableListOf<Arc>()

        val status = listOf("missing", "WD'd", "failed", "low", "good", "great", "excellent")
        val fillColors = listOf(Color.WHITE, Color.DARKSLATEGRAY, Color.LIGHTCORAL, Color.LIGHTBLUE,
            Color.LIGHTGREEN, Color.SILVER, Color.GOLD)
        var offset = 0.0

        var startidx = 0
        var endidx = 6
        if (!model.wdPie) {
            startidx = 1
        }

        (startidx..endidx).forEach {
            myArcs.add(Arc(
                100.0, 20.0, 175.0, 175.0,
                offset * 360.0, model.courseRatio(status[it], wdPie) * 360.0
            ).apply {
                fill = fillColors[it]
                stroke = Color.BLACK
                strokeWidth = 1.0
                type = ArcType.ROUND
                onMouseEntered = EventHandler { _ ->
                    if (model.curTab!=2) {
                        model.updTab(2)
                    }
                    if (model.curPie != status[it]) {
                        model.setPieData(status[it], wdPie)
                    }
                }
                offset += model.courseRatio(status[it], wdPie)
            })
        }
        return myArcs.toList()
    }

    // Enhancement #2
    fun canvasLegend() : Canvas {
        val fillColors = listOf(Color.WHITE, Color.DARKSLATEGRAY, Color.LIGHTCORAL, Color.LIGHTBLUE,
            Color.LIGHTGREEN, Color.SILVER, Color.GOLD)

        var canvas = Canvas(50.0, 250.0)
        canvas.graphicsContext2D.apply {
            var offset = 0
            stroke = Color.BLACK
            (0..fillColors.size-1).forEach {
                fill = fillColors[it]
                strokeRect(10.0, (offset*30.0)+20.0, 25.0, 25.0)
                fillRect(10.0, (offset*30.0)+20.0, 25.0, 25.0)
                offset += 1
            }
        }
        return canvas
    }

    fun legendLabel() : VBox {
        val status = listOf("missing", "WD'd", "failed", "low", "good", "great", "excellent")
        var labels = VBox().apply {
            (0..status.size-1).forEach {
                var label = Label(status[it])
                setMargin(label, Insets(6.0))
                children.add(label)
            }
            padding = Insets(17.5,0.0,0.0,0.0)
        }
        return labels
    }

    fun incrementalAvgGraph () : Canvas {
        println("INCREMENTAL AVG GRAPH")
        println(model.curTab)
        println(model.std)

        var canvas = Canvas(475.0, 350.0)
        canvas.graphicsContext2D.apply {
            fill = Color.LIGHTGREEN
            var offset = 0

            (0..model.terms.size-1).forEach {
                if (model.coursesPerTerm[it].isNotEmpty()) {
                    var incCourses = model.courseRangeTerm(0, it)
                    var incStats = model.courseStats(incCourses)
                    incCourses.map { i ->
                        stroke = model.courseColour(i.grade, false)
                        lineWidth = 1.0
                        strokeOval((offset * 60.0) + 25.0, (310 - ((i.grade / 10.0) * 30.0)) - 5.0, 10.0, 10.0)
                    }

                    // Error bar
                    stroke = Color.BLACK
                    lineWidth = 1.5
                    strokeLine(
                        (offset * 60.0) + 30.0,
                        (310 - (((incStats[0] + incStats[1]) / 10.0) * 30.0)),
                        (offset * 60.0) + 30.0,
                        (310 - (((incStats[0] - incStats[1]) / 10.0) * 30.0))
                    )
                    strokeLine(
                        (offset * 60.0) + 25.0,
                        (310 - (((incStats[0] + incStats[1]) / 10.0) * 30.0)),
                        (offset * 60.0) + 35.0,
                        (310 - (((incStats[0] + incStats[1]) / 10.0) * 30.0))
                    )
                    strokeLine(
                        (offset * 60.0) + 25.0,
                        (310 - (((incStats[0] - incStats[1]) / 10.0) * 30.0)),
                        (offset * 60.0) + 35.0,
                        (310 - (((incStats[0] - incStats[1]) / 10.0) * 30.0))
                    )

                    // Average point
                    fillOval((offset * 60.0) + 25.0, (310 - ((incStats[0] / 10.0) * 30.0)) - 5.0, 10.0, 10.0)
                    strokeOval((offset * 60.0) + 25.0, (310 - ((incStats[0] / 10.0) * 30.0)) - 5.0, 10.0, 10.0)
                }
                offset += 1
            }

            // grid strokes
            stroke = Color.LIGHTGRAY
            lineWidth = 0.5
            (0..9).forEach() {
                strokeLine(10.0, (it * 30.0) + 10, 700.0, (it * 30.0) + 10)
            }
            stroke = Color.BLACK
            lineWidth = 1.0
            strokeLine(10.0, 310.0, 700.0, 310.0) // x-axis
            strokeLine(10.0, 0.0, 10.0, 310.0) // y-axis
        }
        canvas.setOnMouseEntered { evt -> if (model.curTab == 2) { model.updTab(3) } }
        return canvas
    }

    fun statusBar() : HBox {
        return HBox(
            Label("Course average: %.1f".format(model.courseStats(model.allCourses)[0])),
            Separator(Orientation.VERTICAL),
            Label("Course taken: ${model.allCourses.size}"),
            Separator(Orientation.VERTICAL),
            Label("Course failed: ${model.allCourses.filter{ it.grade in 0 until 50 }.size}"),
            Separator(Orientation.VERTICAL),
            Label("Course WD'ed: ${model.allCourses.filter{ it.wd }.size}")
        )
    }
}
