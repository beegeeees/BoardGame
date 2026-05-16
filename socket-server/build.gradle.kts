plugins {
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.java.websocket)
}

application {
    mainClass.set("com.example.boardgame.server.BoardGameSocketServer")
}
