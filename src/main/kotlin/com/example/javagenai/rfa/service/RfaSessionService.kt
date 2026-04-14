package com.example.javagenai.rfa.service

import com.example.javagenai.rfa.model.ImplementationPlan
import com.example.javagenai.rfa.model.RfaSpec
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class RfaSessionService(private val project: Project) {
    var currentRfa: RfaSpec? = null
    var currentPlan: ImplementationPlan? = null
}
