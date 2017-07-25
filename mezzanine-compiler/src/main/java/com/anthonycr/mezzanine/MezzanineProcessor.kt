package com.anthonycr.mezzanine

import com.anthonycr.mezzanine.filter.SupportedElementFilter
import com.anthonycr.mezzanine.generator.MezzanineGenerator
import com.anthonycr.mezzanine.map.MezzanineMapper
import com.anthonycr.mezzanine.source.MezzanineElementSource
import com.anthonycr.mezzanine.utils.MessagerUtils
import com.google.auto.service.AutoService
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
class MezzanineProcessor : AbstractProcessor() {

    private var isProcessed: Boolean = false

    override fun getSupportedAnnotationTypes() = mutableSetOf("com.anthonycr.mezzanine.FileStream")

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun process(set: Set<TypeElement>, roundEnvironment: RoundEnvironment): Boolean {
        if (isProcessed) {
            return true
        }

        isProcessed = true

        MessagerUtils.messager = processingEnv.messager

        val mezzanineElementSource = MezzanineElementSource(roundEnvironment)

        MessagerUtils.reportInfo("Starting Mezzanine processing")

        mezzanineElementSource.createElementStream()
                .filter(SupportedElementFilter)
                .map(MezzanineMapper.elementToTypeAndFilePair())
                .doOnNext { typeElementFileEntry -> MessagerUtils.reportInfo("Processing file: " + typeElementFileEntry.second) }
                .map(MezzanineMapper.fileToStringContents())
                .map(MezzanineGenerator.generateGeneratorTypeSpecs())
                .toList()
                .map(MezzanineGenerator.generateMezzanineTypeSpec())
                .map(MezzanineGenerator.generateJavaFile())
                .flatMapCompletable(MezzanineGenerator.writeFileToDisk(processingEnv.filer))
                .subscribe { MessagerUtils.reportInfo("File successfully processed") }

        return true
    }
}