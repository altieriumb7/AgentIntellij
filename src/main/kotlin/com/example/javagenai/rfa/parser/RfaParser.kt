package com.example.javagenai.rfa.parser

import com.example.javagenai.rfa.model.RfaSpec

interface RfaParser {
    fun supports(extension: String): Boolean
    fun parse(content: String, sourceFilePath: String): RfaSpec
}
