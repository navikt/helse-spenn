package no.nav.helse.spenn.blackbox

import java.util.Collections

internal fun setEnv(name: String, value: String) {
    setEnv(mapOf(name to value))
}

internal fun setEnv(newenv: Map<String, String>) {
    try {
        val processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment")
        val theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment")
        theEnvironmentField.isAccessible = true
        val env = theEnvironmentField.get(null) as MutableMap<String, String>
        env.putAll(newenv)
        val theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment")
        theCaseInsensitiveEnvironmentField.isAccessible = true
        val cienv = theCaseInsensitiveEnvironmentField.get(null) as MutableMap<String, String>
        cienv.putAll(newenv)
    } catch (e: NoSuchFieldException) {
        val classes = Collections::class.java.declaredClasses
        val env = System.getenv()
        for (cl in classes) {
            if ("java.util.Collections\$UnmodifiableMap" == cl.name) {
                val field = cl.getDeclaredField("m")
                field.isAccessible = true
                val obj = field.get(env)
                val map = obj as MutableMap<String, String>
                map.clear()
                map.putAll(newenv)
            }
        }
    }
}

private fun MutableMap<String, String>.putAll(newvals:Map<String, String>){
    newvals.entries.forEach {
        this[it.key] = it.value
    }
}