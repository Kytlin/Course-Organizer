package com.example.a2enhanced

import com.example.a2enhanced.model.Model
import com.example.a2enhanced.view.View
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.VBox
import javafx.stage.Stage

class HelloApplication : Application() {
    override fun start(stage: Stage) {
        var model = Model()
        val view = View(model)

        stage.apply {
            title = "Course Organizer"
            scene = Scene(VBox(view), 900.0, 450.0)
        }.show()
    }
}

fun main() {
    Application.launch(HelloApplication::class.java)
}