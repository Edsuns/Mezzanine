package com.anthonycr.mezzanine.function

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import org.apache.commons.lang3.StringEscapeUtils
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

/**
 * A mapping function that generates the [TypeSpec] for the interface represented by the
 * [TypeElement] which returns the [String].
 */
object GenerateFileStreamTypeSpecFunction : (Pair<TypeElement, String>) -> TypeSpec {

    /**
     * Constants have a limit of length, max to 65534.
     */
    private const val MAX_LENGTH = 65534

    /**
     * String constants cannot exceed 65535 bytes in size.
     */
    private const val MAX_BYTES_SIZE = 65535

    override fun invoke(fileStreamPair: Pair<TypeElement, String>): TypeSpec {
        val fileContent = fileStreamPair.second

        val singleMethod = fileStreamPair.first.enclosedElements[0] as ExecutableElement
        val methodName = singleMethod.simpleName.toString()

        return TypeSpec
                .classBuilder(fileStreamPair.first.simpleName.toString())
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC)
                .addSuperinterface(ClassName.get(fileStreamPair.first))
                .addMethods(createMethodSpecs(methodName, fileContent))
                .build()
    }

    private fun createMethodSpecs(name: String, src: String): List<MethodSpec> {
        val methodList = ArrayList<MethodSpec>()
        if (src.length > MAX_LENGTH) {
            var count = 0
            var start = 0
            while (start < src.length) {
                // make sure the String length does not exceed 65534
                var end = if (start + MAX_LENGTH >= src.length) {
                    src.length
                } else {
                    src.offsetByCodePoints((start + MAX_LENGTH + 1).coerceAtMost(src.length - 1), -1)
                }
                var content = src.substring(start, end)
                // In general, a String with length 65534 has 65535 bytes.
                // But there are still exceptions, make sure the String size in byte does not exceed 65535.
                val size = content.toByteArray(Charsets.UTF_8).size
                if (size > MAX_BYTES_SIZE) {
                    val safeEnd = end.coerceAtMost(src.length - 1)
                    end = src.offsetByCodePoints(safeEnd, -(size - MAX_BYTES_SIZE))
                    content = src.substring(start, end)
                }

                val methodSpec = MethodSpec
                        .methodBuilder("__$name$count")
                        .addModifiers(Modifier.PRIVATE)
                        .returns(String::class.java)
                        .addCode("return \"$1L\";\n", StringEscapeUtils.escapeJava(content))
                        .build()
                methodList.add(methodSpec)
                start = end
                count++
            }

            val code = StringBuilder()
            for (i in 0 until count) {
                if (i > 0) {
                    code.append(" + ")
                }
                code.append("__$name$i").append("()")
            }
            methodList.add(MethodSpec
                    .methodBuilder(name)
                    .addAnnotation(Override::class.java)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(String::class.java)
                    .addCode("return $1L;\n", code)
                    .build()
            )
        } else {
            methodList.add(MethodSpec
                    .methodBuilder(name)
                    .addAnnotation(Override::class.java)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(String::class.java)
                    .addCode("return \"$1L\";\n", StringEscapeUtils.escapeJava(src))
                    .build()
            )
        }

        return methodList
    }

}
