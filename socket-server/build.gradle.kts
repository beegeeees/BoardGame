plugins {
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.firebase.admin)
    implementation(libs.java.websocket)

    testImplementation(libs.junit)
}

application {
    mainClass.set("com.example.boardgame.server.BoardGameSocketServer")
}
