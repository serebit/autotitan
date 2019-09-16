import com.serebit.strife.buildsrc.ProjectInfo

rootProject.name = ProjectInfo.name

pluginManagement.resolutionStrategy.eachPlugin {
    when(requested.id.id) {
        "kotlinx-serialization" -> useModule("org.jetbrains.kotlin:kotlin-serialization:${requested.version}")
        "kotlinx-atomicfu" -> useModule("org.jetbrains.kotlinx:atomicfu-gradle-plugin:${requested.version}")
    }
}